package fr.jrds.SmiExtensions;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.utils.LogUtils;


public class LoadTest {

    private static final LogAdapter logger = LogAdapter.getLogger(LoadTest.class);

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(logger, LogLevel.TRACE);
    }

    @Test
    public void testDefaultLoad() {
        MibTree resolver = new MibTree();
        Assert.assertEquals("std", resolver.getInfos("std").name);
        Assert.assertEquals("ipOutDiscards", resolver.getInfos("ipOutDiscards").name);
    }
    
    @Test
    public void testEmptyLoad() throws IOException {
        MibTree resolver = new MibTree(true);
        resolver.load(getClass().getClassLoader().getResourceAsStream("custommibs.txt"));
        Assert.assertEquals("testprivate", resolver.getInfos("testprivate").name);
    }

    @Test
    public void testCustomLoad() throws IOException {
        MibTree resolver = new MibTree(false);
        resolver.load(getClass().getClassLoader().getResourceAsStream("custommibs.txt"));
        Assert.assertEquals("std", resolver.getInfos("std").name);
        Assert.assertEquals("ipOutDiscards", resolver.getInfos("ipOutDiscards").name);
        Assert.assertEquals("testprivate", resolver.getInfos("testprivate").name);
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
