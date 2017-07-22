package jagoclient.igs.users;

import static org.junit.Assert.assertEquals;

import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion;
import org.junit.Test;

/**
 * Created by johndaniels on 7/21/17.
 */
public class UserInfoTest {
    @Test
    public void testParse() {
        String line = "    mk4155  <none>          Japan     1d* 3439/3487  -   -   20s    Q- default  0 - T BWN 0-3 19-19 60-60 600-600 25-25 0-0 0-0 0-0";
        UserInfo info = new UserInfo(line);
        assertEquals("mk4155", info.getUsername());
        assertEquals("<none>", info.getInfoString());
        assertEquals("Japan", info.getCountry());
        assertEquals("1d*", info.getRank());
        assertEquals(36, info.getRankValue());
        assertEquals(3439, info.getWins());
        assertEquals(3487, info.getLosses());
    }

    @Test
    public void testParseZeros() {
        String line = "    guest8                  --        NR    0/   0 592  -    1m    -X default  0 - F";
        UserInfo info = new UserInfo(line);
        assertEquals(0, info.getWins());
    }

    @Test
    public void testAsteriskRank() {
        String line = " keimanofd  �ޗǎs          Japan     8k* 2304/2350  -   -   32s    QX default  0 - T";
        UserInfo info = new UserInfo(line);
        assertEquals(2304, info.getWins());
    }

    @Test
    public void testUnicode() {
        String line = "  Jirochou  ���ނ̌�D��    Japan     1k* 5902/5765  -   -    4m    Q- default  0 - T BWN 0-0 19-19 60-60 600-600 25-25 0-0 0-0 0-0";
        UserInfo info = new UserInfo(line);
        assertEquals(5902, info.getWins());
    }
}
