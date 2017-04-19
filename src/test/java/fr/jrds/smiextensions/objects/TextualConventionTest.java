package fr.jrds.smiextensions.objects;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

public class TextualConventionTest {
    
    @Test
    public void test1() {
        Map<String, TextualConvention> annotations = new HashMap<>();
        
        TextualConvention.addAnnotation("T11FabricIndex", "d", annotations);
        Variable v = new OctetString(new byte[]{10});
        Assert.assertEquals("10", annotations.get("T11FabricIndex").format(v));
    }

    @Test
    public void test2() {
        Map<String, TextualConvention> annotations = new HashMap<>();
        
        TextualConvention.addAnnotation("T11ZsZoneMemberType", "x", annotations);
        Variable v = new OctetString(new byte[]{10});
        Assert.assertEquals("a", annotations.get("T11ZsZoneMemberType").format(v));
    }

    @Test
    public void test3() {
        Map<String, TextualConvention> annotations = new HashMap<>();
        
        TextualConvention.addAnnotation("Ipv6AddressPrefix", "2x", annotations);
        Variable v = new OctetString(new byte[]{(byte)255,(byte)255});
        Assert.assertEquals("ffff", annotations.get("Ipv6AddressPrefix").format(v));
    }

    @Test
    public void test4() {
        Map<String, TextualConvention> annotations = new HashMap<>();
        
        TextualConvention.addAnnotation("MplsLdpIdentifier", "1d.1d.1d.1d:2d:", annotations);
        Variable v = new OctetString(new byte[]{(byte)1,(byte)2,(byte)3,(byte)4,(byte)5,(byte)6});
        Assert.assertEquals("1.2.3.4:1286:", annotations.get("MplsLdpIdentifier").format(v));
    }

}
