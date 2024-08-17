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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StackChecker {

    private final TypeKindStack stack;
    private final Map<Label, List<TypeKind>> labelStack;
    private final List<Label> afterGotoLabels;
    private final List<Label> jsrLabels;

    private boolean lastGoto;
    
    public StackChecker() {

        this.stack = new TypeKindStack();
        this.labelStack = new HashMap<>();
        this.afterGotoLabels = new ArrayList<>();
        this.jsrLabels = new ArrayList<>();
        this.lastGoto = false;
    }

    public Optional<String> stackAsString() {
        if (lastGoto) {
            return Optional.empty();
        }
        var str = stack.toString();
        return Optional.of(str);
    }
    
    public Optional<String> stackAsDescriptor() {
        if (lastGoto) {
            return Optional.empty();
        }
        var desc = stack.descriptor();
        desc = desc.replace("L", "Ljava/lang/Object;");
        return Optional.of(desc);
    }
    
    public boolean isJsrTarget(Label target) {
        return jsrLabels.contains(target);
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
                var exstack = List.of(TypeKind.ReferenceType);
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

    private void labelBinding(Label label) {
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

    private void setAfterLabels(List<TypeKind> mystack) {
        lastGoto = false;
        stack.set(mystack);
        for (var label: afterGotoLabels) {
            branch(label, mystack);
        }
        afterGotoLabels.clear();
    }
    
    public void check(List<TypeKind> mystack) {
        if (lastGoto) {
            setAfterLabels(mystack);
        } else {
            compare(stack.toList(), mystack);
        }
    }
    
    public void labelBinding(Label label, StackMapFrameInfo stackInfo) {
        labelBinding(label);
        var typeKindList = stackInfo.stack().stream()
                .map(StackChecker::convert)
                .toList();
        check(typeKindList);
    }

    private static TypeKind convert(VerificationTypeInfo type) {
        return switch(type) {
            case StackMapFrameInfo.SimpleVerificationTypeInfo simple -> {
                yield switch(simple) {
                    case ITEM_DOUBLE -> TypeKind.DoubleType;
                    case ITEM_FLOAT -> TypeKind.FloatType;
                    case ITEM_INTEGER  -> TypeKind.IntType;
                    case ITEM_LONG  -> TypeKind.LongType;
                    default -> TypeKind.ReferenceType;
                };
            }
            default -> TypeKind.ReferenceType;
        };
    }
    
    public void instruction(Instruction instruction) {
        var op = instruction.opcode();
        
        if (lastGoto) {
            if (afterGotoLabels.isEmpty()) {
                String msg = String.format("opcode %s is unreachable", op);
                throw new IllegalStateException(msg);
            }
            setAfterLabels(Collections.emptyList()); // ASSUMPTION
        }
        
        adjustStackForInstruction(instruction);
        
        if (op.isUnconditionalBranch()) {
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
        switch (instruction) {
            case BranchInstruction inst -> {
                branch(inst.target());
            }
            case DiscontinuedInstruction.JsrInstruction inst -> {
                var list = new ArrayList<>(stack.toList());
                list.add(TypeKind.ReferenceType); // return address
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
