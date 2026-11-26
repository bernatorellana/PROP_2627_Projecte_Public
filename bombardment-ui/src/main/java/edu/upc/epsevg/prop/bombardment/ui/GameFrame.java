package edu.upc.epsevg.prop.bombardment.ui;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.PlayerType;
import edu.upc.epsevg.prop.bombardment.engine.EngineOptions;
import edu.upc.epsevg.prop.bombardment.engine.GameRecord;
import edu.upc.epsevg.prop.bombardment.engine.GameRunner;
import edu.upc.epsevg.prop.bombardment.engine.InProcessPlayer;
import edu.upc.epsevg.prop.bombardment.engine.PlayerHandle;
import edu.upc.epsevg.prop.bombardment.engine.PlayerSpec;
import edu.upc.epsevg.prop.bombardment.players.HumanPlayer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Finestra principal: configuració de la partida, tauler, informació de la cerca i llista de jugades.
 *
 * @author bernat
 */
public class GameFrame extends JFrame {

    /** Àlies que surten als desplegables (també s'hi pot escriure qualsevol especificació de jugador). */
    public static final String[] PLAYER_CHOICES = {
        "human", "edu.upc.epsevg.prop.bombardment.player.MyPlayer", "profe1", "random"
    };

    private final BoardPanel board = new BoardPanel();
    private final JComboBox<String> p1Box = new JComboBox<>(PLAYER_CHOICES);
    private final JComboBox<String> p2Box = new JComboBox<>(PLAYER_CHOICES);
    private final JSpinner sizeSpin = new JSpinner(new SpinnerNumberModel(8, 5, 12, 1));
    private final JSpinner rowsSpin = new JSpinner(new SpinnerNumberModel(2, 1, 3, 1));
    private final JSpinner timeSpin = new JSpinner(new SpinnerNumberModel(1000, 50, 600_000, 100));
    private final JCheckBox isolatedBox = new JCheckBox("Jugadors aïllats (JVM pròpia)");
    private final JCheckBox pauseBox = new JCheckBox("Pausa després de cada jugada automàtica");
    private final JButton startButton = new JButton("Nova partida");
    private final JButton pauseButton = new JButton("Pausa");
    private final JButton stepButton = new JButton("Pas");
    private final PlayerCard card1 = new PlayerCard(PlayerType.PLAYER1);
    private final PlayerCard card2 = new PlayerCard(PlayerType.PLAYER2);
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextArea moveList = new JTextArea(10, 24);
    private final Timer clock;

    private final Object lock = new Object();
    private volatile boolean paused, stepOnce, cancelled;
    private Thread gameThread;
    private PlayerHandle h1, h2;
    private volatile long thinkStart;
    private volatile PlayerType thinking;

    public GameFrame(String p1, String p2, int size, long timeMs) {
        super("Bombardment - PROP");
        p1Box.setEditable(true);
        p2Box.setEditable(true);
        p1Box.setSelectedItem(p1);
        p2Box.setSelectedItem(p2);
        sizeSpin.setValue(size);
        timeSpin.setValue((int) timeMs);
        isolatedBox.setToolTipText("<html>Marcat: cada jugador corre en una JVM pròpia i es mata si es penja (com als challenges).<br>"
                + "Desmarcat: mateix procés, permet posar punts d'aturada al vostre codi.</html>");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(board, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        startButton.addActionListener(e -> startGame());
        pauseButton.addActionListener(e -> togglePause());
        stepButton.addActionListener(e -> step());
        pauseButton.setEnabled(false);
        stepButton.setEnabled(false);
        clock = new Timer(100, e -> tick());
        clock.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                stopGame();
                clock.stop();
            }
        });
        board.setStatus(new GameStatus(size), null);
        pack();
        setLocationRelativeTo(null);
    }

    private JComponent buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        side.setPreferredSize(new Dimension(390, 640));

        JPanel setup = new JPanel(new GridBagLayout());
        setup.setBorder(BorderFactory.createTitledBorder("Partida"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        row = addRow(setup, c, row, "Blanques (P1)", p1Box);
        row = addRow(setup, c, row, "Negres (P2)", p2Box);
        row = addRow(setup, c, row, "Mida del tauler", sizeSpin);
        row = addRow(setup, c, row, "Files inicials", rowsSpin);
        row = addRow(setup, c, row, "Temps/jugada (ms)", timeSpin);
        c.gridx = 0; c.gridy = row++; c.gridwidth = 2;
        setup.add(isolatedBox, c);
        c.gridy = row++;
        setup.add(pauseBox, c);
        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
        buttons.add(startButton);
        buttons.add(pauseButton);
        buttons.add(stepButton);
        c.gridy = row;
        setup.add(buttons, c);
        side.add(setup);

        side.add(Box.createVerticalStrut(6));
        side.add(card1);
        side.add(Box.createVerticalStrut(4));
        side.add(card2);
        side.add(Box.createVerticalStrut(6));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        statusLabel.setAlignmentX(0f);
        JPanel st = new JPanel(new BorderLayout());
        st.add(statusLabel);
        side.add(st);
        moveList.setEditable(false);
        moveList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        moveList.setLineWrap(true);
        moveList.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(moveList);
        sp.setBorder(BorderFactory.createTitledBorder("Jugades"));
        side.add(sp);
        return side;
    }

    private static int addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent comp) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        p.add(comp, c);
        return row + 1;
    }

    // ------------------------------------------------------------------ control de partida

    /** Comença una partida amb la configuració actual (equivalent a prémer "Nova partida"). */
    public void startGame() {
        stopGame();
        int size = (Integer) sizeSpin.getValue();
        int rows = (Integer) rowsSpin.getValue();
        if (2 * rows >= size) {
            JOptionPane.showMessageDialog(this, "Massa files inicials per a aquesta mida de tauler.");
            return;
        }
        long timeMs = ((Integer) timeSpin.getValue()).longValue();
        EngineOptions opt = new EngineOptions();
        opt.isolated = isolatedBox.isSelected();
        try {
            h1 = PlayerSpec.parse(String.valueOf(p1Box.getSelectedItem())).open(opt, "ui-P1");
            h2 = PlayerSpec.parse(String.valueOf(p2Box.getSelectedItem())).open(opt, "ui-P2");
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "No puc crear el jugador:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GameStatus start = new GameStatus(size, rows);
        board.clearOverlay();
        board.setStatus(start, null);
        moveList.setText("");
        card1.reset(h1.getName());
        card2.reset(h2.getName());
        cancelled = false;
        paused = false;
        pauseButton.setText("Pausa");
        pauseButton.setEnabled(true);
        stepButton.setEnabled(false);
        PlayerHandle a = h1, b = h2;
        gameThread = new Thread(() -> runGame(a, b, start, timeMs), "ui-game");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void runGame(PlayerHandle a, PlayerHandle b, GameStatus start, long timeMs) {
        final GameStatus[] prev = {start};
        GameRecord rec = GameRunner.play(a, b, start, timeMs, new GameRunner.Listener() {
            @Override
            public boolean beforeMove(GameStatus s, PlayerType toMove) {
                synchronized (lock) {
                    while (paused && !stepOnce && !cancelled) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            return false;
                        }
                    }
                    stepOnce = false;
                }
                if (cancelled) return false;
                PlayerHandle h = toMove == PlayerType.PLAYER1 ? a : b;
                card1.setPlayerName(a.getName());
                card2.setPlayerName(b.getName());
                thinking = toMove;
                thinkStart = System.currentTimeMillis();
                SwingUtilities.invokeLater(() -> {
                    if (h.isHuman()) {
                        statusLabel.setText("Torn de " + h.getName() + ": cliqueu una pedra");
                        HumanPlayer hp = (HumanPlayer) ((InProcessPlayer) h).getPlayer();
                        board.enableHumanInput(hp::submit);
                    } else {
                        statusLabel.setText(h.getName() + " està pensant...");
                    }
                });
                return true;
            }

            @Override
            public void onMove(GameStatus after, GameRecord.Ply ply) {
                if (cancelled) return;
                thinking = null;
                GameStatus before = prev[0];
                prev[0] = after;
                PlayerHandle h = ply.mover == PlayerType.PLAYER1 ? a : b;
                SwingUtilities.invokeLater(() -> {
                    board.setStatus(after, before);
                    (ply.mover == PlayerType.PLAYER1 ? card1 : card2).update(ply, after);
                    (ply.mover == PlayerType.PLAYER1 ? card2 : card1).updateStones(after);
                    int num = after.getPlies();
                    moveList.append((ply.mover == PlayerType.PLAYER1 ? ((num + 1) / 2) + ". " : "") + ply.move
                            + (ply.mover == PlayerType.PLAYER1 ? " " : "\n"));
                });
                if (pauseBox.isSelected() && !h.isHuman()) {
                    synchronized (lock) {
                        paused = true;
                    }
                    SwingUtilities.invokeLater(() -> {
                        pauseButton.setText("Continua");
                        stepButton.setEnabled(true);
                    });
                }
            }
        });
        thinking = null;
        if (cancelled || rec.end == null) return;
        SwingUtilities.invokeLater(() -> {
            String who = rec.winner == null ? "Taules" : "Guanya " + (rec.winner == PlayerType.PLAYER1 ? a.getName() : b.getName());
            statusLabel.setText("<html>" + who + "<br><small>" + rec.end.text
                    + (rec.detail != null ? "<br>" + rec.detail : "") + "</small></html>");
            pauseButton.setEnabled(false);
            stepButton.setEnabled(false);
            board.enableHumanInput(null);
            showEnd(rec, a.getName(), b.getName());
        });
        a.close();
        b.close();
    }

    /** Avís de final de partida sobre el tauler: guanyador i motiu. */
    private void showEnd(GameRecord rec, String name1, String name2) {
        if (rec.winner == null) {
            board.showOverlay(0, "Taules", "Una explosió ha buidat el tauler: no queda cap pedra.", null);
            return;
        }
        boolean w1 = rec.winner == PlayerType.PLAYER1;
        String winner = w1 ? name1 : name2, loser = w1 ? name2 : name1;
        String colour = w1 ? "blanques" : "negres";
        String reason;
        switch (rec.end) {
            case GOAL: reason = "Una pedra ha arribat a la fila del fons del rival."; break;
            case ANNIHILATION: reason = "Ha eliminat totes les pedres del rival."; break;
            case HUNG: reason = loser + " no ha respost a timeout() dins el marge de temps: perd per temps."; break;
            case ILLEGAL: reason = loser + " ha fet una jugada il·legal i perd la partida."; break;
            case ERROR: reason = loser + " ha llançat una excepció o ha retornat una jugada nul·la i perd la partida."; break;
            case CRASHED: reason = "El procés de " + loser + " ha mort i perd la partida."; break;
            default: reason = rec.end.text;
        }
        boolean forfeit = rec.end.isForfeit();
        board.showOverlay(w1 ? 1 : -1, "Guanyen les " + colour + ": " + winner, reason, forfeit ? rec.detail : null);
    }

    private void togglePause() {
        synchronized (lock) {
            paused = !paused;
            lock.notifyAll();
        }
        pauseButton.setText(paused ? "Continua" : "Pausa");
        stepButton.setEnabled(paused);
    }

    private void step() {
        synchronized (lock) {
            stepOnce = true;
            lock.notifyAll();
        }
    }

    private void stopGame() {
        cancelled = true;
        synchronized (lock) {
            lock.notifyAll();
        }
        if (gameThread != null) {
            gameThread.interrupt();   // allibera un humà que esperava clic
        }
        if (h1 != null) h1.close();   // mata els processos aïllats que estiguin pensant
        if (h2 != null) h2.close();
        board.enableHumanInput(null);
        thinking = null;
    }

    private void tick() {
        PlayerType t = thinking;
        card1.setThinking(t == PlayerType.PLAYER1 ? System.currentTimeMillis() - thinkStart : -1);
        card2.setThinking(t == PlayerType.PLAYER2 ? System.currentTimeMillis() - thinkStart : -1);
    }

    // ------------------------------------------------------------------ targeta de jugador

    /** Nom, pedres, estat de la cerca i dades de la darrera jugada d'un jugador. */
    private static final class PlayerCard extends JPanel {
        private final PlayerType type;
        private final JLabel name = new JLabel();
        private final JLabel stones = new JLabel();
        private final JLabel think = new JLabel(" ");
        private final JLabel last = new JLabel(" ");

        PlayerCard(PlayerType type) {
            this.type = type;
            setLayout(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder(type == PlayerType.PLAYER1 ? "Blanques (P1)" : "Negres (P2)"));
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            name.setFont(name.getFont().deriveFont(Font.BOLD));
            c.gridy = 0; add(name, c);
            c.gridy = 1; add(stones, c);
            c.gridy = 2; add(last, c);
            c.gridy = 3; add(think, c);
            think.setForeground(new Color(0x2060C0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        }

        void reset(String n) {
            name.setText(n);
            stones.setText(" ");
            last.setText(" ");
            think.setText(" ");
        }

        void setPlayerName(String n) {
            SwingUtilities.invokeLater(() -> name.setText(n));
        }

        void update(GameRecord.Ply p, GameStatus s) {
            last.setText(String.format(Locale.ROOT, "<html>%s &nbsp; prof. %d &nbsp; %,d nodes<br>%d ms &nbsp; h = %.1f</html>",
                    p.move, p.depth, p.nodes, p.elapsedMs, p.heuristic));
            updateStones(s);
        }

        void updateStones(GameStatus s) {
            stones.setText("Pedres: " + s.getStoneCount(type));
        }

        void setThinking(long ms) {
            think.setText(ms < 0 ? " " : String.format(Locale.ROOT, "pensant... %.1f s", ms / 1000.0));
        }
    }
}
