package com.mauriciotaguchi.smartbudget.infrastructure.ai;

import com.mauriciotaguchi.smartbudget.application.dto.CreateTransactionCommand;
import com.mauriciotaguchi.smartbudget.application.dto.FinancialSummary;
import com.mauriciotaguchi.smartbudget.application.dto.TransactionView;
import com.mauriciotaguchi.smartbudget.application.usecase.CreateTransactionUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.GetFinancialSummaryUseCase;
import com.mauriciotaguchi.smartbudget.application.usecase.ListTransactionsUseCase;
import com.mauriciotaguchi.smartbudget.domain.model.TransactionType;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.Objects;

@Service
@Profile("!openai")
public class DemoFinancialAssistant implements FinancialAssistant {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d{1,3}(?:\\.\\d{3})*(?:,\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?!\\d)"
    );

    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
            "categoria\\s+([\\p{L}\\d_-]+)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final CreateTransactionUseCase createTransaction;
    private final ListTransactionsUseCase listTransactions;
    private final GetFinancialSummaryUseCase getSummary;
    private final AiAuditService auditService;
    private final AiCommandPolicy commandPolicy;

    public DemoFinancialAssistant(
            CreateTransactionUseCase createTransaction,
            ListTransactionsUseCase listTransactions,
            GetFinancialSummaryUseCase getSummary,
            AiAuditService auditService,
            AiCommandPolicy commandPolicy
    ) {
        this.createTransaction = createTransaction;
        this.listTransactions = listTransactions;
        this.getSummary = getSummary;
        this.auditService = auditService;
        this.commandPolicy = commandPolicy;
    }

    @Override
    public AssistantTextResult processText(UUID ownerId, String message) {
        Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        try {
            String answer = interpret(ownerId, message);
            auditService.success(ownerId, "TEXT", message, answer);
            return new AssistantTextResult(message, answer, "demo");
        } catch (RuntimeException exception) {
            auditService.failure(ownerId, "TEXT", message, exception);
            throw exception;
        }
    }

    @Override
    public AssistantVoiceResult processVoice(UUID ownerId, Resource audio) {
        Objects.requireNonNull(ownerId, "Proprietário obrigatório.");
        throw new AiProviderUnavailableException(
                "O processamento de áudio exige o perfil openai e a variável OPENAI_API_KEY."
        );
    }

    private String interpret(UUID ownerId, String originalMessage) {
        commandPolicy.validate(originalMessage);

        String message = normalize(originalMessage);

        if (containsAny(message, "saldo", "resumo", "quanto tenho", "quanto gastei")) {
            return formatSummary(getSummary.execute(ownerId));
        }

        if (containsAny(message, "listar", "liste", "extrato", "transacoes", "movimentacoes")) {
            return formatTransactions(listTransactions.execute(ownerId));
        }

        TransactionType type = detectType(message);

        if (type != null) {
            BigDecimal amount = extractAmount(originalMessage);
            String category = extractCategory(originalMessage);
            String description = extractDescription(originalMessage, type);

            if (!commandPolicy.isConfirmed(originalMessage)) {
                String label = type == TransactionType.INCOME ? "receita" : "despesa";
                return "Confirme antes de salvar: %s de R$ %s na categoria %s. "
                        .formatted(label, amount.toPlainString(), category)
                        + "Reenvie o comando incluindo a palavra CONFIRMAR.";
            }

            TransactionView saved = createTransaction.execute(ownerId,
                    new CreateTransactionCommand(type, amount, description, category)
            );

            String typeLabel = type == TransactionType.INCOME ? "Receita" : "Despesa";
            return "%s de R$ %s registrada com sucesso na categoria %s. ID: %s"
                    .formatted(
                            typeLabel,
                            saved.amount().toPlainString(),
                            saved.category(),
                            saved.id()
                    );
        }

        return """
                Não entendi o comando. Tente algo como:
                "Registre uma despesa de 89,90 com mercado na categoria alimentação",
                "Recebi uma receita de 3500 de salário",
                "Qual é o meu saldo?" ou "Liste minhas transações".
                """.trim();
    }

    private TransactionType detectType(String message) {
        if (containsAny(message, "despesa", "gasto", "gastei", "paguei", "comprei", "saida")) {
            return TransactionType.EXPENSE;
        }

        if (containsAny(message, "receita", "recebi", "ganhei", "salario", "entrada")) {
            return TransactionType.INCOME;
        }

        return null;
    }

    private BigDecimal extractAmount(String originalMessage) {
        Matcher matcher = AMOUNT_PATTERN.matcher(originalMessage);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Não encontrei um valor no comando.");
        }

        String raw = matcher.group(1);

        if (raw.contains(",") && raw.contains(".")) {
            raw = raw.replace(".", "").replace(",", ".");
        } else if (raw.contains(",")) {
            raw = raw.replace(",", ".");
        }

        return new BigDecimal(raw);
    }

    private String extractCategory(String originalMessage) {
        Matcher matcher = CATEGORY_PATTERN.matcher(originalMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String normalized = normalize(originalMessage);

        if (containsAny(normalized, "mercado", "supermercado", "restaurante", "comida", "cafe")) {
            return "ALIMENTACAO";
        }
        if (containsAny(normalized, "combustivel", "gasolina", "uber", "onibus", "metro")) {
            return "TRANSPORTE";
        }
        if (containsAny(normalized, "salario", "trabalho", "freelance")) {
            return "TRABALHO";
        }
        if (containsAny(normalized, "aluguel", "condominio", "energia", "agua", "internet")) {
            return "MORADIA";
        }

        return "OUTROS";
    }

    private String extractDescription(String originalMessage, TransactionType type) {
        String prefix = type == TransactionType.INCOME
                ? "Comando de receita: "
                : "Comando de despesa: ";

        String trimmed = originalMessage.trim();
        return prefix + (trimmed.length() > 150 ? trimmed.substring(0, 150) : trimmed);
    }

    private String formatSummary(FinancialSummary summary) {
        return """
                Resumo financeiro:
                receitas: R$ %s;
                despesas: R$ %s;
                saldo: R$ %s;
                transações cadastradas: %d.
                """.formatted(
                summary.totalIncome().toPlainString(),
                summary.totalExpense().toPlainString(),
                summary.balance().toPlainString(),
                summary.transactionCount()
        ).replace("\n", " ").trim();
    }

    private String formatTransactions(List<TransactionView> transactions) {
        if (transactions.isEmpty()) {
            return "Nenhuma transação foi cadastrada.";
        }

        StringBuilder result = new StringBuilder("Transações: ");

        for (int index = 0; index < transactions.size(); index++) {
            TransactionView transaction = transactions.get(index);
            result.append("%s de R$ %s em %s (%s)"
                    .formatted(
                            transaction.type(),
                            transaction.amount().toPlainString(),
                            transaction.category(),
                            transaction.description()
                    ));

            if (index < transactions.size() - 1) {
                result.append("; ");
            }
        }

        return result.toString();
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
