package edu.upc.epsevg.prop.bombardment.player;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IAuto;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.PlayerType;
import edu.upc.epsevg.prop.bombardment.SearchType;
import java.util.List;

/**
 * EL VOSTRE JUGADOR. Esquelet de partida: canvieu-li el nom i implementeu la cerca.
 *
 * <p>Requisits:
 * <ul>
 * <li>Minimax amb poda alfa-beta i aprofundiment iteratiu, limitat pel temps (vegeu {@link #timeout()}).</li>
 * <li>Una heurística pròpia, documentada i justificada amb experiments.</li>
 * </ul>
 *
 * @author (el vostre equip)
 */
public class MyPlayer implements IPlayer, IAuto {

    private static final java.util.Random RANDOM = new java.util.Random();
    private final String name;
    /** Es posa a cert des d'un altre fil quan s'acaba el temps: per això és volatile. */
    private volatile boolean timeoutReached;

    public MyPlayer() {
        this("MyPlayer");
    }

    public MyPlayer(String name) {
        this.name = name;
    }

    @Override
    public PlayerMove move(GameStatus s) {
        timeoutReached = false;
        // TODO: aprofundiment iteratiu: per a depth = 1, 2, 3... cerca alfa-beta completa;
        //       guardeu la millor jugada de la darrera iteració COMPLETADA i atureu-vos quan timeoutReached.
        // Mentrestant, una jugada a l'atzar (la primera de la llista seria un "corredor" que no representa res).
        List<Move> moves = s.getMoves();
        Move best = moves.get(RANDOM.nextInt(moves.size()));
        return new PlayerMove(best, 0, 0, SearchType.MINIMAX_IDS);
    }

    @Override
    public void timeout() {
        timeoutReached = true;
    }

    @Override
    public String getName() {
        return name;
    }

    /** Heurística des del punt de vista de {@code p}. */
    private double evaluate(GameStatus s, PlayerType p) {
        // TODO: la vostra heurística. Exemple trivial: diferència de pedres.
        return s.getStoneCount(p) - s.getStoneCount(p.opposite());
    }
}
