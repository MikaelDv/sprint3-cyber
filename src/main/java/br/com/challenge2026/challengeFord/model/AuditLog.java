package br.com.challenge2026.challengeFord.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;

    @Column(length = 60)
    private String actor;

    @Column(name = "actor_roles", length = 200)
    private String actorRoles;

    @Column(length = 45)
    private String ip;

    @Column(length = 10)
    private String metodo;

    @Column(length = 200)
    private String recurso;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(nullable = false, length = 20)
    private String resultado;

    @Column(length = 500)
    private String detalhe;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @PrePersist
    void prePersist() {
        if (ocorridoEm == null) ocorridoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getOcorridoEm() { return ocorridoEm; }
    public String getActor() { return actor; }
    public String getActorRoles() { return actorRoles; }
    public String getIp() { return ip; }
    public String getMetodo() { return metodo; }
    public String getRecurso() { return recurso; }
    public String getAcao() { return acao; }
    public String getResultado() { return resultado; }
    public String getDetalhe() { return detalhe; }
    public String getRequestId() { return requestId; }

    public void setActor(String actor) { this.actor = actor; }
    public void setActorRoles(String actorRoles) { this.actorRoles = actorRoles; }
    public void setIp(String ip) { this.ip = ip; }
    public void setMetodo(String metodo) { this.metodo = metodo; }
    public void setRecurso(String recurso) { this.recurso = recurso; }
    public void setAcao(String acao) { this.acao = acao; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
