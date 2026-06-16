package com.mauriciotaguchi.smartbudget.infrastructure.web;

import com.mauriciotaguchi.smartbudget.infrastructure.ai.AiAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audits")
public class AuditController {

    private final AiAuditService auditService;

    public AuditController(AiAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AiAuditService.AiAuditView> list() {
        return auditService.list();
    }
}
