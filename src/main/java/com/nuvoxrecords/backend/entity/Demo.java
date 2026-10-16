package com.nuvoxrecords.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "demos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Opcional en el POST: si viene vacío, el servicio genera uno (NVX-AAAA-XXXXXX).
    @Size(max = 30, message = "El código de expediente admite máximo 30 caracteres")
    @Column(name = "codigo_expediente", nullable = false, unique = true, length = 30)
    private String codigoExpediente;

    @NotBlank(message = "El título de la canción u obra es obligatorio")
    @Column(name = "titulo_cancion", nullable = false, length = 150)
    private String tituloCancion;

    @NotBlank(message = "El nombre artístico es obligatorio")
    @Column(name = "nombre_artistico", nullable = false, length = 120)
    private String nombreArtistico;

    @NotBlank(message = "El correo electrónico del artista es obligatorio")
    @Email(message = "Debe proporcionar un formato de correo electrónico válido")
    @Column(name = "correo_artista", nullable = false, length = 120)
    private String correoArtista;

    @NotBlank(message = "El género musical o corriente sonora es obligatorio")
    @Column(name = "genero_musical", nullable = false, length = 80)
    private String generoMusical;

    @NotBlank(message = "El país de origen es obligatorio")
    @Column(name = "pais", nullable = false, length = 60)
    private String pais;

    @Column(name = "bpm")
    private Integer bpm;

    @Column(name = "tonalidad", length = 40)
    private String tonalidad;

    @NotBlank(message = "El formato de audio es obligatorio")
    @Column(name = "formato_audio", nullable = false, length = 60)
    private String formatoAudio;

    @NotBlank(message = "El enlace de la demo es obligatorio")
    @Size(max = 500, message = "El enlace admite máximo 500 caracteres")
    @Pattern(regexp = "(?i)^https://(www\\.|on\\.)?(soundcloud\\.com|dropbox\\.com)/\\S+$",
            message = "El enlace debe ser https de SoundCloud o Dropbox")
    @Column(name = "enlace_audio", nullable = false, length = 500)
    private String enlaceAudio;

    @Column(name = "notas_artista", columnDefinition = "TEXT")
    private String notasArtista;

    @Pattern(regexp = "EN_REVISION|APROBADO|RECHAZADO",
            message = "El estado debe ser EN_REVISION, APROBADO o RECHAZADO")
    @Column(name = "estado_curatorial", nullable = false, length = 30)
    private String estadoCuratorial;

    @Column(name = "dictamen_comite", columnDefinition = "TEXT")
    private String dictamenComite;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    public void prePersist() {
        if (this.fechaEnvio == null) {
            this.fechaEnvio = LocalDateTime.now();
        }
        if (this.estadoCuratorial == null || this.estadoCuratorial.isBlank()) {
            this.estadoCuratorial = "EN_REVISION";
        }
    }
}
