package fr.jrds.SmiExtensions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.smi.OID;

import fr.jrds.SmiExtensions.objects.ObjectInfos;

class OidTreeNode {

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
        int[] oidElements = object.getOidElements();
        if(find(oidElements) != null) {
            //already exists, don't add
            return;
        }
        int[] elements = oidElements;
        int[] oidParent = Arrays.copyOf(elements, elements.length - 1);
        //Adding a first level child
        if(oidParent.length == 0) {
            new OidTreeNode(root, elements[elements.length - 1], object);
        } else {
            OidTreeNode parent = root.find(oidParent);
            if(parent != null) {
                new OidTreeNode(parent, elements[elements.length - 1], object);
            } else {
                throw new IllegalStateException("adding orphan child " + object);
            }
        }
    }

    public ObjectInfos getObject() {
        return object;
    }

    public OidTreeNode search(int[] oid) {
        return search(oid, false);
    }

    public OidTreeNode find(int[] oid) {
        OidTreeNode found = search(oid, true);
        if(found!= null && found.object.oidEquals(oid)) {
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
        return Utils.dottedNotation(object.getOidElements()) + ":" + object.getName();
    }

    public int[] getElements() {
        return object.getOidElements();
    }

    public OID getOID() {
        return new OID(object.getOidElements());
    }

}
