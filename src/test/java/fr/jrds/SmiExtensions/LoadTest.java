package fr.jrds.SmiExtensions;

import java.io.IOException;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import fr.jrds.SMI4J.utils.LogUtils;
import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.log.LogAdapter;


public class LoadTest {

    private static final LogAdapter logger = LogAdapter.getLogger(LoadTest.class);

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(logger, LogLevel.TRACE, "loghub.SmartContext", "loghub.receivers.SnmpTrap", "loghub.Receiver", "loghub.SMIResolve");
    }

    @Test
    public void testparseetree() throws InterruptedException, IOException {
        MibTree resolver = new MibTree();
        OID trap = new OID(resolver.getFromName("experimental"));
        trap.append("94.1.11.1.9.80.12.15.241.185.69.32.0.0.0.0.0.0.0.0.0.2436");
        Variable[] parsed = resolver.parseIndexOID(trap.getValue());
        System.out.println(Arrays.toString(parsed));
        logger.warn("%s %s", trap, parsed);

        trap = new OID(resolver.getFromName("lldpRemPortDesc"));
        trap.append("38400.3.1");
        parsed = resolver.parseIndexOID(trap.getValue());
        System.out.println(Arrays.toString(parsed));
        logger.error("%s %s", trap, parsed);
    }

}
