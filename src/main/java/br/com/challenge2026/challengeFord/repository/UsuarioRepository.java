package br.com.challenge2026.challengeFord.repository;

import br.com.challenge2026.challengeFord.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmailHash(String emailHash);
    boolean existsByUsername(String username);
    boolean existsByEmailHash(String emailHash);
}
