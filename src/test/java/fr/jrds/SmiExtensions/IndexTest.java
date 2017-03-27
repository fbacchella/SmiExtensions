package fr.jrds.SmiExtensions;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.utils.LogUtils;

public class IndexTest {

    private static LogAdapter logger;

    private static final MibTree resolver = new MibTree();

    @BeforeClass
    static public void configure() throws IOException {
        logger = LogUtils.setLevel(IndexTest.class, LogLevel.TRACE);
        OIDFormatter formatter = new OIDFormatter(resolver);
        SNMP4JSettings.setOIDTextFormat(formatter);
        SNMP4JSettings.setVariableTextFormat(formatter);

    }

    public void check(OID trap, String expected) {
        Object[] parsed = resolver.parseIndexOID(trap.getValue());
        logger.warn("trap %s resovled as %s", trap.getValue(), parsed);
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
    public void oidfromFAQ() {
        OID vacmAccessContextMatch = new OID("1.3.6.1.6.3.16.1.4.1.4.7.118.51.103.114.111.117.112.0.3.1");
        Assert.assertEquals("vacmAccessContextMatch[v3group][][3][noAuthNoPriv(1)]", vacmAccessContextMatch.toString());
        Arrays.stream(resolver.parseIndexOID(vacmAccessContextMatch)).forEach( i-> System.out.println("'" + i + "' " + i.getClass()));
        logger.debug(Arrays.toString(resolver.parseIndexOID(vacmAccessContextMatch)));
    }

    @Test
    public void varfromFAQ() throws ParseException {
        OID ifAdminStatus = new OID("ifAdminStatus");
        ifAdminStatus= ifAdminStatus.append(4);
        VariableBinding vbEnum = new VariableBinding(ifAdminStatus, "down(2)");
        Assert.assertEquals(new VariableBinding(new OID(new int[] { 1,3,6,1,2,1,2,2,1,7,4 }), new Integer32(2)), vbEnum);
        Assert.assertEquals("down(2)", vbEnum.toValueString());
        OID nlmLogDateAndTime = new OID("nlmLogDateAndTime");
        nlmLogDateAndTime.append(1);
        VariableBinding vbDateAndTime = new VariableBinding(nlmLogDateAndTime,"2015-10-13,12:45:53.8,+2:0");
        Assert.assertEquals(new VariableBinding(new OID(new int[] { 1,3,6,1,2,1,92,1,3,1,1,3,1 }), OctetString.fromHexString("07:df:0a:0d:0c:2d:35:08:2b:02:00")), vbDateAndTime);
    }

}
