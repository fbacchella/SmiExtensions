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
import java.util.stream.Collectors;

import org.snmp4j.smi.OID;

import fr.jrds.SmiExtensions.log.LogAdapter;
import fr.jrds.SmiExtensions.objects.ObjectInfos;
import fr.jrds.SmiExtensions.objects.ObjectInfos.Attribute;

public class MibTree {

    private final static LogAdapter logger = LogAdapter.getLogger(MibTree.class);

    private final static Pattern p;
    static {
        String empty = "(?: |\\|)";
        String line1 = String.format("%s*", empty);
        String line2 = String.format("(?<depth>%s*)\\+--(?<object>[A-Za-z0-9-]+|anonymous#[0-9]+)(?<trap>#)?\\((?<oid>\\d+)\\).*", empty);
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

    private final Map<OID, Map<Integer, String>> traps = new HashMap<>();

    /**
     * Build a MIB tree, using default content
     */
    public MibTree() {
        this(false);
    }

    /**
     * Build a MIB tree, 
     * @param empty if set to true, don't load the default content
     */
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
        load(new InputStreamReader(is, Charset.defaultCharset()));
    }

    public void load(Reader reader) throws IOException {
        BufferedReader linereader = new BufferedReader(reader);
        int linenumber = 0;
        int olddepth = -1;
        List<String> oidBuilder = new ArrayList<>();
        String line;
        Map<Attribute, String> current = new HashMap<>();
        boolean inTrapList = false;
        while((line = linereader.readLine()) != null) {
            linenumber++;
            Matcher m = p.matcher(line);
            if(m.matches()) {
                if(m.group("object") != null || m.group("type") != null) {
                    // Save the last object
                    saveObject(current, inTrapList);
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
                            current.put(Attribute.TYPE, m.group("type") );
                        } catch (IllegalArgumentException e) {
                            logger.error("invalid type at line %s: '%s'",linenumber, m.group("type"));
                        }
                        depthGroupContent = m.group("depthType");
                        oidGroupContent = m.group("oidType");
                    }
                    int depth = depthGroupContent.length() / 3;
                    if(depth > olddepth) {
                        oidBuilder.add(oidGroupContent);
                    } else if (depth == olddepth){
                        oidBuilder.set(depth, oidGroupContent);
                    } else if(depth != 0 && depth < olddepth) {
                        oidBuilder.subList(depth + 1, oidBuilder.size()).clear();
                        oidBuilder.set(depth, oidGroupContent);
                        inTrapList = false;
                    }
                    current.put(Attribute.NAME, objectName);
                    current.put(Attribute.OID, oidBuilder.stream().collect(Collectors.joining(".")));
                    olddepth = depth;
                    // A set of v1 trap is always under a node labeled #(0)
                    if (m.group("trap") != null && "#".equals(m.group("trap")) && "0".equals(m.group("oid"))) {
                        inTrapList = true;
                        // Drop the current object, it's just a trap specific wrapper, useless
                        current.clear();;
                    }
                }
                else if (m.group("indexes") != null) {
                    current.put(Attribute.INDEX, m.group("indexes"));
                }
                else if (m.group("values") != null) {
                    current.put(Attribute.VALUES, m.group("values"));
                }
                else if (m.group("textConv") != null) {
                    current.put(Attribute.TEXTCONT, m.group("textConv"));
                }
                else if (m.group("size") != null) {
                    current.put(Attribute.SIZE, m.group("size"));
                }
                else if (m.group("range") != null) {
                    current.put(Attribute.RANGE, m.group("range"));
                }
            } else {
                logger.error("invalid line %s: '%s'",linenumber, line);
            }
        }
        // Save the last iterated object
        saveObject(current, inTrapList);
    }

    private void saveObject(Map<Attribute, String> current, boolean inTrapList) {
        if(current.size() > 0) {
            ObjectInfos oi = new ObjectInfos(this, current);
            if(oi.getOidElements() != null && oi.getName() != null && ! inTrapList) {
                top.add(oi);
                if(_names.put(oi.getName(), oi) != null) {
                    logger.warn("duplicate name: %s", oi.getName());
                };
            } else {
                OID oid = new OID(oi.getOID());
                int specific = oid.removeLast();
                // Remove the useless 0 in the OID
                oid.removeLast();
                if (! traps.containsKey(oid)) {
                    traps.put(oid, new HashMap<>());
                }
                traps.get(oid).put(specific, oi.getName());
            }
            current.clear();
        }
    }

    /**
     * Parse an OID that contains an array's index and resolve it.
     * @param oid
     * @return a array of index parts, starting with the entry name
     */
    Object[] parseIndexOID(int[] oid) {
        OidTreeNode found = top.search(oid);
        if(found == null) {
            return new Object[] {new OID(oid)};
        }
        List<Object> parts = new ArrayList<Object>();
        int[] foundOID = found.getElements();
        parts.add(found.getObject().getName());
        //The full path was not found, try to resolve the left other
        if(foundOID.length < oid.length ) {
            ObjectInfos parent = top.find(Arrays.copyOf(foundOID, foundOID.length -1 )).getObject();
            if(parent != null && parent.isIndex()) {
                int[] index = Arrays.copyOfRange(oid, foundOID.length, oid.length);
                Arrays.stream(parent.resolve(index)).forEach(i -> parts.add(i));
            }
        }
        return parts.toArray(new Object[parts.size()]);
    }

    /**
     * Parse an OID that contains an array's index and resolve it.
     * @param oid The OID to parse
     * @return a array of index parts, starting with the entry name
     */
    public Object[] parseIndexOID(OID oid) {
        return parseIndexOID(oid.getValue());
    }

    public ObjectInfos searchInfos(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString);
        } else {
            try {
                int [] oidElements = Arrays.stream(oidString.split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray();
                OidTreeNode node = top.search(oidElements);
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

    public ObjectInfos searchInfos(int[] oidElements) {
        OidTreeNode node = top.search(oidElements);
        if(node != null) {
            return node.getObject();
        } else {
            return null;
        }
    }

    public ObjectInfos searchInfos(OID oid) {
        return searchInfos(oid.getValue());
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

    public ObjectInfos getInfos(int[] oidElements) {
        OidTreeNode node = top.find(oidElements);
        if(node != null) {
            return node.getObject();
        } else {
            return null;
        }
    }

    public ObjectInfos getInfos(OID oid) {
        return getInfos(oid.getValue());
    }

    public int[] getFromName(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString).getOidElements();
        } else {
            return null;
        }
    }

    public ObjectInfos getParent(int[] oidElements) {
        OidTreeNode node = top.search(Arrays.copyOf(oidElements, oidElements.length - 1));
        if(node != null) {
            return node.getObject();
        } else {
            return null;
        }
    }

    public ObjectInfos getParent(OID oid) {
        return getParent(oid.getValue());
    }

    /**
     * Return the string value of a trap if a specific trap Id was given
     * @param oid The enterprise OID
     * @param specific The specific trap Id
     * @return
     */
    public String resolveTrapSpecific(OID oid, int specific) {
        return traps.getOrDefault(oid, Collections.emptyMap()).getOrDefault(specific, Integer.toString(specific));
    }

}
