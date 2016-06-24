package fr.jrds.SMI4J;

import java.text.ParseException;
import java.util.stream.IntStream;

import org.snmp4j.SNMP4JSettings;
import org.snmp4j.smi.Variable;
import org.snmp4j.util.OIDTextFormat;

public class OIDFormatter implements OIDTextFormat {

    private final MibTree resolver;
    private final OIDTextFormat previous;
    
    public OIDFormatter(MibTree resolver) {
        this.resolver = resolver;
        previous = SNMP4JSettings.getOIDTextFormat();
    }

    @Override
    public String format(int[] value) {
        Variable[] parsed = resolver.parseIndexOID(value);
        if(parsed != null && parsed.length > 0) {
            StringBuffer buffer = new StringBuffer(parsed[0].toString());
            IntStream.range(1, parsed.length).forEach(i -> buffer.append("[" + parsed[i] + "]"));
            return buffer.toString();
        } else {
            return "";
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

}
