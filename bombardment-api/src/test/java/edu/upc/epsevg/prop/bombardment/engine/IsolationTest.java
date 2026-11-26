package edu.upc.epsevg.prop.bombardment.engine;

import static org.junit.jupiter.api.Assertions.*;

import edu.upc.epsevg.prop.bombardment.GameStatus;
import edu.upc.epsevg.prop.bombardment.PlayerType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El motor ha de sobreviure a tots els jugadors defectuosos i adjudicar la partida al rival. */
class IsolationTest {

    @ParameterizedTest
    @CsvSource({"HANG,HUNG", "SLOW,HUNG", "EXCEPTION,ERROR", "NULL,ERROR", "ILLEGAL,ILLEGAL", "EXIT,CRASHED"})
    void faultyPlayerLosesAndEngineSurvives(String mode, String expected) {
        EngineOptions opt = new EngineOptions();
        opt.graceMs = 300;
        opt.logDir = null;
        PlayerSpec bad = PlayerSpec.parse("edu.upc.epsevg.prop.bombardment.players.faulty.FaultyPlayer(" + mode + ",2)");
        PlayerSpec good = PlayerSpec.parse("random");
        try (PlayerHandle a = bad.open(opt, null); PlayerHandle b = good.open(opt, null)) {
            long t0 = System.currentTimeMillis();
            GameRecord r = GameRunner.play(a, b, new GameStatus(6), 200, null);
            assertEquals(GameRecord.End.valueOf(expected), r.end, r.detail);
            assertEquals(PlayerType.PLAYER2, r.winner);
            assertTrue(System.currentTimeMillis() - t0 < 15000);
            // el handle es recupera: torna a jugar una partida després de la mort del procés
            GameRecord r2 = GameRunner.play(b, a, new GameStatus(6), 200, null);
            assertNotNull(r2.end);
        }
    }
}
