package edu.upc.epsevg.prop.bombardment;

import edu.upc.epsevg.prop.bombardment.ui.GameFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Bombardment: el joc amb interfície gràfica.
 * <p>
 * Els jugadors, la mida i el temps es trien a la finestra; aquí només es posen els valors inicials.
 * Arguments opcionals: {@code jugador1 jugador2 mida tempsMs [start]} (p.ex. {@code human profe1 8 1000});
 * amb {@code start} la partida comença sola.
 *
 * @author bernat
 */
public class Game {

    public static void main(String[] args) {
        String p1 = args.length > 0 ? args[0] : "human";
        String p2 = args.length > 1 ? args[1] : "edu.upc.epsevg.prop.bombardment.player.MyPlayer";
        int size = args.length > 2 ? Integer.parseInt(args[2]) : 8;
        long time = args.length > 3 ? Long.parseLong(args[3]) : 1000;
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // aspecte per defecte
            }
            GameFrame f = new GameFrame(p1, p2, size, time);
            f.setVisible(true);
            if (args.length > 4 && args[4].equalsIgnoreCase("start")) f.startGame();
        });
    }
}
