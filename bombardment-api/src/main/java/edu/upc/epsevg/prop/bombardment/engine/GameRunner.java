package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.PlayerType;

/**
 * Juga una partida sencera entre dos {@link PlayerHandle} (sense UI).
 *
 * @author bernat
 */
public final class GameRunner {

    /** Observador opcional (UI, registre en viu...). */
    public interface Listener {
        /**
         * Abans de demanar cada jugada (al fil de la partida). Pot bloquejar (pausa).
         *
         * @return fals per abandonar la partida (es retorna el registre tal com està, sense resultat)
         */
        default boolean beforeMove(GameStatus s, PlayerType toMove) { return true; }
        default void onMove(GameStatus after, GameRecord.Ply ply) { }
        default void onEnd(GameRecord record) { }
    }

    private GameRunner() {
    }

    public static GameRecord play(PlayerHandle p1, PlayerHandle p2, GameStatus start, long timeMs, Listener l) {
        GameStatus s = new GameStatus(start);
        GameRecord rec = new GameRecord(p1.getName(), p2.getName(), s.toFen(), timeMs);
        int cap = 8 * s.getSize() * s.getSize(); // tota partida acaba molt abans; només per seguretat
        while (!s.isGameOver() && rec.plies.size() < cap) {
            PlayerType me = s.getCurrentPlayer();
            if (l != null && !l.beforeMove(new GameStatus(s), me)) {
                return rec;  // abandonada: end == null
            }
            PlayerHandle h = me == PlayerType.PLAYER1 ? p1 : p2;
            MoveOutcome o = h.requestMove(s, h.isHuman() ? 0 : timeMs);
            if (!o.isOk()) {
                s.forceLoser(me);
                rec.end = o.getStatus() == MoveOutcome.Status.HUNG ? GameRecord.End.HUNG
                        : o.getStatus() == MoveOutcome.Status.CRASHED ? GameRecord.End.CRASHED : GameRecord.End.ERROR;
                rec.detail = h.getName() + ": " + o.getDetail();
                break;
            }
            PlayerMove pm = o.getMove();
            if (!s.isLegal(pm.getMove())) {
                s.forceLoser(me);
                rec.end = GameRecord.End.ILLEGAL;
                rec.detail = h.getName() + ": " + pm.getMove() + " a " + s.toFen();
                break;
            }
            s.movePiece(pm.getMove());
            GameRecord.Ply ply = new GameRecord.Ply(me, pm.getMove(), o.getElapsedMs(), pm.getMaxDepthReached(),
                    pm.getNumberOfNodesExplored(), pm.getHeuristic());
            rec.plies.add(ply);
            if (l != null) l.onMove(new GameStatus(s), ply);
        }
        rec.winner = s.getWinner();
        if (rec.end == null) {
            if (s.isDraw() || !s.isGameOver()) rec.end = GameRecord.End.DRAW;
            else if (s.getStoneCount(rec.winner.opposite()) == 0) rec.end = GameRecord.End.ANNIHILATION;
            else rec.end = GameRecord.End.GOAL;
        }
        if (l != null) l.onEnd(rec);
        return rec;
    }
}
