package fr.jrds.SMI4J.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.smi.Variable;

import fr.jrds.SMI4J.MibTree;
import fr.jrds.SMI4J.ObjectInfos;
import fr.jrds.SMI4J.utils.LogAdapter;

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
            int[] toformat;
            if(oi.size != null) {
                toformat = oi.size.extract(oidParsed);
            } else {
                toformat = Arrays.copyOf(oidParsed, 1);
            }
            if(toformat == null) {
                break;
            }
            oidParsed = Arrays.copyOfRange(oidParsed, toformat.length, oidParsed.length);
            indexesValues.add(oi.type.make(toformat));
            if(oidParsed.length == 0) {
                break;
            }
        }
        return indexesValues.toArray(new Variable[indexesValues.size()]);
    }

}
