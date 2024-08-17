package com.github.david32768.jynxto.utility;

import static java.lang.classfile.Opcode.*;

import java.lang.classfile.instruction.*;

import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.util.EnumSet;

public interface InstructionVisitor {
    
    public void arrayLoad(Opcode op, ArrayLoadInstruction inst);
    public void arrayStore(Opcode op, ArrayStoreInstruction inst);
    public void branch(Opcode op, BranchInstruction inst);
    public void constant(Opcode op, ConstantInstruction inst);
    public void convert(Opcode op, ConvertInstruction inst);
    public void discontinued(Opcode op, DiscontinuedInstruction inst);
    public void field(Opcode op, FieldInstruction inst);
    public void invokeDynamic(Opcode op, InvokeDynamicInstruction inst);
    public void invoke(Opcode op, InvokeInstruction inst);
    public void increment(Opcode op, IncrementInstruction inst);
    public void load(Opcode op, LoadInstruction inst);
    public void store(Opcode op, StoreInstruction inst);
    public void lookupSwitch(Opcode op, LookupSwitchInstruction inst);
    public void monitor(Opcode op, MonitorInstruction inst);
    public void newMultiArray(Opcode op, NewMultiArrayInstruction inst);
    public void newObject(Opcode op, NewObjectInstruction inst);
    public void newPrimitiveArray(Opcode op, NewPrimitiveArrayInstruction inst);
    public void newReferenceArray(Opcode op, NewReferenceArrayInstruction inst);
    public void nop(Opcode op, NopInstruction inst);
    public void operator(Opcode op, OperatorInstruction inst);
    public void return_(Opcode op, ReturnInstruction inst);
    public void stack(Opcode op, StackInstruction inst);
    public void tableSwitch(Opcode op, TableSwitchInstruction inst);
    public void throw_(Opcode op, ThrowInstruction inst);
    public void typeCheck(Opcode op, TypeCheckInstruction inst);
    
    
    public static final String MISSING = "missing case for op - ";
    public static final String BAD_OP = "misclassified or unknown op - ";
    
    public static void visit(InstructionVisitor iv, Instruction instruction) {
        var op = instruction.opcode();
        switch (instruction) {
            case ArrayLoadInstruction inst -> {
                assert EnumSet.of(IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD)
                    .contains(op):BAD_OP + op;
                iv.arrayLoad(inst.opcode(), inst);
            }
            case ArrayStoreInstruction inst -> {
                assert EnumSet.of(IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE)
                    .contains(op):BAD_OP + op;
                iv.arrayStore(inst.opcode(), inst);
            }
            case BranchInstruction inst -> {
                assert EnumSet.of(IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IFNULL, IFNONNULL,
                        IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE,
                        GOTO, GOTO_W)
                    .contains(op):BAD_OP + op;
                iv.branch(inst.opcode(), inst);
            }
            case ConstantInstruction inst -> {
                assert EnumSet.of(ACONST_NULL, BIPUSH, SIPUSH, LDC, LDC_W, LDC2_W,
                        ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
                        LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1)
                    .contains(op):BAD_OP + op;
                iv.constant(inst.opcode(), inst);
            }
            case ConvertInstruction inst -> {
                assert EnumSet.of(I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S)
                        .contains(op):BAD_OP + op;
                iv.convert(inst.opcode(), inst);
            }
            case DiscontinuedInstruction inst -> {
                assert EnumSet.of(JSR, JSR_W, RET, RET_W).contains(op):BAD_OP + op;
                iv.discontinued(inst.opcode(), inst);
            }
            case FieldInstruction inst -> {
                assert EnumSet.of(GETSTATIC, PUTSTATIC, GETFIELD, PUTFIELD).contains(op):BAD_OP + op;
                iv.field(inst.opcode(), inst);
            }
            case InvokeDynamicInstruction inst -> {
                assert op == Opcode.INVOKEDYNAMIC:BAD_OP + op;
                iv.invokeDynamic(inst.opcode(), inst);
            }
            case InvokeInstruction inst -> {
                assert EnumSet.of(INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE)
                    .contains(op):BAD_OP + op;
                iv.invoke(inst.opcode(), inst);
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
                iv.load(inst.opcode(), inst);
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
                iv.store(inst.opcode(), inst);
            }
            case IncrementInstruction inst -> {
                assert EnumSet.of(IINC, IINC_W).contains(op):BAD_OP + op;
                iv.increment(inst.opcode(), inst);
            }
            case LookupSwitchInstruction inst -> {
                assert op == Opcode.LOOKUPSWITCH:BAD_OP + op;
                iv.lookupSwitch(inst.opcode(), inst);
            }
            case MonitorInstruction inst -> {
                assert EnumSet.of(MONITORENTER, MONITOREXIT).contains(op):BAD_OP + op;
                iv.monitor(inst.opcode(), inst);
            }
            case NewMultiArrayInstruction inst -> {
                assert op == Opcode.MULTIANEWARRAY:BAD_OP + op;
                iv.newMultiArray(inst.opcode(), inst);
            }
            case NewObjectInstruction inst -> {
                assert op == Opcode.NEW:BAD_OP + op;
                iv.newObject(inst.opcode(), inst);
            }
            case NewPrimitiveArrayInstruction inst -> {
                assert op == Opcode.NEWARRAY:BAD_OP + op;
                iv.newPrimitiveArray(inst.opcode(), inst);
            }
            case NewReferenceArrayInstruction inst -> {
                assert op == Opcode.ANEWARRAY:BAD_OP + op;
                iv.newReferenceArray(inst.opcode(), inst);
            }
            case NopInstruction inst -> {
                assert op == Opcode.NOP:BAD_OP + op;
                iv.nop(inst.opcode(), inst);
            }
            case OperatorInstruction inst -> {
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
                iv.operator(inst.opcode(), inst);
            }
            case ReturnInstruction inst -> {
                assert EnumSet.of(IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN)
                    .contains(op):BAD_OP + op;
                iv.return_(inst.opcode(), inst);
            }
            case StackInstruction inst -> {
                assert EnumSet.of(POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP)
                    .contains(op):BAD_OP + op;
                iv.stack(inst.opcode(), inst);
            }
            case TableSwitchInstruction inst -> {
                assert op == Opcode.TABLESWITCH:BAD_OP + op;
                iv.tableSwitch(inst.opcode(), inst);
            }
            case ThrowInstruction inst -> {
                assert op == Opcode.ATHROW:BAD_OP + op;
                iv.throw_(inst.opcode(), inst);
            }
            case TypeCheckInstruction inst -> {
                assert EnumSet.of(INSTANCEOF, CHECKCAST).contains(op):BAD_OP + op;
                iv.typeCheck(inst.opcode(), inst);
            }
        }
    }
}
