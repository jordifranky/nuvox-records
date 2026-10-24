package com.nuvoxrecords.backend.controller;

import com.nuvoxrecords.backend.entity.Demo;
import com.nuvoxrecords.backend.service.DemoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Ingreso público real: únicamente datos del artista; estado y código se asignan en servidor. */
@RestController
@RequestMapping("/api/public/demos")
public class PublicDemoController {
    public PublicDemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    private final DemoService demoService;

    @PostMapping
    public ResponseEntity<Respuesta> enviar(@Valid @RequestBody Solicitud req) {
        Demo demo = new Demo();
        demo.setTituloCancion(req.getTituloCancion().trim());
        demo.setNombreArtistico(req.getNombreArtistico().trim());
        demo.setCorreoArtista(req.getCorreoArtista().trim().toLowerCase());
        demo.setGeneroMusical(req.getGeneroMusical().trim());
        demo.setPais(req.getPais().trim());
        demo.setFormatoAudio("Enlace externo");
        demo.setEnlaceAudio(req.getEnlaceAudio().trim());
        demo.setNotasArtista(req.getNotasArtista());
        demo.setEstadoCuratorial("EN_REVISION");
        demo.setDictamenComite(null);
        Demo creado = demoService.insertarDemo(demo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new Respuesta(creado.getCodigoExpediente(), "Demo enviada correctamente."));
    }

    public record Respuesta(String codigoExpediente, String mensaje) {}

    public static class Solicitud {
        @NotBlank @Size(max=150) private String tituloCancion;
        @NotBlank @Size(max=120) private String nombreArtistico;
        @NotBlank @Email @Size(max=120) private String correoArtista;
        @NotBlank @Size(max=80) private String generoMusical;
        @NotBlank @Size(max=60) private String pais;
        @NotBlank @Pattern(regexp="(?i)^https://(www\\.|on\\.)?(soundcloud\\.com|dropbox\\.com)/\\S+$",
               message="Ingresa un enlace HTTPS de SoundCloud o Dropbox")
        @Size(max=500) private String enlaceAudio;
        @Size(max=2000) private String notasArtista;
    
        public Solicitud() {}
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
        public String getEnlaceAudio() { return enlaceAudio; }
        public void setEnlaceAudio(String value) { this.enlaceAudio = value; }
        public String getNotasArtista() { return notasArtista; }
        public void setNotasArtista(String value) { this.notasArtista = value; }
}
}
