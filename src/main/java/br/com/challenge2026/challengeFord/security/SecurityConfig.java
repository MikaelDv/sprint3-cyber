package br.com.challenge2026.challengeFord.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final HmacSignatureFilter hmacFilter;
    private final SecurityHeadersFilter headersFilter;
    private final SuspiciousActivityFilter suspiciousActivityFilter;

    @Value("${security.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${security.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${security.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${security.cors.max-age}")
    private long maxAge;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          RateLimitingFilter rateLimitingFilter,
                          HmacSignatureFilter hmacFilter,
                          SecurityHeadersFilter headersFilter,
                          SuspiciousActivityFilter suspiciousActivityFilter) {
        this.jwtFilter = jwtFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.hmacFilter = hmacFilter;
        this.headersFilter = headersFilter;
        this.suspiciousActivityFilter = suspiciousActivityFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(true);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(parseList(allowedOrigins));
        cfg.setAllowedMethods(parseList(allowedMethods));
        cfg.setAllowedHeaders(parseList(allowedHeaders));
        cfg.setExposedHeaders(List.of("X-Request-Id", "X-RateLimit-Remaining"));
        cfg.setAllowCredentials(allowCredentials);
        cfg.setMaxAge(maxAge);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private List<String> parseList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/registrar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/veiculos").hasAnyRole("ADMIN", "ANALISTA")
                .requestMatchers(HttpMethod.POST, "/veiculos/consultar").hasAnyRole("ADMIN", "ANALISTA")
                .requestMatchers(HttpMethod.GET, "/veiculos/**").hasAnyRole("ADMIN", "ANALISTA", "USER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, resp, ex) -> {
                    resp.setStatus(401);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"erro\":\"Não autenticado\",\"status\":401}");
                })
                .accessDeniedHandler((req, resp, ex) -> {
                    resp.setStatus(403);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"erro\":\"Acesso negado\",\"status\":403}");
                })
            )
            .addFilterBefore(headersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(suspiciousActivityFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(hmacFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
