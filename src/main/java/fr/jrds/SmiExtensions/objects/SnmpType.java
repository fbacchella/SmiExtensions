package fr.jrds.SmiExtensions.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.Variable;

import fr.jrds.SmiExtensions.Utils;
import fr.jrds.SmiExtensions.log.LogAdapter;

/**
 * A enumeration of Snmp types to help conversion and parsing.
 * @author Fabrice Bacchella
 *
 */
public enum SnmpType {

    /**
     * This can also manage the special float type as defined by Net-SNMP. But it don't parse float.
     * @author Fabrice Bacchella
     */
    Opaque {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Opaque();
        }
        @Override
        public Object convert(Variable v) {
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
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Opaque(text.getBytes());
        }
    },
    /**
     * This type can use the context OidInfos to format numerical value to string and parse them.
     * @author Fabrice Bacchella
     *
     */
    EnumVal {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Integer32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toInt();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return java.lang.String.format("%s(%d)", oi.values.resolve(v.toInt()), v.toInt());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            Matcher m = VARIABLEPATTERN.matcher(text);
            m.find();
            String numval = m.group("num");
            String textval = m.group("text");
            if( numval != null && oi.values.resolve(Integer.parseInt(numval)) !=null) {
                return new Integer32(Integer.parseInt(numval));
            } else if (textval != null && oi.values.resolve(textval) != null){
                return new Integer32(oi.values.resolve(textval));
            } else {
                return null;
            }
        }
    },
    String {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.OctetString();
        }
        @Override
        public Object convert(Variable v) {
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
        @Override
        public String format(OidInfos oi, Variable v) {
            return v.toString();
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return OctetString.fromByteArray(text.getBytes());
        }
    },
    Unsigned {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.UnsignedInteger32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
    },
    Unsigned32 {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.UnsignedInteger32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
    },
    /**
     * Net-Snmp create it from LMS-COMPONENT-MIB, as an alias for UInteger32
     * @author Fabrice Bacchella
     *
     */
    UInteger {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.UnsignedInteger32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
    },
    BitString {
        @SuppressWarnings("deprecation")
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.BitString();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.IpAddress} variable.</li>
     * <li>{@link #convert(Variable)} return a {@link java.net.InetAddress}.</li>
     * <li>{@link #format(OidInfos, Variable)} try to resolve the hostname associated with the IP address.</li>
     * <li>{@link #parse(OidInfos, String)} parse the string as an hostname or a IP address.</li>
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    IpAddr {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.IpAddress();
        }
        @Override
        public Object convert(Variable v) {
            return ((IpAddress)v).getInetAddress();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            IpAddress ip = (IpAddress) v;
            return ip.getInetAddress().getHostName();
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.IpAddress(text);
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.IpAddress} variable.</li>
     * <li>{@link #convert(Variable)} return a {@link java.net.InetAddress}.</li>
     * <li>{@link #format(OidInfos, Variable)} try to resolve the hostname associated with the IP address.</li>
     * <li>{@link #parse(OidInfos, String)} parse the string as an hostname or a IP address.</li>
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    NetAddr {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.IpAddress();
        }
        @Override
        public Object convert(Variable v) {
            return ((IpAddress)v).getInetAddress();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            IpAddress ip = (IpAddress) v;
            return ip.getInetAddress().getHostName();
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.IpAddress(text);
        }
    },
    ObjID {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.OID();
        }
        @Override
        public Object convert(Variable v) {
            return v;
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return ((OID)v).format();
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new OID(text);
        }
    },
    INTEGER {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Integer32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toInt();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return java.lang.String.valueOf(v.toInt());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Integer32(Integer.getInteger(text));
        }
    },
    Integer32 {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Integer32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toInt();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return java.lang.String.valueOf(v.toInt());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Integer32(Integer.getInteger(text));
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.Counter32} variable.</li>
     * <li>{@link #convert(Variable)} return the value stored in a {@link java.lang.Long}.</li>
     * <li>{@link #parse(OidInfos, String)} parse the string as a long value.</li>
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    Counter {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Counter32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return java.lang.String.valueOf(v.toLong());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Counter32(Long.getLong(text));
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.Counter64} variable.</li>
     * <li>{@link #convert(Variable)} return the value stored in a {@link fr.jrds.SmiExtensions.Utils.UnsignedLong}.</li>
     * <li>{@link #parse(OidInfos, String)} parse the string as a long value.</li>
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    Counter64 {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Counter64();
        }
        @Override
        public Object convert(Variable v) {
            return Utils.getUnsigned(v.toLong());
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return Long.toUnsignedString(v.toLong());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Counter64(Long.getLong(text));
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.Gauge32} variable.</li>
     * <li>{@link #convert(Variable)} return the value stored in a Long.</li>
     * <li>{@link #parse(OidInfos, String)} parse the string as a long value.</li>
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    Gauge {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.Gauge32();
        }
        @Override
        public Object convert(Variable v) {
            return v.toLong();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return java.lang.String.valueOf(v.toLong());
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            return new org.snmp4j.smi.Gauge32(Long.getLong(text));
        }
    },
    /**
     * <ul>
     * <li>{@link #getVariable()} return an empty {@link org.snmp4j.smi.TimeTicks} variable.</li>
     * <li>{@link #convert(Variable)} return the time ticks as a number of milliseconds stored in a Long</li>
     * <li>{@link #format(OidInfos, Variable)} format the value using {@link org.snmp4j.smi.TimeTicks#toString()}
     * <li>{@link #parse(OidInfos, String)} can parse a number, expressing timeticks or the result of {@link org.snmp4j.smi.TimeTicks#toString()}
     * </ul>
     * @author Fabrice Bacchella
     *
     */
    TimeTicks {
        @Override
        public Variable getVariable() {
            return new org.snmp4j.smi.TimeTicks();
        }
        @Override
        public Object convert(Variable v) {
            return ((TimeTicks)v).toMilliseconds();
        }
        @Override
        public String format(OidInfos oi, Variable v) {
            return v.toString();
        }
        @Override
        public Variable parse(OidInfos oi, String text) {
            try {
                long duration = Long.parseLong(text);
                return new org.snmp4j.smi.TimeTicks(duration);
            } catch (NumberFormatException e) {
                Matcher m = TimeTicksPattern.matcher(text);
                if (m.matches()) {
                    String days = m.group("days") != null ? m.group("days") : "0";
                    String hours = m.group("hours");
                    String minutes = m.group("minutes");
                    String seconds = m.group("seconds");
                    String fraction = m.group("fraction");
                    String formatted = java.lang.String.format("P%sDT%sH%sM%s.%sS", days, hours, minutes,seconds, fraction);
                    TimeTicks tt = new TimeTicks();
                    tt.fromMilliseconds(Duration.parse(formatted).toMillis());
                    return tt;
                } else {
                    return new org.snmp4j.smi.Null();
                }
            }
        }
    },
    ;

    // Used to parse time ticks
    static final private Pattern TimeTicksPattern = Pattern.compile("(?:(?<days>\\d+) days?, )?(?<hours>\\d+):(?<minutes>\\d+):(?<seconds>\\d+)(?:\\.(?<fraction>\\d+))?");

    static final private LogAdapter logger = LogAdapter.getLogger(SnmpType.class);

    static final private byte TAG1 = (byte) 0x9f;
    static final private byte TAG_FLOAT = (byte) 0x78;
    static final private byte TAG_DOUBLE = (byte) 0x79;

    static final private Pattern VARIABLEPATTERN = Pattern.compile("(?<text>.*?)\\((?<num>\\d+)\\)");

    /**
     * @return a empty instance of the associated Variable type
     */
    public abstract Variable getVariable();
    public String format(OidInfos oi, Variable v) {
        return v.toString();
    };
    public Variable parse(OidInfos oi, String text) {
        return null;
    };
    public abstract Object convert(Variable v);
    public Object make(int[] in){
        Variable v = getVariable();
        OID oid = new OID(in);
        v.fromSubIndex(oid, true);
        return convert(v);
    };

}
