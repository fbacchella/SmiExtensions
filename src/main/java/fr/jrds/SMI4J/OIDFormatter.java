package fr.jrds.SMI4J;

import java.text.ParseException;

import org.snmp4j.SNMP4JSettings;
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
        return resolver.parseIndexOID(value);
    }

    @Override
    public String formatForRoundTrip(int[] value) {
        return resolver.parseIndexOID(value);
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
