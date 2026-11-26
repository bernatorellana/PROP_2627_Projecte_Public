package edu.upc.epsevg.prop.bombardment;

/**
 * Interfície que han d'implementar tots els jugadors (automàtics i manuals).
 *
 * <h2>Contracte de temps</h2>
 * Quan s'esgota el temps de la jugada, el motor crida {@link #timeout()} des d'un altre fil.
 * A partir d'aquest moment {@link #move(GameStatus)} ha de retornar <b>de seguida</b> la millor jugada
 * trobada fins aleshores (normalment la de la darrera iteració completa de l'aprofundiment iteratiu).
 * Si no ho fa dins el marge de gràcia, el motor mata el procés del jugador i la partida es perd.
 * <p>
 * Consell: feu servir un camp {@code volatile boolean} que {@code timeout()} posa a cert i que la cerca
 * consulta a cada node.
 *
 * <h2>Constructors</h2>
 * El motor crea els jugadors per reflexió. Cal un constructor públic sense paràmetres, o bé amb
 * paràmetres de tipus String/int/long/double/boolean (es passen des de la configuració dels challenges,
 * p.ex. {@code edu.upc.epsevg.prop.bombardment.player.MyPlayer(4,true)}).
 *
 * @author bernat
 */
public interface IPlayer {

    /**
     * Decideix la jugada a fer.
     *
     * @param s còpia de l'estat actual (la podeu modificar lliurement); el jugador que mou és
     *          {@code s.getCurrentPlayer()}.
     * @return la jugada (mai null: retornar null o una jugada il·legal fa perdre la partida).
     */
    PlayerMove move(GameStatus s);

    /** Ens avisa que hem de parar la cerca en curs perquè s'ha exhaurit el temps de la jugada. */
    void timeout();

    /** @return nom del jugador (UI i informes). */
    String getName();
}
