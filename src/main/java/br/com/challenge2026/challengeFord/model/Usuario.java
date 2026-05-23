package br.com.challenge2026.challengeFord.model;

import br.com.challenge2026.challengeFord.crypto.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60, unique = true)
    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "email_cifrado", nullable = false, columnDefinition = "VARBINARY(512)")
    private String email;

    @Column(name = "email_hash", nullable = false, length = 64, unique = true)
    private String emailHash;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "nome_cifrado", columnDefinition = "VARBINARY(512)")
    private String nome;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "falhas_login", nullable = false)
    private int falhasLogin = 0;

    @Column(name = "bloqueado_ate")
    private LocalDateTime bloqueadoAte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @ElementCollection(targetClass = Role.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "role", length = 30, nullable = false)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

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

    public boolean isBloqueado() {
        return bloqueadoAte != null && bloqueadoAte.isAfter(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getEmailHash() { return emailHash; }
    public String getSenhaHash() { return senhaHash; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public int getFalhasLogin() { return falhasLogin; }
    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Set<Role> getRoles() { return roles; }

    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setNome(String nome) { this.nome = nome; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public void setFalhasLogin(int falhasLogin) { this.falhasLogin = falhasLogin; }
    public void setBloqueadoAte(LocalDateTime bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}
