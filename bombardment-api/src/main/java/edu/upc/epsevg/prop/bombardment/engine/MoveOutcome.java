package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.PlayerMove;

/**
 * Resultat d'una petició de jugada a un jugador: la jugada (si n'hi ha) i com ha anat.
 *
 * @author bernat
 */
public final class MoveOutcome {

    /** Com ha acabat la petició. */
    public enum Status {
        /** Jugada retornada dins el temps + marge de gràcia. */
        OK,
        /** No ha respost dins el marge de gràcia després de timeout(): procés mort (o fil abandonat). */
        HUNG,
        /** El jugador ha llançat una excepció o ha retornat null. */
        ERROR,
        /** El procés del jugador ha mort pel seu compte (System.exit, OutOfMemory fatal...). */
        CRASHED
    }

    private final Status status;
    private final PlayerMove move;
    private final long elapsedMs;
    private final String detail;

    private MoveOutcome(Status status, PlayerMove move, long elapsedMs, String detail) {
        this.status = status;
        this.move = move;
        this.elapsedMs = elapsedMs;
        this.detail = detail;
    }

    public static MoveOutcome ok(PlayerMove m, long elapsedMs) {
        return new MoveOutcome(Status.OK, m, elapsedMs, null);
    }

    public static MoveOutcome failure(Status st, long elapsedMs, String detail) {
        return new MoveOutcome(st, null, elapsedMs, detail);
    }

    public Status getStatus() { return status; }
    public PlayerMove getMove() { return move; }
    public long getElapsedMs() { return elapsedMs; }
    public String getDetail() { return detail; }
    public boolean isOk() { return status == Status.OK; }

    @Override
    public String toString() {
        return status + (move != null ? " " + move : "") + " (" + elapsedMs + " ms)" + (detail != null ? " " + detail : "");
    }
}
