package fr.jrds.smiextensions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {

    public static String dottedNotation(int[] elements){
        // SimpleOIDTextFormat does masking too
        return Arrays.stream(elements).mapToObj(i -> Long.toString(i & 0xFFFFFFFFL)).collect(Collectors.joining("."));
    }

    public static final class UnsignedLong extends Number implements Comparable<Long>{
        private final long l;

        public UnsignedLong(long l) {
            super();
            this.l = l;
        }

        public byte byteValue() {
            return (byte) l;
        }

        public short shortValue() {
            return (short) l;
        }

        public int intValue() {
            return (int) l;
        }

        public long longValue() {
            return l;
        }

        public float floatValue() {
            return (float) l;
        }

        public double doubleValue() {
            return(double) l;
        }

        public String toString() {
            return Long.toUnsignedString(l);
        }

        public int hashCode() {
            return Long.hashCode(l);
        }

        public boolean equals(Object obj) {
            if (obj instanceof Long) {
                return l == ((Long)obj).longValue();
            }
            return false;
        }

        public int compareTo(Long l2) {
            return Long.compareUnsigned(l, l2);
        }

    }

    public static Utils.UnsignedLong getUnsigned(long l) {
        return new Utils.UnsignedLong(l);
    }
}
