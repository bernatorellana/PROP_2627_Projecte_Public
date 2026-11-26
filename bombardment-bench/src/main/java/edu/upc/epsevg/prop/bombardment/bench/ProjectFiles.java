package edu.upc.epsevg.prop.bombardment.bench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Troba fitxers del projecte tant si s'executa des del superprojecte com des de bombardment-engine
 * (NetBeans executa els mòduls Maven amb la carpeta del mòdul com a directori de treball).
 *
 * @author bernat
 */
public final class ProjectFiles {

    private ProjectFiles() {
    }

    public static Path locate(String name) {
        Path p = Paths.get(name);
        if (p.isAbsolute() || Files.exists(p)) return p;
        Path up = Paths.get("..").resolve(name);
        return Files.exists(up) ? up.normalize() : p;
    }
}
