package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.jayway.jsonpath.JsonPath;
import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.application.usecase.CreateTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.DeleteTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.GetFinancialSummaryUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.ListTransactionsUseCase;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FinancialToolsOwnershipTest {

    private final CreateTransactionUseCase create = mock(CreateTransactionUseCase.class);
    private final ListTransactionsUseCase list = mock(ListTransactionsUseCase.class);
    private final GetFinancialSummaryUseCase summary = mock(GetFinancialSummaryUseCase.class);
    private final DeleteTransactionUseCase delete = mock(DeleteTransactionUseCase.class);
    private final FinancialToolsFactory factory = new FinancialToolsFactory(create, list, summary, delete);

    @Test
    void callbacksKeepTheirCapturedOwnerOnAnotherThreadWithEmptyOrForeignSecurityContext() throws Exception {
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        TransactionView aliceTransaction = new TransactionView(transactionId, TransactionType.EXPENSE,
                BigDecimal.TEN, "Only Alice", "TEST", LocalDateTime.now());
        when(list.execute(alice)).thenReturn(List.of(aliceTransaction));
        when(list.execute(bob)).thenReturn(List.of());
        when(summary.execute(alice)).thenReturn(new FinancialSummary(BigDecimal.ZERO, BigDecimal.TEN,
                BigDecimal.TEN.negate(), 1));
        when(create.execute(eq(alice), any())).thenReturn(aliceTransaction);

        Map<String, ToolCallback> aliceCallbacks = callbacks(factory.forOwner(alice));
        Map<String, ToolCallback> bobCallbacks = callbacks(factory.forOwner(bob));
        long requestThreadId = Thread.currentThread().getId();

        for (boolean installForeignContext : List.of(false, true)) {
            CompletableFuture.runAsync(() -> {
                assertThat(Thread.currentThread().getId()).isNotEqualTo(requestThreadId);
                SecurityContextHolder.clearContext();
                try {
                    if (installForeignContext) {
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(bob, null, List.of()));
                    } else {
                        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
                    }
                    assertThat(aliceCallbacks.get("listFinancialTransactions").call("{}"))
                            .contains("Only Alice");
                    assertThat(aliceCallbacks.get("getFinancialSummary").call("{}"))
                            .contains("totalExpense");
                    aliceCallbacks.get("registerTransaction").call("""
                            {"type":"EXPENSE","amount":10,"description":"Only Alice",\
                            "category":"TEST","confirmed":true}
                            """);
                    aliceCallbacks.get("deleteFinancialTransaction").call(
                            "{\"transactionId\":\"" + transactionId + "\",\"confirmed\":true}");
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }).get(15, TimeUnit.SECONDS);
        }

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.clearContext();
            try {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(alice, null, List.of()));
                assertThat(bobCallbacks.get("listFinancialTransactions").call("{}")).isEqualTo("[]");
            } finally {
                SecurityContextHolder.clearContext();
            }
        }).get(15, TimeUnit.SECONDS);

        verify(list, times(2)).execute(alice);
        verify(list).execute(bob);
        verify(summary, times(2)).execute(alice);
        verify(create, times(2)).execute(eq(alice), argThat(command ->
                command.type() == TransactionType.EXPENSE
                        && command.amount().compareTo(BigDecimal.TEN) == 0
                        && command.description().equals("Only Alice")
                        && command.category().equals("TEST")));
        verify(delete, times(2)).execute(alice, transactionId);
        verifyNoMoreInteractions(create, list, summary, delete);
    }

    @Test
    void modelVisibleSchemasExposeBusinessParametersButNoPrincipalOrOwnerSelector() {
        Map<String, ToolCallback> tools = callbacks(factory.forOwner(UUID.randomUUID()));
        Map<String, Set<String>> expectedParameters = Map.of(
                "registerTransaction", Set.of("type", "amount", "description", "category", "confirmed"),
                "getFinancialSummary", Set.of(),
                "listFinancialTransactions", Set.of(),
                "deleteFinancialTransaction", Set.of("transactionId", "confirmed"));
        assertThat(tools.keySet()).isEqualTo(expectedParameters.keySet());

        tools.forEach((name, callback) -> {
            Map<String, Object> properties = JsonPath.read(callback.getToolDefinition().inputSchema(), "$.properties");
            assertThat(properties.keySet()).as("Model parameters for %s", name)
                    .isEqualTo(expectedParameters.get(name));
        });
        verifyNoInteractions(create, list, summary, delete);
    }

    @Test
    void factoryRejectsMissingTrustedIdentityBeforeAnyFinancialOperation() {
        assertThatThrownBy(() -> factory.forOwner(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(create, list, summary, delete);
    }

    private Map<String, ToolCallback> callbacks(FinancialTools tools) {
        return Arrays.stream(ToolCallbacks.from(tools)).collect(Collectors.toMap(
                callback -> callback.getToolDefinition().name(), Function.identity()));
    }
}
