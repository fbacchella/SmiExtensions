package fr.jrds.smiextensions;

import java.text.ParseException;
import java.util.stream.IntStream;

import org.snmp4j.SNMP4JSettings;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.OIDTextFormat;
import org.snmp4j.util.VariableTextFormat;

import fr.jrds.smiextensions.log.LogAdapter;
import fr.jrds.smiextensions.objects.OidInfos;
import fr.jrds.smiextensions.objects.SnmpType;
import fr.jrds.smiextensions.objects.TextualConvention;

public class OIDFormatter implements OIDTextFormat, VariableTextFormat {

    private static final LogAdapter logger = LogAdapter.getLogger(OIDFormatter.class);

    private final MibTree resolver;
    private OIDTextFormat previous;
    private VariableTextFormat previousVar;

    public OIDFormatter(MibTree resolver) {
        this.resolver = resolver;
        previous = SNMP4JSettings.getOIDTextFormat();
        previousVar = SNMP4JSettings.getVariableTextFormat();
    }

    /**
     * Register in SNMP4J a default {@link MibTree}, it can be called many times
     * @return the new OIDFormatter
     */
    public static OIDFormatter register() {
        MibTree resolver = new MibTree();
        return register(resolver);
    }

    /**
     * Register in SNMP4J a custom  {@link MibTree}, it can be called many times
     * @param resolver the new OIDFormatter
     * @return the new OIDFormatter
     */
    public static OIDFormatter register(MibTree resolver) {
        OIDTextFormat previousTextFormat = SNMP4JSettings.getOIDTextFormat();
        VariableTextFormat previousVarFormat = SNMP4JSettings.getVariableTextFormat();
        OIDFormatter formatter = new OIDFormatter(resolver);
        SNMP4JSettings.setOIDTextFormat(formatter);
        SNMP4JSettings.setVariableTextFormat(formatter);
        if (previousTextFormat instanceof OIDFormatter) {
            formatter.previous = ((OIDFormatter) previousTextFormat).previous;
        }
        if (previousVarFormat instanceof OIDFormatter) {
            formatter.previousVar = ((OIDFormatter) previousTextFormat).previousVar;
        }
        return formatter;
    }

    /**
     * Added a new custom TextualConvention to the current mib base
     * @param clazz
     */
    public void addTextualConvention(Class<? extends TextualConvention> clazz) {
        resolver.addTextualConvention(clazz);
    }

    /**
     * Added a new TextualConvention described using a display hint string to the current mib base
     * @param name the name of the textual convention
     * @param displayHint, taken from the <code>DISPLAY-HINT</code> field from the <code>TEXTUAL-CONVENTION</code>.
     */
    public void addTextualConvention(String name, String displayHint) {
        resolver.addTextualConvention(name, displayHint);
    }

    @Override
    public String format(int[] value) {
        Object[] parsed = resolver.parseIndexOID(value);
        if(parsed != null && parsed.length > 0) {
            StringBuffer buffer = new StringBuffer(parsed[0].toString());
            IntStream.range(1, parsed.length).forEach(i -> buffer.append("[" + parsed[i] + "]"));
            return buffer.toString();
        } else {
            return previous.format(value);
        }
    }

    @Override
    public String formatForRoundTrip(int[] value) {
        return format(value);
    }

    @Override
    public int[] parse(String text) throws ParseException {
        if(resolver.names.containsKey(text)) {
            return resolver.getFromName(text);
        } else {
            return previous.parse(text);
        }

    }

    @Override
    public String format(OID instanceOID, Variable variable, boolean withOID) {
        OidInfos oi = resolver.searchInfos(instanceOID);
        String formatted = oi.format(variable);
        if (formatted != null) {
            return formatted;
        } else {
            return previousVar.format(instanceOID, variable, withOID);
        }
    }

    @Override
    public VariableBinding parseVariableBinding(String text) throws ParseException {
        return previousVar.parseVariableBinding(text);
    }

    @Override
    public Variable parse(OID classOrInstanceOID, String text) throws ParseException {
        OidInfos oi = resolver.searchInfos(classOrInstanceOID);
        Variable v = oi.getVariable(text);
        if (v != null) {
            return v;
        } else {
            return previousVar.parse(classOrInstanceOID, text);
        }
    }

    @Override
    public Variable parse(int smiSyntax, String text) throws ParseException {
        switch (smiSyntax) {
        case SMIConstants.SYNTAX_COUNTER32:  // Value os unsigned int32
            return SnmpType.Counter.parse(null, text);
        case SMIConstants.SYNTAX_COUNTER64 :
            return SnmpType.Counter64.parse(null, text);
        case SMIConstants.SYNTAX_INTEGER:    // Also know as Integer32
            return SnmpType.Integer32.parse(null, text);
        case SMIConstants.SYNTAX_GAUGE32:    // Also know as Unsigned32
            return SnmpType.Unsigned32.parse(null, text);
        case SMIConstants.SYNTAX_IPADDRESS:
            return SnmpType.IpAddr.parse(null, text);
        case SMIConstants.SYNTAX_NULL:
            return new org.snmp4j.smi.Null();
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER :
            return SnmpType.ObjID.parse(null, text);
        case SMIConstants.SYNTAX_OCTET_STRING:    // Also know as Bits
            return SnmpType.String.parse(null, text);
        case SMIConstants.SYNTAX_OPAQUE:
            return SnmpType.Opaque.parse(null, text);
        case SMIConstants.SYNTAX_TIMETICKS:
            return SnmpType.TimeTicks.parse(null, text);
        }
        logger.debug("parsing to variable %s with %d", text, smiSyntax);
        return previousVar.parse(smiSyntax, text);
    }

}
