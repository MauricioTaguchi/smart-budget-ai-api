package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.mauriciotaguchi.smartbudget.infrastructure.ai.AssistantTextResult;
import com.mauriciotaguchi.smartbudget.infrastructure.ai.AssistantVoiceResult;
import com.mauriciotaguchi.smartbudget.infrastructure.ai.FinancialAssistant;
import com.mauriciotaguchi.smartbudget.infrastructure.web.dto.TextCommandRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.mauriciotaguchi.smartbudget.infrastructure.security.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final FinancialAssistant assistant;

    public AssistantController(FinancialAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/text")
    public AssistantTextResult text(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody TextCommandRequest request
    ) {
        return assistant.processText(principal.id(), request.message());
    }

    @PostMapping(
            value = "/voice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "audio/mpeg"
    )
    public ResponseEntity<byte[]> voice(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestPart("audio") MultipartFile audio
    ) throws IOException {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("O arquivo de áudio está vazio.");
        }

        String filename = audio.getOriginalFilename() == null
                ? "audio.mp3"
                : audio.getOriginalFilename();

        Resource resource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        AssistantVoiceResult result = assistant.processVoice(principal.id(), resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("resposta.mp3", StandardCharsets.UTF_8)
                .build());
        headers.add("X-Assistant-Mode", result.mode());

        return ResponseEntity.ok()
                .headers(headers)
                .body(result.audio());
    }
}
