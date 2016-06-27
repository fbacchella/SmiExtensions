package fr.jrds.SmiExtensions;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.Counter64;

import fr.jrds.SmiExtensions.Utils.UnsignedLong;

public class UnsignedTest {

    @Test
    public void one() {
        Counter64 v = new Counter64(-1);
        UnsignedLong n = Utils.getUnsigned(v.getValue());
        Assert.assertEquals("18446744073709551615",n.toString());
        Assert.assertEquals(1,n.compareTo(Long.MAX_VALUE));
        
    }
}
