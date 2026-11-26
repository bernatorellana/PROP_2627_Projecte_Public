package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.PlayerFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

/**
 * Descriptor de text d'un jugador, el que s'escriu als fitxers de challenge i a la línia de comandes.
 * <ul>
 * <li>{@code random}, {@code profe1}, {@code human}: àlies predefinits.</li>
 * <li>{@code edu.upc.epsevg.prop.bombardment.player.MyPlayer} o {@code ...MyPlayer(4,true)}: una classe del
 * classpath del motor, amb arguments opcionals per al constructor.</li>
 * <li>{@code jar:equips/equip07.jar} o {@code jar:equips/equip07.jar!pkg.Classe(args)}: un jugador lliurat en un
 * jar; si no es diu la classe, es llegeix de l'atribut {@code Bombardment-Player} del manifest.</li>
 * </ul>
 * Opcionalment, {@code etiqueta=spec} posa un nom curt per als informes (p.ex. {@code meu=...MyPlayer(3)}).
 *
 * @author bernat
 */
public final class PlayerSpec {

    private final String label;
    private final String classSpec;
    private final String jar;     // null = classpath del motor

    private PlayerSpec(String label, String classSpec, String jar) {
        this.label = label;
        this.classSpec = classSpec;
        this.jar = jar;
    }

    public static PlayerSpec parse(String text) {
        text = text.trim();
        String label = null;
        int eq = text.indexOf('=');
        int par = text.indexOf('(');
        if (eq > 0 && (par < 0 || eq < par)) {
            label = text.substring(0, eq).trim();
            text = text.substring(eq + 1).trim();
        }
        String cls;
        String jar = null;
        switch (text.toLowerCase()) {
            case "random": cls = "edu.upc.epsevg.prop.bombardment.players.RandomPlayer"; break;
            case "profe1": cls = "edu.upc.epsevg.prop.bombardment.profe1.Profe1Player"; break;
            case "human": cls = "edu.upc.epsevg.prop.bombardment.players.HumanPlayer"; break;
            default:
                if (text.matches("profe[0-9]")) {
                    // Altres nivells del professor: només existeixen si el jar és al classpath.
                    String n = text.substring(5);
                    cls = "edu.upc.epsevg.prop.bombardment.profe" + n + ".Profe" + n + "Player";
                } else if (text.startsWith("jar:")) {
                    String rest = text.substring(4);
                    int bang = rest.indexOf('!');
                    jar = bang >= 0 ? rest.substring(0, bang) : rest;
                    cls = bang >= 0 ? rest.substring(bang + 1) : mainClassOf(jar);
                } else {
                    cls = text;
                }
        }
        if (label == null) {
            label = text.startsWith("jar:") ? Paths.get(jar).getFileName().toString().replaceAll("\\.jar$", "") : text;
        }
        return new PlayerSpec(label, cls, jar);
    }

    private static String mainClassOf(String jar) {
        try (JarFile jf = new JarFile(jar)) {
            String c = jf.getManifest() == null ? null : jf.getManifest().getMainAttributes().getValue("Bombardment-Player");
            if (c == null) {
                throw new IllegalArgumentException("El jar " + jar + " no té l'atribut Bombardment-Player al manifest");
            }
            return c.trim();
        } catch (IOException e) {
            throw new IllegalArgumentException("No puc llegir " + jar + ": " + e.getMessage(), e);
        }
    }

    public String getLabel() { return label; }
    public String getClassSpec() { return classSpec; }
    public boolean isHuman() { return classSpec.endsWith(".HumanPlayer"); }

    /**
     * Crea el handle. Els humans i els jugadors en mode no aïllat es creen al mateix procés.
     *
     * @param tag sufix per al nom del fitxer de registre
     */
    public PlayerHandle open(EngineOptions opt, String tag) {
        if (!opt.isolated || isHuman()) {
            return new InProcessPlayer(instantiate(), opt.graceMs);
        }
        String cp = jar == null ? IsolatedPlayer.engineClasspath()
                : IsolatedPlayer.apiLocation() + File.pathSeparator + Paths.get(jar).toAbsolutePath();
        Path log = opt.logDir == null ? null : opt.logDir.resolve(safe(label) + (tag == null ? "" : "-" + tag) + ".log");
        return new IsolatedPlayer(classSpec, cp, opt.jvmArgs, opt.graceMs, log);
    }

    /** Crea el jugador dins d'aquest procés (els jars es carreguen amb un class loader propi). */
    public IPlayer instantiate() {
        if (jar == null) {
            return PlayerFactory.create(classSpec);
        }
        try {
            URLClassLoader cl = new URLClassLoader(new URL[]{Paths.get(jar).toUri().toURL()},
                    PlayerSpec.class.getClassLoader());
            return PlayerFactory.create(classSpec, cl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    @Override
    public String toString() {
        return label;
    }
}
