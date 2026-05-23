package br.com.challenge2026.challengeFord.config;

import br.com.challenge2026.challengeFord.crypto.AesGcmCipher;
import br.com.challenge2026.challengeFord.model.Role;
import br.com.challenge2026.challengeFord.model.Usuario;
import br.com.challenge2026.challengeFord.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final AesGcmCipher cipher;

    @Value("${security.bootstrap.admin.username:admin}")
    private String username;

    @Value("${security.bootstrap.admin.email:admin@challenge-ford.local}")
    private String email;

    @Value("${security.bootstrap.admin.nome:Administrador}")
    private String nome;

    @Value("${security.bootstrap.admin.password:Admin@2026Senha!}")
    private String password;

    @Value("${security.bootstrap.admin.enabled:true}")
    private boolean enabled;

    public BootstrapAdminRunner(UsuarioRepository repository,
                                PasswordEncoder encoder,
                                AesGcmCipher cipher) {
        this.repository = repository;
        this.encoder = encoder;
        this.cipher = cipher;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        if (repository.count() > 0) {
            log.info("Bootstrap admin: já existem usuários no banco — nada a fazer");
            return;
        }
        Usuario admin = new Usuario();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setEmailHash(cipher.sha256Hex(email.toLowerCase()));
        admin.setNome(nome);
        admin.setSenhaHash(encoder.encode(password));
        admin.setAtivo(true);
        admin.setRoles(EnumSet.of(Role.ADMIN));
        repository.save(admin);
        log.warn("Bootstrap admin criado com username='{}'. TROQUE A SENHA IMEDIATAMENTE.", username);
    }
}
