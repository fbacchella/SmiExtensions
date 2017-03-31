package fr.jrds.smiextensions.objects;

import java.util.Arrays;
import java.util.Map;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import fr.jrds.smiextensions.MibTree;

/**
 * Store the OID's associated info, parsed from a net-snmp mib tree
 * @author Fabrice Bacchella
 *
 */
public class OidInfos implements Comparable<OID>{

    public enum Attribute {
        OID,
        NAME,
        INDEX,
        VALUES,
        TEXTCONT,
        SIZE,
        RANGE,
        TYPE,
        TRAP,
    }

    final int[] oidElements;
    public  final String name;
    public final Index index;
    public final EnumVal values;
    public final String textcont;
    public final Size size;
    public final String range;
    public final SnmpType type;
    private final MibTree tree;

    public OidInfos(MibTree tree, Map<Attribute, String> attr) {
        oidElements = attr.containsKey(Attribute.OID) ? Arrays.stream(attr.get(Attribute.OID).split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray() : null;
        name = attr.get(Attribute.NAME);
        index = attr.containsKey(Attribute.INDEX) ? new Index(tree, attr.get(Attribute.INDEX)) : null;
        values = attr.containsKey(Attribute.VALUES) ? new EnumVal(attr.get(Attribute.VALUES)) : null;
        textcont = attr.get(Attribute.TEXTCONT);
        size = attr.containsKey(Attribute.SIZE) ? new Size(attr.get(Attribute.SIZE)) : null;
        range = attr.get(Attribute.RANGE);
        type = attr.containsKey(Attribute.TYPE) ? SnmpType.valueOf(attr.get(Attribute.TYPE)) : null;
        this.tree = tree;
    }

    public OidInfos(int[] oidElements, String name) {
        this.oidElements = Arrays.copyOf(oidElements, oidElements.length);
        this.name = name;
        index = null;
        values = null;
        textcont = null;
        size = null;
        range = null;
        type = null;
        tree = null;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("{");
        buffer.append(oidElements != null ? String.format("oid=%s, ", Arrays.toString(oidElements)): "");
        buffer.append(name != null ? String.format("name=%s, ", name): "");
        buffer.append(index != null ? String.format("index=%s, ", index): "");
        buffer.append(values != null ? String.format("values=%s, ", values): "");
        buffer.append(textcont != null ? String.format("textcont=%s, ", textcont): "");
        buffer.append(size != null ? String.format("size=%s, ", size): "");
        buffer.append(range != null ? String.format("range=%s, ", range): "");
        buffer.append(type != null ? String.format("type=%s, ", type): "");
        buffer.delete(buffer.length() -2, buffer.length());
        buffer.append("}");
        return buffer.toString();
    }

    public Parsed extract(int[] oidElements) {
        return size != null ? size.extract(oidElements) : null;
    }

    public Object[] resolve(int[] oid) {
        return index != null ? index.resolve(oid) : null;
    }

    public String getName() {
        return name;
    }

    public boolean isIndex() {
        return index != null;
    }

    public OID getOID() {
        return oidElements != null ? new OID(oidElements) : null;
    }

    public int[] getOidElements() {
        return oidElements != null ? Arrays.copyOf(oidElements, oidElements.length) : null;
    }

    public boolean oidEquals(int[] other) {
        return other != null && Arrays.equals(oidElements, other);
    }

    public Variable getVariable(String text) {
        TextualConvention tc = tree.getTextualConvention(textcont);
        if (tc != null) {
            return tc.parse(text);
        } else {
            return type.parse(this, text);
        }
    }

    public String format(Variable v) {
        TextualConvention tc = tree.getTextualConvention(textcont);
        if (tc != null) {
            return tc.format(v);
        } else {
            return type.format(this, v);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(oidElements);
        return result;
    }

    /**
     * Two OidInfos are equals if they share the same name and path.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof OidInfos)) {
            return false;
        }
        OidInfos other = (OidInfos) obj;
        return name.equals(other.name) && Arrays.equals(oidElements, other.oidElements);
    }

    /**
     * Compare the found OID to another OID.
     * Its semantics is similar to java.lang.Comparable#compareTo(java.lang.Object) and org.snmp4j.smi.OID#compareTo(org.snmp4j.smi.Variable), but with added signification.
     * The absolute number is the position of the first different element. Counting start from 1 because 0 means equality. So it can be
     * higher than the last element of shortest OID.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @see org.snmp4j.smi.OID#compareTo(org.snmp4j.smi.Variable)
     * 
     * @param oid - OID to compare to.
     * @return a negative integer, zero, or a positive integer as common path from OID and the compare OID is shortest, equals or longer than the given OID.
     */
    @Override
    public int compareTo(OID oid) {
        if(oid == null) {
            throw new NullPointerException("empty OID");
        }
        int i;
        int end = Math.max(oidElements.length, oid.size());
        for(i = 0; i < end; i++) {
            if(i >= oid.size()) {
                return i +1 ;
            } else if(i >= oidElements.length) {
                return -i - 1;
            } else if (oidElements[i] != oid.get(i)) {
                return i + 1;
            }
        }
        return 0;
    }

}
