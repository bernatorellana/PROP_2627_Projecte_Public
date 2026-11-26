package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.runner.PlayerHost;
import edu.upc.epsevg.prop.bombardment.runner.Protocol;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Jugador executat en una JVM pròpia ({@link PlayerHost}). Si no respon dins el marge de gràcia després de
 * timeout(), el motor mata el procés: és l'única manera fiable d'aturar codi Java penjat
 * (Thread.stop() ja no existeix des de Java 20).
 * <p>
 * El procés es reutilitza entre jugades i partides; si s'ha hagut de matar, es torna a llançar a la
 * jugada següent. La sortida del jugador (System.out i System.err) va al fitxer de registre.
 *
 * @author bernat
 */
public class IsolatedPlayer implements PlayerHandle {

    private static final String EOF = "\u0000EOF";
    private static final long STARTUP_MS = 30_000;

    private final String classSpec;
    private final String classpath;
    private final List<String> jvmArgs;
    private final long graceMs;
    private final Path logFile;

    private Process process;
    private PrintWriter toChild;
    private BlockingQueue<String> fromChild;
    private String name;
    private int restarts;
    /** Jugades fetes pel procés actual: les primeres tenen més marge perquè la JVM encara és freda (JIT). */
    private int movesSinceStart;

    /** Nombre de jugades, després d'arrencar el procés, que tenen el marge ampliat {@link #COLD_GRACE_MS}. */
    public static final int COLD_MOVES = 3;
    /** Marge per a les primeres jugades d'un procés acabat d'arrencar (JVM freda, sobretot amb la màquina carregada). */
    public static final long COLD_GRACE_MS = 1000;

    /**
     * @param classSpec especificació del jugador (classe i arguments, vegeu PlayerFactory)
     * @param classpath classpath del procés fill (ha d'incloure l'API i el jugador)
     * @param jvmArgs   opcions de la JVM filla (p.ex. -Xmx1g)
     * @param graceMs   marge després de timeout() abans de matar el procés
     * @param logFile   on van les sortides del jugador (null = es descarten)
     */
    public IsolatedPlayer(String classSpec, String classpath, List<String> jvmArgs, long graceMs, Path logFile) {
        this.classSpec = classSpec;
        this.classpath = classpath;
        this.jvmArgs = new ArrayList<>(jvmArgs);
        this.graceMs = graceMs;
        this.logFile = logFile;
        this.name = classSpec;
    }

    /** Llança el procés si no està viu. @throws IllegalStateException si el jugador no arrenca. */
    public synchronized void start() {
        if (process != null && process.isAlive()) {
            return;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        cmd.addAll(jvmArgs);
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(PlayerHost.class.getName());
        cmd.add(classSpec);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (logFile != null) {
            try {
                Files.createDirectories(logFile.toAbsolutePath().getParent());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        } else {
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        }
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException("No puc llançar la JVM del jugador: " + e.getMessage(), e);
        }
        toChild = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8), true);
        BlockingQueue<String> q = new LinkedBlockingQueue<>();
        fromChild = q;
        Process p = process;
        Thread reader = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    q.add(line);
                }
            } catch (IOException ignored) {
                // procés mort
            }
            q.add(EOF);
        }, "reader-" + classSpec);
        reader.setDaemon(true);
        reader.start();
        String first = poll(STARTUP_MS);
        if (first != null && first.startsWith(Protocol.READY)) {
            name = first.substring(Protocol.READY.length()).trim();
            movesSinceStart = 0;
            return;
        }
        kill();
        throw new IllegalStateException("El jugador " + classSpec + " no arrenca: "
                + (first == null ? "sense resposta" : first.equals(EOF) ? "el procés ha mort" : first)
                + (logFile != null ? " (vegeu " + logFile + ")" : ""));
    }

    private String poll(long ms) {
        try {
            return fromChild.poll(Math.max(0, ms), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public synchronized String getName() {
        return name;
    }

    /** @return quantes vegades s'ha hagut de matar i rellançar el procés. */
    public synchronized int getRestarts() {
        return restarts;
    }

    @Override
    public synchronized MoveOutcome requestMove(GameStatus s, long timeMs) {
        try {
            start();
        } catch (IllegalStateException e) {
            return MoveOutcome.failure(MoveOutcome.Status.CRASHED, 0, e.getMessage());
        }
        fromChild.clear();
        long currentGrace = movesSinceStart < COLD_MOVES ? Math.max(graceMs, COLD_GRACE_MS) : graceMs;
        long t0 = System.nanoTime();
        toChild.println(Protocol.MOVE + " " + s.toFen());
        String line = poll(timeMs > 0 ? timeMs : Long.MAX_VALUE / 4);
        if (line == null) {
            toChild.println(Protocol.TIMEOUT);
            line = poll(currentGrace);
        }
        movesSinceStart++;
        long ms = (System.nanoTime() - t0) / 1_000_000;
        if (line == null) {
            kill();
            restarts++;
            return MoveOutcome.failure(MoveOutcome.Status.HUNG, ms, "no ha respost " + currentGrace
                    + " ms després de timeout(): procés mort");
        }
        if (line.equals(EOF)) {
            kill();
            restarts++;
            return MoveOutcome.failure(MoveOutcome.Status.CRASHED, ms, "el procés del jugador ha mort");
        }
        if (line.startsWith(Protocol.ERROR)) {
            return MoveOutcome.failure(MoveOutcome.Status.ERROR, ms, line.substring(Protocol.ERROR.length()).trim());
        }
        try {
            return MoveOutcome.ok(Protocol.parseResult(line), ms);
        } catch (RuntimeException e) {
            return MoveOutcome.failure(MoveOutcome.Status.ERROR, ms, "resposta il·legible: " + line);
        }
    }

    private void kill() {
        if (process != null) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            process = null;
        }
    }

    @Override
    public synchronized void close() {
        if (process != null && process.isAlive()) {
            toChild.println(Protocol.QUIT);
            try {
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    kill();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                kill();
            }
        }
        process = null;
    }

    /** Classpath del procés actual (API, jugadors del profe, jugadors predefinits i el jugador de l'alumne). */
    public static String engineClasspath() {
        List<String> parts = new ArrayList<>();
        for (String c : new String[]{
            "edu.upc.epsevg.prop.bombardment.runner.PlayerHost",
            "edu.upc.epsevg.prop.bombardment.profe1.Profe1Player",
            "edu.upc.epsevg.prop.bombardment.players.RandomPlayer",
            "edu.upc.epsevg.prop.bombardment.player.MyPlayer"}) {
            try {
                Class<?> k = Class.forName(c, false, IsolatedPlayer.class.getClassLoader());
                String loc = Paths.get(k.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
                if (!parts.contains(loc)) parts.add(loc);
            } catch (Exception ignored) {
                // classe absent: no passa res
            }
        }
        for (String p : System.getProperty("java.class.path").split(File.pathSeparator)) {
            if (!p.isEmpty() && !parts.contains(p)) parts.add(p);
        }
        return String.join(File.pathSeparator, parts);
    }

    /** Ubicació (jar o carpeta de classes) de l'API, per construir classpaths de jugadors externs. */
    public static String apiLocation() {
        try {
            return Paths.get(PlayerHost.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
