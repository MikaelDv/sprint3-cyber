package br.com.challenge2026.challengeFord.security;

import br.com.challenge2026.challengeFord.model.Usuario;
import br.com.challenge2026.challengeFord.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioUserDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenhaHash())
                .disabled(!usuario.isAtivo())
                .accountLocked(usuario.isBloqueado())
                .authorities(usuario.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                        .toList())
                .build();
    }
}
