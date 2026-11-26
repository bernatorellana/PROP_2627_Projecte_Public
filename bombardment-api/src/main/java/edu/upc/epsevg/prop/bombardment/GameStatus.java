package edu.upc.epsevg.prop.bombardment;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Estat d'una partida de Bombardment (Chris Huntoon, 2003) en un tauler n x n.
 *
 * <h2>Regles</h2>
 * <ul>
 * <li>Posició inicial: PLAYER1 ocupa les files 0..k-1 i PLAYER2 les files n-k..n-1 (per defecte k = 2).
 * PLAYER1 mou primer.</li>
 * <li>A cada torn el jugador fa <b>una</b> d'aquestes jugades:
 * <ul>
 * <li><b>Pas</b>: una pedra pròpia avança una fila, recte o en diagonal, a una casella <b>buida</b>
 * (no hi ha captures per desplaçament).</li>
 * <li><b>Explosió</b>: una pedra pròpia esclata; s'eliminen ella i totes les pedres, de qualsevol color,
 * de les 8 caselles veïnes.</li>
 * </ul></li>
 * <li>Guanya qui porta una pedra a la fila del fons del contrari, o qui és l'únic que conserva pedres.</li>
 * <li>Si una explosió buida tot el tauler, la partida és taules.</li>
 * </ul>
 * Cada jugada avança una pedra o n'elimina, de manera que les posicions no es repeteixen mai i tota
 * partida acaba.
 *
 * <h2>Coordenades</h2>
 * {@code x} = columna (0..n-1), {@code y} = fila (0..n-1). {@link #getPos(int, int)} retorna 1 si hi ha una
 * pedra de PLAYER1, -1 si n'hi ha una de PLAYER2 i 0 si la casella és buida.
 *
 * <h2>Extensió</h2>
 * Podeu heretar d'aquesta classe (p.ex. per mantenir un hash Zobrist incremental): totes les modificacions
 * del tauler passen per {@link #setCell(int, int, int)}.
 *
 * @author bernat
 */
public class GameStatus {

    /** Files inicials de cada jugador si no es diu el contrari. */
    public static final int DEFAULT_ROWS = 2;

    protected final int n;
    protected final byte[] cells;          // index = y * n + x ; 1, -1 o 0
    protected PlayerType currentPlayer;
    protected PlayerType winner;           // null si no ha acabat o si és empat
    protected boolean gameOver;
    protected int stones1, stones2;
    protected Move lastMove;
    protected int plies;

    /** Tauler n x n amb la posició inicial (2 files per jugador). */
    public GameStatus(int n) {
        this(n, DEFAULT_ROWS);
    }

    /** Tauler n x n amb {@code rows} files de pedres per jugador. */
    public GameStatus(int n, int rows) {
        if (n < 4 || n > 16) {
            throw new IllegalArgumentException("Mida de tauler no suportada: " + n + " (4..16)");
        }
        if (rows < 1 || 2 * rows >= n) {
            throw new IllegalArgumentException("Nombre de files inicials invàlid: " + rows);
        }
        this.n = n;
        this.cells = new byte[n * n];
        this.currentPlayer = PlayerType.PLAYER1;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < n; x++) {
                put(x, y, 1);
                put(x, n - 1 - y, -1);
            }
        }
    }

    /**
     * Estat a partir d'una matriu {@code board[y][x]} amb valors 1, -1 i 0 (fila 0 = la de PLAYER1).
     * Útil per a proves unitàries.
     */
    public GameStatus(byte[][] board, PlayerType nextTurnPlayer) {
        this.n = board.length;
        this.cells = new byte[n * n];
        for (int y = 0; y < n; y++) {
            if (board[y].length != n) {
                throw new IllegalArgumentException("El tauler ha de ser quadrat");
            }
            for (int x = 0; x < n; x++) {
                if (board[y][x] != 0) {
                    put(x, y, board[y][x] > 0 ? 1 : -1);
                }
            }
        }
        this.currentPlayer = nextTurnPlayer;
        updateEndOfGame(null);
    }

    /** Constructor de còpia. */
    public GameStatus(GameStatus other) {
        this.n = other.n;
        this.cells = other.cells.clone();
        this.currentPlayer = other.currentPlayer;
        this.winner = other.winner;
        this.gameOver = other.gameOver;
        this.stones1 = other.stones1;
        this.stones2 = other.stones2;
        this.lastMove = other.lastMove;
        this.plies = other.plies;
    }

    // ------------------------------------------------------------------ consultes

    public int getSize() {
        return n;
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < n && y < n;
    }

    /** @return 1 (PLAYER1), -1 (PLAYER2) o 0 (buida). */
    public int getPos(int x, int y) {
        return cells[y * n + x];
    }

    public int getPos(Point p) {
        return getPos(p.x, p.y);
    }

    public PlayerType getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /** @return el guanyador, o null si la partida no ha acabat o ha acabat en taules. */
    public PlayerType getWinner() {
        return winner;
    }

    /** @return cert si la partida ha acabat en taules (explosió que buida el tauler). */
    public boolean isDraw() {
        return gameOver && winner == null;
    }

    public int getStoneCount(PlayerType p) {
        return p == PlayerType.PLAYER1 ? stones1 : stones2;
    }

    /** @return la llista de caselles amb pedres del jugador p. */
    public List<Point> getStones(PlayerType p) {
        List<Point> res = new ArrayList<>();
        int c = p.getColor();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == c) {
                res.add(new Point(i % n, i / n));
            }
        }
        return res;
    }

    /** @return la fila que ha d'assolir el jugador p per guanyar. */
    public int getGoalRow(PlayerType p) {
        return p == PlayerType.PLAYER1 ? n - 1 : 0;
    }

    /** @return la darrera jugada aplicada a aquest estat, o null. */
    public Move getLastMove() {
        return lastMove;
    }

    /** @return el nombre de jugades fetes des de la posició de partida (o des de la creació). */
    public int getPlies() {
        return plies;
    }

    /**
     * Totes les jugades legals del jugador que mou: primer els passos i després les explosions
     * (incloses les que no toquen cap pedra contrària, que són legals tot i que normalment dolentes).
     * L'ordre no està pensat per a la poda alfa-beta: ordenar és feina vostra.
     *
     * @return llista nova (buida si la partida ha acabat).
     */
    public List<Move> getMoves() {
        List<Move> res = new ArrayList<>();
        if (gameOver) {
            return res;
        }
        int me = currentPlayer.getColor(), dy = currentPlayer.forward();
        for (int y = 0; y < n; y++) {
            int ty = y + dy;
            if (ty < 0 || ty >= n) {
                continue;
            }
            for (int x = 0; x < n; x++) {
                if (cells[y * n + x] != me) {
                    continue;
                }
                for (int dx = -1; dx <= 1; dx++) {
                    int tx = x + dx;
                    if (tx >= 0 && tx < n && cells[ty * n + tx] == 0) {
                        res.add(Move.step(x, y, tx, ty));
                    }
                }
            }
        }
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == me) {
                res.add(Move.explosion(i % n, i / n));
            }
        }
        return res;
    }

    /** @return cert si m és legal en aquest estat. */
    public boolean isLegal(Move m) {
        if (gameOver || m == null) {
            return false;
        }
        int fx = m.getFromX(), fy = m.getFromY();
        if (!isInside(fx, fy) || getPos(fx, fy) != currentPlayer.getColor()) {
            return false;
        }
        if (m.isExplosion()) {
            return true;
        }
        int tx = m.getToX(), ty = m.getToY();
        return isInside(tx, ty) && ty - fy == currentPlayer.forward() && Math.abs(tx - fx) <= 1
                && getPos(tx, ty) == 0;
    }

    /**
     * Balanç d'una explosió hipotètica a (x,y) per al propietari de la pedra:
     * pedres contràries eliminades menys pedres pròpies eliminades (inclosa la que esclata).
     * Retorna 0 si la casella és buida.
     */
    public int explosionBalance(int x, int y) {
        int owner = getPos(x, y);
        if (owner == 0) {
            return 0;
        }
        int g = 0;
        for (int yy = Math.max(0, y - 1); yy <= Math.min(n - 1, y + 1); yy++) {
            for (int xx = Math.max(0, x - 1); xx <= Math.min(n - 1, x + 1); xx++) {
                int v = cells[yy * n + xx];
                if (v == owner) {
                    g--;
                } else if (v != 0) {
                    g++;
                }
            }
        }
        return g;
    }

    // ------------------------------------------------------------------ modificació

    /**
     * Aplica la jugada m (del jugador que mou) i passa el torn.
     *
     * @throws IllegalArgumentException si la jugada no és legal
     */
    public void movePiece(Move m) {
        if (!isLegal(m)) {
            throw new IllegalArgumentException("Jugada il·legal: " + m + " (torn de " + currentPlayer + ")");
        }
        int me = currentPlayer.getColor();
        int fx = m.getFromX(), fy = m.getFromY();
        if (m.isExplosion()) {
            for (int yy = Math.max(0, fy - 1); yy <= Math.min(n - 1, fy + 1); yy++) {
                for (int xx = Math.max(0, fx - 1); xx <= Math.min(n - 1, fx + 1); xx++) {
                    if (cells[yy * n + xx] != 0) {
                        setCell(xx, yy, 0);
                    }
                }
            }
            updateEndOfGame(null);
        } else {
            setCell(fx, fy, 0);
            setCell(m.getToX(), m.getToY(), me);
            updateEndOfGame(m.getToY() == getGoalRow(currentPlayer) ? currentPlayer : null);
        }
        lastMove = m;
        plies++;
        currentPlayer = currentPlayer.opposite();
    }

    /**
     * Ús exclusiu del motor: el jugador p perd la partida (jugada il·legal, excepció, temps esgotat...).
     */
    public void forceLoser(PlayerType p) {
        gameOver = true;
        winner = p.opposite();
    }

    /**
     * Única porta d'entrada per canviar el contingut d'una casella. Manté els comptadors de pedres.
     * Si l'hereteu (p.ex. per a Zobrist), crideu {@code super.setCell(...)}. Els constructors NO la criden
     * (en Java la subclasse encara no està inicialitzada): calculeu el vostre hash inicial al vostre constructor.
     *
     * @param v 1, -1 o 0
     */
    protected void setCell(int x, int y, int v) {
        int i = y * n + x;
        int old = cells[i];
        if (old == 1) stones1--; else if (old == -1) stones2--;
        cells[i] = (byte) v;
        if (v == 1) stones1++; else if (v == -1) stones2++;
    }

    /** Escriptura directa per als constructors (no crida setCell: la subclasse encara no està construïda). */
    private void put(int x, int y, int v) {
        cells[y * n + x] = (byte) v;
        if (v == 1) stones1++; else if (v == -1) stones2++;
    }

    private void updateEndOfGame(PlayerType reachedGoal) {
        if (reachedGoal != null) {
            gameOver = true;
            winner = reachedGoal;
        } else if (stones1 == 0 && stones2 == 0) {
            gameOver = true;
            winner = null;
        } else if (stones1 == 0) {
            gameOver = true;
            winner = PlayerType.PLAYER2;
        } else if (stones2 == 0) {
            gameOver = true;
            winner = PlayerType.PLAYER1;
        } else {
            // posició carregada des de matriu/FEN amb una pedra ja a la fila objectiu
            for (int x = 0; x < n; x++) {
                if (cells[(n - 1) * n + x] == 1) { gameOver = true; winner = PlayerType.PLAYER1; return; }
                if (cells[x] == -1) { gameOver = true; winner = PlayerType.PLAYER2; return; }
            }
        }
    }

    // ------------------------------------------------------------------ text

    /**
     * Representació compacta: files de dalt (y = n-1) a baix separades per '/', 'W' = PLAYER1,
     * 'B' = PLAYER2, '.' = buida, i després " w" o " b" segons qui mou. Exemple 4x4 inicial (k=1):
     * {@code BBBB/..../..../WWWW w}
     */
    public String toFen() {
        StringBuilder sb = new StringBuilder();
        for (int y = n - 1; y >= 0; y--) {
            for (int x = 0; x < n; x++) {
                int v = cells[y * n + x];
                sb.append(v == 1 ? 'W' : v == -1 ? 'B' : '.');
            }
            if (y > 0) sb.append('/');
        }
        return sb.append(currentPlayer == PlayerType.PLAYER1 ? " w" : " b").toString();
    }

    /** Inversa de {@link #toFen()}. */
    public static GameStatus fromFen(String fen) {
        String[] parts = fen.trim().split("\\s+");
        String[] rows = parts[0].split("/");
        int n = rows.length;
        byte[][] b = new byte[n][n];
        for (int i = 0; i < n; i++) {
            int y = n - 1 - i;
            if (rows[i].length() != n) {
                throw new IllegalArgumentException("FEN invàlid: " + fen);
            }
            for (int x = 0; x < n; x++) {
                char c = rows[i].charAt(x);
                b[y][x] = (byte) (c == 'W' ? 1 : c == 'B' ? -1 : 0);
            }
        }
        PlayerType next = parts.length > 1 && parts[1].equals("b") ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
        return new GameStatus(b, next);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = n - 1; y >= 0; y--) {
            sb.append(String.format("%2d ", y + 1));
            for (int x = 0; x < n; x++) {
                int v = cells[y * n + x];
                sb.append(v == 1 ? " W" : v == -1 ? " B" : " .");
            }
            sb.append('\n');
        }
        sb.append("   ");
        for (int x = 0; x < n; x++) sb.append(' ').append((char) ('a' + x));
        sb.append("\nTorn: ").append(currentPlayer).append(currentPlayer == PlayerType.PLAYER1 ? " (W)" : " (B)");
        if (gameOver) sb.append("  (acabada, guanya ").append(winner == null ? "ningú: taules" : winner).append(')');
        return sb.append('\n').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameStatus)) return false;
        GameStatus g = (GameStatus) o;
        return n == g.n && currentPlayer == g.currentPlayer && Arrays.equals(cells, g.cells);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(cells) + currentPlayer.hashCode();
    }
}
