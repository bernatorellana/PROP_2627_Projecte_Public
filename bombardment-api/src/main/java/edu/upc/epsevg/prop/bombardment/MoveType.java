package edu.upc.epsevg.prop.bombardment;

/**
 * Tipus de jugada.
 * <ul>
 * <li>STEP: una pedra pròpia avança una casella, recte o en diagonal, a una casella buida.</li>
 * <li>EXPLOSION: una pedra pròpia esclata i elimina ella mateixa i totes les pedres (de qualsevol color)
 * de les 8 caselles veïnes.</li>
 * </ul>
 */
public enum MoveType {
    STEP,
    EXPLOSION
}
