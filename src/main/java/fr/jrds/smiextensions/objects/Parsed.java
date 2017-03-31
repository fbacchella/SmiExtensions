package fr.jrds.smiextensions.objects;

import fr.jrds.smiextensions.Utils;

public class Parsed {
    public int[] content = null;
    public int[] next = null;
    @Override
    public String toString() {
        return (content != null ? Utils.dottedNotation(content) : "") + "/" + (next != null ? Utils.dottedNotation(next) : "");
    }

}
