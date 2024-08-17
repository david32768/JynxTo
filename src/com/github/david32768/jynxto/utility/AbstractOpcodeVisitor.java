package com.github.david32768.jynxto.utility;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.*;

public abstract class AbstractOpcodeVisitor implements InstructionVisitor {

    public abstract void noarg(Opcode op, Instruction inst);

    @Override
    public final void arrayLoad(Opcode op, ArrayLoadInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void arrayStore(Opcode op, ArrayStoreInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void convert(Opcode op, ConvertInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void monitor(Opcode op, MonitorInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void nop(Opcode op, NopInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void operator(Opcode op, OperatorInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void return_(Opcode op, ReturnInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void stack(Opcode op, StackInstruction inst) {
        noarg(op, inst);
    }

    @Override
    public final void throw_(Opcode op, ThrowInstruction inst) {
        noarg(op, inst);
    }

}
