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
import java.time.LocalDateTime;

@Entity
@Table(name = "demos")
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

    public Demo() {}

    public Demo(Long id, String codigoExpediente, String tituloCancion, String nombreArtistico, String correoArtista, String generoMusical, String pais, Integer bpm, String tonalidad, String formatoAudio, String enlaceAudio, String notasArtista, String estadoCuratorial, String dictamenComite, LocalDateTime fechaEnvio) {
        this.id = id;
        this.codigoExpediente = codigoExpediente;
        this.tituloCancion = tituloCancion;
        this.nombreArtistico = nombreArtistico;
        this.correoArtista = correoArtista;
        this.generoMusical = generoMusical;
        this.pais = pais;
        this.bpm = bpm;
        this.tonalidad = tonalidad;
        this.formatoAudio = formatoAudio;
        this.enlaceAudio = enlaceAudio;
        this.notasArtista = notasArtista;
        this.estadoCuratorial = estadoCuratorial;
        this.dictamenComite = dictamenComite;
        this.fechaEnvio = fechaEnvio;
    }

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public String getCodigoExpediente() { return codigoExpediente; }
    public void setCodigoExpediente(String value) { this.codigoExpediente = value; }

    public String getTituloCancion() { return tituloCancion; }
    public void setTituloCancion(String value) { this.tituloCancion = value; }

    public String getNombreArtistico() { return nombreArtistico; }
    public void setNombreArtistico(String value) { this.nombreArtistico = value; }

    public String getCorreoArtista() { return correoArtista; }
    public void setCorreoArtista(String value) { this.correoArtista = value; }

    public String getGeneroMusical() { return generoMusical; }
    public void setGeneroMusical(String value) { this.generoMusical = value; }

    public String getPais() { return pais; }
    public void setPais(String value) { this.pais = value; }

    public Integer getBpm() { return bpm; }
    public void setBpm(Integer value) { this.bpm = value; }

    public String getTonalidad() { return tonalidad; }
    public void setTonalidad(String value) { this.tonalidad = value; }

    public String getFormatoAudio() { return formatoAudio; }
    public void setFormatoAudio(String value) { this.formatoAudio = value; }

    public String getEnlaceAudio() { return enlaceAudio; }
    public void setEnlaceAudio(String value) { this.enlaceAudio = value; }

    public String getNotasArtista() { return notasArtista; }
    public void setNotasArtista(String value) { this.notasArtista = value; }

    public String getEstadoCuratorial() { return estadoCuratorial; }
    public void setEstadoCuratorial(String value) { this.estadoCuratorial = value; }

    public String getDictamenComite() { return dictamenComite; }
    public void setDictamenComite(String value) { this.dictamenComite = value; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime value) { this.fechaEnvio = value; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Demo value = new Demo();
        public Builder id(Long x) { value.setId(x); return this; }
        public Builder codigoExpediente(String x) { value.setCodigoExpediente(x); return this; }
        public Builder tituloCancion(String x) { value.setTituloCancion(x); return this; }
        public Builder nombreArtistico(String x) { value.setNombreArtistico(x); return this; }
        public Builder correoArtista(String x) { value.setCorreoArtista(x); return this; }
        public Builder generoMusical(String x) { value.setGeneroMusical(x); return this; }
        public Builder pais(String x) { value.setPais(x); return this; }
        public Builder bpm(Integer x) { value.setBpm(x); return this; }
        public Builder tonalidad(String x) { value.setTonalidad(x); return this; }
        public Builder formatoAudio(String x) { value.setFormatoAudio(x); return this; }
        public Builder enlaceAudio(String x) { value.setEnlaceAudio(x); return this; }
        public Builder notasArtista(String x) { value.setNotasArtista(x); return this; }
        public Builder estadoCuratorial(String x) { value.setEstadoCuratorial(x); return this; }
        public Builder dictamenComite(String x) { value.setDictamenComite(x); return this; }
        public Builder fechaEnvio(LocalDateTime x) { value.setFechaEnvio(x); return this; }
        public Demo build() { return value; }
    }
}
