package jagoclient.igs.games;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GameInfoTest {

    @Test
    public void testGameInfoParse() {
        GameInfo info = new GameInfo("[797]      joy751 [ 3k*] vs.      ut0915 [ 4k*] (196   19  0 -5.5 60/900/25/0/0/0  I) (  1) 0 - - - - - -");
        assertEquals(797, info.getGameNumber());
        assertEquals("joy751", info.getWhitePlayer());
        assertEquals("3k*", info.getWhiteRank());
        assertEquals("ut0915", info.getBlackPlayer());
        assertEquals("4k*", info.getBlackRank());
        assertEquals(196, info.getMove());
        assertEquals(19, info.getSize());
        assertEquals(0, info.getHandicap());
        assertEquals(-6, info.getKomi());
        assertEquals(60, info.getInitialTime());
        assertEquals(900, info.getByoYomi());
        assertEquals(25, info.getByoYomiMoves());
    }

    @Test
    public void testWeirdSpaces() {
        GameInfo gameInfo = new GameInfo("[390]      weixng [ 4d*] vs.  floatcloud [ 4d*] ( 97   19  0  6.5 60/720/25/0/0/0  I) (  2) 0 - - - - - -");
        assertEquals(97, gameInfo.getMove());
    }
}
