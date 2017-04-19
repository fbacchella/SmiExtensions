package fr.jrds.smiextensions.mib;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;

import fr.jrds.smiextensions.mib.ASNParser.AssignementTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.AssignmentContext;
import fr.jrds.smiextensions.mib.ASNParser.BooleanValueContext;
import fr.jrds.smiextensions.mib.ASNParser.ChoiceTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.ComplexAssignementContext;
import fr.jrds.smiextensions.mib.ASNParser.ComplexAttributContext;
import fr.jrds.smiextensions.mib.ASNParser.DefinedTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.IntegerTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.IntegerValueContext;
import fr.jrds.smiextensions.mib.ASNParser.MacroAssignementContext;
import fr.jrds.smiextensions.mib.ASNParser.ModuleDefinitionContext;
import fr.jrds.smiextensions.mib.ASNParser.ObjIdComponentsListContext;
import fr.jrds.smiextensions.mib.ASNParser.SequenceOfTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.SequenceTypeContext;
import fr.jrds.smiextensions.mib.ASNParser.StringValueContext;
import fr.jrds.smiextensions.mib.ASNParser.SymbolsFromModuleContext;
import fr.jrds.smiextensions.mib.ASNParser.TextualConventionAssignementContext;
import fr.jrds.smiextensions.mib.ASNParser.TypeAssignmentContext;
import fr.jrds.smiextensions.mib.ASNParser.TypeContext;
import fr.jrds.smiextensions.mib.ASNParser.ValueAssignmentContext;

public class ModuleListener extends ASNBaseListener {

    static abstract class MibObject {

    };

    static class Import extends MibObject {
        public final String name;

        Import(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Imported from " + name;
        }

    };

    static class Macro extends MibObject {

        Macro() {
        }

        @Override
        public String toString() {
            return "Macro, ignored content";
        }

    };

    static class Value extends MibObject {
        public TypeDescription type;
        public Object value;

        Value(TypeDescription type, Object value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Value " + type + " ::= " + value;
        }

    };

    static class Type extends MibObject {
        public TypeDescription type;

        Type(TypeDescription type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type.toString();
        }

    };

    static class ObjectType extends MibObject {
        public String type;
        public Object value;

        ObjectType(String type, Object value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return type + " ::= " + value;
        }

    };

    static class MappedObject extends MibObject {
        String name;
        Map <String, Object> values = new HashMap<>();
        public MappedObject(String name) {
            this.name = name;
        }
        @Override
        public String toString() {
            return name + " " + values;
        }
    }

    static class ComplexObject<T> extends MappedObject {
        ValueType<T> value;
        public ComplexObject(String name) {
            super(name);
        }
        @Override
        public String toString() {
            return name + "/" + value + values;
        }
    }

    static class TextualConvention extends MappedObject {
        OidType oid;
        public TextualConvention() {
            super("TEXTUAL-CONVENTION");
        }
        @Override
        public String toString() {
            return name;
        }
    }

    //    static class TextualConvention extends MibObject {
    //        public final String displayHint;
    //
    //        TextualConvention(String displayHint) {
    //            this.displayHint = displayHint;
    //        }
    //
    //        @Override
    //        public String toString() {
    //            return String.format("Textual convention (%s)", displayHint != null ? "Display-Hint:" + displayHint : "");
    //        }
    //
    //    };



    static class Dummy extends MibObject {
        public final int assignementType;

        Dummy(int assignementType) {
            this.assignementType = assignementType;
        }

        @Override
        public String toString() {
            String assignementName;
            switch (assignementType) {
            case ASNParser.RULE_typeAssignment: assignementName = "type assignement";
            case ASNParser.RULE_valueAssignment: assignementName = "value assignement";
            default: assignementName = "Assignement " + Integer.toString(assignementType);
            }
            return assignementName;
        }

    };

    static class OidComponent {
        Integer value = null;
        Symbol symbol = null;
         @Override
        public String toString() {
            return String.format("%s%s", symbol !=null ? symbol.toString() : "", value != null ? String.format("(%d)", value): "");
        }
    }

    static abstract class ValueType<T> {
        T value;
        @Override
        public String toString() {
            return value.toString();
        }
    }

    static class OidType extends ValueType<List<OidComponent>> {
        OidType(List<OidComponent> value) {
            this.value = value;
        }
    }

    static class BooleanValue extends ValueType<Boolean> {
    }

    static class StringValue extends ValueType<String> {
        public StringValue(String value) {
            this.value = value;
        }
        @Override
        public String toString() {
            return "\"" + value + "\"";
        }
    }

    static class IntegerValue extends ValueType<Number> {
        IntegerValue(Number value) {
            this.value = value;
        }
    }

    enum BuiltinType {
        octetStringType,
        bitStringType,
        choiceType,
        enumeratedType,
        integerType,
        sequenceType,
        sequenceOfType,
        setType,
        setOfType,
        objectidentifiertype,
        objectClassFieldType,
        nullType,
        referencedType
    }

    static class TypeDescription {
        BuiltinType type;
        Object content = null;
        @Override
        public String toString() {
            return "" + type + (content != null ? " " + content : "");
        }
    }
    
    final Deque<Object> stack = new ArrayDeque<>();

    Parser parser;

    Map<String, Object> objects = new HashMap<>();
    Map<String, String> importedFrom = new HashMap<>();
    Map<OidType, MibObject> symbols = new HashMap<>();

    String currentModule = null;

    private String streamToText(Stream<ParserRuleContext> stream, String separator) {
        return String.join(separator, stream.map(i -> i.getText()).toArray( i-> new String[i]));
    }

    @Override
    public void enterModuleDefinition(ModuleDefinitionContext ctx) {
        if (ctx.IDENTIFIER() == null) {
            throw new ModuleException("No module name", parser.getInputStream().getSourceName());
        }
        currentModule = ctx.IDENTIFIER().getText();
        objects.clear();
        importedFrom.clear();
    }

    @Override
    public void enterSymbolsFromModule(SymbolsFromModuleContext ctx) {
        Import imported = new Import(ctx.globalModuleReference().getText());
        ctx.symbolList().symbol().stream()
        .forEach( i->  {
            objects.put(i.getText(), imported);
            importedFrom.put(i.getText(), ctx.globalModuleReference().getText());

        });
    }

    @Override
    public void exitAssignementType(AssignementTypeContext ctx) {
        if( ! (stack.peek() instanceof MibObject)) {
            stack.push(new Dummy(ctx.getChild(ParserRuleContext.class, 0).getRuleIndex()));
        }
    }

    //    @Override
    //    public void enterAssignementType(AssignementTypeContext ctx) {
    //        if (ctx.children == null) {
    //            throw new ModuleException("Invalid assignement", parser.getInputStream().getSourceName(), ctx.start);
    //        }
    //    }

    //    @Override
    //    public void enterTextualConventionDescription(TextualConventionDescriptionContext ctx) {
    //        if (ctx.dh != null) {
    //            stack.push(new TextualConvention(ctx.dh.getText()));
    //            //            AssignmentContext assignement = (AssignmentContext) ctx.parent.getParent();
    //            //            displayHints.put(assignement.IDENTIFIER().getText(), ctx.CSTRING().getText());
    //        }
    //    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        //System.out.format("%s\n", (new ModuleException("Invalid assignement", parser.getInputStream().getSourceName(), node.getSymbol()).getMessage()));
        throw new ModuleException("Invalid assignement: " + node.getText(), parser.getInputStream().getSourceName(), node.getSymbol());
        //System.out.format("Error in %s:\n,%s\n", parser.getInputStream().getSourceName(), node.getSymbol());
    }

    /****************************************
     * Manage assignemnts and push them on stack
     * assignements: objectTypeAssignement, valueAssignment, typeAssignment, textualConventionAssignement, macroAssignement
     ***************************************/

    @Override
    public void enterAssignment(AssignmentContext ctx) {
        stack.push(ctx.identifier.getText());
    }

    @Override
    public void exitAssignment(AssignmentContext ctx) {
        MibObject value = (MibObject) stack.pop();
        while (stack.peek() instanceof ValueType) {
            stack.pop();
        }
        //        if (! (stack.peek() instanceof String)) {
        //            System.out.println(new ModuleException("Invalid number " + ctx.getText(), parser.getInputStream().getSourceName(), ctx.start).getLocation());
        //
        //            //System.out.println(stack);
        //        }
        String name = (String) stack.pop();
        //        System.out.format("Assign %s = %s\n", name, value);
        objects.put(name, value);
//        if (value instanceof Value) {
//            Object v = ((Value) value).value;
//            System.out.format("value: %s\n", v);
//        } else if (value instanceof ComplexObject) {
//            ComplexObject v = ((ComplexObject) value);
//            System.out.format("ComplexObject: %s\n", v.value);
//        } else if (value instanceof Type) {
//            Type v = ((Type) value);
//            System.out.format("ComplexObject: %s\n", v.type);
//        } else {
//            System.out.format("Other: %s\n", value.getClass());
//        }
        stack.clear();
    }

    @Override
    public void enterComplexAssignement(ComplexAssignementContext ctx) {
        stack.push(new ComplexObject(ctx.macroName().getText()));
    }

    @Override
    public void exitComplexAssignement(ComplexAssignementContext ctx) {
        ValueType<?> value = (ValueType<?>) stack.pop();
        while ( ! (stack.peek() instanceof ComplexObject)) {
            stack.pop();
        }
        ((ComplexObject)stack.peek()).value = value;
        //         String type = ctx.macroName() == null ? null : ctx.macroName().getText();
        //        Object value;
        //        if (stack.peek() instanceof ValueType) {
        //            value = stack.pop();
        //        } else {
        //            value = ctx.value() == null ? null : ctx.value().getText();
        //        }
        //        stack.push(new ObjectType(type, value));
    }

    @Override
    public void exitMacroAssignement(MacroAssignementContext ctx) {
        while ( !(stack.peek() instanceof String)) {
            stack.pop();
        }
        //String macroName = (String) stack.peek();
        //ModuleException ex = new ModuleException("Invalid number " + ctx.getText(), parser.getInputStream().getSourceName(), ctx.start);
        //System.out.format("Macro %s %s\n", macroName, ex.getLocation());
        stack.push(new Macro());
    }

    @Override
    public void enterTextualConventionAssignement(TextualConventionAssignementContext ctx) {
        stack.push(new TextualConvention());
    }
    @Override
    public void exitTextualConventionAssignement(TextualConventionAssignementContext ctx) {
        while (! (stack.peek() instanceof TextualConvention)) {
            stack.pop();
        }
        //        stack.pop();
        //        Map<String, String> description = new HashMap<>();
        //        ctx.textualConventionDescription().forEach( i -> {
        //            if (i.val != null) {
        //                description.put(i.key.getText(), i.val.getText());
        //            }
        //        });
        //        stack.push(new TextualConvention(description.get("DISPLAY-HINT")));
    }

    @Override
    public void exitTypeAssignment(TypeAssignmentContext ctx) {
        stack.push(new Type((TypeDescription) stack.pop()));
    }

    @Override
    public void exitValueAssignment(ValueAssignmentContext ctx) {
        TypeDescription type = null;
        Object value;
        if (stack.peek() instanceof ValueType) {
            value = stack.pop();
        } else {
            value = ctx.value() == null ? null : ctx.value().getText();
        }
        type = (TypeDescription) stack.pop();
        stack.push(new Value(type, value));
    }

    /****************************************
     * Manage values and push them on stack
     ***************************************/

    @Override
    public void enterObjIdComponentsList(ObjIdComponentsListContext ctx) {
        //new StringBuilder(), i-> null
        //StringBuilder dest = new StringBuilder();
        List<OidComponent> oidParts = ctx.objIdComponents().stream().map( i-> {
            OidComponent oidc = new OidComponent();
            if( i.IDENTIFIER() != null) {
                String name = i.IDENTIFIER().getText();
                if (importedFrom.containsKey(name)) {
                    oidc.symbol = new Symbol(importedFrom.get(name), name);
                } else {
                    oidc.symbol = new Symbol(currentModule, name);
                }
            }
            if ( i.NUMBER() != null) {
                oidc.value = Integer.parseInt(i.NUMBER().getText());
            }
            return oidc;
        })
                .collect(ArrayList::new, ArrayList::add,
                        ArrayList::addAll);
        //ctx.objIdComponents().stream().map( i -> new StringBuilder(i.getText())).reduce( dest, (i, j) -> { i.append(j).append(" ") ; return i;});
        //System.out.println(oidParts);
        stack.push(new OidType(oidParts));

        //String path = streamToText(ctx.objIdComponents(), ".");
        //System.out.println(path);
    }

    @Override
    public void enterBooleanValue(BooleanValueContext ctx) {
        BooleanValue v = new BooleanValue();
        if (ctx.TRUE_LITERAL() != null || ctx.TRUE_SMALL_LITERAL() != null) {
            v.value = true;
        } else {
            v.value = false;
        }
        stack.push(v);
    }

    @Override
    public void enterIntegerValue(IntegerValueContext ctx) {
        Number v = null;
        try {
            if (ctx.signedNumber() != null) {
                v = new BigInteger(ctx.signedNumber().getText());
            } else if (ctx.hexaNumber() != null) {
                String hexanumber = ctx.hexaNumber().HEXANUMBER().getText();
                hexanumber = hexanumber.substring(1, hexanumber.length() - 2);
                if (! hexanumber.isEmpty()) {
                    v = new BigInteger(hexanumber, 16);
                } else {
                    v = 0;
                }
            } else if (ctx.binaryNumber() != null) {
                String binarynumber = ctx.binaryNumber().BINARYNUMBER().getText();
                binarynumber = binarynumber.substring(1, binarynumber.length() - 2);
                if (! binarynumber.isEmpty()) {
                    v = new BigInteger(binarynumber, 2);
                } else {
                    v = 0;
                }
            }
        } catch (Exception e) {
            throw new ModuleException("Invalid number " + ctx.getText(), parser.getInputStream().getSourceName(), ctx.start);
        }
        stack.push(new IntegerValue(v));

    }

    @Override
    public void enterStringValue(StringValueContext ctx) {
        String cstring = ctx.CSTRING().getText();
        cstring = cstring.substring(1, cstring.length() - 1);
        StringValue v = new StringValue(cstring);
        stack.push(v);
    }

    /****************************************
     * Manage complex attributes and push them on stack
     ***************************************/

    @Override
    public void exitComplexAttribut(ComplexAttributContext ctx) {
        String name = ctx.name.getText();
        Object value = null;

        if (ctx.stringValue() != null) {
            value = ctx.stringValue().getText();
        } else if (ctx.IDENTIFIER() != null) {
            value = ctx.IDENTIFIER().getText();
        } else if (ctx.objects() != null) {
            List<ValueType<?>> objects = new ArrayList<>();
            while( (stack.peek() instanceof ValueType)) {
                ValueType<?> vt = (ValueType<?>) stack.pop();
                objects.add(vt);
            }
            value = objects;
        } else if (ctx.groups() != null) {
            value = ctx.groups().IDENTIFIER().stream().map( i -> i.getText()).collect(ArrayList::new, ArrayList::add,
                    ArrayList::addAll);
        } else if (ctx.variables() != null) {
            value = ctx.variables().IDENTIFIER().stream().map( i -> i.getText()).collect(ArrayList::new, ArrayList::add,
                    ArrayList::addAll);
        } else if (ctx.notifications() != null) {
            value = ctx.notifications().IDENTIFIER().stream().map( i -> i.getText()).collect(ArrayList::new, ArrayList::add,
                    ArrayList::addAll);
        } else if (ctx.augments() != null) {
            value = ctx.augments().IDENTIFIER().stream().map( i -> i.getText()).collect(ArrayList::new, ArrayList::add,
                    ArrayList::addAll);
        } else if (ctx.index() != null) {
            //System.out.println("In index " + stack);
            //System.out.println(parser.getInputStream().getSourceName());
            List<String> types = new ArrayList<>();
            while (stack.peek() instanceof TypeDescription) {
                TypeDescription td = (TypeDescription) stack.pop();
                if (td.content != null) {
                    types.add(td.content.toString());
                }
            }
            value = types;
        } else if (stack.peek() instanceof ValueType) {
            value = stack.pop();
        } else if (stack.peek() instanceof TypeDescription) {
            value = stack.pop();
        }

        while( ! (stack.peek() instanceof MappedObject)) {
            stack.pop();
        }

        MappedObject co = (MappedObject) stack.peek();
        if ("DESCRIPTION".equals(name)) {
            value = "Some description";
        } else if ("CONTACT-INFO".equals(name)) {
            value = "Some description";
        }
        co.values.put(name, value);
    }

    /****************************************
     * Manage type
     ***************************************/

    @Override
    public void enterType(TypeContext ctx) {
        TypeDescription td = new TypeDescription();
        if (ctx.builtinType() != null) {
            switch(ctx.builtinType().getChild(ParserRuleContext.class, 0).getRuleIndex()) {
            case ASNParser.RULE_integerType:
                td.type = BuiltinType.integerType;
                break;
            case ASNParser.RULE_octetStringType:
                td.type = BuiltinType.octetStringType;
                break;
            case ASNParser.RULE_bitStringType:
                td.type = BuiltinType.bitStringType;
                break;
            case ASNParser.RULE_choiceType:
                td.type = BuiltinType.choiceType;
                break;
            case ASNParser.RULE_enumeratedType:
                td.type = BuiltinType.enumeratedType;
                break;
            case ASNParser.RULE_sequenceType:
                td.type = BuiltinType.sequenceType;
                break;
            case ASNParser.RULE_sequenceOfType:
                td.type = BuiltinType.sequenceOfType;
                break;
            case ASNParser.RULE_setType:
                td.type = BuiltinType.setType;
                break;
            case ASNParser.RULE_setOfType:
                td.type = BuiltinType.setOfType;
                break;
            case ASNParser.RULE_objectidentifiertype:
                td.type = BuiltinType.objectidentifiertype;
                break;
            case ASNParser.RULE_objectClassFieldType:
                td.type = BuiltinType.objectClassFieldType;
                break;
            case ASNParser.RULE_nullType:
                td.type = BuiltinType.nullType;
                break;
            }
        } else if (ctx.referencedType() != null) {
            td.type = BuiltinType.referencedType;
            td.content = ctx.referencedType().definedType();
        }
        stack.push(td);
    }

    @Override
    public void exitType(TypeContext ctx) {
        while ( ! (stack.peek() instanceof TypeDescription)) {
            stack.pop();
        }
    }

    @Override
    public void enterSequenceType(SequenceTypeContext ctx) {
        TypeDescription td = (TypeDescription) stack.peek();
        Map<String, TypeDescription> content = new LinkedHashMap<String, TypeDescription>();
        td.type = BuiltinType.sequenceType;
        ctx.namedType().forEach( i -> {
            content.put(i.IDENTIFIER().getText(), null);
        });
        td.content = content;
        stack.push("SEQUENCE");
    }

    @Override
    public void exitSequenceType(SequenceTypeContext ctx) {
        List<TypeDescription> nt = new ArrayList<>();
        while ( ! ("SEQUENCE".equals(stack.peek()))) {
            nt.add((TypeDescription)stack.pop());
        }
        stack.pop();
        int i = nt.size() - 1;
        TypeDescription td = (TypeDescription) stack.peek();
        @SuppressWarnings("unchecked")
        Map<String, TypeDescription> content = (Map<String, TypeDescription>) td.content;
        content.keySet().forEach( name -> {
            content.put(name, nt.get(i));
        });
    }

    @Override
    public void exitSequenceOfType(SequenceOfTypeContext ctx) {
        TypeDescription seqtd = (TypeDescription) stack.pop();
        TypeDescription td = (TypeDescription) stack.peek();
        td.content = "SEQUENCE OF " + seqtd.content;
    }

    @Override
    public void enterChoiceType(ChoiceTypeContext ctx) {
        TypeDescription td = (TypeDescription) stack.peek();
        Map<String, TypeDescription> content = new LinkedHashMap<String, TypeDescription>();
        td.type = BuiltinType.choiceType;
        ctx.namedType().forEach( i -> {
            content.put(i.IDENTIFIER().getText(), null);
        });
        td.content = content;
        stack.push("CHOICE");
    }

    @Override
    public void exitChoiceType(ChoiceTypeContext ctx) {
        List<TypeDescription> nt = new ArrayList<>();
        while ( ! ("CHOICE".equals(stack.peek()))) {
            nt.add((TypeDescription)stack.pop());
        }
        stack.pop();
        int i = nt.size() - 1;
        TypeDescription td = (TypeDescription) stack.peek();
        @SuppressWarnings("unchecked")
        Map<String, TypeDescription> content = (Map<String, TypeDescription>) td.content;
        content.keySet().forEach( name -> {
            content.put(name, nt.get(i));
        });
    }

    @Override
    public void enterIntegerType(IntegerTypeContext ctx) {
        if (ctx.namedNumberList() != null) {
            TypeDescription td = (TypeDescription) stack.peek();
            Map<String, Number> names = new HashMap<>();
            ctx.namedNumberList().namedNumber().forEach( i -> {
                Number value = new BigInteger(i.signedNumber().getText());
                String name = i.name.getText();
                names.put(name, value);
            });
            td.content = names;
        }
    }

    @Override
    public void enterDefinedType(DefinedTypeContext ctx) {
        TypeDescription td = (TypeDescription) stack.peek();
        td.content = ctx.getText();
    }

}
