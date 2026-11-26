package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerType;
import java.util.ArrayList;
import java.util.List;

/**
 * Registre complet d'una partida: posició inicial, jugades amb estadístiques i resultat.
 *
 * @author bernat
 */
public class GameRecord {

    /** Com ha acabat la partida. */
    public enum End {
        GOAL("arriba al fons"),
        ANNIHILATION("elimina totes les pedres contràries"),
        DRAW("taules: tauler buit"),
        HUNG("el perdedor no ha respost a timeout()"),
        ERROR("el perdedor ha llançat una excepció o jugada nul·la"),
        ILLEGAL("el perdedor ha fet una jugada il·legal"),
        CRASHED("el procés del perdedor ha mort");

        public final String text;

        End(String text) {
            this.text = text;
        }

        public boolean isForfeit() {
            return this == HUNG || this == ERROR || this == ILLEGAL || this == CRASHED;
        }
    }

    /** Una jugada amb les seves dades. */
    public static final class Ply {
        public final PlayerType mover;
        public final Move move;
        public final long elapsedMs;
        public final int depth;
        public final long nodes;
        public final double heuristic;

        public Ply(PlayerType mover, Move move, long elapsedMs, int depth, long nodes, double heuristic) {
            this.mover = mover;
            this.move = move;
            this.elapsedMs = elapsedMs;
            this.depth = depth;
            this.nodes = nodes;
            this.heuristic = heuristic;
        }
    }

    public final String player1, player2;
    public final String startFen;
    public final long timeMs;
    public final List<Ply> plies = new ArrayList<>();
    public PlayerType winner;      // null = taules
    public End end;
    public String detail;          // detall de la incidència si n'hi ha

    public GameRecord(String player1, String player2, String startFen, long timeMs) {
        this.player1 = player1;
        this.player2 = player2;
        this.startFen = startFen;
        this.timeMs = timeMs;
    }

    /** @return 1, 0.5 o 0 per a PLAYER1. */
    public double scoreOf(PlayerType p) {
        if (winner == null) return 0.5;
        return winner == p ? 1 : 0;
    }

    public double avgDepth(PlayerType p) {
        return plies.stream().filter(x -> x.mover == p).mapToInt(x -> x.depth).average().orElse(0);
    }

    public double avgNodes(PlayerType p) {
        return plies.stream().filter(x -> x.mover == p).mapToLong(x -> x.nodes).average().orElse(0);
    }

    public long maxElapsed(PlayerType p) {
        return plies.stream().filter(x -> x.mover == p).mapToLong(x -> x.elapsedMs).max().orElse(0);
    }

    /** Jugades en notació textual separades per espais. */
    public String moveList() {
        StringBuilder sb = new StringBuilder();
        for (Ply p : plies) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.move);
        }
        return sb.toString();
    }
}
