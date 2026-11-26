package edu.upc.epsevg.prop.bombardment.engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Opcions d'execució dels jugadors.
 *
 * @author bernat
 */
public class EngineOptions {

    /** Cert = cada jugador en una JVM pròpia (obligatori per a challenges i torneig). */
    public boolean isolated = true;
    /** Marge després de timeout() abans de declarar el jugador penjat. */
    public long graceMs = 100;
    /** Opcions de la JVM filla. ActiveProcessorCount=1: la cerca ha de ser d'un sol fil. */
    public List<String> jvmArgs = new ArrayList<>(Arrays.asList("-Xmx1g", "-XX:ActiveProcessorCount=1"));
    /** Carpeta on es desen els registres de sortida dels jugadors. */
    public Path logDir = Paths.get("logs");

    public EngineOptions copy() {
        EngineOptions o = new EngineOptions();
        o.isolated = isolated;
        o.graceMs = graceMs;
        o.jvmArgs = new ArrayList<>(jvmArgs);
        o.logDir = logDir;
        return o;
    }
}
