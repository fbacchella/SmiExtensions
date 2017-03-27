package fr.jrds.SmiExtensions;

import java.text.ParseException;
import java.util.stream.IntStream;

import org.snmp4j.SNMP4JSettings;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.OIDTextFormat;
import org.snmp4j.util.VariableTextFormat;

import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.objects.ObjectInfos;
import fr.jrds.SmiExtensions.objects.TextualConvention;

public class OIDFormatter implements OIDTextFormat, VariableTextFormat {

    private static LogAdapter logger = LogAdapter.getLogger(OIDFormatter.class);

    private final MibTree resolver;
    private final OIDTextFormat previous;
    private final VariableTextFormat previousVar;

    public OIDFormatter(MibTree resolver) {
        this.resolver = resolver;
        previous = SNMP4JSettings.getOIDTextFormat();
        previousVar = SNMP4JSettings.getVariableTextFormat();
    }

    public void addTextualConvention(Class<? extends TextualConvention> clazz) {
        resolver.addTextualConvention(clazz);
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
        ObjectInfos oi = resolver.searchInfos(instanceOID);
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
        ObjectInfos oi = resolver.searchInfos(classOrInstanceOID);
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
            return new org.snmp4j.smi.Counter32(Long.parseLong(text));
        case SMIConstants.SYNTAX_COUNTER64 :
            return new org.snmp4j.smi.Counter64(Long.parseLong(text));
        case SMIConstants.SYNTAX_INTEGER:    // Also know as Integer32
            return new org.snmp4j.smi.Integer32(Integer.parseInt(text));
        case SMIConstants.SYNTAX_GAUGE32:    // Also know as Unsigned32
            return new org.snmp4j.smi.UnsignedInteger32(Long.parseLong(text));
        case SMIConstants.SYNTAX_IPADDRESS:
            return new org.snmp4j.smi.IpAddress(text);
        case SMIConstants.SYNTAX_NULL:
            return new org.snmp4j.smi.Null();
        case SMIConstants.SYNTAX_OBJECT_IDENTIFIER :
            return new OID(resolver.getFromName(text));
        case SMIConstants.SYNTAX_OCTET_STRING:
            return new org.snmp4j.smi.OctetString(text.getBytes());
        case SMIConstants.SYNTAX_OPAQUE:
            return new org.snmp4j.smi.Opaque(text.getBytes());
        case SMIConstants.SYNTAX_TIMETICKS:
            return new org.snmp4j.smi.TimeTicks(Long.parseLong(text));
        }
        logger.debug("parsing to variable %s with %d", text, smiSyntax);
        return previousVar.parse(smiSyntax, text);
    }

}
