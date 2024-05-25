package com.github.david32768.jynxto.tojynx;

import static java.lang.classfile.Opcode.*;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CustomAttribute;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.attribute.LocalVariableTypeTableAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.instruction.*;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jvm.FrameType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

import static jynx.Message.M137;
import static jynx.Message.M311;

public class CodePrinter {

    private static final int MAXSIZE = 2*Short.MAX_VALUE + 1;
    
    private static record VTypeAnnotation(boolean visible, TypeAnnotation annotation){}
    
    private static record Varxyzn(int slot, Label start, Label end, String name) {}
    
    private final JynxPrinter ptr;
    private final Map<Label,String> labelNames;
    private final List<LocalVariable> vars; 
    private final Map<Varxyzn,LocalVariableType> varSignatures; 
    
    private final Map<Integer, List<VTypeAnnotation>> exceptAnnotation;
    private final Map<Label, List<VTypeAnnotation>> labelAnnotation;
    private final List<VTypeAnnotation> pendingAnnotation;
    private final List<VTypeAnnotation> varAnnotations;
    private final Map<Label,StackMapFrameInfo> stackMapInfo;
    private final List<StackMapFrameInfo.VerificationTypeInfo> previousLocals;

    private int nextlab;
    private int offset;
    private int handlerIndex;
    
    CodePrinter(JynxPrinter ptr, List<StackMapFrameInfo.VerificationTypeInfo> initialLocals) {
        this.ptr = ptr;
        this.labelNames = new HashMap<>();
        this.vars = new ArrayList<>();
        this.varSignatures = new HashMap<>();
        this.exceptAnnotation = new HashMap<>();
        this.labelAnnotation = new HashMap<>();
        this.pendingAnnotation = new ArrayList<>();
        this.varAnnotations = new ArrayList<>();
        this.stackMapInfo = new HashMap<>();
        this.previousLocals = initialLocals;
        this.nextlab = 0;
        this.offset = 0;
        this.handlerIndex = 0;
    }

    private String labelName(Label label) {
        return labelNames.computeIfAbsent(label, lab -> "@L" + nextlab++);
    }
    
    void process(CodeModel cm) {
        ptr.incrDepth().incrDepth();
        process(cm.attributes(), cm.elementList());
        ptr.decrDepth();
        ptr.print(Directive.dir_limit, ReservedWord.res_locals, cm.maxLocals()).nl();
        ptr.print(Directive.dir_limit, ReservedWord.res_stack, cm.maxStack()).nl();
        ptr.decrDepth();
    }
    
    public static void printElements(Consumer<String> consumer, List<CodeElement> elements) {
        var ptr = new JynxPrinter(consumer);
        ptr.incrDepth();
        var codeptr = new CodePrinter(ptr, Collections.emptyList());
        for (var element : elements) {
            codeptr.processElement(element);
        }
    }
    
    private void process(List<Attribute<?>> attributes, List<CodeElement> elements) {
        for (var attribute : attributes) {
            preProcessAttribute(attribute);
        }
        for (var element : elements) {
            processElement(element);
        }
        assert exceptAnnotation.isEmpty();
        assert labelAnnotation.isEmpty();
        assert pendingAnnotation.isEmpty();
        assert stackMapInfo.isEmpty();
        for (var local : vars) {
            processLocalVariable(local);
        }
        assert varSignatures.isEmpty();
        for (var local : varAnnotations) {
            var ap = new AnnotationPrinter(ptr);
            ap.processLocalVarAnnotation(local.visible(), local.annotation(), labelNames);
        }
    }

    private void preProcessAttribute(Attribute<?> attribute) {
        switch(attribute) {
            case LineNumberTableAttribute _ -> {}
            case LocalVariableTableAttribute _  -> {}
            case LocalVariableTypeTableAttribute _ -> {}                
            case RuntimeInvisibleTypeAnnotationsAttribute attr -> {
                for (var annotation : attr.annotations()) {
                    processTypeAnnotation(false, (TypeAnnotation)annotation);
                }
            }
            case RuntimeVisibleTypeAnnotationsAttribute attr -> {
                for (var annotation : attr.annotations()) {
                    processTypeAnnotation(true, (TypeAnnotation)annotation);
                }
            }
            case StackMapTableAttribute attr -> {
                if (!Global.OPTION(GlobalOption.SKIP_FRAMES)) {
                    for (var info : attr.entries()) {
                        var mustBeNull = stackMapInfo.put(info.target(), info);
                        assert mustBeNull == null;
                    }
                }
            }
            default -> {
                ptr.print("; code attribute", attribute).nl();
                assert false;
            }
        }
    }

    private void processElement(CodeElement element) {
        switch (element) {
            case Instruction inst -> {
                processInstruction(inst);
            }
            case PseudoInstruction pseudo -> {
                processPseudo(pseudo);
            }
            case StackMapTableAttribute _ -> {}
            case RuntimeVisibleTypeAnnotationsAttribute _ -> {}
            case RuntimeInvisibleTypeAnnotationsAttribute _ -> {}
            case CustomAttribute _ -> {}
        }
    }

    private void processHandler(ExceptionCatch handler) {
        String type = handler.catchType()
                .map(ce -> ce.asInternalName())
                .orElse(ReservedWord.res_all.toString());
        ptr.decrDepth().print(Directive.dir_catch, type,
                ReservedWord.res_from, labelName(handler.tryStart()),
                ReservedWord.res_to, labelName(handler.tryEnd()),
                ReservedWord.res_using, labelName(handler.handler())
        ).nl().incrDepth();
        AnnotationPrinter ap = new AnnotationPrinter(ptr);
        var annotations = exceptAnnotation.remove(handlerIndex);
        if (annotations != null) {
            for (var annotation: annotations) {
                ap.processRuntimeTypeAnnotation(annotation.visible(), annotation.annotation());
            }
        }
        ++handlerIndex;
    }
    
    private void processLocalVariable(LocalVariable local) {
        var key = new Varxyzn(local.slot(), local.startScope(), local.endScope(), local.name().stringValue());
        var type = varSignatures.remove(key);
        ptr.print(Directive.dir_var, local.slot());
        ptr.print(ReservedWord.res_is, local.name());
        ptr.print(local.typeSymbol());
        if (type != null) {
            if (type.name().equals(local.name())) {
                ptr.print(ReservedWord.res_signature, type.signature());
            } else {
                // "local variable name %s is different from name in type %s"
                throw new LogIllegalArgumentException(M137, local.name(), type.name());
            }
        }
        String start = labelName(local.startScope());
        String end = labelName(local.endScope());
        ptr.print(ReservedWord.res_from, start, ReservedWord.res_to, end).nl();
    }

    private void processTypeAnnotation(boolean visible, TypeAnnotation annotation) {
        var type = new VTypeAnnotation(visible, annotation);
        var target = annotation.targetInfo();
        switch (target) {
            case TypeAnnotation.OffsetTarget t -> {
                labelAnnotation.computeIfAbsent(t.target(), l -> new ArrayList<>()).add(type);
            }
            case TypeAnnotation.TypeArgumentTarget t -> {
                labelAnnotation.computeIfAbsent(t.target(), l -> new ArrayList<>()).add(type);
            }
            case TypeAnnotation.CatchTarget t -> {
                exceptAnnotation.computeIfAbsent(t.exceptionTableIndex(), i -> new ArrayList<>()).add(type);
            }
            case TypeAnnotation.LocalVarTarget t -> {
                varAnnotations.add(type);
            }
            default -> {
                ptr.print("; type annotation info ", target).nl();
                assert false;
            }
        }
    }
    
    private void processPseudo(PseudoInstruction pseudo) {
        switch (pseudo) {
            case ExceptionCatch handler -> {
                processHandler(handler);
            }
            case LabelTarget inst -> {
                var label = inst.label();
                String name = labelName(label) + ":";
                ptr.decrDepth().print(name).nl().incrDepth();
                var annotations = labelAnnotation.remove(label);
                if (annotations != null) {
                    pendingAnnotation.addAll(annotations);
                }
                var stackInfo = stackMapInfo.remove(label);
                if (stackInfo != null) {
                    var locals = stackInfo.locals();
                    if (locals.equals(previousLocals)) {
                        ptr.print(Directive.dir_stack, ReservedWord.res_use, ReservedWord.res_locals)
                                .nl().incrDepth();
                    } else {
                        ptr.print(Directive.dir_stack).nl().incrDepth();
                        for (var info : locals) {
                            processStackMap(ReservedWord.res_locals, info);
                        }
                        previousLocals.clear();
                        previousLocals.addAll(locals);
                    }
                    for (var info : stackInfo.stack()) {
                        processStackMap(ReservedWord.res_stack, info);
                    }
                    ptr.decrDepth().print(Directive.end_stack).nl();
                }
            }
            case CharacterRange _ -> {} // ? whats this
            case LineNumber inst -> {
                int line = inst.line();
                ptr.decrDepth().print(Directive.dir_line, line).incrDepth().nl();
            }
            case LocalVariable lv -> {
                vars.add(lv);
            }
            case LocalVariableType lvt -> {
                var key = new Varxyzn(lvt.slot(), lvt.startScope(), lvt.endScope(), lvt.name().stringValue());
                var shouldBeNull = varSignatures.put(key, lvt);
                assert shouldBeNull == null;
            }
        }
    }

    private void processStackMap(ReservedWord res, StackMapFrameInfo.VerificationTypeInfo typeInfo) {
        int tag = typeInfo.tag();
        var frameType = FrameType.fromJVMType(tag);
        switch(typeInfo) {
            case StackMapFrameInfo.ObjectVerificationTypeInfo info -> {
                assert FrameType.ft_Object == frameType;
                ptr.print(res, frameType, info.className()).nl();
            }
            case StackMapFrameInfo.UninitializedVerificationTypeInfo info -> {
                assert FrameType.ft_Uninitialized == frameType;
                var label = info.newTarget();
                var labelName = labelNames.get(label);
                Objects.requireNonNull(labelName);
                ptr.print(res, frameType, labelName).nl();
            }
            case StackMapFrameInfo.SimpleVerificationTypeInfo info -> {
                assert !frameType.extra():
                        String.format("frame type = %s", frameType);
                assert res == ReservedWord.res_locals || frameType != FrameType.ft_Top:
                        String.format("frame type = %s res = %s", frameType, res);
                ptr.print(res, frameType).nl();
            }
        }
    }
    
    private static final String BAD_OP = "misclassified or unknown op - ";
    private static final String MISSING = "missing case for op - ";
    
    private void processInstruction(Instruction instruction) {
        var op = instruction.opcode();
        String opstr = op.name().toLowerCase();
        int size = op.sizeIfFixed();
        switch (instruction) {
            case ArrayLoadInstruction _ -> {
                assert EnumSet.of(IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case ArrayStoreInstruction _ -> {
                assert EnumSet.of(IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case BranchInstruction inst -> {
                assert EnumSet.of(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL,
                        IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                        GOTO, GOTO_W)
                    .contains(op):BAD_OP + op;
                var label = inst.target();
                String name = labelName(label);
                ptr.print(opstr, name).nl();
            }
            case ConstantInstruction.IntrinsicConstantInstruction _ -> {
                assert EnumSet.of(ACONST_NULL, 
                        ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                        LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1)
                    .contains(op):BAD_OP + op;
                assert op.sizeIfFixed() == 1;
                ptr.print(opstr).nl();
            }
            case ConstantInstruction.ArgumentConstantInstruction inst -> {
                assert EnumSet.of(BIPUSH, SIPUSH)
                    .contains(op):BAD_OP + op;
                Integer value = inst.constantValue();
                ptr.print(opstr, value).nl();
            }
            case ConstantInstruction.LoadConstantInstruction inst -> {
                assert EnumSet.of(LDC, LDC_W, LDC2_W)
                    .contains(op):BAD_OP + op;
                var type = inst.constantValue();
                ptr.print(opstr);
                if (type instanceof String str) {
                    ptr.printQuoted(str);
                } else {
                    ptr.print(type);
                }
                ptr.nl();
            }
            case ConvertInstruction _ -> {
                assert EnumSet.of(I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S)
                        .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case DiscontinuedInstruction.JsrInstruction inst -> {
                assert EnumSet.of(JSR, JSR_W).contains(op):BAD_OP + op;
                var label = inst.target();
                String name = labelName(label);
                ptr.print(opstr, name).nl();
            }
            case DiscontinuedInstruction.RetInstruction inst -> {
                assert EnumSet.of(RET, RET_W).contains(op):BAD_OP + op;
                int slot = inst.slot();
                ptr.print(opstr, slot).nl();
            }
            case FieldInstruction inst -> {
                assert EnumSet.of(GETSTATIC, PUTSTATIC, GETFIELD, PUTFIELD).contains(op):BAD_OP + op;
                ptr.print(opstr, inst.field()).nl();
            }
            case InvokeDynamicInstruction inst -> {
                assert op == Opcode.INVOKEDYNAMIC:BAD_OP + op;
                DynamicCallSiteDesc dcs = DynamicCallSiteDesc
                        .of(inst.bootstrapMethod(),
                                inst.name().stringValue(),
                                inst.typeSymbol(),
                                inst.bootstrapArgs().toArray(ConstantDesc[]::new));
                ptr.print(opstr).print(dcs).nl();
            }
            case InvokeInstruction inst -> {
                assert EnumSet.of(INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr, inst.method()).nl();
            }
            case LoadInstruction inst -> {
                assert EnumSet.of(ILOAD, LLOAD, FLOAD, DLOAD, ALOAD,
                        ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3,
                        LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3,
                        FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3,
                        DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3,
                        ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3,
                        ILOAD_W, LLOAD_W, FLOAD_W, DLOAD_W, ALOAD_W)
                    .contains(op):BAD_OP + op;
                if (inst.sizeInBytes() == 1) {
                    ptr.print(opstr).nl();
                } else {
                    int slot = inst.slot();
                    ptr.print(opstr,slot).nl();
                }
            }
            case StoreInstruction inst -> {
                assert EnumSet.of(ISTORE, LSTORE, FSTORE, DSTORE, ASTORE,
                        ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3,
                        LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3,
                        FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3,
                        DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3,
                        ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3,
                        ISTORE_W, LSTORE_W, FSTORE_W, DSTORE_W, ASTORE_W)
                    .contains(op):BAD_OP + op;
                if (inst.sizeInBytes() == 1) {
                    ptr.print(opstr).nl();
                } else {
                    int slot = inst.slot();
                    ptr.print(opstr,slot).nl();
                }
            }
            case IncrementInstruction inst -> {
                assert EnumSet.of(IINC, IINC_W).contains(op):BAD_OP + op;
                    int slot = inst.slot();
                    int incr = inst.constant();
                    ptr.print(opstr, slot, incr).nl();
            }
            case LookupSwitchInstruction inst -> {
                assert op == Opcode.LOOKUPSWITCH:BAD_OP + op;
                var deflab = inst.defaultTarget();
                String defname = labelName(deflab);
                ptr.print(opstr, ReservedWord.res_default, defname, ReservedWord.dot_array)
                        .nl().incrDepth();
                for (var valueLabel : inst.cases()) {
                    int value = valueLabel.caseValue();
                    var label = valueLabel.target();
                    String labname = labelName(label);
                    ptr.print(value, ReservedWord.right_arrow, labname).nl();
                }
                ptr.decrDepth().print(Directive.end_array).nl();
                int padding = 3 - (offset & 3);
                size = 1 + padding + 4 + 4 + 8*inst.cases().size();
            }
            case MonitorInstruction _ -> {
                assert EnumSet.of(MONITORENTER, MONITOREXIT).contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case NewMultiArrayInstruction inst -> {
                assert op == Opcode.MULTIANEWARRAY:BAD_OP + op;
                var type = inst.arrayType();
                int n = inst.dimensions();
                ptr.print(opstr, type, n).nl();
            }
            case NewObjectInstruction inst -> {
                assert op == Opcode.NEW:BAD_OP + op;
                var type = inst.className();
                ptr.print(opstr, type).nl();
            }
            case NewPrimitiveArrayInstruction inst -> {
                assert op == Opcode.NEWARRAY:BAD_OP + op;
                String type = inst.typeKind().descriptor();
                ptr.print(opstr, type).nl();                
            }
            case NewReferenceArrayInstruction inst -> {
                assert op == Opcode.ANEWARRAY:BAD_OP + op;
                var type = inst.componentType();
                ptr.print(opstr, type).nl();
            }
            case NopInstruction _ -> {
                assert op == Opcode.NOP:BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case OperatorInstruction _ -> {
                assert EnumSet.of(IADD, LADD, FADD, DADD,
                        ISUB, LSUB, FSUB, DSUB,
                        IMUL, LMUL, FMUL, DMUL,
                        IDIV, LDIV, FDIV, DDIV,
                        IREM, LREM, FREM, DREM,
                        INEG, LNEG, FNEG, DNEG,
                        ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR,
                        IAND, LAND, IOR, LOR, IXOR, LXOR,
                        LCMP, FCMPL, FCMPG, DCMPL, DCMPG,
                        ARRAYLENGTH)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case ReturnInstruction _ -> {
                assert EnumSet.of(IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case StackInstruction _ -> {
                assert EnumSet.of(POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP)
                    .contains(op):BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case TableSwitchInstruction inst -> {
                assert op == Opcode.TABLESWITCH:BAD_OP + op;
                var deflab = inst.defaultTarget();
                String defname = labelName(deflab);
                ptr.print(opstr, ReservedWord.res_default, defname, ReservedWord.dot_array)
                        .nl().incrDepth();
                for (var valueLabel : inst.cases()) {
                    int value = valueLabel.caseValue();
                    var label = valueLabel.target();
                    String labname = labelName(label);
                    ptr.print(value, ReservedWord.right_arrow, labname).nl();
                }
                ptr.decrDepth().print(Directive.end_array).nl();
                int padding = 3 - (offset & 3);
                size = 1 + padding + 4 + 4 + 4 + 4*inst.cases().size();
            }
            case ThrowInstruction _ -> {
                assert op == Opcode.ATHROW:BAD_OP + op;
                ptr.print(opstr).nl();
            }
            case TypeCheckInstruction inst -> {
                assert EnumSet.of(INSTANCEOF, CHECKCAST).contains(op):BAD_OP + op;
                var type = inst.type();
                ptr.print(opstr, type).nl();
            }
        }
        ptr.incrDepth();
        AnnotationPrinter ap = new AnnotationPrinter(ptr);
        for (var annotation: pendingAnnotation) {
            ap.processRuntimeTypeAnnotation(annotation.visible(), annotation.annotation());
        }
        pendingAnnotation.clear();
        ptr.decrDepth();
        assert size > 0;
        offset += size;
        if (offset > MAXSIZE) {
            // "maximum code size exceeded"
            throw new LogIllegalArgumentException(M311);
        }
    }

}
