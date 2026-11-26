package edu.upc.epsevg.prop.bombardment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameStatusTest {

    @Test
    void initialPosition() {
        GameStatus s = new GameStatus(8);
        assertEquals(16, s.getStoneCount(PlayerType.PLAYER1));
        assertEquals(16, s.getStoneCount(PlayerType.PLAYER2));
        assertEquals(PlayerType.PLAYER1, s.getCurrentPlayer());
        // 8 stones on row 1 with 3,3,..,2 steps = 22 steps, plus 16 explosions
        assertEquals(22 + 16, s.getMoves().size());
    }

    @Test
    void stepAndExplosion() {
        GameStatus s = GameStatus.fromFen("....../....../..B.../...W../....../...... w");
        s.movePiece(Move.explosion(3, 2));
        assertTrue(s.isDraw());
    }

    @Test
    void explosionWinsWhenOpponentWiped() {
        GameStatus s = GameStatus.fromFen("....../....../..B.../...W../....../.....W w");
        assertEquals(1, s.explosionBalance(3, 2) + 1); // removes 1 enemy and itself: balance 0
        s.movePiece(Move.explosion(3, 2));
        assertTrue(s.isGameOver());
        assertEquals(PlayerType.PLAYER1, s.getWinner());
    }

    @Test
    void goalRowWins() {
        GameStatus s = GameStatus.fromFen("....../...W../....../....../..B.../...... w");
        s.movePiece(Move.step(3, 4, 3, 5));
        assertTrue(s.isGameOver());
        assertEquals(PlayerType.PLAYER1, s.getWinner());
    }

    @Test
    void illegalMovesRejected() {
        GameStatus s = new GameStatus(6);
        assertThrows(IllegalArgumentException.class, () -> s.movePiece(Move.step(0, 1, 0, 3)));
        assertThrows(IllegalArgumentException.class, () -> s.movePiece(Move.step(0, 0, 0, 1))); // occupied
        assertThrows(IllegalArgumentException.class, () -> s.movePiece(Move.explosion(0, 5)));  // not mine
    }

    @Test
    void fenRoundTrip() {
        GameStatus s = new GameStatus(7);
        s.movePiece(Move.step(2, 1, 3, 2));
        assertEquals(s, GameStatus.fromFen(s.toFen()));
        assertEquals("c2-d3", Move.step(2, 1, 3, 2).toString());
        assertEquals(Move.explosion(4, 4), Move.parse("e5*"));
    }

    @Test
    void randomGamesTerminate() {
        java.util.Random r = new java.util.Random(1);
        for (int g = 0; g < 200; g++) {
            GameStatus s = new GameStatus(5 + g % 6);
            int plies = 0;
            while (!s.isGameOver()) {
                var ms = s.getMoves();
                s.movePiece(ms.get(r.nextInt(ms.size())));
                assertTrue(++plies < 1000);
            }
        }
    }
}
