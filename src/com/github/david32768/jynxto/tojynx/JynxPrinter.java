package com.github.david32768.jynxto.tojynx;

import com.github.david32768.jynxto.jynx.AccessName;
import com.github.david32768.jynxto.jynx.DirectiveAccessName;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Opcode;
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
import java.lang.invoke.TypeDescriptor;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static jynx.Message.M911;
import static jynx.ReservedWordType.LABEL;
import static jynx.ReservedWordType.NAME;
import static jynx.ReservedWordType.QUOTED;

import jvm.AccessFlag;
import jvm.HandleType;
import static jynx.Global.LOG;
import jynx.LogAssertionError;
import jynx.LogMsgType;
import jynx.Message;
import jynx.ReservedWord;
import jynx.ReservedWordType;
import jynx.StringUtil;

public class JynxPrinter {
    
    private static final char TOKEN_SEPARATOR = ' ';
    private static final int DEPTH_UNDENT = 2;
    private static final char NEWLINE = '\n';

    private final StringBuilder sb;
    private final Consumer<String> consumer;
    private final int lwm;
    
    private int depth;

    public JynxPrinter(Consumer<String> consumer) {
        this(consumer, 0);
    }
    
    private JynxPrinter(Consumer<String> consumer, int lwm) {
        this.sb = new StringBuilder();
        this.consumer = consumer;
        this.lwm = lwm;
        this.depth = lwm;
    }
    
    public JynxPrinter copy() {
        return new JynxPrinter(consumer, depth);
    }
    
    public JynxPrinter nested() {
        return new JynxPrinter(consumer, depth + 1);
    }
    
    public JynxPrinter incrDepth() {
        depth += 1;
        return this;
    }
    
    public JynxPrinter decrDepth() {
        if (depth <= lwm) {
            // "indent depth would be below lower limit %d"
            throw new LogAssertionError(M911, lwm);
        }
        depth -= 1;
        return this;
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
                assert wasRW && isRW;
                print(res);
                res = (ReservedWord)object;
            }
        }
        if (res != null) {
            print(res);
        }
        return this;
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
                printDynamic(c.invocationName(), c.invocationType(),
                        c.bootstrapMethod(), c.bootstrapArgs());
            }
            case ConstantDesc c -> {
                printConstant(c);
            }
            case AnnotationValue.OfConstant val -> {
                printAnnotationValue(val);
            }
            case PoolEntry c -> {
                printPoolEntry(c);
            }
            case Opcode op -> {
                printString(op.name().toLowerCase());
            }
            case AccessName an -> {
                printAccessName(an.flags(), an.name());
            }
            case DirectiveAccessName dan -> {
                printString(dan.dir().toString());
                printAccessName(dan.flags(), dan.name());
            }
            default -> {
                printString(object);
            }
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

    public JynxPrinter comment(Message msg, Object... objs) {
        if (msg.getLogtype().compareTo(LogMsgType.INFO) > 0) {
            LOG(msg, objs);
        }
        assert sb.isEmpty();
        sep();
        sb.append(';');
        sep();
        String comment = msg.format(objs);
        sb.append(StringUtil.printable(comment));
        return nl();
    }
        
    private void printAccessName(Set<AccessFlag> flags, Optional<CharSequence> name) {
        printFlags(flags);
        name.ifPresent(this::printName);
    }
    
    private void printString(Object obj) {
        String str = switch(obj) {
            case TypeDescriptor type -> type.descriptorString();        
            case Utf8Entry utf8 -> utf8.stringValue();        
            default-> obj.toString();
        };
        printString(str);
    }
    
    private void printString(String string) {
        sep();
        sb.append(StringUtil.visible(string));
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

    private void print(ReservedWord res, Object value) {
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
    
    private void printFlags(Set<AccessFlag> flags) {
        for (var flag : flags) {
            printString(flag.toString());
        }
    }
    
    private void printName(CharSequence name) {
        printString(StringUtil.escapeName(name.toString()));
    }

    private void printConstant(ConstantDesc cd) {
        switch(cd) {
            case ClassDesc c -> {
                printString(c);
            }
            case MethodTypeDesc c -> {
                printString(c);
            }
            case DirectMethodHandleDesc c -> {
                printString(stringOf(c));
            }
            case MethodHandleDesc c -> {
                printString(c.invocationType());
            }
            case DynamicConstantDesc c -> {
                printDynamic(c.constantName(), c.constantType(),
                        c.bootstrapMethod(), c.bootstrapArgs());
            }
            case Long L -> {
                printString(L.toString() + 'L');
            }
            case Float c -> {
                String fstr = Float.toHexString(c) + "F";
                if (c.isNaN() || c.compareTo(Float.MAX_VALUE) > 0) { // NaN or positive infinity
                    fstr = "+" + fstr;
                } 
                printString(fstr);
            }
            case Double c -> {
                String dstr = Double.toHexString(c);
                if (c.isNaN() || c.compareTo(Double.MAX_VALUE) > 0) { // NaN or positive infinity
                    dstr = "+" + dstr;
                } 
                printString(dstr);
            }
            case Number n -> {
                printString(n);
            }
            case String s -> {
                printString(s);
            }
        }
    }

    private void printPoolEntry(PoolEntry entry) {
        switch(entry) {
            case Utf8Entry c -> {
                printString(c);
            }
            case AnnotationConstantValueEntry e -> {
                throw new UnsupportedOperationException("" + e);
            }
            case DynamicConstantPoolEntry e -> {
                throw new UnsupportedOperationException("" + e);
            }
            case ClassEntry c -> { // print without L; unlike LoadableConstantEntry
                printString(c.asInternalName());
            }
            case LoadableConstantEntry c -> {
                printConstant(c.constantValue());
            }
            case FieldRefEntry e -> {
                var name = e.owner().asInternalName() + "." + e.name().stringValue();
                printString(name);
                printString(e.type());
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
                printString(e.name());
            }
             case NameAndTypeEntry e -> {
                throw new UnsupportedOperationException("" + e);
            }
            case PackageEntry e -> {
                printString(e.name());
            }
       }
    }
    
    private void printDynamic(String name, TypeDescriptor type,
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
    
    private void printAnnotationValue(AnnotationValue.OfConstant value) {
        switch(value) {
            case AnnotationValue.OfString val -> {
                printQuoted(val.stringValue());
            }
            case AnnotationValue.OfBoolean val -> {
                printString(val.booleanValue()?"true":"false");
            }
            case AnnotationValue.OfChar val -> {
                printString("'" + StringUtil.escapeChar(val.charValue()) + "'");
            }
            default -> {
                printConstant(value.constant().constantValue());
            }
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
    
}
