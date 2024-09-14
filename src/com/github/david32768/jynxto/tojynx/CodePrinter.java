package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CustomAttribute;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jynx.Global.LOG;
import static jynx.Message.M137;
import static jynx.Message.M167;
import static jynx.Message.M339;

import jvm.Context;
import jvm.FrameType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

import com.github.david32768.jynxto.stack.StackChecker;
import com.github.david32768.jynxto.utility.InstructionVisitor;
import com.github.david32768.jynxto.utility.UnknownAttributes;

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
    
    private final StackChecker checker;
    
    private int nextlab;
    private int offset;
    private int handlerIndex;
    
    CodePrinter(JynxPrinter ptr, List<StackMapFrameInfo.VerificationTypeInfo> initialLocals) {
        this.ptr = ptr.nested();
        this.labelNames = new HashMap<>();
        this.vars = new ArrayList<>();
        this.varSignatures = new HashMap<>();
        this.exceptAnnotation = new HashMap<>();
        this.labelAnnotation = new HashMap<>();
        this.pendingAnnotation = new ArrayList<>();
        this.varAnnotations = new ArrayList<>();
        this.stackMapInfo = new HashMap<>();
        this.previousLocals = initialLocals;
        this.checker = new StackChecker();
        this.nextlab = 0;
        this.offset = 0;
        this.handlerIndex = 0;
    }

    private String labelName(Label label) {
        return labelNames.computeIfAbsent(label, lab -> "@L" + nextlab++);
    }
    
    void process(CodeModel cm) {
        ptr.incrDepth();
        process(cm.attributes(), cm.elementList());
        ptr.decrDepth();
    }
    
    public static void printElements(Consumer<String> consumer, List<CodeElement> elements) {
        var ptr = new JynxPrinter(consumer);
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
                for (var info : attr.entries()) {
                    var mustBeNull = stackMapInfo.put(info.target(), info);
                    assert mustBeNull == null;
                }
            }
            default -> {
                UnknownAttributes.unknown(attribute, Context.CODE);
            }
        }
    }

    private void processElement(CodeElement element) {
        switch (element) {
            case Instruction inst -> {
                processInstruction(inst);
                checker.instruction(inst);
                var stackStr = checker.stackAsString();
                if (stackStr.isPresent()) {
                    ptr.incrDepth().print("; stack = ", stackStr).nl().decrDepth();
                }
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
                // "type annotation %s not known"
                LOG(M167,target);
            }
        }
    }
    
    private void processPseudo(PseudoInstruction pseudo) {
        switch (pseudo) {
            case ExceptionCatch handler -> {
                processHandler(handler);
                checker.element(handler);
            }
            case LabelTarget target -> {
                processLabel(target);
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

    private void processLabel(LabelTarget target) {
        var label = target.label();
        var stackInfo = stackMapInfo.remove(label);
        if (stackInfo == null) {
            checker.pseudo(target);
        } else {
            checker.labelBinding(label, stackInfo);
        }

        String name = labelName(label) + ":";
        var stackStr = checker.stackAsDescriptor();
        if (checker.isJsrTarget(label)) {
            ptr.decrDepth().print(name, "; subroutine", stackStr).nl().incrDepth();
        } else {
            ptr.decrDepth().print(name, stackStr).nl().incrDepth();
        }
        
        var annotations = labelAnnotation.remove(label);
        if (annotations != null) {
            pendingAnnotation.addAll(annotations);
        }
        
        if (stackInfo != null && !Global.OPTION(GlobalOption.SKIP_FRAMES)) {
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
    
    private void processInstruction(Instruction instruction) {
        var instptr = new InstructionPrinter(ptr, this::labelName);
        InstructionVisitor.visit(instptr, instruction);
        ptr.incrDepth();
        AnnotationPrinter ap = new AnnotationPrinter(ptr);
        for (var annotation: pendingAnnotation) {
            ap.processRuntimeTypeAnnotation(annotation.visible(), annotation.annotation());
        }
        pendingAnnotation.clear();
        ptr.decrDepth();
        var op = instruction.opcode();
        int padding = 3 - (offset & 3);
        int size = switch(instruction) {
            case LookupSwitchInstruction swinst -> 1 + padding + 4 + 4 + 8*swinst.cases().size();                    
            case TableSwitchInstruction swinst -> 1 + padding + 4 + 4 + 4 + 4*swinst.cases().size();
            default -> op.sizeIfFixed();
        };
        assert size > 0;
        offset += size;
        if (offset > MAXSIZE) {
            // "maximum code size of %d exceeded; current size = [%d,%d]"
            throw new LogIllegalArgumentException(M339, MAXSIZE, offset, offset);
        }
    }

}
