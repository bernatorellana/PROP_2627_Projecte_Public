package edu.upc.epsevg.prop.bombardment.players;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IAuto;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.SearchType;
import java.util.List;
import java.util.Random;

/**
 * Jugador aleatori. Per defecte evita les explosions que no toquen cap pedra contrària (si pot).
 *
 * @author bernat
 */
public class RandomPlayer implements IPlayer, IAuto {

    private final String name;
    private final Random rnd;

    public RandomPlayer() {
        this("Random");
    }

    public RandomPlayer(String name) {
        this.name = name;
        this.rnd = new Random();
    }

    public RandomPlayer(String name, long seed) {
        this.name = name;
        this.rnd = new Random(seed);
    }

    @Override
    public PlayerMove move(GameStatus s) {
        List<Move> moves = s.getMoves();
        List<Move> useful = moves.stream()
                .filter(m -> !m.isExplosion() || s.explosionBalance(m.getFromX(), m.getFromY()) > -1)
                .toList();
        List<Move> pool = useful.isEmpty() ? moves : useful;
        return new PlayerMove(pool.get(rnd.nextInt(pool.size())), 0, 0, SearchType.RANDOM);
    }

    @Override
    public void timeout() {
        // Mai m'exhaureixo el temps 8-)
    }

    @Override
    public String getName() {
        return "Random(" + name + ")";
    }
}
