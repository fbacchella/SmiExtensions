package fr.jrds.smiextensions.log;

import java.io.Serializable;
import java.util.Arrays;

class LogString implements CharSequence, Serializable {

    private String formatted = null;
    private final String format;
    private final Object[] objects;

    private LogString(String format, Object...objects) {
        this.format = format;
        this.objects = objects;
    }
    @Override
    public int length() {
        if(formatted == null) {
            formatted = format();
        }
        return formatted.length();
    }

    @Override
    public char charAt(int index) {
        if(formatted == null) {
            formatted = format();
        }
        return formatted.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if(formatted == null) {
            formatted = format();
        }
        return formatted.substring(start, end);
    }

    private final String format() {
        Object[] mapped = Arrays.stream(objects).map(i -> i instanceof Object[] ? Arrays.toString((Object[])i):i)
                .map(i -> i instanceof int[] ? Arrays.toString((int[])i):i)
                .map(i -> i instanceof long[] ? Arrays.toString((long[])i):i)
                .map(i -> i instanceof byte[] ? Arrays.toString((byte[])i):i)
                .toArray();
        return formatted = String.format(format, mapped);
    }

    public final static LogString make(String format, Object...objects) {
        return new LogString(format, objects);
    }

    @Override
    public String toString() {
        if(formatted == null) {
            formatted = format();
        }
        return formatted;
    }

}
