package fr.jrds.SmiExtensions;

import org.junit.Assert;
import org.junit.Test;

public class OidTreeNodeTest {

    @Test
    public void manualfill() {
        OidTreeNode top = new OidTreeNode();
        top.add(new ObjectInfos(new int[] {1}, "iso"));
        top.add(new ObjectInfos(new int[] {1, 1}, "std"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802}, "iso8802"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802, 1}, "ieee802dot1"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802, 1}, "ieee802dot1"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802, 1, 1}, "ieee802dot1mibs"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802, 1, 1, 1}, "ieee8021paeMIB"));
        top.add(new ObjectInfos(new int[] {1, 1, 8802, 1, 1, 1, 2}, "dot1xPaeConformance"));
        top.add(new ObjectInfos(new int[] {1, 2}, "member-body"));
        Assert.assertEquals("1.2:member-body", top.search(new int[] {1,2}).toString());
        Assert.assertEquals(null, top.search(new int[] {2}));
        Assert.assertEquals("1:iso", top.search(new int[] {1,3}).toString());
        Assert.assertEquals("1.1.8802.1.1.1.2:dot1xPaeConformance", top.search(new int[] {1, 1, 8802, 1, 1, 1, 2}).toString());
    }

}
