package fr.jrds.SmiExtensions.objects;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.TimeTicks;

public class SnmpTypeTest {

    @Test
    public void testTimeTicks() {
        // 0x100000000L = 2^32-1, biggest allowed time ticks
        for (long i = 1 ; i <= (0x100000000L - 1); i *=2 ) {
            TimeTicks tt = new TimeTicks(i);
            TimeTicks newtt = (TimeTicks) SnmpType.TimeTicks.parse(null, tt.toString());
            Assert.assertEquals(tt.toMilliseconds(), newtt.toMilliseconds());
        }
    }

}
