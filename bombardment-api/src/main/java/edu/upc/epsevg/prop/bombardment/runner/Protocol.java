package edu.upc.epsevg.prop.bombardment.runner;

import edu.upc.epsevg.prop.bombardment.Move;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import edu.upc.epsevg.prop.bombardment.SearchType;

/**
 * Protocol de línies de text entre el motor i {@link PlayerHost}.
 * <pre>
 * host  → motor : READY &lt;nom&gt;                         (en arrencar)
 * motor → host  : MOVE &lt;fen&gt;                           (demana jugada)
 * motor → host  : TIMEOUT                              (s'ha esgotat el temps)
 * host  → motor : RESULT &lt;jugada&gt; &lt;nodes&gt; &lt;prof&gt; &lt;h&gt; &lt;tipus&gt;
 * host  → motor : ERROR &lt;missatge&gt;                    (excepció o jugada nul·la)
 * motor → host  : QUIT
 * </pre>
 *
 * @author bernat
 */
public final class Protocol {

    public static final String READY = "READY", MOVE = "MOVE", TIMEOUT = "TIMEOUT", RESULT = "RESULT",
            ERROR = "ERROR", QUIT = "QUIT";

    private Protocol() {
    }

    public static String oneLine(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ');
    }

    public static String formatResult(PlayerMove m) {
        if (m == null || m.getMove() == null) {
            return ERROR + " el jugador ha retornat una jugada nul·la";
        }
        return RESULT + " " + m.getMove() + " " + m.getNumberOfNodesExplored() + " " + m.getMaxDepthReached()
                + " " + m.getHeuristic() + " " + (m.getSearchType() == null ? SearchType.MINIMAX_IDS : m.getSearchType());
    }

    public static PlayerMove parseResult(String line) {
        String[] t = line.trim().split("\\s+");
        if (t.length < 6 || !t[0].equals(RESULT)) {
            throw new IllegalArgumentException("Resposta mal formada: " + line);
        }
        PlayerMove pm = new PlayerMove(Move.parse(t[1]), Long.parseLong(t[2]), Integer.parseInt(t[3]),
                SearchType.valueOf(t[5]));
        pm.setHeuristic(Double.parseDouble(t[4]));
        return pm;
    }
}
