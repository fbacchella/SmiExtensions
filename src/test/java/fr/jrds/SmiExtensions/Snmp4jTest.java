package fr.jrds.SmiExtensions;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;

import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.OIDFormatter;
import fr.jrds.SmiExtensions.utils.LogUtils;

public class Snmp4jTest {

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(Snmp4jTest.class, LogLevel.TRACE, MibTree.class.getName());
    }

    @Test
    public void testparseetree() throws InterruptedException, IOException {
        
        MibTree resolver = new MibTree();
        resolver.load(getClass().getClassLoader().getResourceAsStream("custommibs.txt"));
        Assert.assertEquals("dot1xPaeConformance", resolver.getInfos("dot1xPaeConformance").name);

        SNMP4JSettings.setOIDTextFormat(new OIDFormatter(resolver));
        
        Assert.assertEquals("std", new OID("std").format());
        Assert.assertEquals("dot1xPaeConformance", new OID("dot1xPaeConformance").format());
        Assert.assertEquals("cevPaPosswSm", new OID("cevPaPosswSm").format());
        Assert.assertEquals("testprivate", new OID("testprivate").format());
    }

}
