package com.nuvoxrecords.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nuvoxrecords.backend.entity.Demo;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Pruebas de la CAPA DE ACCESO A DATOS contra PostgreSQL real
 * (base nuvox_records_test, ver src/test/resources/application-test.properties).
 * Cada prueba se ejecuta en una transacción que se revierte al terminar.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // usar PostgreSQL, no una BD en memoria
class DemoRepositoryTest {

    @Autowired
    private DemoRepository demoRepository;

    // Permite forzar el SQL (flush) y vaciar la caché (clear) para leer de verdad desde la BD
    @Autowired
    private TestEntityManager em;

    private Demo nuevaDemo(String codigo, String titulo) {
        return Demo.builder()
                .codigoExpediente(codigo)
                .tituloCancion(titulo)
                .nombreArtistico("Arvo Valen")
                .correoArtista("arvo.valen@estudio.com")
                .generoMusical("Ambient Sonics")
                .pais("Perú")
                .bpm(118)
                .tonalidad("D Minor")
                .formatoAudio("WAV 24-bit / 96kHz")
                .enlaceAudio("https://soundcloud.com/arvo-valen/" + codigo.toLowerCase())
                .build();
    }

    private void sincronizarConBd() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. Insertar: guarda la demo en PostgreSQL con id, fecha y estado inicial")
    void testInsertarDemo() {
        Demo guardada = demoRepository.save(nuevaDemo("TEST-001", "Nocturne in Obsidian"));
        sincronizarConBd();

        assertNotNull(guardada.getId());
        Demo enBd = demoRepository.findById(guardada.getId()).orElseThrow();
        assertEquals("Nocturne in Obsidian", enBd.getTituloCancion());
        assertEquals("EN_REVISION", enBd.getEstadoCuratorial()); // asignado por @PrePersist
        assertNotNull(enBd.getFechaEnvio());
    }

    @Test
    @DisplayName("2. Actualizar: cambia el estado curatorial y el dictamen en PostgreSQL")
    void testActualizarDemo() {
        Long id = demoRepository.save(nuevaDemo("TEST-002", "Aura Caelestis")).getId();
        sincronizarConBd();

        Demo aEditar = demoRepository.findById(id).orElseThrow();
        aEditar.setTituloCancion("Aura Caelestis (Master Final)");
        aEditar.setEstadoCuratorial("APROBADO");
        aEditar.setDictamenComite("Aprobado para prensado en vinilo 180g.");
        demoRepository.save(aEditar);
        sincronizarConBd();

        Demo actualizada = demoRepository.findById(id).orElseThrow();
        assertEquals("Aura Caelestis (Master Final)", actualizada.getTituloCancion());
        assertEquals("APROBADO", actualizada.getEstadoCuratorial());
        assertEquals("Aprobado para prensado en vinilo 180g.", actualizada.getDictamenComite());
    }

    @Test
    @DisplayName("3. Eliminar: la demo deja de existir en PostgreSQL")
    void testEliminarDemo() {
        Long id = demoRepository.save(nuevaDemo("TEST-003", "Boceto de Cuerdas")).getId();
        sincronizarConBd();
        assertTrue(demoRepository.existsById(id));

        demoRepository.deleteById(id);
        sincronizarConBd();

        assertFalse(demoRepository.existsById(id));
    }

    @Test
    @DisplayName("4. Listar: devuelve todas las demos registradas")
    void testListarDemos() {
        long antes = demoRepository.count();
        demoRepository.save(nuevaDemo("TEST-004", "Resonancia Líquida"));
        demoRepository.save(nuevaDemo("TEST-005", "Nocturne Op. 5"));
        sincronizarConBd();

        List<Demo> lista = demoRepository.findAll();
        assertEquals(antes + 2, lista.size());
        assertTrue(lista.stream().anyMatch(d -> "TEST-004".equals(d.getCodigoExpediente())));
        assertTrue(lista.stream().anyMatch(d -> "TEST-005".equals(d.getCodigoExpediente())));
    }

    @Test
    @DisplayName("5. Buscar por código y por estado (consultas derivadas del repositorio)")
    void testConsultasPersonalizadas() {
        demoRepository.save(nuevaDemo("TEST-006", "Demo por código"));
        sincronizarConBd();

        assertTrue(demoRepository.findByCodigoExpediente("TEST-006").isPresent());
        assertTrue(demoRepository.findByEstadoCuratorial("EN_REVISION").stream()
                .anyMatch(d -> "TEST-006".equals(d.getCodigoExpediente())));
    }
}