package br.com.challenge2026.challengeFord.dto;

import br.com.challenge2026.challengeFord.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequestDTO(
        @NotBlank
        @Size(min = 3, max = 60)
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,60}$", message = "Username inválido")
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Size(max = 120)
        @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'.-]{0,120}$", message = "Nome contém caracteres inválidos")
        String nome,

        @NotBlank
        @Size(min = 12, max = 128, message = "A senha deve ter entre 12 e 128 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{12,128}$",
                message = "A senha deve conter maiúscula, minúscula, número e símbolo"
        )
        String senha,

        Set<Role> roles
) {
}
