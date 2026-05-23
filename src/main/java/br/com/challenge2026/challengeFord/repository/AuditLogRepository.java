package br.com.challenge2026.challengeFord.repository;

import br.com.challenge2026.challengeFord.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Modifying
    @Query("delete from AuditLog a where a.ocorridoEm < :antes")
    int deleteAntesDe(@Param("antes") LocalDateTime antes);
}
