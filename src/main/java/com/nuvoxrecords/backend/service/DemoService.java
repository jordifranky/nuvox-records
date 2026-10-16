package com.nuvoxrecords.backend.service;

import com.nuvoxrecords.backend.entity.Demo;
import java.util.List;

public interface DemoService {

    List<Demo> listarDemos();

    Demo insertarDemo(Demo demo);

    Demo actualizarDemo(Long id, Demo demoActualizada);

    void eliminarDemo(Long id);
}
