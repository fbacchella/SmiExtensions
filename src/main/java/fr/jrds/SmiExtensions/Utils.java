package fr.jrds.SmiExtensions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    
    public static boolean startsWith(int[] test, int[] prefix) {
        if(prefix.length > test.length) {
            return false;
        } else {
            return Arrays.equals(Arrays.copyOf(test, prefix.length), prefix);
        }
    }

    public static String dottedNotation(int[] elements){
        // SimpleOIDTextFormat does masking too
        return Arrays.stream(elements).mapToObj(i -> Long.toString(i & 0xFFFFFFFFL)).collect(Collectors.joining("."));
    }
}
