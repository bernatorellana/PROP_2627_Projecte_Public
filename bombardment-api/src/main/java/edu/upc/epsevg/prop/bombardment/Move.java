package edu.upc.epsevg.prop.bombardment;

import java.awt.Point;
import java.util.Objects;

/**
 * Una jugada de Bombardment. Classe immutable.
 * <p>
 * Coordenades: {@code x} = columna (0..n-1), {@code y} = fila (0..n-1). Per a una explosió,
 * {@code getFrom()} i {@code getTo()} són la mateixa casella (la pedra que esclata).
 * <p>
 * Notació textual: un pas s'escriu {@code a2-b3}; una explosió {@code c4*} (columna en lletra, fila des d'1).
 *
 * @author bernat
 */
public final class Move {

    private final int fx, fy, tx, ty;
    private final MoveType type;

    private Move(int fx, int fy, int tx, int ty, MoveType type) {
        this.fx = fx;
        this.fy = fy;
        this.tx = tx;
        this.ty = ty;
        this.type = type;
    }

    /** Pas de la pedra (fx,fy) a (tx,ty). */
    public static Move step(int fx, int fy, int tx, int ty) {
        return new Move(fx, fy, tx, ty, MoveType.STEP);
    }

    public static Move step(Point from, Point to) {
        return step(from.x, from.y, to.x, to.y);
    }

    /** Explosió de la pedra (x,y). */
    public static Move explosion(int x, int y) {
        return new Move(x, y, x, y, MoveType.EXPLOSION);
    }

    public static Move explosion(Point p) {
        return explosion(p.x, p.y);
    }

    public MoveType getType() {
        return type;
    }

    public boolean isExplosion() {
        return type == MoveType.EXPLOSION;
    }

    /** @return una còpia de la casella d'origen (la pedra que es mou o esclata). */
    public Point getFrom() {
        return new Point(fx, fy);
    }

    /** @return una còpia de la casella de destí (igual que l'origen si és una explosió). */
    public Point getTo() {
        return new Point(tx, ty);
    }

    public int getFromX() { return fx; }
    public int getFromY() { return fy; }
    public int getToX() { return tx; }
    public int getToY() { return ty; }

    private static String sq(int x, int y) {
        return "" + (char) ('a' + x) + (y + 1);
    }

    @Override
    public String toString() {
        return isExplosion() ? sq(fx, fy) + "*" : sq(fx, fy) + "-" + sq(tx, ty);
    }

    /** Llegeix la notació de {@link #toString()} ({@code a2-b3} o {@code c4*}). */
    public static Move parse(String s) {
        s = s.trim();
        try {
            if (s.endsWith("*")) {
                int[] p = parseSq(s.substring(0, s.length() - 1));
                return explosion(p[0], p[1]);
            }
            String[] parts = s.split("-");
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            int[] a = parseSq(parts[0]), b = parseSq(parts[1]);
            return step(a[0], a[1], b[0], b[1]);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Jugada mal escrita: '" + s + "'");
        }
    }

    private static int[] parseSq(String s) {
        return new int[]{s.charAt(0) - 'a', Integer.parseInt(s.substring(1)) - 1};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return fx == m.fx && fy == m.fy && tx == m.tx && ty == m.ty && type == m.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fx, fy, tx, ty, type);
    }
}
