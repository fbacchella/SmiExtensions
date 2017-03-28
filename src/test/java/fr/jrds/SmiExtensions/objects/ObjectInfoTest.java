package fr.jrds.SmiExtensions.objects;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.OID;

public class ObjectInfoTest {

    @Test
    public void testCompare() {
        OidInfos oi = new OidInfos(new int[] {1, 2, 3, 4}, "test");
        Assert.assertEquals(3, oi.compareTo(new OID(new int[] {1, 2})));
        Assert.assertEquals(0, oi.compareTo(new OID(new int[] {1, 2, 3, 4})));
        Assert.assertEquals(-5, oi.compareTo(new OID(new int[] {1, 2, 3, 4, 5, 6})));
        Assert.assertEquals(2, oi.compareTo(new OID(new int[] {1, 0, 3, 4, 5, 6})));
    }
}
