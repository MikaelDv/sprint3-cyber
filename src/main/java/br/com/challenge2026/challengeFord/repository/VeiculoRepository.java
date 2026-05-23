package br.com.challenge2026.challengeFord.repository;

import br.com.challenge2026.challengeFord.model.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByMarcaAndModeloAndVersao(String marca, String modelo, String versao);

    @Query("select v from Veiculo v where v.atualizadoEm < :limite and v.anonimizado = false")
    List<Veiculo> findStaleNaoAnonimizados(@Param("limite") LocalDateTime limite);
}
