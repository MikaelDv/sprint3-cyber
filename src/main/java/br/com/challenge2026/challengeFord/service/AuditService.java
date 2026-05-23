package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.model.AuditLog;
import br.com.challenge2026.challengeFord.repository.AuditLogRepository;
import br.com.challenge2026.challengeFord.util.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger("audit");

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void registrar(String acao, String resultado, String recurso, String detalhe, HttpServletRequest request) {
        AuditLog entry = new AuditLog();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            entry.setActor(auth.getName());
            entry.setActorRoles(auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(",")));
        }
        if (request != null) {
            entry.setIp(extractIp(request));
            entry.setMetodo(request.getMethod());
        }
        entry.setRecurso(LogSanitizer.safe(recurso));
        entry.setAcao(LogSanitizer.safe(acao));
        entry.setResultado(LogSanitizer.safe(resultado));
        entry.setDetalhe(LogSanitizer.safe(detalhe));
        entry.setRequestId(MDC.get("requestId"));
        try {
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Falha ao persistir auditoria acao={} resultado={}", entry.getAcao(), entry.getResultado());
        }
        log.info("audit acao={} resultado={} actor={} recurso={} ip={}",
                entry.getAcao(), entry.getResultado(), entry.getActor(),
                entry.getRecurso(), entry.getIp());
    }

    private String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
