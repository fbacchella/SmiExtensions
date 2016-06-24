package fr.jrds.SMI4J.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jrds.SMI4J.utils.LogAdapter;

public class Size {

    LogAdapter logger = LogAdapter.getLogger(Size.class);

    private static class Range {
        final int from;
        final int to;
        Range(int from, int to) {
            this.from = from;
            this.to= to;
        }
        @Override
        public String toString() {
            return "(" + from + ".." + to + ")";
        }

    }

    private final static Pattern p;
    static {
        String range = String.format("(?<from>\\d+)\\.\\.(?<to>\\d+)");
        String size = String.format("(?<value>\\d+)");
        p = Pattern.compile(String.format("^(?:(?:(?<range>%s)|(?<size>%s))(?: +\\| +(?<choice>.*))?)|(?<noise>.*)$", range, size));
    }

    private final List<Range> ranges = new ArrayList<>();

    public Size(String sizes) {
        Matcher m = p.matcher(sizes);
        if (! parse(m)) {
            logger.error("invalid size line" , m.group("noise"));
        };
    }

    private boolean parse(Matcher m) {
        if(m.matches()) {
            if(m.group("noise") != null) {
                return false;
            }
            if(m.group("range") != null) {
                int from = Integer.parseInt(m.group("from"));
                int to = Integer.parseInt(m.group("to"));
                ranges.add(new Range(from, to));
            } else if(m.group("size") != null) {
                int from = Integer.parseInt(m.group("value"));
                int to = Integer.parseInt(m.group("value"));
                ranges.add(new Range(from, to));
            }
            if(m.group("choice") != null) {
                m.region(m.start("choice"), m.end());
                parse(m);
            }
            return true;
        }
        return false;
    }

    public int[] extract(int[] oidElements) {
        int[] tryExtract = null;
        for(Range i: ranges) {
            if (oidElements.length >= i.from && oidElements.length <= i.to) {
                return oidElements;
            } else if (oidElements.length >= i.from) {
                tryExtract = Arrays.copyOf(oidElements, i.to);
            }
        }
        return tryExtract;
    }

    @Override
    public String toString() {
        return ranges.toString();
    }
    
    
}
