package com.nuvoxrecords.backend.service;

import com.nuvoxrecords.backend.entity.Demo;
import com.nuvoxrecords.backend.repository.DemoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoServiceImpl implements DemoService {
    public DemoServiceImpl(DemoRepository demoRepository) {
        this.demoRepository = demoRepository;
    }


    private final DemoRepository demoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Demo> listarDemos() {
        return demoRepository.findAll();
    }

    @Override
    @Transactional
    public Demo insertarDemo(Demo demo) {
        // Nunca confiar en el id del cliente: evita que un POST sobrescriba un registro existente.
        demo.setId(null);
        if (demo.getCodigoExpediente() == null || demo.getCodigoExpediente().isBlank()) {
            demo.setCodigoExpediente("NVX-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        return demoRepository.save(demo);
    }

    @Override
    @Transactional
    public Demo actualizarDemo(Long id, Demo demoActualizada) {
        Demo demoExistente = demoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el expediente de demo con ID: " + id));

        if (demoActualizada.getCodigoExpediente() != null && !demoActualizada.getCodigoExpediente().isBlank()) {
            demoExistente.setCodigoExpediente(demoActualizada.getCodigoExpediente());
        }
        demoExistente.setTituloCancion(demoActualizada.getTituloCancion());
        demoExistente.setNombreArtistico(demoActualizada.getNombreArtistico());
        demoExistente.setCorreoArtista(demoActualizada.getCorreoArtista());
        demoExistente.setGeneroMusical(demoActualizada.getGeneroMusical());
        demoExistente.setPais(demoActualizada.getPais());
        demoExistente.setBpm(demoActualizada.getBpm());
        demoExistente.setTonalidad(demoActualizada.getTonalidad());
        demoExistente.setFormatoAudio(demoActualizada.getFormatoAudio());
        demoExistente.setEnlaceAudio(demoActualizada.getEnlaceAudio());
        demoExistente.setNotasArtista(demoActualizada.getNotasArtista());
        if (demoActualizada.getEstadoCuratorial() != null) {
            demoExistente.setEstadoCuratorial(demoActualizada.getEstadoCuratorial());
        }
        demoExistente.setDictamenComite(demoActualizada.getDictamenComite());

        return demoRepository.save(demoExistente);
    }

    @Override
    @Transactional
    public void eliminarDemo(Long id) {
        if (!demoRepository.existsById(id)) {
            throw new EntityNotFoundException("No existe el expediente de demo con ID: " + id);
        }
        demoRepository.deleteById(id);
    }
}
