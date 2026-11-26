package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.Move;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Jocs d'obertures: posicions de sortida obtingudes amb unes quantes jugades aleatòries (només passos, cap
 * explosió) a partir de la posició inicial. Un mateix (mida, files, jugades, llavor) dona sempre el mateix
 * joc, però per a l'avaluació es recomana desar-lo en un fitxer i fer servir el fitxer.
 * Cada obertura es juga dues vegades canviant els colors, per compensar l'avantatge de qui comença.
 *
 * @author bernat
 */
public final class Openings {

    private Openings() {
    }

    public static List<String> generate(int size, int rows, int count, int plies, long seed) {
        Random r = new Random(seed);
        Set<String> res = new LinkedHashSet<>();
        int attempts = 0;
        while (res.size() < count && attempts++ < count * 100) {
            GameStatus s = new GameStatus(size, rows);
            for (int i = 0; i < plies && !s.isGameOver(); i++) {
                List<Move> steps = s.getMoves().stream().filter(m -> !m.isExplosion()).collect(Collectors.toList());
                if (steps.isEmpty()) break;
                s.movePiece(steps.get(r.nextInt(steps.size())));
            }
            if (!s.isGameOver()) res.add(s.toFen());
        }
        return new ArrayList<>(res);
    }

    /** Llegeix un fitxer amb un FEN per línia (s'ignoren línies buides i les que comencen per #). */
    public static List<String> load(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8).stream().map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#")).collect(Collectors.toList());
    }

    /** Genera i desa un joc d'obertures: {@code Openings mida quantes plies llavor fitxer}. */
    public static void main(String[] a) throws IOException {
        int size = Integer.parseInt(a[0]), count = Integer.parseInt(a[1]), plies = Integer.parseInt(a[2]);
        long seed = Long.parseLong(a[3]);
        List<String> l = generate(size, GameStatus.DEFAULT_ROWS, count, plies, seed);
        save(java.nio.file.Paths.get(a[4]), l, count + " obertures " + size + "x" + size + ", " + plies
                + " plies aleatòries (només passos), llavor " + seed);
        System.out.println(l.size() + " obertures a " + a[4]);
    }

    public static void save(Path file, List<String> fens, String comment) throws IOException {
        List<String> lines = new ArrayList<>();
        if (comment != null) lines.add("# " + comment);
        lines.addAll(fens);
        if (file.toAbsolutePath().getParent() != null) Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}
