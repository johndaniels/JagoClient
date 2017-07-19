package jagoclient.igs.who;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Created by johndaniels on 7/18/17.
 */
public class WhoObjectTest {
    @Test
    public void testParse() {
        WhoObject who = new WhoObject(" QX --   -- isfadm02   16s     2k  ", false);
        assertEquals(false, who.looking());
        assertEquals(true, who.quiet());
        assertEquals(true, who.silent());
        assertEquals("isfadm02", who.Name);
        assertEquals(36, who.rankValue);
    }

    @Test
    public void testParseMore() {
        WhoObject who = new WhoObject("  ! --   -- LeArlequin  0s    12k* ", false);
        assertEquals(true, who.looking());
        assertEquals(false, who.quiet());
        assertEquals(false, who.silent());
        assertEquals("LeArlequin", who.Name);
        assertEquals(16, who.rankValue);

    }
}
