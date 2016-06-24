package fr.jrds.SmiExtensions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.OID;

public class OidTreeNode {

    private final ObjectInfos object;
    private final Map<Integer, OidTreeNode> childs = new HashMap<Integer, OidTreeNode>();
    private final OidTreeNode root;

    public OidTreeNode() {
        object = null;
        root = this;
    }

    private OidTreeNode(OidTreeNode parent, int id, ObjectInfos object) {
        this.object = object;
        parent.childs.put(id, this);
        this.root = parent.root;
    }

    public void add(ObjectInfos object) {
        int[] elements = object.oidElements;
        int[] oidParent = Arrays.copyOf(elements, elements.length - 1);
        OidTreeNode parent = root.search(oidParent, 0);
        new OidTreeNode(parent, elements[elements.length - 1], object);
    }

    public ObjectInfos getObject() {
        return object;
    }

    public OidTreeNode search(int[] oid) {
        return search(oid, false);
    }

    public OidTreeNode find(int[] oid) {
        OidTreeNode found = search(oid, true);
        if(Arrays.equals(oid, found.object.oidElements)) {
            return found;
        } else {
            return null;
        }
    }

    private OidTreeNode search(int[] oid, boolean Strict) {
        if(root.childs.containsKey(oid[0])) {
            return root.childs.get(oid[0]).search(oid, 1);
        } else {
            return null;
        }
    }

    private OidTreeNode search(int[] oid, int level) {
        if(oid.length == level) {
            return this;
        } else {
            int key = oid[level];
            if (childs.containsKey(key)) {
                return childs.get(key).search(oid, level + 1);
            } else {
                return this;
            }
        }
    }

    @Override
    public String toString() {
        return Utils.dottedNotation(object.oidElements) + ":" + object.name;
    }

    public int[] getElements() {
        return Arrays.copyOf(object.oidElements, object.oidElements.length);
    }

    public OID getOID() {
        return new OID(object.oidElements);
    }

}
