package com.nuvoxrecords.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de usuario o correo es obligatorio")
    @Column(name = "username", nullable = false, unique = true, length = 120)
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(name = "rol", nullable = false, length = 50)
    private String rol;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Usuario() {}

    public Usuario(Long id, String username, String password, String nombreCompleto, String rol, Boolean activo) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.activo = activo;
    }

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }

    public String getPassword() { return password; }
    public void setPassword(String value) { this.password = value; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String value) { this.nombreCompleto = value; }

    public String getRol() { return rol; }
    public void setRol(String value) { this.rol = value; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean value) { this.activo = value; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Usuario value = new Usuario();
        public Builder id(Long x) { value.setId(x); return this; }
        public Builder username(String x) { value.setUsername(x); return this; }
        public Builder password(String x) { value.setPassword(x); return this; }
        public Builder nombreCompleto(String x) { value.setNombreCompleto(x); return this; }
        public Builder rol(String x) { value.setRol(x); return this; }
        public Builder activo(Boolean x) { value.setActivo(x); return this; }
        public Usuario build() { return value; }
    }
}
