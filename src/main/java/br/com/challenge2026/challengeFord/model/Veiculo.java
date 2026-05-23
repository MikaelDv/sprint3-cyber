package br.com.challenge2026.challengeFord.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "veiculos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"marca", "modelo", "versao"}))
public class Veiculo {

    private static final String SAFE_TEXT = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 1, max = 80)
    @Pattern(regexp = SAFE_TEXT, message = "Marca contém caracteres inválidos")
    @Column(length = 80, nullable = false)
    private String marca;

    @NotBlank
    @Size(min = 1, max = 80)
    @Pattern(regexp = SAFE_TEXT, message = "Modelo contém caracteres inválidos")
    @Column(length = 80, nullable = false)
    private String modelo;

    @NotBlank
    @Size(min = 1, max = 80)
    @Pattern(regexp = SAFE_TEXT, message = "Versão contém caracteres inválidos")
    @Column(length = 80, nullable = false)
    private String versao;

    @Valid
    @ElementCollection
    @CollectionTable(name = "veiculo_especificacoes", joinColumns = @JoinColumn(name = "veiculo_id"))
    @Size(max = 50, message = "Máximo de 50 especificações por veículo")
    private List<Especificacoes> especificacoesList = new ArrayList<>();

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(nullable = false)
    private boolean anonimizado = false;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (criadoEm == null) criadoEm = now;
        atualizadoEm = now;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public String getVersao() { return versao; }
    public List<Especificacoes> getEspecificacoesList() { return especificacoesList; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public boolean isAnonimizado() { return anonimizado; }

    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setVersao(String versao) { this.versao = versao; }
    public void setEspecificacoesList(List<Especificacoes> especificacoesList) { this.especificacoesList = especificacoesList; }
    public void setAnonimizado(boolean anonimizado) { this.anonimizado = anonimizado; }
}
