package fr.jrds.SmiExtensions;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;

import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.utils.LogUtils;

public class IndexTest {

    private static final LogAdapter logger = LogAdapter.getLogger(IndexTest.class);

    private static final MibTree resolver = new MibTree();

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(logger, LogLevel.TRACE);
        SNMP4JSettings.setOIDTextFormat(new OIDFormatter(resolver));

    }

    public void check(OID trap, String expected) {
        Object[] parsed = resolver.parseIndexOID(trap.getValue());
        //System.out.println(Arrays.toString(parsed));
        logger.warn("%s %s", trap, parsed);
        Assert.assertEquals(expected, Arrays.toString(parsed));
    }

    @Test
    public void testMSA2040() throws InterruptedException, IOException {
        OID trap = new OID(resolver.getFromName("experimental"));
        trap.append("94.1.11.1.9.80.12.15.241.185.69.32.0.0.0.0.0.0.0.0.0.2436");
        //0x50 = 80
        check(trap, "[connUnitEventDescr, 50:0c:0f:f1:b9:45:20:00:00:00:00:00:00:00:00:00, 2436]");
    }

    @Test
    public void testLldp() {
        OID trap = new OID(resolver.getFromName("lldpRemPortDesc"));
        trap.append("38400.3.1");
        check(trap, "[lldpRemPortDesc, 384.0, 3, 1]");
    }

    @Test
    public void fromFAQ() {
        OID vacmAccessContextMatch = new OID("1.3.6.1.6.3.16.1.4.1.4.7.118.51.103.114.111.117.112.0.3.1");
        System.out.println(vacmAccessContextMatch.toString());

        Assert.assertEquals("vacmAccessContextMatch[v3group][][3][noAuthNoPriv(1)]", vacmAccessContextMatch.toString());
    }

}
