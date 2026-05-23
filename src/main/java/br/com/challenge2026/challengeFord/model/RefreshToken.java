package br.com.challenge2026.challengeFord.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    private boolean revogado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public boolean isRevogado() { return revogado; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public void setRevogado(boolean revogado) { this.revogado = revogado; }

    public boolean isValido() {
        return !revogado && expiraEm.isAfter(LocalDateTime.now());
    }
}
