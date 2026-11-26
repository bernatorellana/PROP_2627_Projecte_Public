package edu.upc.epsevg.prop.bombardment.engine;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.PlayerType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enfrontament de moltes partides entre A i B: cada obertura es juga dos cops (A amb PLAYER1 i A amb
 * PLAYER2). Les partides es reparteixen entre {@code parallel} fils; cada fil té els seus propis processos
 * de jugador.
 *
 * @author bernat
 */
public class Match {

    /** Observador de progrés. */
    public interface Listener {
        void onGame(MatchResult partial, MatchResult.Game game);
    }

    private final PlayerSpec a, b;
    private final List<String> openings;
    private final long timeMs;
    private final int parallel;
    private final EngineOptions opt;
    private GameRunner.Listener gameListener;
    private java.util.function.BiConsumer<String, String> namesListener;

    public Match(PlayerSpec a, PlayerSpec b, List<String> openings, long timeMs, int parallel, EngineOptions opt) {
        this.a = a;
        this.b = b;
        this.openings = openings;
        this.timeMs = timeMs;
        this.parallel = Math.max(1, parallel);
        this.opt = opt;
    }

    /** Observador de les jugades de cada partida (només té sentit amb parallel = 1). */
    public void setGameListener(GameRunner.Listener l) {
        this.gameListener = l;
    }

    /** Es crida abans de cada partida amb els noms de blanques i negres. */
    public void setPlayerNamesListener(java.util.function.BiConsumer<String, String> l) {
        this.namesListener = l;
    }

    public MatchResult run(Listener l) throws InterruptedException {
        MatchResult res = new MatchResult(a.getLabel(), b.getLabel());
        ConcurrentLinkedQueue<int[]> queue = new ConcurrentLinkedQueue<>();
        int idx = 0;
        for (int o = 0; o < openings.size(); o++) {
            queue.add(new int[]{idx++, o, 0});   // A = PLAYER1
            queue.add(new int[]{idx++, o, 1});   // A = PLAYER2
        }
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        List<Thread> workers = new ArrayList<>();
        for (int w = 0; w < parallel; w++) {
            final int wid = w;
            Thread t = new Thread(() -> {
                try (PlayerHandle ha = a.open(opt, "A" + wid); PlayerHandle hb = b.open(opt, "B" + wid)) {
                    int[] job;
                    while ((job = queue.poll()) != null && failure.get() == null) {
                        GameStatus start = GameStatus.fromFen(openings.get(job[1]));
                        boolean aFirst = job[2] == 0;
                        if (namesListener != null) {
                            namesListener.accept(aFirst ? a.getLabel() : b.getLabel(), aFirst ? b.getLabel() : a.getLabel());
                        }
                        GameRecord rec = aFirst ? GameRunner.play(ha, hb, start, timeMs, gameListener)
                                : GameRunner.play(hb, ha, start, timeMs, gameListener);
                        synchronized (res) {
                            res.nameA = ha.getName();
                            res.nameB = hb.getName();
                        }
                        MatchResult.Game g = new MatchResult.Game(job[0], job[1],
                                aFirst ? PlayerType.PLAYER1 : PlayerType.PLAYER2, rec);
                        res.games.add(g);
                        if (l != null) {
                            synchronized (l) {
                                l.onGame(res, g);
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    failure.compareAndSet(null, e);
                }
            }, "match-worker-" + w);
            workers.add(t);
            t.start();
        }
        for (Thread t : workers) t.join();
        if (failure.get() != null) throw failure.get();
        res.games.sort((x, y) -> Integer.compare(x.index, y.index));
        return res;
    }
}
