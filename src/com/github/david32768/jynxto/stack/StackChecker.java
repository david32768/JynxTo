package com.github.david32768.jynxto.stack;


import java.lang.classfile.instruction.*;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CustomAttribute;
import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.PseudoInstruction;
import java.lang.classfile.TypeKind;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.david32768.jynxto.utility.Instructions;

public class StackChecker {

    private static final int MAX_CODESIZE = 2*Short.MAX_VALUE + 1;

    private final TypeKindStack stack;
    private final Map<Label, List<TypeKind>> labelStack;
    private final List<Label> afterGotoLabels;
    private final Set<Label> jsrLabels;
    private final StackMap stackMap;

    private boolean lastGoto;
    private boolean subroutine;
    private int offset;
    private boolean lastLabel;
    
    private StackChecker(Set<Label> jsrlabels, StackMap stackmap) {

        this.stack = new TypeKindStack();
        this.labelStack = new HashMap<>();
        this.afterGotoLabels = new ArrayList<>();
        this.jsrLabels = jsrlabels;
        this.lastGoto = false;
        this.subroutine = false;
        this.stackMap = stackmap;
        this.offset = 0;
        this.lastLabel = false;
    }

    public static StackChecker of(StackMap stackmap) {
        return new StackChecker(new HashSet<>(), stackmap);
    }
    
    public void setJsrLabels(List<CodeElement> elements) {
        for (var element : elements) {
            if (element instanceof DiscontinuedInstruction.JsrInstruction inst) {
                jsrLabels.add(inst.target());
            }
        }
    }

    public int maxStack() {
        return stack.maxStack();
    }

    public boolean isLastLabel() {
        return lastLabel;
    }
    
    public Optional<String> stackAsString() {
        if (lastGoto) {
            return Optional.empty();
        }
        var str = stack.stream()
                .map(t -> t.toString())
                .collect(Collectors.joining(",", "(", ")"));
        return Optional.of(str);
    }
    
    public Optional<String> stackAsDescriptor() {
        if (lastGoto) {
            return Optional.empty();
        }
        var desc = stack.stream()
                .map(StackChecker::descriptor)
                .collect(Collectors.joining("", "(", ")"));
        return Optional.of(desc);
    }

    public int offset() {
        return offset;
    }
    
    private static String descriptor(TypeKind kind) {
        TypeKind stacktype = kind.asLoadable();
        return switch(stacktype) {
            case INT -> "I";
            case LONG -> "J";
            case FLOAT -> "F";
            case DOUBLE -> "D";
            case REFERENCE -> "Ljava/lang/Object;";
            default -> throw new AssertionError("unexpected TypeKind for stack: " + stacktype);
        };
    }
    
    public boolean isJsrLabel(Label label) {
        return jsrLabels.contains(label);
    }

    private void checkSubroutine() {
        if (lastGoto) {
            subroutine = true;
        } else if (stack.peek() != TypeKind.REFERENCE){
            throw new IllegalStateException("subroutine top of stack is not reference");
        }
    }
    
    public void element(CodeElement element) {
        switch (element) {
            case Instruction inst -> {
                instruction(inst);
            }
            case PseudoInstruction pseudo -> {
                pseudo(pseudo);
            }
            case StackMapTableAttribute _ -> {}
            case RuntimeVisibleTypeAnnotationsAttribute _ -> {}
            case RuntimeInvisibleTypeAnnotationsAttribute _ -> {}
            case CustomAttribute _ -> {}
        }        
    }
    
    public void pseudo(PseudoInstruction pseudo) {
        switch (pseudo) {
            case ExceptionCatch handler -> {
                var exstack = List.of(TypeKind.REFERENCE);
                branch(handler.handler(), exstack);
            }
            case LabelTarget i -> {
                labelBinding(i.label());
            }
            case CharacterRange _ -> {}
            case LineNumber _ -> {}
            case LocalVariable _ -> {}
            case LocalVariableType _ -> {}
        }
    }

    private void setAfterLabels(List<TypeKind> mystack) {
        lastGoto = false;
        stack.set(mystack);
        for (var label: afterGotoLabels) {
            branch(label, mystack);
        }
        afterGotoLabels.clear();
        if (subroutine && stack.peek() != TypeKind.REFERENCE){
            throw new IllegalStateException("subroutine top of stack is not reference");
        }
        subroutine = false;
    }
    
    public void check(List<TypeKind> mystack) {
        if (lastGoto) {
            setAfterLabels(mystack);
        } else {
            compare(stack.toList(), mystack);
        }
    }
    
    public void labelBinding(Label label) {
        bindLabel(label);
        var stackInfo = stackMap.stackFrameFor(label);
        if (stackInfo != null) {
            var typeKindList = stackInfo.stream()
                    .map(StackChecker::convert)
                    .toList();
            check(typeKindList);
        }
        if (isJsrLabel(label)) {
            checkSubroutine();
        }
    }

    private void bindLabel(Label label) {
        lastLabel = true;
        if (lastGoto) {
            var myStack = labelStack.get(label);
            if (myStack == null) {
                afterGotoLabels.add(label);
            } else {
                setAfterLabels(myStack);
            }
        } else {
            branch(label);
        }
    }

    private static TypeKind convert(VerificationTypeInfo type) {
        return switch(type) {
            case StackMapFrameInfo.SimpleVerificationTypeInfo simple -> {
                yield switch(simple) {
                    case DOUBLE -> TypeKind.DOUBLE;
                    case FLOAT -> TypeKind.FLOAT;
                    case INTEGER  -> TypeKind.INT;
                    case LONG  -> TypeKind.LONG;
                    default -> TypeKind.REFERENCE;
                };
            }
            default -> TypeKind.REFERENCE;
        };
    }
    
    public void instruction(Instruction instruction) {
        lastLabel = false;
        var op = instruction.opcode();
        
        if (lastGoto) {
            if (afterGotoLabels.isEmpty()) {
                String msg = String.format("opcode %s is unreachable", op);
                throw new IllegalStateException(msg);
            }
            if (subroutine) {
                setAfterLabels(List.of(TypeKind.REFERENCE)); // ASSUMPTION
            } else {
                setAfterLabels(Collections.emptyList()); // ASSUMPTION
            }
        }
        
        int size = Instructions.sizeOfAt(instruction, offset);
        assert size > 0;
        offset += size;
        if (offset > MAX_CODESIZE) {
            String msg = String.format("maximum code size of %d exceeded; current size = [%d,%d]",
                    MAX_CODESIZE, offset, offset);
            throw new IllegalArgumentException(msg);
        }

        adjustStackForInstruction(instruction);
        
        if (Instructions.isUnconditional(op)) {
            lastGoto = true;
            stack.clear();
        }
    }

    private void branch(Label label) {
        branch(label, stack.toList());
    }
    
    private void branch(Label label, List<TypeKind> list) {
        var old = labelStack.putIfAbsent(label, list);
        if (old != null) {
            compare(old,list);
        }
    }

    private void compare(List<TypeKind> old, List<TypeKind> list) {
        if (!old.equals(list)) {
            String msg = String.format("mismatch stack for label, old = %s new = %s",
                    old, list);
            throw new IllegalArgumentException(msg);
        }
    }
    
    private void adjustStackForInstruction(Instruction instruction) {
        stack.adjustForInstruction(instruction);
        adjustTargets(instruction);
    }
    
    private void adjustTargets(Instruction instruction) {
        switch (instruction) {
            case BranchInstruction inst -> {
                branch(inst.target());
            }
            case DiscontinuedInstruction.JsrInstruction inst -> {
                var list = new ArrayList<>(stack.toList());
                list.add(TypeKind.REFERENCE); // return address
                branch(inst.target(), list);
                jsrLabels.add(inst.target());
            }
            case LookupSwitchInstruction inst -> {
                branch(inst.defaultTarget());
                for (SwitchCase switchCase : inst.cases()) {
                    branch(switchCase.target());
                }
            }
            case TableSwitchInstruction inst -> {
                branch(inst.defaultTarget());
                for (SwitchCase switchCase : inst.cases()) {
                    branch(switchCase.target());
                }
            }
            default -> {}
        }
    }
}
