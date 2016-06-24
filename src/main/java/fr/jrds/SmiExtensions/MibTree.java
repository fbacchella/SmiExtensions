package fr.jrds.SmiExtensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;

import fr.jrds.SmiExtensions.ObjectInfos.Attribute;
import fr.jrds.SmiExtensions.ObjectInfos.SnmpType;
import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.types.EnumVal;
import fr.jrds.SmiExtensions.types.Index;
import fr.jrds.SmiExtensions.types.Size;

public class MibTree {

    LogAdapter logger = LogAdapter.getLogger(MibTree.class);

    private final static Pattern p;
    static {
        String empty = "(?: |\\|)";
        String line1 = String.format("%s*", empty);
        String line2 = String.format("(?<depth>%s*)\\+--(?<object>[A-Za-z0-9-#]+)\\((?<oid>\\d+)\\).*", empty);
        String line3 = String.format("%s*Index: (?<indexes>.+)", empty);
        String line4 = String.format("%s*Values: (?<values>.+)", empty);
        String line5 = String.format("%s*Textual Convention: (?<textConv>.+)", empty);
        String line6 = String.format("%s*Size: (?<size>.+)", empty);
        String line7 = String.format("%s*Range: (?<range>.+)", empty);
        String line8 = String.format("(?<depthType>%s*)\\+-- (-|C)(-|R)(-|W)(-|N) (?<type>[A-Za-z0-9]+) +(?<typeName>[A-Za-z0-9]+)\\((?<oidType>\\d+)\\).*", empty);
        p = Pattern.compile(String.format("^(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)$", line1, line2, line3, line4, line5, line6, line7, line8));
    }

    private final Map<String, ObjectInfos> _names = new HashMap<>();
    public final Map<String, ObjectInfos> names = Collections.unmodifiableMap(_names);

    private final OidTreeNode top = new OidTreeNode();

    public MibTree() {
        this(false);
    }

    public MibTree(boolean empty) {
        if(! empty) {
            InputStream is = getClass().getClassLoader().getResourceAsStream("mibstree.txt");
            try {
                load(new InputStreamReader(is, Charset.forName("US-ASCII")));
            } catch (IOException e) {
                throw new RuntimeException("impossible to load default mibstree", e);
            }
        }
    }

    public void load(InputStream is) throws IOException {
        load(new InputStreamReader(is));
    }

    public void load(Reader reader) throws IOException {
        BufferedReader linereader = new BufferedReader(reader);
        int linenumber = 0;
        int olddepth = -1;
        List<Integer> oidBuilder = new ArrayList<>();
        String line;
        Map<Attribute, Object> current = new HashMap<>();
        while((line = linereader.readLine()) != null) {
            linenumber++;
            Matcher m = p.matcher(line);
            if(m.matches()) {
                if(m.group("object") != null || m.group("type") != null) {
                    // Save the last object
                    saveObject(current);
                    String depthGroupContent;
                    String oidGroupContent;
                    String objectName;
                    if(m.group("object") != null) {
                        objectName = m.group("object");
                        depthGroupContent = m.group("depth");
                        oidGroupContent = m.group("oid");
                    } else {
                        objectName = m.group("typeName");
                        try {
                            current.put(Attribute.TYPE, SnmpType.valueOf(m.group("type")) );
                        } catch (IllegalArgumentException e) {
                            logger.error("invalid type at line %s: '%s'",linenumber, m.group("type"));
                        }
                        depthGroupContent = m.group("depthType");
                        oidGroupContent = m.group("oidType");
                    }
                    int depth = depthGroupContent.length() / 3;
                    if(depth > olddepth) {
                        oidBuilder.add(Integer.parseInt(oidGroupContent));
                    } else if (depth == olddepth){
                        oidBuilder.set(depth, Integer.parseInt(oidGroupContent));
                    } else if(depth != 0 && depth < olddepth) {
                        oidBuilder.subList(depth + 1, oidBuilder.size()).clear();
                        oidBuilder.set(depth, Integer.parseInt(oidGroupContent));
                    }
                    int[] oidints = new int[depth + 1];
                    Arrays.setAll(oidints, i -> oidBuilder.get(i));
                    current.put(Attribute.NAME, objectName);
                    current.put(Attribute.OID, oidints);
                    olddepth = depth;
                }
                else if (m.group("indexes") != null) {
                    current.put(Attribute.INDEX, new Index(this, m.group("indexes")));
                }
                else if (m.group("values") != null) {
                    current.put(Attribute.VALUES, new EnumVal(m.group("values")));
                }
                else if (m.group("textConv") != null) {
                    current.put(Attribute.TEXTCONT, m.group("textConv"));
                }
                else if (m.group("size") != null) {
                    current.put(Attribute.SIZE, new Size(m.group("size")));
                }
                else if (m.group("range") != null) {
                    current.put(Attribute.RANGE, m.group("range"));
                }
            } else {
                logger.error("invalid line %s: '%s'",linenumber, line);
            }
        }
        saveObject(current);
    }

    private void saveObject(Map<Attribute, Object> current) {
        if(current.size() > 0) {
            ObjectInfos oi = new ObjectInfos(current);
            if(oi.oidElements != null) {
                if(oi.name != null) {
                    top.add(oi);
                    if(_names.put(oi.name, oi) != null) {
                        logger.warn("duplicate name: %s", oi.name);
                    };
                }
            }
            current.clear();
        }
    }

    public Variable[] parseIndexOID(int[] oid) {
        OidTreeNode found = top.search(oid);
        if(found == null) {
            return new Variable[] {new OID(oid)};
        }
        List<Variable> parts = new ArrayList<Variable>();
        int[] foundOID = found.getElements();
        parts.add(new OctetString(found.getObject().name));
        //StringBuffer result = new StringBuffer();
        //The full path was not found, try to resolve the left other
        if(foundOID.length < oid.length ) {
            ObjectInfos parent = top.find(Arrays.copyOf(foundOID, foundOID.length -1 )).getObject();
            if(parent != null && parent.index != null) {
                int[] index = Arrays.copyOfRange(oid, foundOID.length, oid.length);
                Arrays.stream(parent.index.resolve(index)).forEach(i -> parts.add(i));
            }
        }
        return parts.toArray(new Variable[parts.size()]);
    }

    public ObjectInfos getInfos(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString);
        } else {
            try {
                int [] oidElements = Arrays.stream(oidString.split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray();
                OidTreeNode node = top.find(oidElements);
                if(node != null) {
                    return node.getObject();
                }
            } catch (NumberFormatException e) {
                //parsing failed, was not an oid
            }
        }
        //Nothing works, give up
        return null;
    }

    public int[] getFromName(String oidString) {
        if(names.containsKey(oidString)) {
            int[] oidElements = names.get(oidString).oidElements;
            return Arrays.copyOf(oidElements, oidElements.length);
        } else {
            return null;
        }
    }
}
