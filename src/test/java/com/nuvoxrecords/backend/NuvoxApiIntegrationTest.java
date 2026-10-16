package com.nuvoxrecords.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuvoxrecords.backend.entity.Demo;
import com.nuvoxrecords.backend.entity.Usuario;
import com.nuvoxrecords.backend.repository.DemoRepository;
import com.nuvoxrecords.backend.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integraci\u00f3n contra PostgreSQL real (base nuvox_records_test, ver
 * src/test/resources/application.properties). Cubren login BCrypt, seguridad por rol
 * y las cuatro operaciones CRUD (GET, POST, PUT, DELETE) verificando en la base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NuvoxApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DemoRepository demoRepository;
    @Autowired UsuarioRepository usuarioRepository;

    static final String COMITE = basic("curator@nuvoxrecords.com", "Nuvox2024!");
    static final String ARTISTA = basic("artista@nuvoxrecords.com", "Artista2024!");

    static String basic(String u, String p) {
        return "Basic " + Base64.getEncoder().encodeToString((u + ":" + p).getBytes(StandardCharsets.UTF_8));
    }

    Map<String, Object> demoBody(String titulo) {
        return Map.ofEntries(
                Map.entry("tituloCancion", titulo),
                Map.entry("nombreArtistico", "Test Artist"),
                Map.entry("correoArtista", "artista@nuvoxrecords.com"),
                Map.entry("generoMusical", "Ambient"),
                Map.entry("pais", "Per\u00fa"),
                Map.entry("formatoAudio", "WAV 24-bit"),
                Map.entry("enlaceAudio", "https://soundcloud.com/test/demo-1"));
    }

    String login(String user, String pass) throws Exception {
        return json.writeValueAsString(Map.of("username", user, "password", pass));
    }

    // ---------------- LOGIN ----------------

    @Test @Order(1)
    @DisplayName("Login correcto devuelve usuario y rol; el hash en BD es BCrypt")
    void loginOk() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("curator@nuvoxrecords.com", "Nuvox2024!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autenticado").value(true))
                .andExpect(jsonPath("$.rol").value("ROLE_COMITE_AR"));
        Usuario u = usuarioRepository.findByUsername("curator@nuvoxrecords.com").orElseThrow();
        assertTrue(u.getPassword().startsWith("$2"), "La clave debe estar guardada con BCrypt");
        assertFalse(u.getPassword().contains("Nuvox2024"));
    }

    @Test @Order(2)
    @DisplayName("Login con clave incorrecta o usuario inexistente da 401 con el mismo mensaje")
    void loginFallido() throws Exception {
        String m1 = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("curator@nuvoxrecords.com", "mala")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String m2 = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("nadie@x.com", "mala")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(json.readTree(m1).get("mensaje"), json.readTree(m2).get("mensaje"));
    }

    // ---------------- SEGURIDAD / ROLES ----------------

    @Test @Order(3)
    @DisplayName("Registro p\u00fablico ignora 'rol' (ROLE_ADMIN) y crea ROLE_ARTISTA; no puede modificar ni borrar")
    void registroNoPermiteAdmin() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "username", "hacker@test.com", "password", "Clave12345",
                "nombreCompleto", "Hacker", "rol", "ROLE_ADMIN"));
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("ROLE_ARTISTA"));
        assertEquals("ROLE_ARTISTA", usuarioRepository.findByUsername("hacker@test.com").orElseThrow().getRol());

        String h = basic("hacker@test.com", "Clave12345");
        mvc.perform(put("/api/demos/1").header("Authorization", h).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(demoBody("x")))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/demos/1").header("Authorization", h)).andExpect(status().isForbidden());
    }

    @Test @Order(4)
    @DisplayName("Sin credenciales la API de demos responde 401")
    void sinCredenciales() throws Exception {
        mvc.perform(get("/api/demos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/demos").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(demoBody("x")))).andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    @DisplayName("Solo se aceptan enlaces https de SoundCloud o Dropbox")
    void validaEnlace() throws Exception {
        Map<String, Object> malo = new java.util.HashMap<>(demoBody("Link malo"));
        malo.put("enlaceAudio", "https://youtube.com/watch?v=1");
        mvc.perform(post("/api/demos").header("Authorization", ARTISTA).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(malo))).andExpect(status().isBadRequest());

        Map<String, Object> dropbox = new java.util.HashMap<>(demoBody("Link dropbox"));
        dropbox.put("enlaceAudio", "https://www.dropbox.com/s/abc123/demo.wav?dl=0");
        mvc.perform(post("/api/demos").header("Authorization", ARTISTA).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(dropbox))).andExpect(status().isCreated());
    }

    @Test @Order(6)
    @DisplayName("Un artista no puede autoaprobar su demo ni enviar con otro correo")
    void artistaNoAutoaprueba() throws Exception {
        Map<String, Object> b = new java.util.HashMap<>(demoBody("Intento de aprobaci\u00f3n"));
        b.put("estadoCuratorial", "APROBADO");
        b.put("dictamenComite", "Aprobado por m\u00ed");
        b.put("correoArtista", "otro@x.com");
        mvc.perform(post("/api/demos").header("Authorization", ARTISTA).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(b)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoCuratorial").value("EN_REVISION"))
                .andExpect(jsonPath("$.correoArtista").value("artista@nuvoxrecords.com"))
                .andExpect(jsonPath("$.dictamenComite").doesNotExist());
    }

    // ---------------- CRUD ----------------

    @Test @Order(10)
    @DisplayName("CRUD completo (POST, GET, PUT, DELETE) persistido en PostgreSQL")
    void crudCompleto() throws Exception {
        long antes = demoRepository.count();

        // POST
        String resp = mvc.perform(post("/api/demos").header("Authorization", COMITE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(demoBody("CRUD Demo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigoExpediente").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resp).get("id").asLong();
        assertEquals(antes + 1, demoRepository.count(), "POST debe a\u00f1adir una fila en BD");
        assertEquals("CRUD Demo", demoRepository.findById(id).orElseThrow().getTituloCancion());

        // GET
        String lista = mvc.perform(get("/api/demos").header("Authorization", COMITE))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        boolean encontrada = false;
        for (JsonNode n : json.readTree(lista)) encontrada |= n.get("id").asLong() == id;
        assertTrue(encontrada, "GET debe listar la demo creada");

        // PUT
        Map<String, Object> cambios = new java.util.HashMap<>(demoBody("CRUD Demo editada"));
        cambios.put("estadoCuratorial", "APROBADO");
        cambios.put("dictamenComite", "Aprobada para prensado");
        mvc.perform(put("/api/demos/" + id).header("Authorization", COMITE)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cambios)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoCuratorial").value("APROBADO"));
        Demo enBd = demoRepository.findById(id).orElseThrow();
        assertEquals("CRUD Demo editada", enBd.getTituloCancion());
        assertEquals("APROBADO", enBd.getEstadoCuratorial());
        assertEquals("Aprobada para prensado", enBd.getDictamenComite());

        // DELETE
        mvc.perform(delete("/api/demos/" + id).header("Authorization", COMITE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eliminado").value(true));
        assertFalse(demoRepository.existsById(id), "DELETE debe quitar la fila de BD");
        assertEquals(antes, demoRepository.count());
    }

    @Test @Order(11)
    @DisplayName("PUT/DELETE de un id inexistente devuelven 404; POST con id ajeno no sobrescribe")
    void inexistenteYSobrescritura() throws Exception {
        mvc.perform(put("/api/demos/999999").header("Authorization", COMITE)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(demoBody("x"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/demos/999999").header("Authorization", COMITE)).andExpect(status().isNotFound());

        Demo existente = demoRepository.findAll().get(0);
        String tituloOriginal = existente.getTituloCancion();
        Map<String, Object> b = new java.util.HashMap<>(demoBody("Intento de sobrescribir"));
        b.put("id", existente.getId());
        mvc.perform(post("/api/demos").header("Authorization", COMITE)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(b)))
                .andExpect(status().isCreated());
        assertEquals(tituloOriginal, demoRepository.findById(existente.getId()).orElseThrow().getTituloCancion());
    }
}
