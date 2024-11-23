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
import java.lang.classfile.instruction.SwitchCase;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.util.Collection;
import java.util.function.Function;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static jynx.Message.M603;
import static jynx.Message.M604;
import static jynx.Message.M605;
import static jynx.Message.M606;
import static jynx.Message.M607;
import static jynx.Message.M610;
import static jynx.Message.M611;

import jvm.NumType;
import jynx.Directive;
import jynx.ReservedWord;

import com.github.david32768.jynxto.utility.AbstractOpcodeVisitor;

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

    private static final int MAX_CODESIZE = 2*Short.MAX_VALUE + 1;
        
    private final int SWITCH_OVERHEAD = 1 + 2 + 1 + 4 + 1;
    // iload_ + padding + op + default + (cases) + return
    
    private final int MAX_LOOKUP_ENTRIES = (MAX_CODESIZE - (SWITCH_OVERHEAD + 4))/8; // = 8190
    
    @Override
    public void lookupSwitch(Opcode op, LookupSwitchInstruction inst) {
        var map = sortCases(op, inst.cases()); // find duplicate case values

        var deftarget = inst.defaultTarget();
        boolean removed = map.values()
                .removeIf(c -> c.target() == deftarget);
        if (removed) {
            // "case(s) with branch to default label in %s dropped"
            ptr.comment(M611, op);
        }
        long sz = map.size();
        if (sz > MAX_LOOKUP_ENTRIES) {
            // "number of entries in %s (%d) exceeds maximum possible %d"
            String msg = M607.format(op, sz, MAX_LOOKUP_ENTRIES);
            throw new IllegalArgumentException(msg);
        }

        switch_(op, deftarget, map.values());
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

    private final int MAX_TABLE_ENTRIES = (MAX_CODESIZE - (SWITCH_OVERHEAD + 4 + 4))/4; // = 16379
    
    @Override
    public void tableSwitch(Opcode op, TableSwitchInstruction inst) {
        var deflab = inst.defaultTarget();
        var cases = inst.cases();
        int low = inst.lowValue();
        int high = inst.highValue();

        var map = sortCases(op, cases);
        map.putIfAbsent(low, SwitchCase.of(low, deflab));
        map.putIfAbsent(high, SwitchCase.of(high, deflab));

        if (high < low) {
            // "high (%d) is less than low (%d) in %s"
            ptr.comment(M606, high, low, op);
        }
        int caselow = cases.getFirst().caseValue();
        int casehigh = cases.getLast().caseValue();
        if (caselow < low) {
            // "lowest case value (%d) is lower than low (%d)"
            ptr.comment(M604, caselow, low);
            low = caselow;
        }
        if (casehigh > high) {
            // "highest case value (%d) is higher than high (%d)"
            ptr.comment(M605, casehigh, high);
            high = casehigh;
        }

        long entryct = 1L + high - low;
        if (entryct > MAX_TABLE_ENTRIES) {
            // "number of entries in %s (%d) exceeds maximum possible %d"
            String msg = M607.format(op, entryct, MAX_TABLE_ENTRIES);
            throw new IllegalArgumentException(msg);
        }
        
        switch_(op, deflab, map.values());
    }

    @Override
    public void typeCheck(Opcode op, TypeCheckInstruction inst) {
        var type = inst.type();
        ptr.print(op, type).nl();
    }
    
    private SortedMap<Integer,SwitchCase> sortCases(Opcode opcode, List<SwitchCase> cases) {
        SortedMap<Integer,SwitchCase> map = new TreeMap<>();
        for (var c : cases) {
            var previous = map.putIfAbsent(c.caseValue(), c);
            if (previous != null) {
                if (c.target() == previous.target()) {
                    // "duplicate case %d in %s dropped"
                    ptr.comment(M603, c.caseValue(), opcode);                    
                } else {
                    // "ambiguous case %d in %s dropped"
                    ptr.comment(M610, c.caseValue(), opcode);
                }
            }
        }
        return map;
    }
    
    private void switch_(Opcode op, Label deflab, Collection<SwitchCase> cases) {
        String defname = labelName(deflab);
        ptr.print(op, ReservedWord.res_default, defname, ReservedWord.dot_array)
                .nl().incrDepth();
        long next = Integer.MIN_VALUE;
        for (SwitchCase c : cases) {
            int value = c.caseValue();
            assert value >= next;		
            var label = c.target();
            String labname = labelName(label);
            ptr.print(value, ReservedWord.right_arrow, labname).nl();
	    next = value + 1L; // next is long because value might be Integer.MAX_VALUE          
        }
        ptr.decrDepth().print(Directive.end_array).nl();
    }

}
