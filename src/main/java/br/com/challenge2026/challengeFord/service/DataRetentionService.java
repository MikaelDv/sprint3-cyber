package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.model.Veiculo;
import br.com.challenge2026.challengeFord.repository.AuditLogRepository;
import br.com.challenge2026.challengeFord.repository.RefreshTokenRepository;
import br.com.challenge2026.challengeFord.repository.VeiculoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final VeiculoRepository veiculoRepository;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${security.retention.audit-days}")
    private long auditRetentionDays;

    @Value("${security.retention.veiculo-stale-days}")
    private long veiculoStaleDays;

    public DataRetentionService(VeiculoRepository veiculoRepository,
                                AuditLogRepository auditLogRepository,
                                RefreshTokenRepository refreshTokenRepository) {
        this.veiculoRepository = veiculoRepository;
        this.auditLogRepository = auditLogRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void executar() {
        LocalDateTime auditCutoff = LocalDateTime.now().minusDays(auditRetentionDays);
        int auditRemovidos = auditLogRepository.deleteAntesDe(auditCutoff);
        log.info("Retenção: removidos {} registros de auditoria anteriores a {}", auditRemovidos, auditCutoff);

        refreshTokenRepository.deleteExpiradosAntesDe(LocalDateTime.now());

        LocalDateTime veiculoCutoff = LocalDateTime.now().minusDays(veiculoStaleDays);
        List<Veiculo> stale = veiculoRepository.findStaleNaoAnonimizados(veiculoCutoff);
        for (Veiculo v : stale) {
            v.setMarca("ANON-" + v.getId());
            v.setModelo("ANON");
            v.setVersao("ANON");
            v.setEspecificacoesList(java.util.List.of());
            v.setAnonimizado(true);
            veiculoRepository.save(v);
        }
        if (!stale.isEmpty()) {
            log.info("Retenção: anonimizados {} veículos sem atualização desde {}", stale.size(), veiculoCutoff);
        }
    }
}
