package br.com.challenge2026.challengeFord.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Especificacoes {

    @NotBlank
    @Size(min = 1, max = 80)
    @Pattern(regexp = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$",
            message = "Nome da especificação inválido")
    @Column(length = 80, nullable = false)
    private String nome;

    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "^[^<>\\\\]{1,255}$", message = "Valor contém caracteres não permitidos")
    @Column(length = 255, nullable = false)
    private String valor;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
