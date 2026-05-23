package br.com.challenge2026.challengeFord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
        @NotBlank
        @Size(min = 3, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,60}$", message = "Username inválido")
        String username,

        @NotBlank
        @Size(min = 8, max = 128)
        String senha
) {
}
