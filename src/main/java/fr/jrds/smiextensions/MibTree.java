package fr.jrds.smiextensions;

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

import fr.jrds.smiextensions.log.LogAdapter;
import fr.jrds.smiextensions.objects.OidInfos;
import fr.jrds.smiextensions.objects.OidInfos.Attribute;
import fr.jrds.smiextensions.objects.TextualConvention;
import fr.jrds.smiextensions.objects.TextualConvention.DateAndTime;
import fr.jrds.smiextensions.objects.TextualConvention.StorageType;

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
        // Some OID miss the size
        String line6 = String.format("%s*Size: (?<size>.+)?", empty);
        String line7 = String.format("%s*Range: (?<range>.+)", empty);
        String line8 = String.format("(?<depthType>%s*)\\+-- (-|C)(-|R)(-|W)(-|N) (?<type>[A-Za-z0-9]+) +(?<typeName>[A-Za-z0-9-]+)\\((?<oidType>\\d+)\\).*", empty);
        p = Pattern.compile(String.format("^(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)|(?:%s)$", line1, line2, line3, line4, line5, line6, line7, line8));
    }

    private final Map<String, OidInfos> _names = new HashMap<>();
    public final Map<String, OidInfos> names = Collections.unmodifiableMap(_names);

    private final OidTreeNode top = new OidTreeNode();

    private final Map<OID, Map<Integer, String>> traps = new HashMap<>();

    private final Map<String, TextualConvention> conventions = new HashMap<>();

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
                load(new InputStreamReader(is, Charset.forName("US-ASCII")), false);
            } catch (IOException e) {
                throw new RuntimeException("impossible to load default mibstree", e);
            }
            addTextualConvention(DateAndTime.class);
            addTextualConvention(StorageType.class);
        }
    }

    /**
     * Add a new MIB from a input stream
     * @param stream the MIB stream
     * @throws IOException if source can't be read
     */
    public void load(InputStream stream) throws IOException {
        load(new InputStreamReader(stream, Charset.defaultCharset()), true);
    }

    /**
     * Add a new mib from a reader
     * @param reader the MIB reader
     * @throws IOException if source can't be read
     */
    public void load(Reader reader) throws IOException {
        load(reader, true);
    }

    private void load(Reader reader, boolean reload) throws IOException {
        BufferedReader linereader = new BufferedReader(reader);
        int linenumber = 0;
        int olddepth = -1;
        int traplistdepth = -1;
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
                    saveObject(current, inTrapList, reload);
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
                    // Check if we reach the end of a trap list
                    if (traplistdepth > 0 && depth <= traplistdepth) {
                        inTrapList = false;
                        traplistdepth = -1;
                    }
                    if(depth > olddepth) {
                        oidBuilder.add(oidGroupContent);
                    } else if (depth == olddepth){
                        oidBuilder.set(depth, oidGroupContent);
                    } else if(depth != 0 && depth < olddepth) {
                        oidBuilder.subList(depth + 1, oidBuilder.size()).clear();
                        oidBuilder.set(depth, oidGroupContent);
                    }
                    current.put(Attribute.NAME, objectName);
                    current.put(Attribute.OID, oidBuilder.stream().collect(Collectors.joining(".")));
                    olddepth = depth;
                    // A set of v1 trap is always under a node labeled #(0)
                    if (m.group("trap") != null && "#".equals(m.group("trap")) && "0".equals(m.group("oid"))) {
                        inTrapList = true;
                        traplistdepth = depth;
                        // Drop the current object, it's just a trap specific wrapper, useless
                        current.clear();
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
        saveObject(current, inTrapList, reload);
    }

    private void saveObject(Map<Attribute, String> current, boolean inTrapList, boolean reload) {
        if(current.size() > 0) {
            OidInfos oi = new OidInfos(this, current);
            if(oi.getOidElements() != null && oi.getName() != null && ! inTrapList) {
                top.add(oi);
                // warning about duplicate names are not sent when adding a new tree
                if(_names.put(oi.getName(), oi) != null && ! reload) {
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
     * Parse an OID that contains an array's index and split it in sub component.
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
            OidInfos parent = top.find(Arrays.copyOf(foundOID, foundOID.length -1 )).getObject();
            if(parent != null && parent.isIndex()) {
                int[] index = Arrays.copyOfRange(oid, foundOID.length, oid.length);
                Arrays.stream(parent.resolve(index)).forEach(i -> parts.add(i));
            }
        }
        return parts.toArray(new Object[parts.size()]);
    }

    /**
     * Parse an OID that contains an array's index and split it in sub component.
     * @param oid The OID to parse
     * @return a array of index parts, starting with the entry name
     */
    public Object[] parseIndexOID(OID oid) {
        return parseIndexOID(oid.getValue());
    }

    private OidInfos searchInfos(int[] oidElements) {
        OidTreeNode node = top.search(oidElements);
        if(node != null) {
            return node.getObject();
        } else {
            return null;
        }
    }

    /**
     * Parse a string and return the associated node's object info. It can be an OID or a OID name
     * @param oidString
     * @return
     */
    public OidInfos searchInfos(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString);
        } else {
            try {
                int [] oidElements = Arrays.stream(oidString.split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray();
                return searchInfos(oidElements);
            } catch (NumberFormatException e) {
                //parsing failed, was not an oid
            }
        }
        //Nothing works, give up
        return null;
    }

    /**
     * Return the associated nodes's object info from an OID.
     * @param oid
     * @return
     */
   public OidInfos searchInfos(OID oid) {
        return searchInfos(oid.getValue());
    }

   private OidInfos getInfos(int[] oidElements) {
       OidTreeNode node = top.find(oidElements);
       if(node != null) {
           return node.getObject();
       } else {
           return null;
       }
   }

    public OidInfos getInfos(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString);
        } else {
            try {
                int [] oidElements = Arrays.stream(oidString.split("\\.")).mapToInt(i -> Integer.parseInt(i)).toArray();
                return getInfos(oidElements);
            } catch (NumberFormatException e) {
                //parsing failed, was not an oid
            }
        }
        //Nothing works, give up
        return null;
    }

    public OidInfos getInfos(OID oid) {
        return getInfos(oid.getValue());
    }

    public int[] getFromName(String oidString) {
        if(names.containsKey(oidString)) {
            return names.get(oidString).getOidElements();
        } else {
            return null;
        }
    }

    public OidInfos getParent(int[] oidElements) {
        OidTreeNode node = top.search(Arrays.copyOf(oidElements, oidElements.length - 1));
        if(node != null) {
            return node.getObject();
        } else {
            return null;
        }
    }

    public OidInfos getParent(OID oid) {
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

    /**
     * Added a new custom TextualConvention to the current mib base
     * @param clazz
     */
    public void addTextualConvention(Class<? extends TextualConvention> clazz) {
        TextualConvention.addAnnotation(clazz, conventions);
    }

    /**
     * Added a new TextualConvention described using a display hint string to the current mib base
     * @param name the name of the textual convention
     * @param displayHint, taken from the <code>DISPLAY-HINT</code> field from the <code>TEXTUAL-CONVENTION</code>.
     */
    public void addTextualConvention(String name, String displayHint) {
        TextualConvention.addAnnotation(name, displayHint, conventions);
    }

    public TextualConvention getTextualConvention(String textConventionName) {
        return conventions.get(textConventionName);
    }
}
