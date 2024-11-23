package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.CodeModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CustomAttribute;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.LineNumberTableAttribute;
import java.lang.classfile.attribute.LocalVariableTableAttribute;
import java.lang.classfile.attribute.LocalVariableTypeTableAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.instruction.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jynx.Directive.dir_limit;
import static jynx.Global.JVM_VERSION;
import static jynx.Global.LOG;
import static jynx.Message.M193;
import static jynx.Message.M22;
import static jynx.Message.M137;
import static jynx.Message.M167;
import static jynx.Message.M608;
import static jynx.ReservedWord.res_locals;
import static jynx.ReservedWord.res_stack;

import jvm.Context;
import jvm.Feature;
import jvm.FrameType;
import jynx.Directive;
import jynx.Global;
import jynx.GlobalOption;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

import com.github.david32768.jynxto.stack.StackChecker;
import com.github.david32768.jynxto.stack.StackMap;
import com.github.david32768.jynxto.utility.InstructionVisitor;

public class CodePrinter {

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
        
    private final StackChecker checker;
    private final StackMap stackMap;
    private final boolean printStack;

    private List<VerificationTypeInfo> previousLocals;
    private int nextlab;
    private int handlerIndex;
    
    CodePrinter(JynxPrinter ptr, StackMap stackmap, boolean printstack) {
        this.ptr = ptr.copy();
        this.printStack = printstack;
        this.labelNames = new HashMap<>();
        this.vars = new ArrayList<>();
        this.varSignatures = new HashMap<>();
        this.exceptAnnotation = new HashMap<>();
        this.labelAnnotation = new HashMap<>();
        this.pendingAnnotation = new ArrayList<>();
        this.varAnnotations = new ArrayList<>();
        this.previousLocals = Collections.emptyList();  // to print first stackmap in full
                                                        // or use stackmap.initialLocals() for changws
        this.stackMap = stackmap;
        this.checker = StackChecker.of(this.stackMap);
        this.nextlab = 0;
        this.handlerIndex = 0;
    }

    private String labelName(Label label) {
        return labelNames.computeIfAbsent(label, lab -> newLabel());
    }
    
    private String newLabel() {
        return "@L" + nextlab++;
    }
    
    void process(CodeModel cm, CodeAttribute codeAttribute) {
        assert codeAttribute != null;

        if (JVM_VERSION().supports(Feature.subroutines)) {
            checker.setJsrLabels(cm.elementList());
        }
        ptr.incrDepth().incrDepth();
        process(cm.attributes(), cm.elementList());
        ptr.decrDepth().decrDepth();
        int mystack = checker.maxStack();
        ptr.print(dir_limit, res_locals, codeAttribute.maxLocals()).nl();
        int codestack = codeAttribute.maxStack();
        if (codestack > mystack) {
            // "value required (%d) for %s is less than limit value (%d); %d used"
            ptr.comment(M193, mystack, res_stack, codestack, mystack);
        } else if (codestack < mystack) {
            // "value required (%d) for %s is more than limit value (%d); %d used"
            ptr.comment(M22, mystack, res_stack, codestack, mystack);
        }
        ptr.print(dir_limit, res_stack, mystack).nl();
    }

    
    public static void printElements(Consumer<String> consumer, List<CodeElement> elements) {
        var ptr = new JynxPrinter(consumer);
        var codeptr = new CodePrinter(ptr, StackMap.NONE, true);
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
            case StackMapTableAttribute _ -> {}
            default -> {
                UnknownAttributes.unknown(ptr.copy(), attribute, Context.CODE);
            }
        }
    }

    private void processElement(CodeElement element) {
        switch (element) {
            case Instruction inst -> {
                processInstruction(inst);
                checker.instruction(inst);
                if (printStack) {
                    var stackStr = checker.stackAsString();
                    if (stackStr.isPresent()) {
                        // "bci -> %d stack -> %s"
                        ptr.incrDepth().comment(M608, checker.offset(), stackStr.orElseThrow()).decrDepth();
                    }
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
        ptr.print(local.type());
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
                ptr.decrDepth().print(Directive.dir_line, line).nl().incrDepth();
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
        checker.labelBinding(label);

        String name = labelName(label) + ":";
        if (checker.isJsrLabel(label)) {
            ptr.decrDepth().print(name, "; subroutine", checker.stackAsString());
        } else {
            ptr.decrDepth().print(name, checker.stackAsDescriptor());
        }
        ptr.print(bciComment()).nl().incrDepth();
        var annotations = labelAnnotation.remove(label);
        if (annotations != null) {
            pendingAnnotation.addAll(annotations);
        }
        
        var locals = stackMap.localsFrameFor(label);
        if (locals != null && !Global.OPTION(GlobalOption.SKIP_FRAMES)) {
            ptr.print(Directive.dir_stack);

            boolean prefix = isPrefixOf(previousLocals, locals);
            if (prefix) {
                ptr.print(ReservedWord.res_use, res_locals);
            }
     
            ptr.nl().incrDepth();

            int start = prefix? previousLocals.size(): 0;
            for (var info : locals.subList(start, locals.size())) {
                processStackMap(ReservedWord.res_locals, info);
            }
            previousLocals = locals;
            for (var info : stackMap.stackFrameFor(label)) {
                processStackMap(ReservedWord.res_stack, info);
            }
            ptr.decrDepth().print(Directive.end_stack).nl();
        }
    }

    private <T> boolean isPrefixOf(List<T> list1, List<T> list2) {
        int sz1 = list1.size();
        int sz2 = list2.size();
        return sz1 != 0 && sz1 <= sz2 && list2.subList(0, sz1).equals(list1);
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
    }

    private String bciComment() {
        return "; bci=" + checker.offset();
    }
}
