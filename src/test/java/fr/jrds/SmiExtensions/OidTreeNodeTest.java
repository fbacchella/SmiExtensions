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
        top.add(new ObjectInfos(new int[] {1, 2}, "member-body"));
        Assert.assertEquals(top.search(new int[] {1,2}).toString(), "1.2:member-body");
        Assert.assertEquals(top.search(new int[] {2}), null);
        Assert.assertEquals(top.search(new int[] {1,3}).toString(), "1:iso");
    }

}
