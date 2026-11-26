package edu.upc.epsevg.prop.bombardment.players;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.SearchType;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Jugador humà: la UI li passa la jugada amb {@link #submit(Move)}. No implementa IAuto.
 *
 * @author bernat
 */
public class HumanPlayer implements IPlayer {

    private final String name;
    private final BlockingQueue<Move> queue = new LinkedBlockingQueue<>();

    public HumanPlayer() {
        this("Humà");
    }

    public HumanPlayer(String name) {
        this.name = name;
    }

    /** La UI crida aquest mètode quan l'usuari ha triat una jugada. */
    public void submit(Move m) {
        queue.add(m);
    }

    @Override
    public PlayerMove move(GameStatus s) {
        queue.clear();
        try {
            return new PlayerMove(queue.take(), 0, 0, SearchType.HUMAN);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void timeout() {
        // els humans no tenen límit de temps
    }

    @Override
    public String getName() {
        return name;
    }
}
