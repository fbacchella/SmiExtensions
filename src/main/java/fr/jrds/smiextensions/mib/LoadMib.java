package fr.jrds.smiextensions.mib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizNode;
import org.anarres.graphviz.builder.GraphVizScope;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import fr.jrds.smiextensions.mib.ModuleListener.ComplexObject;
import fr.jrds.smiextensions.mib.ModuleListener.Import;
import fr.jrds.smiextensions.mib.ModuleListener.IntegerValue;
import fr.jrds.smiextensions.mib.ModuleListener.OidComponent;
import fr.jrds.smiextensions.mib.ModuleListener.OidType;

public class LoadMib {

    public static class Oid {
        final Map<Symbol, Oid> oids;
        private List<Integer> path = null;
        List<ModuleListener.OidComponent> components;

        Oid(ModuleListener.OidType source, Map<Symbol, Oid> oids) {
            this.oids = oids;
            this.components = source.value;
        }

        List<Integer> getPath() {
            if (path == null) {
                path = new ArrayList<>();
                try {
                    components.forEach( i-> {
                        if (i.value != null) {
                            path.add(i.value);
                        } else if (i.symbol != null) {
                            Oid parent = oids.get(i.symbol);
                            if (parent != null) {
                                List<Integer> parentpath = oids.get(i.symbol).getPath();
                                if (! parentpath.isEmpty()) {
                                    path.addAll(oids.get(i.symbol).getPath());
                                } else {
                                    throw new RuntimeException();
                                }
                            } else {
                                System.out.format("missing symbol %s in %s\n",i.symbol, components);
                                throw new RuntimeException();
                            }
                        }
                    });
                } catch (RuntimeException e) {
                    path.clear();
                }
            }
            return path;
        }
    }

    public final Map<String, String> displayHints = new HashMap<>();
    public final Map<String, Map<String, Object>> modules = new HashMap<>();
    public final Map<String, Map<String, Object>> textualConventions = new HashMap<>();
    public final Map<Symbol, Oid> oids = new HashMap<>();
    public final Map<Number, ComplexObject> trapstype = new HashMap<>();
    {
        // iso oid is not defined in any mibs, and may be called in different
        OidComponent iso = new OidComponent();
        iso.value = 1;
        oids.put(new Symbol("SNMPv2-SMI", "iso"), new Oid(new OidType(Collections.singletonList(iso)), oids));
        oids.put(new Symbol("RFC1155-SMI", "iso"), new Oid(new OidType(Collections.singletonList(iso)), oids));
        oids.put(new Symbol("SNMPv2-SMI-v1", "iso"), new Oid(new OidType(Collections.singletonList(iso)), oids));
    }

    public void load(Stream<Path> source) throws IOException {
        ModuleListener modulelistener = new ModuleListener();
        ANTLRErrorListener errorListener = new ModuleErrorListener();
        source
        .map(i -> i.toString())
        .map(i -> {
            try {
                return new ANTLRFileStream(i);
            } catch (IOException e) {
                return null;
            }
        })
        .filter(i -> i != null)
        .map(i -> new ASNLexer(i))
        .map(i -> new CommonTokenStream(i))
        .map(i -> {
            ASNParser parser = new ASNParser(i);
            parser.removeErrorListeners();
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);
            modulelistener.parser = parser;
            return parser;
        })
        .map(i -> {
            try {
                return i.moduleDefinition();
            } catch (ModuleException e) {
                System.err.println(e.getLocation());
                return null;
            }
        })
        .filter(i -> i != null)
        .forEach(i -> {
            try {
                ParseTreeWalker.DEFAULT.walk(modulelistener, i);
                Map<String, Object> assigned = new HashMap<>();
                modulelistener.objects.entrySet().forEach( j -> {
                    if (j.getValue() instanceof ModuleListener.TextualConvention) {
                        ModuleListener.TextualConvention tc = (ModuleListener.TextualConvention) j.getValue();
                        textualConventions.put(j.getKey(), tc.values);
                    } else if (j.getValue() instanceof ModuleListener.OidType) {
                        Symbol s = new Symbol(modulelistener.currentModule, j.getKey());
                        Oid oid = new Oid((ModuleListener.OidType) j.getValue(), oids);
                        oids.put(s, oid);
                    } else if (j.getValue() instanceof ModuleListener.ComplexObject) {
                        //System.out.format("%s %s %s\n", j.getKey(), j.getValue(), j.getValue().getClass());
                        ModuleListener.ComplexObject<?> co = (ModuleListener.ComplexObject<?>) j.getValue();
                        if (co.value instanceof ModuleListener.OidType) {
                            Symbol s = new Symbol(modulelistener.currentModule, j.getKey());
                            Oid oid = new Oid((ModuleListener.OidType) co.value, oids);
                            oids.put(s, oid);
                            assigned.put(j.getKey(), j.getValue());
                        } else if ("TRAP-TYPE".equals(co.name)){
                            Number n = ((IntegerValue) co.value).value;
                            trapstype.put(n, co);
                        } else {
                            System.out.format("%s %s\n", j.getKey(), j.getValue().getClass());
                            assigned.put(j.getKey(), j.getValue());
                        }
                    } else if (j.getValue() instanceof ModuleListener.Value) {
                        ModuleListener.Value value = (ModuleListener.Value) j.getValue();
                        if (value.value instanceof ModuleListener.OidType) {
                            Symbol s = new Symbol(modulelistener.currentModule, j.getKey());
                            Oid oid = new Oid((ModuleListener.OidType) value.value, oids);
                            oids.put(s, oid);
                        } else {
                            System.out.format("%s %s\n", j.getKey(), value.value.getClass());
                        }
                    } else if (j.getValue() instanceof ModuleListener.Type) {
                    } else if (j.getValue() instanceof ModuleListener.Import) {
                    } else if (j.getValue() instanceof ModuleListener.Macro) {
                    } else {
                        System.out.format("%s %s\n", j.getKey(), j.getValue().getClass());
                    }
                });
                modules.put(modulelistener.currentModule, assigned);
            } catch (ModuleException e) {
                System.err.println(e.getMessage() + " at " + e.getLocation());
            }
        });
        // Some broken symbols
        Symbol RFC1213MIB_mib2 = new Symbol("RFC1213-MIB", "mib-2");
        if (oids.containsKey(RFC1213MIB_mib2)) {
            oids.put(new Symbol("RFC-1213", "mib-2"), oids.get(RFC1213MIB_mib2));
        }
        Symbol enterprise = new Symbol("SNMPv2-SMI", "enterprise");
        if (oids.containsKey(enterprise)) {
            oids.put(new Symbol("RFC1065-SMI", "enterprise"), oids.get(enterprise));
        }
        for (String sname: new String[]{"rmon", "history", "statistics"} ) {
            Symbol rmon = new Symbol("RMON-MIB", sname);
            if (oids.containsKey(rmon)) {
                oids.put(new Symbol("RFC1271-MIB", sname), oids.get(rmon));
            }
        }
    }

    public void dumpModules() {
        modules.entrySet().forEach( i-> {
            System.out.format("%s {\n", i.getKey());
            i.getValue().entrySet().forEach( j -> {
                System.out.format("    %s: %s,\n", j.getKey(), j.getValue());
            });
            System.out.println("}");
        });
    }

    public void makeDot(String dotFileName) throws IOException {
        Map<String, GraphVizNode> nodes = new HashMap<>();
        GraphVizScope scope = new GraphVizScope.Impl();
        GraphVizGraph graph = new GraphVizGraph();
        modules.entrySet().forEach( i-> {
            GraphVizNode cm = graph.node(scope, i.getKey());
            cm.label(i.getKey());
            System.out.format("%s {\n", i.getKey());
            i.getValue().entrySet().forEach( j -> {
                System.out.format("    %s: %s,\n", j.getKey(), j.getValue());
                if (j.getValue() instanceof Import) {
                    Import mi = (Import) j.getValue();
                    GraphVizNode idNode = nodes.computeIfAbsent(mi.name, k -> graph.node(scope, k) );
                    idNode.label(mi.name);
                    graph.edge(idNode, cm);
                }
            });
            System.out.println("}");
        });
        //displayHints.putAll(modulelistener.displayHints);
        graph.writeTo(new File(dotFileName));
    }

    public Collection<Symbol> getSortedOids() {
        Map<Oid, Symbol> sortedoid = new TreeMap<>(new Comparator<Oid>() {

            @Override
            public int compare(Oid o1, Oid o2) {
                int sorted = Integer.compare(o1.getPath().size(), o2.getPath().size());
                if (sorted == 0) {
                    sorted = Integer.compare(o1.hashCode(), o2.hashCode());
                }
                return sorted;
            }
            
        });
        
        oids.entrySet().forEach(i -> {
            if (! i.getValue().getPath().isEmpty()) {
                sortedoid.put(i.getValue(), i.getKey());
            }
        });
        sortedoid.values().forEach( i -> System.out.format("%s %s\n", i, oids.get(i).getPath()));
        return sortedoid.values();
    }

}
