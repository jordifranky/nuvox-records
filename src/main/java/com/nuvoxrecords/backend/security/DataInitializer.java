package com.nuvoxrecords.backend.security;

import com.nuvoxrecords.backend.entity.Demo;
import com.nuvoxrecords.backend.entity.Usuario;
import com.nuvoxrecords.backend.repository.DemoRepository;
import com.nuvoxrecords.backend.repository.UsuarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final DemoRepository demoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!usuarioRepository.existsByUsername("curator@nuvoxrecords.com")) {
            Usuario curador = Usuario.builder()
                    .username("curator@nuvoxrecords.com")
                    .password(passwordEncoder.encode("Nuvox2024!"))
                    .nombreCompleto("Elena Vásquez - Executive A&R")
                    .rol("ROLE_COMITE_AR")
                    .activo(true)
                    .build();
            usuarioRepository.save(curador);
        }

        if (!usuarioRepository.existsByUsername("artista@nuvoxrecords.com")) {
            Usuario artista = Usuario.builder()
                    .username("artista@nuvoxrecords.com")
                    .password(passwordEncoder.encode("Artista2024!"))
                    .nombreCompleto("Arvo Valen")
                    .rol("ROLE_ARTISTA")
                    .activo(true)
                    .build();
            usuarioRepository.save(artista);
        }

        if (demoRepository.count() == 0) {
            Demo demoInicial = Demo.builder()
                    .codigoExpediente("NVX-2024-089")
                    .tituloCancion("Nocturne in Obsidian, Op. 4")
                    .nombreArtistico("Arvo Valen")
                    .correoArtista("artista@nuvoxrecords.com")
                    .generoMusical("Ambient Sonics & Droneworks")
                    .pais("España")
                    .bpm(118)
                    .tonalidad("D Minor / Re menor")
                    .formatoAudio("WAV 24-bit / 96kHz")
                    .enlaceAudio("https://soundcloud.com/arvo-valen/nocturne-in-obsidian-op4")
                    .notasArtista("Grabado en cinta analógica Studer A820 con cuarteto de cuerdas y síntesis modular.")
                    .estadoCuratorial("EN_REVISION")
                    .dictamenComite("El clímax armónico en los cellos es exquisito. Revisar headroom antes del fade final.")
                    .fechaEnvio(LocalDateTime.now())
                    .build();
            demoRepository.save(demoInicial);
        }
    }
}
