package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.PlayerType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resultat agregat d'un enfrontament A contra B (moltes partides).
 *
 * @author bernat
 */
public class MatchResult {

    /** Una partida i quin color portava A. */
    public static final class Game {
        public final int index, opening;
        public final PlayerType colourA;
        public final GameRecord record;

        Game(int index, int opening, PlayerType colourA, GameRecord record) {
            this.index = index;
            this.opening = opening;
            this.colourA = colourA;
            this.record = record;
        }

        public double scoreA() {
            return record.scoreOf(colourA);
        }
    }

    public final String labelA, labelB;
    public String nameA, nameB;
    public final List<Game> games = Collections.synchronizedList(new ArrayList<>());

    public MatchResult(String labelA, String labelB) {
        this.labelA = labelA;
        this.labelB = labelB;
    }

    public synchronized int winsA() { return (int) games.stream().filter(g -> g.record.winner == g.colourA).count(); }
    public synchronized int winsB() { return (int) games.stream().filter(g -> g.record.winner != null && g.record.winner != g.colourA).count(); }
    public synchronized int draws() { return (int) games.stream().filter(g -> g.record.winner == null).count(); }

    /** @return puntuació d'A entre 0 i 1 (taules = mig punt). */
    public synchronized double scoreA() {
        return games.isEmpty() ? 0 : games.stream().mapToDouble(Game::scoreA).sum() / games.size();
    }

    /** Error estàndard de la puntuació (a partir de la variància observada per partida). */
    public synchronized double standardError() {
        int n = games.size();
        if (n < 2) return 0;
        double m = scoreA(), v = 0;
        for (Game g : games) v += (g.scoreA() - m) * (g.scoreA() - m);
        return Math.sqrt(v / (n - 1) / n);
    }

    /** Diferència Elo estimada d'A respecte B (infinit si 0% o 100%). */
    public double eloDiff() {
        double p = scoreA();
        if (p <= 0) return Double.NEGATIVE_INFINITY;
        if (p >= 1) return Double.POSITIVE_INFINITY;
        return -400 * Math.log10(1 / p - 1);
    }

    /** Incidències (penjat, excepció, il·legal, procés mort) que han fet perdre A, per tipus. */
    public synchronized Map<GameRecord.End, Integer> forfeitsOfA() {
        Map<GameRecord.End, Integer> m = new EnumMap<>(GameRecord.End.class);
        for (Game g : games) {
            if (g.record.end.isForfeit() && g.record.winner != g.colourA) m.merge(g.record.end, 1, Integer::sum);
        }
        return m;
    }

    public synchronized Map<GameRecord.End, Integer> forfeitsOfB() {
        Map<GameRecord.End, Integer> m = new EnumMap<>(GameRecord.End.class);
        for (Game g : games) {
            if (g.record.end.isForfeit() && g.record.winner == g.colourA) m.merge(g.record.end, 1, Integer::sum);
        }
        return m;
    }

    public synchronized double avgDepthA() {
        return games.stream().mapToDouble(g -> g.record.avgDepth(g.colourA)).average().orElse(0);
    }

    public synchronized double avgDepthB() {
        return games.stream().mapToDouble(g -> g.record.avgDepth(g.colourA.opposite())).average().orElse(0);
    }

    public synchronized double avgPlies() {
        return games.stream().mapToInt(g -> g.record.plies.size()).average().orElse(0);
    }

    public synchronized long maxElapsedA() {
        return games.stream().mapToLong(g -> g.record.maxElapsed(g.colourA)).max().orElse(0);
    }

    public synchronized long maxElapsedB() {
        return games.stream().mapToLong(g -> g.record.maxElapsed(g.colourA.opposite())).max().orElse(0);
    }

    public String summaryLine() {
        return String.format("%s vs %s: %d-%d-%d (V-D-T)  puntuació %s %.1f%% ± %.1f  Elo %+.0f  plies %.1f  prof. %.2f vs %.2f",
                labelA, labelB, winsA(), winsB(), draws(), labelA, 100 * scoreA(), 100 * standardError(),
                eloDiff(), avgPlies(), avgDepthA(), avgDepthB());
    }
}
