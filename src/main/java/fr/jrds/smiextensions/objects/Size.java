package fr.jrds.smiextensions.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.jrds.smiextensions.log.LogAdapter;

public class Size {

    private final static LogAdapter logger = LogAdapter.getLogger(Size.class);

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
    private boolean variableSize = false;

    Size(String sizes) {
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
                variableSize = true;
            } else if(m.group("size") != null) {
                int from = Integer.parseInt(m.group("value"));
                int to = Integer.parseInt(m.group("value"));
                ranges.add(new Range(from, to));
            }
            if(m.group("choice") != null) {
                m.region(m.start("choice"), m.end());
                parse(m);
                variableSize = true;
            }
            return true;
        }
        return false;
    }

    Parsed extract(int[] oidElements) {
        Parsed tryExtract = new Parsed();
        for(Range i: ranges) {
            if(variableSize) {
                int size = oidElements[0];
                if(size == 0) {
                    tryExtract.content = new int[0];
                    tryExtract.next = oidElements;
                } if(oidElements.length >= size) {
                    tryExtract.content = Arrays.copyOfRange(oidElements, 1, size + 1);
                    if(size + 1 <= oidElements.length) {
                        tryExtract.next = Arrays.copyOfRange(oidElements, size + 1, oidElements.length);
                    } else {
                        tryExtract.next = null;
                    }
                }
            } else if (oidElements.length >= i.from && oidElements.length <= i.to) {
                tryExtract.content = oidElements;
                tryExtract.next = null;
                return tryExtract;
            } else if (oidElements.length >= i.from) {
                tryExtract.content = Arrays.copyOf(oidElements, i.to);
                if(i.to + 1 <= oidElements.length) {
                    tryExtract.next = Arrays.copyOfRange(oidElements, i.to, oidElements.length);
                } else {
                    tryExtract.next = null;
                }
            }
        }
        return tryExtract;
    }

    @Override
    public String toString() {
        return ranges.toString();
    }

}
