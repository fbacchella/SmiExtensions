package fr.jrds.SMI4J;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map.Entry;

import fr.jrds.SMI4J.utils.NaturalOrderComparator;

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
    
    @Test
    public void main() {
        OidTreeNode top = new OidTreeNode();

        SortedMap<String, String> oids = new TreeMap<String, String>(new NaturalOrderComparator());

        InputStream in = OidTreeNode.class.getClassLoader().getResourceAsStream("oid.properties");
        Properties p = new Properties();
        try {
            p.load(in);
            for(Entry<Object, Object> e: p.entrySet()) {
                oids.put((String) e.getKey(), (String) e.getValue());
            }
            for(Entry<String, String> e: oids.entrySet()) {
                //top.addOID(e.getKey(), e.getValue());
            }
        } catch (IOException e) {
        }
        //Enumeration<OidTreeNode>  i = top.depthFirstEnumeration();
        //while(i.hasMoreElements()) {
        //    System.out.println(i.nextElement());
        //}
    }

}
