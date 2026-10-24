package com.nuvoxrecords.backend.controller;

import com.nuvoxrecords.backend.entity.Demo;
import com.nuvoxrecords.backend.service.DemoService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demos")
public class DemoController {
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }


    private final DemoService demoService;

    // Artistas: solo sus demos (correoArtista = su usuario). Comité A&R: todas.
    @GetMapping
    public ResponseEntity<List<Demo>> listarDemos(Authentication auth) {
        List<Demo> demos = demoService.listarDemos();
        if (!esComite(auth)) {
            demos = demos.stream()
                    .filter(d -> d.getCorreoArtista().equalsIgnoreCase(auth.getName()))
                    .toList();
        }
        return ResponseEntity.ok(demos);
    }

    // 2. SERVICIO REST POST: Insertar nuevo expediente de Demo en PostgreSQL
    @PostMapping
    public ResponseEntity<Demo> insertarDemo(@Valid @RequestBody Demo demo, Authentication auth) {
        if (!esComite(auth)) {
            demo.setEstadoCuratorial("EN_REVISION");
            demo.setDictamenComite(null);
            demo.setCorreoArtista(auth.getName());
        }
        Demo nuevaDemo = demoService.insertarDemo(demo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaDemo);
    }

    private boolean esComite(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMITE_AR"));
    }

    // 3. SERVICIO REST PUT: Actualizar estado curatorial o metadatos de Demo en PostgreSQL
    @PutMapping("/{id}")
    public ResponseEntity<Demo> actualizarDemo(
            @PathVariable Long id,
            @Valid @RequestBody Demo demoActualizada) {
        Demo demoGuardada = demoService.actualizarDemo(id, demoActualizada);
        return ResponseEntity.ok(demoGuardada);
    }

    // 4. SERVICIO REST DELETE: Eliminar y purgar expediente de Demo de PostgreSQL
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> eliminarDemo(@PathVariable Long id) {
        demoService.eliminarDemo(id);
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("eliminado", true);
        respuesta.put("id", id);
        respuesta.put("mensaje", "Expediente de demo eliminado permanentemente de PostgreSQL");
        return ResponseEntity.ok(respuesta);
    }
}
