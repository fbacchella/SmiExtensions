package fr.jrds.SmiExtensions.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

import fr.jrds.SmiExtensions.MibTree;
import fr.jrds.SmiExtensions.ObjectInfos;
import fr.jrds.SmiExtensions.log.LogAdapter;

public class Index {
    
    private static final LogAdapter logger = LogAdapter.getLogger(Index.class);

    private final List<String> indexes = new ArrayList<>();
    private final static Pattern p = Pattern.compile("([^,]+)(?:, )?");
    private final MibTree smi;
    public Index(MibTree smi, String indexes) {
        this.smi = smi;
        Matcher m = p.matcher(indexes);
        while(m.find()) {
            this.indexes.add(m.group(1));
        }
    }

    @Override
    public String toString() {
        return indexes.toString();
    }
    
    public Variable[] resolve(int[] oid) {
        List<Variable> indexesValues = new ArrayList<>();
        int[] oidParsed = Arrays.copyOf(oid, oid.length);
        for(String i: indexes) {
            ObjectInfos oi = smi.getInfos(i);
            if(oi == null) {
                logger.error("index not found: %s", i);
                break;
            }
            Size.Parsing parsed;
            if(oi.size != null) {
                parsed = oi.size.extract(oidParsed);
            } else {
                parsed = new Size.Parsing();
                parsed.content = Arrays.copyOf(oidParsed, 1);
                if(oidParsed.length > 1) {
                    parsed.next = Arrays.copyOfRange(oidParsed, 1, oidParsed.length);
                }
            }
            if(parsed == null) {
                break;
            }
            Variable v = oi.type.make(parsed.content);
            if(oi.type == ObjectInfos.SnmpType.EnumVal) {
                v = new OctetString(String.format("%s(%d)", oi.values.resolve(v.toInt()), v.toInt()));
            }
            indexesValues.add(v);
            oidParsed = parsed.next;
        }
        return indexesValues.toArray(new Variable[indexesValues.size()]);
    }

}
