package edu.upc.epsevg.prop.bombardment.players.faulty;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IAuto;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.SearchType;

/**
 * Jugador defectuós a propòsit, per comprovar que el motor sobreviu als errors típics.
 * Es comporta bé fins a la jugada {@code afterMoves} i llavors falla segons el mode:
 * <ul>
 * <li>{@code HANG}: bucle infinit que ignora timeout()</li>
 * <li>{@code SLOW}: respon 3 s després de timeout()</li>
 * <li>{@code EXCEPTION}: llança una excepció</li>
 * <li>{@code NULL}: retorna null</li>
 * <li>{@code ILLEGAL}: retorna una jugada il·legal</li>
 * <li>{@code EXIT}: crida System.exit</li>
 * </ul>
 *
 * @author bernat
 */
public class FaultyPlayer implements IPlayer, IAuto {

    public enum Mode { HANG, SLOW, EXCEPTION, NULL, ILLEGAL, EXIT }

    private final Mode mode;
    private final int afterMoves;
    private int moves;
    private volatile boolean timeout;
    public static volatile long sink;

    public FaultyPlayer(String mode, int afterMoves) {
        this.mode = Mode.valueOf(mode.toUpperCase());
        this.afterMoves = afterMoves;
    }

    @Override
    public PlayerMove move(GameStatus s) {
        timeout = false;
        if (moves++ >= afterMoves) {
            switch (mode) {
                case HANG:
                    while (true) { sink++; }
                case SLOW:
                    while (!timeout) { Thread.onSpinWait(); }
                    try { Thread.sleep(3000); } catch (InterruptedException e) { }
                    break;
                case EXCEPTION:
                    throw new IllegalStateException("excepció de prova");
                case NULL:
                    return null;
                case ILLEGAL:
                    return new PlayerMove(Move.step(0, 0, 5, 5), 0, 0, SearchType.RANDOM);
                case EXIT:
                    System.exit(3);
                    break;
                default:
            }
        }
        return new PlayerMove(s.getMoves().get(0), 0, 0, SearchType.RANDOM);
    }

    @Override
    public void timeout() {
        timeout = true;
    }

    @Override
    public String getName() {
        return "Faulty(" + mode + ")";
    }
}
