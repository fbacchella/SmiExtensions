package fr.jrds.SmiExtensions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    
    public static String dottedNotation(int[] elements){
        // SimpleOIDTextFormat does masking too
        return Arrays.stream(elements).mapToObj(i -> Long.toString(i & 0xFFFFFFFFL)).collect(Collectors.joining("."));
    }
}
