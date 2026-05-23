package br.com.challenge2026.challengeFord.repository;

import br.com.challenge2026.challengeFord.model.RefreshToken;
import br.com.challenge2026.challengeFord.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revogado = true where r.usuario = :usuario")
    void revogarTodosDoUsuario(@Param("usuario") Usuario usuario);

    @Modifying
    @Query("delete from RefreshToken r where r.expiraEm < :antes")
    void deleteExpiradosAntesDe(@Param("antes") LocalDateTime antes);
}
