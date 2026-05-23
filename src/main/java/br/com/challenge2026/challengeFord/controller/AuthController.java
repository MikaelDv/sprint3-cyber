package br.com.challenge2026.challengeFord.controller;

import br.com.challenge2026.challengeFord.dto.LoginRequestDTO;
import br.com.challenge2026.challengeFord.dto.RefreshRequestDTO;
import br.com.challenge2026.challengeFord.dto.RegisterRequestDTO;
import br.com.challenge2026.challengeFord.dto.TokenResponseDTO;
import br.com.challenge2026.challengeFord.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login, refresh e registro")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Autentica usuário e retorna access/refresh tokens")
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok(authService.login(dto, request));
    }

    @Operation(summary = "Renovar token", description = "Troca refresh por novos tokens")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(@RequestBody @Valid RefreshRequestDTO dto,
                                                    HttpServletRequest request) {
        return ResponseEntity.ok(authService.renovar(dto, request));
    }

    @Operation(summary = "Registrar usuário (ADMIN)", description = "Cria conta no sistema")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrar(@RequestBody @Valid RegisterRequestDTO dto) {
        var u = authService.registrar(dto);
        return ResponseEntity.status(201).body(Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "roles", u.getRoles()
        ));
    }
}
