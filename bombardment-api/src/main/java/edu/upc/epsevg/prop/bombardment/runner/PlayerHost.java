package edu.upc.epsevg.prop.bombardment.runner;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.PlayerFactory;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Procés fill que allotja UN jugador. El motor el llança amb
 * {@code java -cp <api>:<jugador> edu.upc.epsevg.prop.bombardment.runner.PlayerHost "<spec>"}
 * i hi parla amb el protocol de text de {@link Protocol}.
 * <p>
 * La sortida estàndard del jugador (System.out) es redirigeix a System.err perquè no interfereixi amb el
 * protocol; el motor la desa en un fitxer de registre.
 * <p>
 * Aquest procés existeix perquè en Java no es pot matar un fil: si el jugador no respon al timeout, el
 * motor mata el procés sencer.
 *
 * @author bernat
 */
public final class PlayerHost {

    private PlayerHost() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream proto = new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);
        System.setOut(System.err);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        IPlayer player;
        try {
            player = PlayerFactory.create(args[0]);
        } catch (Throwable t) {
            proto.println(Protocol.ERROR + " " + Protocol.oneLine(t.toString()));
            t.printStackTrace();
            return;
        }
        proto.println(Protocol.READY + " " + Protocol.oneLine(player.getName()));

        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith(Protocol.MOVE)) {
                GameStatus s = GameStatus.fromFen(line.substring(Protocol.MOVE.length()).trim());
                Thread worker = new Thread(() -> {
                    try {
                        PlayerMove m = player.move(s);
                        synchronized (proto) {
                            proto.println(Protocol.formatResult(m));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        synchronized (proto) {
                            proto.println(Protocol.ERROR + " " + Protocol.oneLine(t.toString()));
                        }
                    }
                }, "player-move");
                worker.setDaemon(true);
                worker.start();
            } else if (line.startsWith(Protocol.TIMEOUT)) {
                try {
                    player.timeout();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            } else if (line.startsWith(Protocol.QUIT)) {
                break;
            }
        }
        System.exit(0);
    }
}
