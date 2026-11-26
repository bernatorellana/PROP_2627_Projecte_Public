package edu.upc.epsevg.prop.bombardment;

/**
 * Els dos jugadors. PLAYER1 comença a les files de baix (y = 0, 1...) i avança cap a y creixent;
 * PLAYER2 comença a dalt i avança cap a y decreixent. PLAYER1 sempre fa la primera jugada.
 *
 * @author bernat
 */
public enum PlayerType {
    PLAYER1(1),
    PLAYER2(-1);

    private final int color;

    PlayerType(int color) {
        this.color = color;
    }

    /** @return 1 per PLAYER1 i -1 per PLAYER2 (el mateix valor que retorna {@link GameStatus#getPos}). */
    public int getColor() {
        return color;
    }

    /** @return el jugador contrari. */
    public PlayerType opposite() {
        return this == PLAYER1 ? PLAYER2 : PLAYER1;
    }

    /** @return +1 si avança cap a y creixent (PLAYER1), -1 si avança cap a y decreixent (PLAYER2). */
    public int forward() {
        return this == PLAYER1 ? 1 : -1;
    }

    /** @param color 1 o -1 @return el jugador corresponent. */
    public static PlayerType fromColor(int color) {
        switch (color) {
            case 1: return PLAYER1;
            case -1: return PLAYER2;
            default: throw new IllegalArgumentException("Color invàlid: " + color);
        }
    }

    /** Equivalent estàtic de {@link #opposite()}, per compatibilitat amb el codi del Hex. */
    public static PlayerType opposite(PlayerType p) {
        return p.opposite();
    }
}
