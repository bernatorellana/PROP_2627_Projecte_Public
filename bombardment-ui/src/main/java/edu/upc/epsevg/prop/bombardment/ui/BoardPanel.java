package edu.upc.epsevg.prop.bombardment.ui;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.Move;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Dibuix del tauler i entrada de jugades humanes.
 * <p>
 * Humà: clic en una pedra pròpia per seleccionar-la (es marquen els destins possibles i la zona
 * d'explosió); clic en un destí per moure-la; segon clic sobre la mateixa pedra (o clic dret) per fer-la
 * esclatar.
 *
 * @author bernat
 */
public class BoardPanel extends JPanel {

    private static final Color LIGHT = new Color(0xE9D8B4), DARK = new Color(0xD2B98B), LINE = new Color(0x6B5530);
    private static final Color LAST = new Color(30, 120, 220, 170), BLAST = new Color(220, 40, 20, 90);
    private static final Color SEL = new Color(40, 170, 60), TARGET = new Color(40, 170, 60, 120);

    private GameStatus status;
    private GameStatus before;        // estat abans de la darrera jugada (per dibuixar l'explosió)
    private Point selected;
    private Point hover;
    private boolean humanInput;
    private Consumer<Move> onHumanMove;
    /** Avís de final de partida (null = cap). Es tanca amb un clic o en començar una partida nova. */
    private String[] overlay;
    private int overlayWinner;

    public BoardPanel() {
        setPreferredSize(new Dimension(640, 640));
        setBackground(new Color(0x3B2F22));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (overlay != null) {
                    clearOverlay();
                    return;
                }
                click(toCell(e.getX(), e.getY()), SwingUtilities.isRightMouseButton(e));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = toCell(e.getX(), e.getY());
                if (p == null ? hover != null : !p.equals(hover)) {
                    hover = p;
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    /**
     * Mostra un avís de final de partida a sobre del tauler.
     *
     * @param winner 1 = blanques, -1 = negres, 0 = taules
     * @param title  línia principal (qui guanya)
     * @param reason motiu
     * @param detail detall tècnic (pot ser null)
     */
    public void showOverlay(int winner, String title, String reason, String detail) {
        overlay = new String[]{title, reason, detail};
        overlayWinner = winner;
        repaint();
    }

    public void clearOverlay() {
        overlay = null;
        repaint();
    }

    /** Estat a mostrar. {@code previous} = estat abans de la darrera jugada (pot ser null). */
    public void setStatus(GameStatus s, GameStatus previous) {
        this.status = s == null ? null : new GameStatus(s);
        this.before = previous == null ? null : new GameStatus(previous);
        this.selected = null;
        repaint();
    }

    /** Activa l'entrada humana: el callback rep la jugada triada (ja comprovada com a legal). */
    public void enableHumanInput(Consumer<Move> callback) {
        this.onHumanMove = callback;
        this.humanInput = callback != null;
        this.selected = null;
        repaint();
    }

    // ---------------------------------------------------------------- geometria

    private int n() {
        return status == null ? 8 : status.getSize();
    }

    private int cell() {
        return Math.max(10, (Math.min(getWidth(), getHeight()) - 2 * margin()) / n());
    }

    private int margin() {
        return 28;
    }

    private int originX() {
        return (getWidth() - n() * cell()) / 2;
    }

    private int originY() {
        return (getHeight() - n() * cell()) / 2;
    }

    /** Pantalla: fila y = 0 a baix (PLAYER1 juga de baix cap a dalt). */
    private int sx(int x) {
        return originX() + x * cell();
    }

    private int sy(int y) {
        return originY() + (n() - 1 - y) * cell();
    }

    private Point toCell(int px, int py) {
        if (status == null) return null;
        int c = cell();
        int x = Math.floorDiv(px - originX(), c);
        int y = n() - 1 - Math.floorDiv(py - originY(), c);
        return status.isInside(x, y) ? new Point(x, y) : null;
    }

    // ---------------------------------------------------------------- entrada

    private void click(Point p, boolean right) {
        if (!humanInput || status == null || p == null || status.isGameOver()) return;
        int me = status.getCurrentPlayer().getColor();
        if (selected != null && (right || p.equals(selected))) {
            if (right && status.getPos(p) == me) selected = p;
            fire(Move.explosion(selected));
            return;
        }
        if (status.getPos(p) == me) {
            if (right) {
                fire(Move.explosion(p));
            } else {
                selected = p;
                repaint();
            }
            return;
        }
        if (selected != null) {
            Move m = Move.step(selected, p);
            if (status.isLegal(m)) fire(m);
        }
    }

    private void fire(Move m) {
        if (!status.isLegal(m)) return;
        selected = null;
        humanInput = false;
        Consumer<Move> cb = onHumanMove;
        onHumanMove = null;
        repaint();
        cb.accept(m);
    }

    private List<Point> targetsOf(Point p) {
        List<Point> res = new ArrayList<>();
        for (Move m : status.getMoves()) {
            if (!m.isExplosion() && m.getFromX() == p.x && m.getFromY() == p.y) res.add(m.getTo());
        }
        return res;
    }

    // ---------------------------------------------------------------- dibuix

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (status == null) return;
        int n = n(), c = cell();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                g.setColor((x + y) % 2 == 0 ? DARK : LIGHT);
                g.fillRect(sx(x), sy(y), c, c);
            }
        }
        g.setColor(LINE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(originX(), originY(), n * c, n * c);
        drawCoordinates(g);

        Move last = status.getLastMove();
        if (last != null) {
            if (last.isExplosion()) {
                g.setColor(BLAST);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int x = last.getFromX() + dx, y = last.getFromY() + dy;
                    if (status.isInside(x, y)) g.fillRect(sx(x), sy(y), c, c);
                }
            } else {
                g.setColor(LAST);
                g.setStroke(new BasicStroke(3));
                g.drawRect(sx(last.getFromX()) + 2, sy(last.getFromY()) + 2, c - 4, c - 4);
                g.drawRect(sx(last.getToX()) + 2, sy(last.getToY()) + 2, c - 4, c - 4);
            }
        }
        // pedres eliminades per la darrera explosió, en fantasma
        if (last != null && last.isExplosion() && before != null) {
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
                if (before.getPos(x, y) != 0 && status.getPos(x, y) == 0) drawStone(g, x, y, before.getPos(x, y), 0.25f);
            }
        }
        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
            if (status.getPos(x, y) != 0) drawStone(g, x, y, status.getPos(x, y), 1f);
        }
        if (humanInput && selected != null) {
            g.setColor(SEL);
            g.setStroke(new BasicStroke(4));
            g.drawOval(sx(selected.x) + 3, sy(selected.y) + 3, c - 6, c - 6);
            for (Point t : targetsOf(selected)) {
                g.setColor(TARGET);
                g.fillOval(sx(t.x) + c / 3, sy(t.y) + c / 3, c / 3, c / 3);
            }
            if (selected.equals(hover)) {
                g.setColor(BLAST);
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int x = selected.x + dx, y = selected.y + dy;
                    if (status.isInside(x, y)) g.fillRect(sx(x), sy(y), c, c);
                }
            }
        }
        if (overlay != null) drawOverlay(g);
    }

    private void drawOverlay(Graphics2D g) {
        int n = n(), c = cell(), bw = n * c, ox = originX(), oy = originY();
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRect(ox, oy, bw, bw);
        int w = Math.min(bw - 2 * c / 3, 460), pad = 18;
        Font fTitle = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(16, Math.min(24, w / 18)));
        Font fReason = new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(13, Math.min(16, w / 28)));
        Font fDetail = new Font(Font.MONOSPACED, Font.PLAIN, 11);
        int icon = overlayWinner == 0 ? 0 : 44;
        int textW = w - 2 * pad - (icon > 0 ? icon + 14 : 0);
        List<String> t = wrap(g.getFontMetrics(fTitle), overlay[0], textW);
        List<String> r = wrap(g.getFontMetrics(fReason), overlay[1], textW);
        List<String> d = overlay[2] == null ? new ArrayList<>() : wrap(g.getFontMetrics(fDetail), overlay[2], w - 2 * pad);
        if (d.size() > 4) {
            d = new ArrayList<>(d.subList(0, 4));
            d.set(3, d.get(3) + " …");
        }
        FontMetrics mt = g.getFontMetrics(fTitle), mr = g.getFontMetrics(fReason), md = g.getFontMetrics(fDetail);
        int textH = t.size() * mt.getHeight() + 6 + r.size() * mr.getHeight();
        int h = pad + Math.max(icon, textH) + (d.isEmpty() ? 0 : 10 + d.size() * md.getHeight()) + 14 + 14 + pad;
        int x = ox + (bw - w) / 2, y = oy + (bw - h) / 2;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 3, y + 5, w, h, 18, 18);
        g.setColor(new Color(0xFBF8F2));
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(overlayWinner == 0 ? new Color(0x8A8172) : new Color(0xC0392B));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, w, h, 18, 18);
        int tx = x + pad;
        if (icon > 0) {
            drawStoneAt(g, x + pad, y + pad + Math.max(0, (textH - icon) / 2), icon, overlayWinner);
            tx += icon + 14;
        }
        int ty = y + pad + Math.max(0, (icon - textH) / 2);
        g.setColor(new Color(0x1D1B18));
        g.setFont(fTitle);
        for (String l : t) { ty += mt.getAscent(); g.drawString(l, tx, ty); ty += mt.getDescent() + mt.getLeading(); }
        ty += 6;
        g.setColor(new Color(0x45413B));
        g.setFont(fReason);
        for (String l : r) { ty += mr.getAscent(); g.drawString(l, tx, ty); ty += mr.getDescent() + mr.getLeading(); }
        int dy = y + pad + Math.max(icon, textH) + 10;
        g.setFont(fDetail);
        g.setColor(new Color(0x6B665E));
        for (String l : d) { dy += md.getAscent(); g.drawString(l, x + pad, dy); dy += md.getDescent() + md.getLeading(); }
        g.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        g.setColor(new Color(0x8A8172));
        String hint = "Clic per tancar";
        g.drawString(hint, x + w - pad - g.getFontMetrics().stringWidth(hint), y + h - pad + 4);
    }

    private static List<String> wrap(FontMetrics fm, String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String tryLine = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(tryLine) <= width || cur.length() == 0) {
                cur.setLength(0);
                cur.append(tryLine);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    /** Pedra amb coet a una posició lliure (per a l'avís de final). */
    private void drawStoneAt(Graphics2D g, int px, int py, int d, int colour) {
        Color base = colour == 1 ? new Color(0xF4F1EA) : new Color(0x222222);
        Color hi = colour == 1 ? Color.WHITE : new Color(0x6A6A6A);
        Color lo = colour == 1 ? new Color(0xB8B2A6) : new Color(0x050505);
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(px + 2, py + 3, d, d);
        g.setPaint(new RadialGradientPaint(px + d * 0.35f, py + d * 0.3f, d * 0.75f, new float[]{0f, 0.5f, 1f},
                new Color[]{hi, base, lo}));
        g.fillOval(px, py, d, d);
        drawRocket(g, px + d / 2.0, py + d / 2.0, d, colour == 1);
    }

    private void drawCoordinates(Graphics2D g) {
        int n = n(), c = cell();
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(0xE9D8B4));
        for (int i = 0; i < n; i++) {
            String col = String.valueOf((char) ('a' + i));
            g.drawString(col, sx(i) + (c - fm.stringWidth(col)) / 2, originY() + n * c + fm.getAscent() + 4);
            String row = String.valueOf(i + 1);
            g.drawString(row, originX() - fm.stringWidth(row) - 8, sy(i) + (c + fm.getAscent()) / 2 - 2);
        }
    }

    private void drawStone(Graphics2D g, int x, int y, int colour, float alpha) {
        int c = cell(), pad = Math.max(3, c / 9);
        int px = sx(x) + pad, py = sy(y) + pad, d = c - 2 * pad;
        Color base = colour == 1 ? new Color(0xF4F1EA) : new Color(0x222222);
        Color hi = colour == 1 ? Color.WHITE : new Color(0x6A6A6A);
        Color lo = colour == 1 ? new Color(0xB8B2A6) : new Color(0x050505);
        java.awt.Composite old = g.getComposite();
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
        g.setColor(new Color(0, 0, 0, 70));
        g.fillOval(px + 2, py + 3, d, d);
        g.setPaint(new RadialGradientPaint(px + d * 0.35f, py + d * 0.3f, d * 0.75f, new float[]{0f, 0.5f, 1f},
                new Color[]{hi, base, lo}));
        g.fillOval(px, py, d, d);
        drawRocket(g, px + d / 2.0, py + d / 2.0, d, colour == 1);
        g.setComposite(old);
    }

    /** Icona del coet: per defecte apunta amunt (sentit de les blanques); per a les negres es gira 180°. */
    private static final java.awt.image.BufferedImage ROCKET = loadRocket();

    private static java.awt.image.BufferedImage loadRocket() {
        try (java.io.InputStream in = BoardPanel.class.getResourceAsStream("rocket.png")) {
            return in == null ? null : javax.imageio.ImageIO.read(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private void drawRocket(Graphics2D g, double cx, double cy, int d, boolean up) {
        if (ROCKET == null) return;
        double h = d * 0.72, w = h * ROCKET.getWidth() / ROCKET.getHeight();
        java.awt.geom.AffineTransform old = g.getTransform();
        Object interp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.translate(cx, cy);
        if (!up) g.rotate(Math.PI);
        g.drawImage(ROCKET, (int) Math.round(-w / 2), (int) Math.round(-h / 2), (int) Math.round(w), (int) Math.round(h), null);
        g.setTransform(old);
        if (interp != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interp);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }
}
