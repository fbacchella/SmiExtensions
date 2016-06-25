package fr.jrds.SmiExtensions;

import java.io.IOException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.log.LogLevel;

import fr.jrds.SmiExtensions.utils.LogUtils;


public class LoadTest {

    @BeforeClass
    static public void configure() throws IOException {
        LogUtils.setLevel(LoadTest.class, LogLevel.TRACE, MibTree.class.getName());
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
    public void testDualLoad() throws IOException {
        MibTree resolver = new MibTree(true);
        resolver.load(getClass().getClassLoader().getResourceAsStream("smallmibs.txt"));
        resolver.load(getClass().getClassLoader().getResourceAsStream("custommibs.txt"));
        Assert.assertEquals("testprivate", resolver.getInfos("testprivate").name);
    }

    @Test
    public void testCustomLoad() throws IOException {
        MibTree resolver = new MibTree(false);
        resolver.load(getClass().getClassLoader().getResourceAsStream("custommibs.txt"));
        Assert.assertEquals("std", resolver.getInfos("std").name);
        Assert.assertEquals("ipOutDiscards", resolver.getInfos("ipOutDiscards").name);
        Assert.assertEquals("dot1xPaeConformance", resolver.getInfos("dot1xPaeConformance").name);
        Assert.assertEquals("testprivate", resolver.getInfos("testprivate").name);
    }

}
