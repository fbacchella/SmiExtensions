package fr.jrds.SmiExtensions.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;

import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.Utils;
import fr.jrds.SmiExtensions.log.LogAdapter;

/**
 * 
 * @author Fabrice Bacchella
 *
 */
public class ObjectInfos implements Comparable<OID>{

    private static final LogAdapter logger = LogAdapter.getLogger(Index.class);

    public enum Attribute {
        OID,
        NAME,
        INDEX,
        VALUES,
        TEXTCONT,
        SIZE,
        RANGE,
        TYPE,
    }

    static final private byte TAG1 = (byte) 0x9f;
    static final private byte TAG_FLOAT = (byte) 0x78;
    static final private byte TAG_DOUBLE = (byte) 0x79;

    public enum SnmpType {
        Opaque {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Opaque();
            }
            @Override
            protected Object convert(Variable v) {
                Opaque var = (Opaque) v;
                //If not resolved, we will return the data as an array of bytes
                Object value = var.getValue();
                try {
                    byte[] bytesArray = var.getValue();
                    ByteBuffer bais = ByteBuffer.wrap(bytesArray);
                    BERInputStream beris = new BERInputStream(bais);
                    byte t1 = bais.get();
                    byte t2 = bais.get();
                    int l = BER.decodeLength(beris);
                    if(t1 == TAG1) {
                        if(t2 == TAG_FLOAT && l == 4)
                            value = new Float(bais.getFloat());
                        else if(t2 == TAG_DOUBLE && l == 8)
                            value = new Double(bais.getDouble());
                    }
                } catch (IOException e) {
                    logger.error(var.toString());
                }
                return value;
            }
        },
        EnumVal {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Integer32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toInt();
            }
        },
        String {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.OctetString();
            }
            @Override
            protected Object convert(Variable v) {
                OctetString octetVar = (OctetString)v;
                //It might be a C string, try to remove the last 0;
                //But only if the new string is printable
                int length = octetVar.length();
                if(length > 1 && octetVar.get(length - 1) == 0) {
                    OctetString newVar = octetVar.substring(0, length - 1);
                    if(newVar.isPrintable()) {
                        v = newVar;
                        logger.debug("Convertion an octet stream from %s to %s", octetVar, v);
                    }
                }
                return v.toString();
            }
        },
        Unsigned {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.UnsignedInteger32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        Unsigned32 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.UnsignedInteger32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        // Net-Snmp create it from LMS-COMPONENT-MIB, as an alias for UInteger32
        UInteger {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.UnsignedInteger32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        BitString {
            @SuppressWarnings("deprecation")
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.BitString();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        IpAddr {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.IpAddress();
            }
            @Override
            protected Object convert(Variable v) {
                return ((IpAddress)v).getInetAddress();
            }
        },
        NetAddr {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.IpAddress();
            }
            @Override
            protected Object convert(Variable v) {
                return ((IpAddress)v).getInetAddress();
            }
        },
        ObjID {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.OID();
            }
            @Override
            protected Object convert(Variable v) {
                return v;
            }
        },
        INTEGER {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Integer32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toInt();
            }
        },
        Integer32 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Integer32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toInt();
            }
        },
        Counter {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Counter32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        Counter64 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Counter64();
            }
            @Override
            protected Object convert(Variable v) {
                return Utils.getUnsigned(v.toLong());
            }
        },
        Gauge {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Gauge32();
            }
            @Override
            protected Object convert(Variable v) {
                return v.toLong();
            }
        },
        TimeTicks {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.TimeTicks();
            }
            @Override
            protected Object convert(Variable v) {
                return new Double(1.0 * ((TimeTicks)v).toMilliseconds() / 1000.0);
            }
        },
        ;
        protected abstract Variable getVariable();
        protected abstract Object convert(Variable v);
        public Object make(int[] in){
            Variable v = getVariable();
            OID oid = new OID(in);
            v.fromSubIndex(oid, true);
            return convert(v);
        };
    }

    final int[] oidElements;
    final String name;
    final Index index;
    final EnumVal values;
    final String textcont;
    final Size size;
    final String range;
    final SnmpType type;

    public ObjectInfos(MibTree tree, Map<Attribute, String> attr) {
        oidElements = attr.containsKey(Attribute.OID) ? Arrays.stream(attr.get(Attribute.OID).split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray() : null;
        name = attr.get(Attribute.NAME);
        index = attr.containsKey(Attribute.INDEX) ? new Index(tree, attr.get(Attribute.INDEX)) : null;
        values = attr.containsKey(Attribute.VALUES) ? new EnumVal(attr.get(Attribute.VALUES)) : null;
        textcont = attr.get(Attribute.TEXTCONT);
        size = attr.containsKey(Attribute.SIZE) ? new Size(attr.get(Attribute.SIZE)) : null;
        range = attr.get(Attribute.RANGE);
        type = attr.containsKey(Attribute.TYPE) ? SnmpType.valueOf(attr.get(Attribute.TYPE)) : null;
    }

    public ObjectInfos(int[] oidElements, String name) {
        this.oidElements = Arrays.copyOf(oidElements, oidElements.length);
        this.name = name;
        index = null;
        values = null;
        textcont = null;
        size = null;
        range = null;
        type = null;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(oidElements);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof ObjectInfos)) {
            return false;
        }
        ObjectInfos other = (ObjectInfos) obj;
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
