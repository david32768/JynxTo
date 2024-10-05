package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Instruction;
import java.lang.classfile.Label;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.DiscontinuedInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.util.function.Function;

import jynx.Directive;
import jynx.ReservedWord;

import com.github.david32768.jynxto.utility.AbstractOpcodeVisitor;
import java.util.Arrays;
import jvm.NumType;

public class InstructionPrinter extends AbstractOpcodeVisitor {

    private final JynxPrinter ptr;
    private final Function<Label,String> labelNamer;

    public InstructionPrinter(JynxPrinter ptr, Function<Label, String> labelNamer) {
        this.ptr = ptr.copy();
        this.labelNamer = labelNamer;
    }

    private String labelName(Label label) {
        return labelNamer.apply(label);
    }
    
    @Override
    public void noarg(Opcode op, Instruction inst) {
        ptr.print(op).nl();
    }

    @Override
    public void branch(Opcode op, BranchInstruction inst) {
        var label = inst.target();
        String name = labelName(label);
        ptr.print(op, name).nl();
    }

    @Override
    public void constant(Opcode op, ConstantInstruction inst) {
        switch(inst) {
            case ConstantInstruction.IntrinsicConstantInstruction _ -> {
                ptr.print(op).nl();
            }
            case ConstantInstruction.ArgumentConstantInstruction arginst -> {
                Integer value = arginst.constantValue();
                ptr.print(op, value).nl();
            }
            case ConstantInstruction.LoadConstantInstruction _ -> {
                var type = inst.constantValue();
                ptr.print(op);
                if (type instanceof String str) {
                    ptr.printQuoted(str);
                } else {
                    ptr.print(type);
                }
                ptr.nl();
            }
        }
    }

    @Override
    public void discontinued(Opcode op, DiscontinuedInstruction inst) {
        switch(inst) {
            case DiscontinuedInstruction.JsrInstruction jsrinst -> {
                var label = jsrinst.target();
                String name = labelName(label);
                ptr.print(op, name).nl();
            }
            case DiscontinuedInstruction.RetInstruction retinst -> {
                int slot = retinst.slot();
                ptr.print(op, slot).nl();
            }
        }
    }

    @Override
    public void field(Opcode op, FieldInstruction inst) {
        ptr.print(op, inst.field()).nl();
    }

    @Override
    public void invokeDynamic(Opcode op, InvokeDynamicInstruction inst) {
        DynamicCallSiteDesc dcs = DynamicCallSiteDesc
                .of(inst.bootstrapMethod(),
                        inst.name().stringValue(),
                        inst.typeSymbol(),
                        inst.bootstrapArgs().toArray(ConstantDesc[]::new));
        ptr.print(op).print(dcs).nl();
    }

    @Override
    public void invoke(Opcode op, InvokeInstruction inst) {
        ptr.print(op, inst.method()).nl();
    }

    @Override
    public void increment(Opcode op, IncrementInstruction inst) {
        int slot = inst.slot();
        int incr = inst.constant();
        ptr.print(op, slot, incr).nl();
    }

    @Override
    public void load(Opcode op, LoadInstruction inst) {
        if (inst.sizeInBytes() == 1) {
            ptr.print(op).nl();
        } else {
            int slot = inst.slot();
            ptr.print(op,slot).nl();
        }
    }

    @Override
    public void store(Opcode op, StoreInstruction inst) {
        if (inst.sizeInBytes() == 1) {
            ptr.print(op).nl();
        } else {
            int slot = inst.slot();
            ptr.print(op,slot).nl();
        }
    }

    @Override
    public void lookupSwitch(Opcode op, LookupSwitchInstruction inst) {
        var deflab = inst.defaultTarget();
        String defname = labelName(deflab);
        ptr.print(op, ReservedWord.res_default, defname, ReservedWord.dot_array)
                .nl().incrDepth();
        for (var valueLabel : inst.cases()) {
            int value = valueLabel.caseValue();
            var label = valueLabel.target();
            String labname = labelName(label);
            ptr.print(value, ReservedWord.right_arrow, labname).nl();
        }
        ptr.decrDepth().print(Directive.end_array).nl();
    }

    @Override
    public void newMultiArray(Opcode op, NewMultiArrayInstruction inst) {
        var type = inst.arrayType();
        int n = inst.dimensions();
        ptr.print(op, type, n).nl();
    }

    @Override
    public void newObject(Opcode op, NewObjectInstruction inst) {
        var type = inst.className();
        ptr.print(op, type).nl();
    }

    @Override
    public void newPrimitiveArray(Opcode op, NewPrimitiveArrayInstruction inst) {
        String type = NumType.getInstance(inst.typeKind().newarrayCode()).classType();
        ptr.print(op, type).nl();                
    }

    @Override
    public void newReferenceArray(Opcode op, NewReferenceArrayInstruction inst) {
        var type = inst.componentType();
        ptr.print(op, type).nl();
    }

    @Override
    public void tableSwitch(Opcode op, TableSwitchInstruction inst) {
        var deflab = inst.defaultTarget();
        String defname = labelName(deflab);
        ptr.print(op, ReservedWord.res_default, defname, ReservedWord.dot_array)
                .nl().incrDepth();
        long low = inst.lowValue();
        long high = inst.highValue();
        long size = high - low + 1;
        Label[] targets = new Label[(int)size];
        Arrays.fill(targets, deflab);
        for (var valueLabel : inst.cases()) {
            long value = valueLabel.caseValue();
            long index = value - low;
            targets[(int)index] = valueLabel.target();
        }
        for (int i = 0; i < size; ++i) {
            var label = targets[i];
            String labname = labelName(label);
            long value = low + i;
            ptr.print((int)value, ReservedWord.right_arrow, labname).nl();
        }
        ptr.decrDepth().print(Directive.end_array).nl();
    }

    @Override
    public void typeCheck(Opcode op, TypeCheckInstruction inst) {
        var type = inst.type();
        ptr.print(op, type).nl();
    }
    
}
