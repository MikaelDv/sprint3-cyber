package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.crypto.AesGcmCipher;
import br.com.challenge2026.challengeFord.dto.LoginRequestDTO;
import br.com.challenge2026.challengeFord.dto.RefreshRequestDTO;
import br.com.challenge2026.challengeFord.dto.RegisterRequestDTO;
import br.com.challenge2026.challengeFord.dto.TokenResponseDTO;
import br.com.challenge2026.challengeFord.model.RefreshToken;
import br.com.challenge2026.challengeFord.model.Role;
import br.com.challenge2026.challengeFord.model.Usuario;
import br.com.challenge2026.challengeFord.repository.RefreshTokenRepository;
import br.com.challenge2026.challengeFord.repository.UsuarioRepository;
import br.com.challenge2026.challengeFord.security.JwtService;
import br.com.challenge2026.challengeFord.util.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FALHAS = 5;
    private static final int BLOQUEIO_MINUTOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AesGcmCipher cipher;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.jwt.refresh-token-ttl-minutes}")
    private long refreshTtlMinutes;

    public AuthService(UsuarioRepository usuarioRepository,
                       RefreshTokenRepository refreshRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AesGcmCipher cipher,
                       AuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.refreshRepository = refreshRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cipher = cipher;
        this.auditService = auditService;
    }

    @Transactional
    public Usuario registrar(RegisterRequestDTO dto) {
        String emailHash = cipher.sha256Hex(dto.email().toLowerCase());
        if (usuarioRepository.existsByUsername(dto.username())
                || usuarioRepository.existsByEmailHash(emailHash)) {
            throw new IllegalArgumentException("Usuário já existente");
        }
        Usuario u = new Usuario();
        u.setUsername(dto.username());
        u.setEmail(dto.email());
        u.setEmailHash(emailHash);
        u.setNome(dto.nome());
        u.setSenhaHash(passwordEncoder.encode(dto.senha()));
        Set<Role> roles = (dto.roles() == null || dto.roles().isEmpty())
                ? EnumSet.of(Role.USER)
                : EnumSet.copyOf(dto.roles());
        u.setRoles(roles);
        return usuarioRepository.save(u);
    }

    @Transactional
    public TokenResponseDTO login(LoginRequestDTO dto, HttpServletRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(dto.username())
                .orElse(null);

        if (usuario == null || !usuario.isAtivo()) {
            auditService.registrar("LOGIN", "FALHA", "/auth/login", "usuario inexistente ou inativo", request);
            throw new IllegalArgumentException("Credenciais inválidas");
        }
        if (usuario.isBloqueado()) {
            auditService.registrar("LOGIN", "BLOQUEADO", "/auth/login", "conta temporariamente bloqueada", request);
            throw new IllegalArgumentException("Credenciais inválidas");
        }
        if (!passwordEncoder.matches(dto.senha(), usuario.getSenhaHash())) {
            registrarFalha(usuario);
            auditService.registrar("LOGIN", "FALHA", "/auth/login",
                    "tentativa " + usuario.getFalhasLogin() + " para " + LogSanitizer.mask(dto.username(), 2), request);
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        usuario.setFalhasLogin(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);

        String access = jwtService.generateAccessToken(usuario);
        String refresh = emitirRefresh(usuario);
        auditService.registrar("LOGIN", "SUCESSO", "/auth/login", null, request);
        return TokenResponseDTO.bearer(access, refresh, jwtService.getAccessTtlSeconds());
    }

    private void registrarFalha(Usuario usuario) {
        int falhas = usuario.getFalhasLogin() + 1;
        usuario.setFalhasLogin(falhas);
        if (falhas >= MAX_FALHAS) {
            usuario.setBloqueadoAte(LocalDateTime.now().plusMinutes(BLOQUEIO_MINUTOS));
            log.warn("Conta bloqueada por {} falhas: user={}", falhas, LogSanitizer.mask(usuario.getUsername(), 2));
        }
        usuarioRepository.save(usuario);
    }

    private String emitirRefresh(Usuario usuario) {
        byte[] raw = new byte[48];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setTokenHash(cipher.sha256Hex(token));
        rt.setExpiraEm(LocalDateTime.now().plusMinutes(refreshTtlMinutes));
        refreshRepository.save(rt);
        return token;
    }

    @Transactional
    public TokenResponseDTO renovar(RefreshRequestDTO dto, HttpServletRequest request) {
        String hash = cipher.sha256Hex(dto.refreshToken());
        RefreshToken rt = refreshRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));
        if (!rt.isValido()) {
            refreshRepository.revogarTodosDoUsuario(rt.getUsuario());
            auditService.registrar("REFRESH", "FALHA", "/auth/refresh", "token inválido ou reutilizado", request);
            throw new IllegalArgumentException("Refresh token inválido");
        }
        rt.setRevogado(true);
        refreshRepository.save(rt);

        Usuario usuario = rt.getUsuario();
        String access = jwtService.generateAccessToken(usuario);
        String novoRefresh = emitirRefresh(usuario);
        auditService.registrar("REFRESH", "SUCESSO", "/auth/refresh", null, request);
        return TokenResponseDTO.bearer(access, novoRefresh, jwtService.getAccessTtlSeconds());
    }
}
