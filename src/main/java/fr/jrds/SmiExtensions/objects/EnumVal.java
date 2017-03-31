package fr.jrds.smiextensions.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class EnumVal {

    private final Map<Integer, String> values= new HashMap<>();
    private final Map<String, Integer> names = new HashMap<>();
    private final static Pattern p = Pattern.compile("(.*?)\\((\\d+)\\)(?:, )?");

    public EnumVal(String values) {
        Matcher m = p.matcher(values);
        while(m.find()) {
            this.values.put(Integer.parseInt(m.group(2)), m.group(1));
            this.names.put(m.group(1), Integer.parseInt(m.group(2)));
        }
    }

    public String resolve(int key) {
        return values.get(key);
    }

    public Integer resolve(String name) {
        return names.get(name);
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
