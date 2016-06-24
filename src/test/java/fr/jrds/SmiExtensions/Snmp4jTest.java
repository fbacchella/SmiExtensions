package fr.jrds.SmiExtensions;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;

import fr.jrds.SMI4J.utils.LogUtils;
import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.OIDFormatter;
import fr.jrds.SmiExtensions.log.LogAdapter;

public class Snmp4jTest {
    
    private static final LogAdapter logger = LogAdapter.getLogger(Snmp4jTest.class);

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(logger, LogLevel.TRACE);
    }

    @Test
    public void testparseetree() throws InterruptedException, IOException {
        
        MibTree resolver = new MibTree();

        SNMP4JSettings.setOIDTextFormat(new OIDFormatter(resolver));

        
        Assert.assertEquals("std", new OID("std").format());
        Assert.assertEquals("dot1xPaeConformance", new OID("dot1xPaeConformance").format());
        Assert.assertEquals("cevPaPosswSm", new OID("cevPaPosswSm").format());
    }

}
