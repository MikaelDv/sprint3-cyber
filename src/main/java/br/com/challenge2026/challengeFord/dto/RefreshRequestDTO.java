package br.com.challenge2026.challengeFord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequestDTO(
        @NotBlank
        @Size(min = 20, max = 512)
        String refreshToken
) {
}
