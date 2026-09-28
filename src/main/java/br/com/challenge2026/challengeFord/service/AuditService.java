package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.model.AuditLog;
import br.com.challenge2026.challengeFord.repository.AuditLogRepository;
import br.com.challenge2026.challengeFord.util.LogSanitizer;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger("audit");

    private static final Map<String, List<String>> EVENTOS_CONHECIDOS = Map.of(
            "LOGIN", List.of("SUCESSO", "FALHA", "BLOQUEADO"),
            "REFRESH", List.of("SUCESSO", "FALHA"),
            "USUARIO_CRIADO", List.of("SUCESSO"),
            "CRIAR_VEICULO", List.of("SUCESSO"),
            "CONSULTAR_IA", List.of("SUCESSO"),
            "CONSULTA_MASSIVA", List.of("SUCESSO"));

    private final AuditLogRepository repository;
    private final MeterRegistry meterRegistry;

    public AuditService(AuditLogRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        // Pré-registra as séries com valor 0: sem isso o increase() do Prometheus
        // ignora o primeiro evento de cada ação (a série já nasceria com valor > 0)
        EVENTOS_CONHECIDOS.forEach((acao, resultados) -> resultados.forEach(resultado ->
                meterRegistry.counter("audit_events", "acao", acao, "resultado", resultado)));
    }

    // Transação própria: o evento de auditoria é gravado mesmo se a operação auditada falhar
    @Transactional(Transactional.TxType.REQUIRES_NEW)
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
        // Cada evento de auditoria vira métrica: audit_events_total{acao,resultado}
        meterRegistry.counter("audit_events", "acao", entry.getAcao(), "resultado", entry.getResultado()).increment();
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
