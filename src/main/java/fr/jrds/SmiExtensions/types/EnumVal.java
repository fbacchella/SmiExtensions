package fr.jrds.SmiExtensions.types;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnumVal {
    private final Map<Integer, String> values= new HashMap<>();
    private final static Pattern p = Pattern.compile("(.*?)\\((\\d+)\\)(?:, )?");

    public EnumVal(String values) {
        Matcher m = p.matcher(values);
        while(m.find()) {
            this.values.put(Integer.parseInt(m.group(2)), m.group(1));
        }
    }

    public String resolve(int key) {
        return values.get(key);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
