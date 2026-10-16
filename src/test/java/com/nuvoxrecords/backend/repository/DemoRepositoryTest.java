package com.nuvoxrecords.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nuvoxrecords.backend.entity.Demo;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DemoRepositoryTest {

    @Mock
    private DemoRepository demoRepository;

    private Demo demoBase;

    @BeforeEach
    void setUp() {
        demoBase = Demo.builder()
                .id(1L)
                .codigoExpediente("NVX-2024-089")
                .tituloCancion("Nocturne in Obsidian, Op. 4")
                .nombreArtistico("Arvo Valen")
                .correoArtista("arvo.valen@estudio.com")
                .generoMusical("Ambient Sonics & Droneworks")
                .pais("España")
                .bpm(118)
                .tonalidad("D Minor")
                .formatoAudio("WAV 24-bit / 96kHz")
                .enlaceAudio("https://soundcloud.com/arvo-valen/nocturne-obsidian")
                .notasArtista("Master analógico en cinta de 1/2 pulgada.")
                .estadoCuratorial("EN_REVISION")
                .dictamenComite("Pendiente de sesión en Salón 04.")
                .fechaEnvio(LocalDateTime.now())
                .build();
    }

    // 1. Prueba de la operación INSERTAR
    @Test
    @DisplayName("1. Operación Insertar: Debe guardar una nueva Demo en el repositorio")
    void testInsertarDemo() {
        when(demoRepository.save(any(Demo.class))).thenReturn(demoBase);

        Demo resultado = demoRepository.save(demoBase);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("NVX-2024-089", resultado.getCodigoExpediente());
        assertEquals("Nocturne in Obsidian, Op. 4", resultado.getTituloCancion());
        verify(demoRepository, times(1)).save(demoBase);
    }

    // 2. Prueba de la operación ACTUALIZAR
    @Test
    @DisplayName("2. Operación Actualizar: Debe modificar el estado curatorial y dictamen de una Demo")
    void testActualizarDemo() {
        when(demoRepository.findById(1L)).thenReturn(Optional.of(demoBase));

        Demo demoActualizada = Demo.builder()
                .id(1L)
                .codigoExpediente("NVX-2024-089")
                .tituloCancion("Nocturne in Obsidian, Op. 4 (Master Final)")
                .nombreArtistico("Arvo Valen")
                .correoArtista("arvo.valen@estudio.com")
                .generoMusical("Ambient Sonics & Droneworks")
                .pais("España")
                .bpm(118)
                .tonalidad("D Minor")
                .formatoAudio("WAV 24-bit / 96kHz")
                .enlaceAudio("https://soundcloud.com/arvo-valen/nocturne-obsidian")
                .estadoCuratorial("APROBADO")
                .dictamenComite("Aprobado para prensado en Vinilo 180g.")
                .fechaEnvio(demoBase.getFechaEnvio())
                .build();

        when(demoRepository.save(any(Demo.class))).thenReturn(demoActualizada);

        Optional<Demo> encontrada = demoRepository.findById(1L);
        assertTrue(encontrada.isPresent());

        Demo guardada = demoRepository.save(demoActualizada);

        assertNotNull(guardada);
        assertEquals("APROBADO", guardada.getEstadoCuratorial());
        assertEquals("Nocturne in Obsidian, Op. 4 (Master Final)", guardada.getTituloCancion());
        verify(demoRepository, times(1)).findById(1L);
        verify(demoRepository, times(1)).save(demoActualizada);
    }

    // 3. Prueba de la operación ELIMINAR
    @Test
    @DisplayName("3. Operación Eliminar: Debe eliminar una Demo existente por su ID")
    void testEliminarDemo() {
        Long idEliminar = 1L;
        doNothing().when(demoRepository).deleteById(idEliminar);

        demoRepository.deleteById(idEliminar);

        verify(demoRepository, times(1)).deleteById(idEliminar);
    }

    // 4. Prueba de la operación LISTAR
    @Test
    @DisplayName("4. Operación Listar: Debe retornar la lista de todas las Demos registradas")
    void testListarDemos() {
        Demo segundaDemo = Demo.builder()
                .id(2L)
                .codigoExpediente("NVX-2024-041")
                .tituloCancion("Aura Caelestis")
                .nombreArtistico("Kaelen Stems")
                .correoArtista("kaelen@estudio.com")
                .generoMusical("Neo-Classical")
                .pais("Alemania")
                .formatoAudio("FLAC 24-bit / 48kHz")
                .enlaceAudio("https://soundcloud.com/kaelen-stems/aura-caelestis")
                .estadoCuratorial("APROBADO")
                .fechaEnvio(LocalDateTime.now())
                .build();

        when(demoRepository.findAll()).thenReturn(Arrays.asList(demoBase, segundaDemo));

        List<Demo> listaResultado = demoRepository.findAll();

        assertNotNull(listaResultado);
        assertEquals(2, listaResultado.size());
        assertEquals("NVX-2024-089", listaResultado.get(0).getCodigoExpediente());
        assertEquals("NVX-2024-041", listaResultado.get(1).getCodigoExpediente());
        verify(demoRepository, times(1)).findAll();
    }
}
