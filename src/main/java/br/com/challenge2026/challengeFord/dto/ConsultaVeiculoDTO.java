package br.com.challenge2026.challengeFord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsultaVeiculoDTO(

        @NotBlank
        @Size(min = 1, max = 80)
        @Pattern(regexp = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$",
                message = "Marca contém caracteres inválidos")
        String marca,

        @NotBlank
        @Size(min = 1, max = 80)
        @Pattern(regexp = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$",
                message = "Modelo contém caracteres inválidos")
        String modelo,

        @NotBlank
        @Size(min = 1, max = 80)
        @Pattern(regexp = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$",
                message = "Versão contém caracteres inválidos")
        String versao,

        @Size(max = 500, message = "Prompt deve ter no máximo 500 caracteres")
        String prompt
) {
}
