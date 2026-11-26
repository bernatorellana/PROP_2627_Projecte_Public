package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;

/**
 * Accés del motor a un jugador, sigui al mateix procés ({@link InProcessPlayer}) o en una JVM pròpia
 * ({@link IsolatedPlayer}). Aplica el contracte de temps: crida timeout() en esgotar-se el temps i
 * espera, com a molt, el marge de gràcia.
 *
 * @author bernat
 */
public interface PlayerHandle extends AutoCloseable {

    String getName();

    /**
     * Demana una jugada. Bloqueja fins que el jugador respon o s'esgota temps + gràcia.
     *
     * @param s      estat (el handle en passa una còpia al jugador)
     * @param timeMs temps de la jugada; 0 = sense límit (jugador humà)
     */
    MoveOutcome requestMove(GameStatus s, long timeMs);

    /** Cert si és un jugador humà (la UI li ha de proporcionar la jugada). */
    default boolean isHuman() {
        return false;
    }

    @Override
    void close();
}
