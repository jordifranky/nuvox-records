package com.nuvoxrecords.backend.controller;

import com.nuvoxrecords.backend.entity.Usuario;
import com.nuvoxrecords.backend.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Usuario usuarioAutenticado = authService.autenticarUsuario(
                    request.getUsername(),
                    request.getPassword()
            );

            response.put("autenticado", true);
            response.put("mensaje", "Acceso concedido a la Plataforma Curatorial de Nuvox Records");
            response.put("username", usuarioAutenticado.getUsername());
            response.put("nombreCompleto", usuarioAutenticado.getNombreCompleto());
            response.put("rol", usuarioAutenticado.getRol());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);

        } catch (AuthenticationException ex) {
            response.put("autenticado", false);
            response.put("mensaje", ex.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    // El rol NO se recibe del cliente: todo registro público nace como ROLE_ARTISTA.
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario creado = authService.registrarUsuario(
                request.getUsername(), request.getPassword(), request.getNombreCompleto());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", creado.getId());
        response.put("username", creado.getUsername());
        response.put("nombreCompleto", creado.getNombreCompleto());
        response.put("rol", creado.getRol());
        response.put("mensaje", "Usuario registrado en PostgreSQL con contraseña cifrada mediante BCrypt");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    public static class RegistroRequest {
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido")
        private String username;

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        private String password;

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120, message = "El nombre admite máximo 120 caracteres")
        private String nombreCompleto;
    
        public RegistroRequest() {}
        public RegistroRequest(String username, String password, String nombreCompleto) {
            this.username = username;
            this.password = password;
            this.nombreCompleto = nombreCompleto;
        }
        public String getUsername() { return username; }
        public void setUsername(String value) { this.username = value; }
        public String getPassword() { return password; }
        public void setPassword(String value) { this.password = value; }
        public String getNombreCompleto() { return nombreCompleto; }
        public void setNombreCompleto(String value) { this.nombreCompleto = value; }
}



    public static class LoginRequest {
        @NotBlank(message = "El usuario o correo electrónico es obligatorio")
        private String username;

        @NotBlank(message = "La clave de acceso es obligatoria")
        private String password;
    
        public LoginRequest() {}
        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
        public String getUsername() { return username; }
        public void setUsername(String value) { this.username = value; }
        public String getPassword() { return password; }
        public void setPassword(String value) { this.password = value; }
}
}
