package fr.jrds.smiextensions.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jrds.smiextensions.MibTree;
import fr.jrds.smiextensions.log.LogAdapter;

class Index {

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

    public Object[] resolve(int[] oid) {
        List<Object> indexesValues = new ArrayList<>();
        int[] oidParsed = Arrays.copyOf(oid, oid.length);
        for(String i: indexes) {
            OidInfos oi = smi.getInfos(i);
            logger.debug("found %s from %s", oi, i);
            if(oi == null) {
                logger.error("index not found: %s", i);
                break;
            }
            Parsed parsed;
            if(oi.size != null) {
                parsed = oi.size.extract(oidParsed);
            } else {
                parsed = new Parsed();
                parsed.content = Arrays.copyOf(oidParsed, 1);
                if(oidParsed.length > 1) {
                    parsed.next = Arrays.copyOfRange(oidParsed, 1, oidParsed.length);
                }
            }
            if(parsed == null) {
                break;
            }
            logger.debug("parsed %s from %s", parsed, oidParsed);
            Object v = oi.type.make(parsed.content);
            if(oi.type == SnmpType.EnumVal) {
                Integer k = (Integer) v;
                v = String.format("%s(%d)", oi.values.resolve(k), k);
            }
            indexesValues.add(v);
            oidParsed = parsed.next;
            if (oidParsed == null) {
                break;
            }
        }
        logger.debug("will resolve %s to %s", oid, indexesValues);
        return indexesValues.toArray(new Object[indexesValues.size()]);
    }

}
