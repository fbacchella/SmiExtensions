package fr.jrds.smiextensions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.Test;

import fr.jrds.smiextensions.mib.ASNLexer;
import fr.jrds.smiextensions.mib.ASNParser;
import fr.jrds.smiextensions.mib.LoadMib;

public class TestParser {
    @Test
    public void test1() throws Exception {
        org.antlr.v4.gui.TestRig.main(new String[] {
                "fr.jrds.smiextensions.mib.ASN",
                "moduleDefinition",
                "-tokens",
                "-diagnostics",
                "-ps","example1.ps",
                getClass().getClassLoader().getResource("ADAPTEC-UNIVERSAL-STORAGE-MIB.txt").getFile()
        });
    }

    @Test
    public void test2() throws Exception {
        Files.find(Paths.get("/Users/fa4/src/net-snmp/mibs/"), 10, (i,j) -> true)
        .forEach(
                p -> {
                    try {
                        String mibname = p.getFileName().toString();
                        org.antlr.v4.gui.TestRig.main(new String[] {
                                "ASN",
                                "moduleDefinition",
                                "-tokens",
                                "-diagnostics",
                                "-ps","/tmp/" + mibname + ".ps",
                                p.toString()});
                    } catch (Exception e) {
                    }
                });

    }

    @Test
    public void test3() throws Exception {
        AtomicInteger bad = new AtomicInteger(0);
        AtomicInteger good = new AtomicInteger(0);
        try {
            BiPredicate<Path,BasicFileAttributes> matcher = (i,j) -> {
                String path = i.toString();
                switch (path) {
                case "mibs/cisco/README-MIB.txt":
                case "mibs/Compaq/readme.txt":
                case "mibs/ftp.cisco.com/pub/mibs/README-MIB.txt":
                case "mibs/IBM/readme.txt":
                case "mibs/IBM/smux/smux.txt":
                case "mibs/vmware/esx/notifications.txt":
                case "mibs/net-snmp/Makefile.mib":
                case "mibs/rfc/HPR-MIB.txt":
                case "mibs/mibs_f5/F5-EM-MIB.txt":
                    return false;
                default:
                    return Files.isRegularFile(i) && (path.toLowerCase().endsWith(".mib") || path.toLowerCase().endsWith(".txt"));
                }
            };
            // 
            Files.find(Paths.get("/tmp/mibs"), 10, matcher)
            .forEach(
                    p -> {
                        try {
                            CharStream cs = new ANTLRFileStream(p.toString());
                            ANTLRErrorListener errorListener = new ANTLRErrorListener() {
                                @Override
                                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                                    //System.err.format("file %s, line %d:%d at %s\n", p, line, charPositionInLine, offendingSymbol);
                                    //System.err.format("file %s, line %d:%d at %s: %s\n", p, line, charPositionInLine, offendingSymbol, msg);
                                    throw new RuntimeException(String.format("file %s, line %d:%d at %s: %s", p, line, charPositionInLine, offendingSymbol, msg));
                                    //                                    throw new RecognitionException(String.format("file %s, line %d:%d at %s: %s", p, line, charPositionInLine, offendingSymbol, msg), 
                                    //                                            recognizer,
                                    //                                            recognizer.getInputStream(), 
                                    //                                            null);
                                }

                                @Override
                                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                                }

                                @Override
                                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
                                }

                                @Override
                                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
                                }
                            };
                            ASNLexer lexer = new ASNLexer(cs);
                            // lexer.removeErrorListeners();
                            // lexer.addErrorListener(errorListener);
                            CommonTokenStream tokens = new CommonTokenStream(lexer);
                            ASNParser parser = new ASNParser(tokens);
                            parser.removeErrorListeners();
                            parser.addErrorListener(errorListener);
                            parser.moduleDefinition();
                            good.getAndIncrement();
                        } catch (RuntimeException e) {
                            bad.getAndIncrement();
                        } catch (IOException e) {
                        }
                    });
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }
        System.out.format("good: %d\n", good.get());
        System.out.format("bad: %d\n", bad.get());
    }
    
    @Test
    public void test4() throws Exception {
        BiPredicate<Path,BasicFileAttributes> matcher = (i,j) -> {
            String path = i.toString();
            switch (path) {
            case "/tmp/mibs/cisco/README-MIB.txt":
            case "/tmp/mibs/Compaq/readme.txt":
            case "/tmp/mibs/ftp.cisco.com/pub/mibs/README-MIB.txt":
            case "/tmp/mibs/IBM/readme.txt":
            case "/tmp/mibs/IBM/smux/smux.txt":
            case "/tmp/mibs/vmware/esx/notifications.txt":
            case "/tmp/mibs/net-snmp/Makefile.mib":
            case "/tmp/mibs/rfc/HPR-MIB.txt":
            case "/tmp/mibs/mibs_f5/F5-EM-MIB.txt":
            case "/tmp/mibs/IBM/dpi/dpi11.txt":
            case "/tmp/mibs/Compaq/GbE mib descriptions.txt":
            case "/tmp/mibs/Compaq/Gbe2 mib descriptions.txt":
            case "/tmp/mibs/IBM/dpi/rfc1228.txt":
            case "/tmp/mibs/IBM/dpi2/dpi20ref.txt":
            case "/tmp/mibs/IBM/dpi2/dpiSimple.mib":
                return false;
            default:
                return Files.isRegularFile(i) && (path.toLowerCase().endsWith(".mib") || path.toLowerCase().endsWith(".txt"));
            }
        };

        LoadMib loader = new LoadMib();
        
        loader.load(Files.find(Paths.get("/tmp/mibs"), 10, matcher));
    }

    @Test
    public void test5() throws Exception {
        Path mib = Paths.get(getClass().getClassLoader().getResource("ADAPTEC-UNIVERSAL-STORAGE-MIB.txt").getFile());
        org.antlr.v4.gui.TestRig.main(new String[] {
                "fr.jrds.smiextensions.mib.ASN",
                "moduleDefinition",
                "-ps","example1.ps",
                mib.toString()
        });

        LoadMib loader = new LoadMib();
        
        loader.load(Collections.singleton(mib).stream());
    }

}
