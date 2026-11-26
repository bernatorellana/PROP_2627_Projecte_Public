package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.IAuto;
import edu.upc.epsevg.prop.bombardment.IPlayer;
import edu.upc.epsevg.prop.bombardment.PlayerMove;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Jugador executat dins la mateixa JVM que el motor. Còmode per depurar des de l'IDE (punts d'aturada),
 * però NO pot aturar un jugador penjat: si no respon dins el marge de gràcia, la partida es perd i el fil
 * queda abandonat (i continua gastant CPU). Per als challenges i el torneig feu servir {@link IsolatedPlayer}.
 *
 * @author bernat
 */
public class InProcessPlayer implements PlayerHandle {

    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "timeout-timer");
        t.setDaemon(true);
        return t;
    });

    private final IPlayer player;
    private final long graceMs;

    public InProcessPlayer(IPlayer player, long graceMs) {
        this.player = player;
        this.graceMs = graceMs;
    }

    public IPlayer getPlayer() {
        return player;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean isHuman() {
        return !(player instanceof IAuto);
    }

    @Override
    public MoveOutcome requestMove(GameStatus s, long timeMs) {
        GameStatus copy = new GameStatus(s);
        long t0 = System.nanoTime();
        CompletableFuture<PlayerMove> f = new CompletableFuture<>();
        Thread worker = new Thread(() -> {
            try {
                f.complete(player.move(copy));
            } catch (Throwable t) {
                f.completeExceptionally(t);
            }
        }, "player-" + player.getName());
        worker.setDaemon(true);
        worker.start();
        ScheduledFuture<?> timer = null;
        if (timeMs > 0) {
            timer = TIMER.schedule(() -> {
                if (!f.isDone()) {
                    player.timeout();
                }
            }, timeMs, TimeUnit.MILLISECONDS);
        }
        try {
            PlayerMove m = timeMs > 0 ? f.get(timeMs + graceMs, TimeUnit.MILLISECONDS) : f.get();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (m == null || m.getMove() == null) {
                return MoveOutcome.failure(MoveOutcome.Status.ERROR, ms, "jugada nul·la");
            }
            return MoveOutcome.ok(m, ms);
        } catch (TimeoutException e) {
            return MoveOutcome.failure(MoveOutcome.Status.HUNG, (System.nanoTime() - t0) / 1_000_000,
                    "no ha respost " + graceMs + " ms després de timeout()");
        } catch (ExecutionException e) {
            e.getCause().printStackTrace();
            return MoveOutcome.failure(MoveOutcome.Status.ERROR, (System.nanoTime() - t0) / 1_000_000,
                    String.valueOf(e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return MoveOutcome.failure(MoveOutcome.Status.ERROR, 0, "interromput");
        } finally {
            if (timer != null) {
                timer.cancel(false);
            }
        }
    }

    @Override
    public void close() {
        // res
    }
}
