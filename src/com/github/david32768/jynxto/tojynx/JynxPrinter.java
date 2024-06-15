package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.AnnotationValue;
import java.lang.classfile.constantpool.AnnotationConstantValueEntry;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.DynamicConstantPoolEntry;
import java.lang.classfile.constantpool.FieldRefEntry;
import java.lang.classfile.constantpool.InterfaceMethodRefEntry;
import java.lang.classfile.constantpool.LoadableConstantEntry;
import java.lang.classfile.constantpool.MethodRefEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.classfile.constantpool.NameAndTypeEntry;
import java.lang.classfile.constantpool.PackageEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jvm.AccessFlag;
import jvm.HandleType;
import jynx.Directive;
import jynx.LogAssertionError;
import jynx.ReservedWord;
import jynx.ReservedWordType;
import jynx.StringUtil;

import static jynx.Message.M910;
import static jynx.ReservedWordType.LABEL;
import static jynx.ReservedWordType.NAME;
import static jynx.ReservedWordType.QUOTED;

public class JynxPrinter {
    
    private static final char TOKEN_SEPARATOR = ' ';
    private static final int DEPTH_UNDENT = 2;
    private static final char NEWLINE = '\n';

    private final StringBuilder sb;
    private final Consumer<String> consumer;
    
    private int depth;

    public JynxPrinter(Consumer<String> consumer) {
        this.sb = new StringBuilder();
        this.consumer = consumer;
        this.depth = 0;
    }
    
    private void startOfLine() {
        int indent = DEPTH_UNDENT * depth;
        for (int i = 0; i < indent;++i) {
            sb.append(TOKEN_SEPARATOR);
        }
    }
    
    private void sep() {
        if (sb.isEmpty()) {
            startOfLine();
        } else if (sb.charAt(sb.length() - 1) != TOKEN_SEPARATOR) {
            sb.append(TOKEN_SEPARATOR);
        }
    }

    public JynxPrinter start(int level) {
        sb.setLength(0);
        this.depth = level;
        return this;
    }

    public JynxPrinter incrDepth() {
        depth += 1;
        return this;
    }
    
    public JynxPrinter decrDepth() {
        depth -= 1;
        if (depth < 0) {
            throw new LogAssertionError(M910); // "indent depth is now negative"
        }
        return this;
    }
    
    private void printString(String string) {
        sep();
        sb.append(StringUtil.visible(string));
    }
    
    public JynxPrinter print(Object... objs) {
        ReservedWord res = null;
        for (var object : objs) {
            boolean wasRW = res != null;
            boolean isRW = object instanceof ReservedWord;
            if (!wasRW && !isRW) {
                print(object);              
            } else if (!wasRW && isRW) {
                res = (ReservedWord)object;
            } else if (wasRW && !isRW) {
                print(res, object);
                res = null;
            } else {
                print(res);
                print(object);
                res = null;
            }
        }
        if (res != null) {
            print(res);
        }
        return this;
    }
    
    public JynxPrinter printQuoted(String str) {
        printString(StringUtil.QuoteEscape(str));
        return this;
    }
    
    public JynxPrinter nl() {
        sb.append(NEWLINE);
        consumer.accept(sb.toString());
        sb.setLength(0);
        return this;
    }

    private  void print(ReservedWord res, Object value) {
        if (value instanceof Optional optvalue) {
            assert res.isOptional();
            if (optvalue.isEmpty()) {
                return;
            }
            value = optvalue.get();
        }
        print(res);
        var rwtype = res.rwtype();
        if (rwtype == ReservedWordType.TOKEN) {
            print(value);
        } else {
            printString(stringify(rwtype,value));
        }
    }
    
    private String stringify(ReservedWordType rwtype, Object obj) {
        String string;
        switch (obj) {
            case ClassEntry c -> {
               string = c.asInternalName(); 
            }
            default -> {
               string = obj.toString(); 
            }
        }
        switch(rwtype) {
            case NAME -> {
                return StringUtil.escapeName(string);
            }
            case QUOTED -> {
                return StringUtil.QuoteEscape(string);
            }
            case LABEL -> {
                return string;
            }
            default -> throw new EnumConstantNotPresentException(rwtype.getClass(), rwtype.name());
        }
    }
    
    public JynxPrinter println(Directive dir, Optional<?> value) {
        if (value.isPresent()) {
            print(dir, value.get()).nl();
        }
        return this;
    }

    public JynxPrinter printAccessName(Set<AccessFlag> flags, CharSequence name) {
        printFlags(flags);
        if (!name.isEmpty()) {
            printName(name.toString());
        }
        return this;
    }
    
    private void printFlags(Set<AccessFlag> flags) {
        for (var flag : flags) {
            String flagName = flag.toString().toLowerCase();
            if (flagName.equals("strict")) {
                flagName = "fpstrict";
            }
            printString(flagName);
        }
    }
    
    private void printName(String name) {
        printString(StringUtil.escapeName(name));
    }

    public JynxPrinter print(Object object) {
        Objects.requireNonNull(object);
        switch(object) {
            case Optional opt  -> {
                if (opt.isPresent()) {
                    print(opt.get());
                }
            }
            case DynamicCallSiteDesc c -> {
                printDynamic(c.invocationName(), c.invocationType().descriptorString(),
                        c.bootstrapMethod(), c.bootstrapArgs());
            }
            case ConstantDesc c -> {
                printConstant(c);
            }
            case AnnotationValue.OfConstant val -> {
                print(val);
            }
            case PoolEntry c -> {
                print(c);
            }
            default -> {
                printString(object.toString());
            }
        }
        return this;
    }

    private void printConstant(ConstantDesc cd) {
        switch(cd) {
            case Number c -> {
               printString(stringOf(c));
            }
            case String c -> {
                printString(c);
            }
            case ClassDesc c -> {
                printString(c.descriptorString());
            }
            case DirectMethodHandleDesc c -> {
                printString(stringOf(c));
            }
            case MethodHandleDesc c -> {
                printString(c.invocationType().descriptorString());
            }
            case MethodTypeDesc c -> {
                printString(c.descriptorString());
            }
            case DynamicConstantDesc c -> {
                printDynamic(c.constantName(), c.constantType().descriptorString(),
                        c.bootstrapMethod(), c.bootstrapArgs());
            }
            default -> {
                printString(cd.toString());
            }
        }
    }

    private void print(PoolEntry entry) {
        switch(entry) {
            case Utf8Entry c -> {
                printString(c.stringValue());
            }
            case AnnotationConstantValueEntry e -> {
                throw new UnsupportedOperationException();
            }
            case DynamicConstantPoolEntry e -> {
                throw new UnsupportedOperationException();
            }
            case ClassEntry c -> { // print without L; unlike LoadableConstantEntry
                printString(c.asInternalName());
            }
            case LoadableConstantEntry c -> {
                printConstant(c.constantValue());
            }
            case FieldRefEntry e -> {
                var name = e.owner().asInternalName() + "." + e.name().stringValue();
                var desc = e.typeSymbol().descriptorString();
                printString(name);
                printString(desc);
            }
            case InterfaceMethodRefEntry e -> {
                String nameDesc = e.name().stringValue() + e.type().stringValue();
                String ownerNameDesc = "@" + e.owner().asInternalName() + "." + nameDesc;
                printString(ownerNameDesc);
            }
            case MethodRefEntry e -> {
                String nameDesc = e.name().stringValue() + e.type().stringValue();
                String ownerNameDesc = e.owner().asInternalName() + "." + nameDesc;
                printString(ownerNameDesc);
            }
            case ModuleEntry e -> {
                printString(e.name().stringValue());
            }
             case NameAndTypeEntry e -> {
                throw new UnsupportedOperationException();
            }
            case PackageEntry e -> {
                printString(e.name().stringValue());
            }
       }
    }
    
    private void printDynamic(String name, String type,
            MethodHandleDesc bootstrapMethod, ConstantDesc[] bootstrapArgs) {
        
        print(ReservedWord.left_brace, name, type, bootstrapMethod);
        for (var arg : bootstrapArgs) {
            if (arg instanceof String str) {
                printQuoted(str);
            } else {
                print(arg);
            }
        }
        print(ReservedWord.right_brace);
    }
    
    private void print(AnnotationValue.OfConstant value) {
        switch(value) {
            case AnnotationValue.OfString val -> {
                printQuoted(val.stringValue());
            }
            case AnnotationValue.OfBoolean val -> {
                printString(val.booleanValue()?"true":"false");
            }
            case AnnotationValue.OfCharacter val -> {
                printString("'" + StringUtil.escapeChar(val.charValue()) + "'");
            }
            default -> {
                printConstant(value.constantValue());
            }
        }
    }
    
    private String stringOf(Number number) {
        String str = number.toString();
        switch(number) {
            case Byte _ -> {}
            case Short _ -> {}
            case Integer _ -> {}
            case Long _ -> {
                str += "L";
            }
            case Float c -> {
                str = Float.toHexString(c) + "F";
                if (c.isNaN() || c.compareTo(Float.MAX_VALUE) > 0) { // NaN or positive infinity
                    str = "+" + str;
                } 
            }
            case Double c -> {
                str = Double.toHexString(c);
                if (c.isNaN() || c.compareTo(Double.MAX_VALUE) > 0) { // NaN or positive infinity
                    str = "+" + str;
                } 
            }
            default -> {
                throw new UnsupportedOperationException();
            }
        }
        return str;
    }

    private String stringOf(ClassDesc owner) {
        String desc = owner.descriptorString();
        if (!owner.isArray() && !owner.isPrimitive()) {
            assert desc.charAt(0) == 'L';
            assert desc.charAt(desc.length() - 1) == ';';
            return desc.substring(1, desc.length() - 1);
        } else {
            return desc;
        }
    }
    
    private String stringOf(DirectMethodHandleDesc bsm) {
        var kind = HandleType.getInstance(bsm.refKind());
        String itf = bsm.isOwnerInterface()? "@": "";
        String owner = kind.getPrefix() + itf + stringOf(bsm.owner());
        String name = owner + "." + bsm.methodName();
        if (kind.isField()) {
            name += "()";
        }
        String nameDesc = name + bsm.lookupDescriptor();
        return nameDesc;
    }
    
}
