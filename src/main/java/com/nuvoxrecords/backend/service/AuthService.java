package com.nuvoxrecords.backend.service;

import com.nuvoxrecords.backend.entity.Usuario;
import com.nuvoxrecords.backend.repository.UsuarioRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    public AuthService(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }


    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Usuario autenticarUsuario(String username, String rawPassword) {
        // Mismo mensaje para usuario inexistente o clave incorrecta (no filtra qué correos existen).
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadCredentialsException("La cuenta de usuario se encuentra deshabilitada.");
        }

        if (!passwordEncoder.matches(rawPassword, usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        return usuario;
    }

    @Transactional
    public Usuario registrarUsuario(String username, String rawPassword, String nombreCompleto) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario o correo ya está registrado.");
        }
        Usuario nuevoUsuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .nombreCompleto(nombreCompleto)
                .rol("ROLE_ARTISTA")
                .activo(true)
                .build();
        return usuarioRepository.save(nuevoUsuario);
    }
}
