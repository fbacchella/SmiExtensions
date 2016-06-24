package fr.jrds.SMI4J;

import java.util.Arrays;
import java.util.Map;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.Variable;

import fr.jrds.SMI4J.types.EnumVal;
import fr.jrds.SMI4J.types.Index;
import fr.jrds.SMI4J.types.Size;

public class ObjectInfos {
    enum Attribute {
        OID,
        NAME,
        INDEX,
        VALUES,
        TEXTCONT,
        SIZE,
        RANGE,
        TYPE,
    }

    public enum SnmpType {
        Opaque {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Opaque();
            }
        },
        EnumVal {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Opaque();
            }
        },
        String {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.OctetString();
            }
        },
        Unsigned {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.UnsignedInteger32();
            }
        },
        Unsigned32 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.UnsignedInteger32();
            }
        },
        BitString {
            @SuppressWarnings("deprecation")
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.BitString();
            }
        },
        IpAddr {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.IpAddress();
            }
        },
        NetAddr {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.IpAddress();
            }
        },
        ObjID {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.OID();
            }
        },
        INTEGER {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Integer32();
            }
        },
        Integer32 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Integer32();
            }
        },
        Counter {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Counter32();
            }
        },
        Counter64 {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Counter64();
            }
        },
        Gauge {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.Gauge32();
            }
        },
        TimeTicks {
            @Override
            protected Variable getVariable() {
                return new org.snmp4j.smi.TimeTicks();
            }
        },
        ;
        protected abstract Variable getVariable();
        public Variable make(int[] in){
            Variable v = getVariable();
            OID oid = new OID(in);
            v.fromSubIndex(oid, true);
            return v;
        };
    }

    public final int[] oidElements;
    public final String name;
    public final Index index;
    public final EnumVal values;
    public final String textcont;
    public final Size size;
    public final String range;
    public final SnmpType type;
    public ObjectInfos(Map<Attribute, Object> attr) {
        oidElements = (int[]) attr.get(Attribute.OID);
        name = (String) attr.get(Attribute.NAME);
        index = (Index) attr.get(Attribute.INDEX);
        values = (EnumVal) attr.get(Attribute.VALUES);
        textcont = (String) attr.get(Attribute.TEXTCONT);
        size = (Size) attr.get(Attribute.SIZE);
        range = (String) attr.get(Attribute.RANGE);
        type = (SnmpType) attr.get(Attribute.TYPE);
    }

    ObjectInfos(int[] oidElements, String name) {
        this.oidElements = oidElements;
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

}
