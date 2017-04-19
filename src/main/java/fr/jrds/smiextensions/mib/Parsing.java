package fr.jrds.smiextensions.mib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import fr.jrds.smiextensions.MibTree;
import fr.jrds.smiextensions.objects.OidInfos;

public class Parsing {

    static LoadMib loader = new LoadMib();

    public static void main(String[] args) {
        Arrays.stream(args)
        .map( i -> Paths.get(i))
        .forEach(i -> {
            try {
                load(i);
            } catch (IOException e) {
                System.out.format("invalid path source: %s", i);
            } catch (ModuleException e) {
                System.out.format("broken mib: %s at %s\n", e.getMessage(), e.getLocation());
            } catch (Exception e) {
                System.out.format("broken mib: %s\n", i);
                e.printStackTrace(System.err);
            }
        });
        loader.displayHints.entrySet().stream()
        .forEach(i -> System.out.format("%s %s\n", i.getKey(), i.getValue()));
    }

    public static void load(Path mibs) throws IOException {
        BiPredicate<Path,BasicFileAttributes> matcher = (i,j) -> {
            if (Files.isDirectory(i)) {
                return false;
            }
            String file = i.getFileName().toString();
            switch (file) {
            //Non mib files
            case "README-MIB.txt":
            case "readme.txt":
            case "smux.txt":
            case "notifications.txt":
            case "Makefile.mib":
            case "dpi11.txt":
            case "GbE mib descriptions.txt":
            case "Gbe2 mib descriptions.txt":
            case "rfc1228.txt":
            case "dpi20ref.txt":
            case "dpiSimple.mib":
            case "TEST-MIB.my":
                // bad mibs
            case "mibs_f5/F5-EM-MIB.txt":
            case "rfc/HPR-MIB.txt":
            case "test-mib-v1smi.my":   // Empty mib
            case "test-mib.my":         // Empty mib
            case "CISCO-OPTICAL-MONITORING-MIB.my": // Bad EOL
            case "CISCO-ATM-PVCTRAP-EXTN-CAPABILITY.my": //What is a VARIATION
            case "rfc1592.txt": //RFC with text
            case "rfc1227.txt": //RFC with text
            case "IBM-WIN32-MIB.mib": // A rfc with a lone \ before "
            case "view.my":      //Invalid comment, line 220:23
            case "CIMWIN32-MIB.mib": // Invalid escape sequence, line 1205:34
            case "IBMIROCAUTH-MIB.mib": // raw IP address, line 426:14
                return false;
            default:
                return (file.toLowerCase().endsWith(".mib") || file.toLowerCase().endsWith(".txt") || file.toLowerCase().endsWith(".my"));
            }
        };


        loader.load(Files.find(Paths.get(mibs.toUri()), 10, matcher));
        //System.out.println(loader.modules.get("RFC1213-MIB"));
        //System.out.println(loader.oids.get(new Symbol("RFC-1213", "mib-2")));

        //loader.textualConventions.entrySet().forEach( i -> {
        //    if (i.getValue().containsKey("DISPLAY-HINT")) {
        //        System.out.format("%s %s\n", i.getKey(), i.getValue().get("DISPLAY-HINT"));
        //    }
        //});
        //loader.oids.values().forEach(action);
        //loader.getSortedOids();
        //System.out.println(loader.getSortedOids());
        //System.out.println(loader.textualConventions);
        //loader.dumpModules();
        //loader.makeDot("/tmp/mibs.dot");
        MibTree tree = new MibTree(true);
        //System.out.println(loader.modules);
        loader.modules.forEach((i, j) -> {
            j.forEach((k, l) -> {
                if (l instanceof ModuleListener.ComplexObject) {
                    ModuleListener.ComplexObject<?> co = (ModuleListener.ComplexObject)l;
                    Map<OidInfos.Attribute, String> properties = new HashMap<>();
                    co.values.forEach((m,n) -> {
                        switch(m) {
                        case "STATUS":
                        case "DESCRIPTION":
                        case "SYNTAX":
                        case "ACCESS":
                        case "INDEX":
                            properties.put(OidInfos.Attribute.INDEX, n.toString());
                        case "MAX-ACCESS":
                        case "ORGANIZATION":
                        case "REVISION":
                            break;
                        default:
                            System.out.println(m);
                        }
                    });
                    OidInfos oi = new OidInfos(tree, properties);
                }
            });
        });
    }
}
