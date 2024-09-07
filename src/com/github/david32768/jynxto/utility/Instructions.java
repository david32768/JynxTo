package com.github.david32768.jynxto.utility;

import static java.lang.classfile.Opcode.*;

import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.OperatorInstruction;
import java.util.EnumSet;
import java.util.stream.Stream;

public class Instructions {
    
    public enum OperationType {
       ARRAY_LENGTH(ARRAYLENGTH),
       UNARY(INEG, LNEG, FNEG, DNEG),
       SHIFT(ISHL, ISHR, IUSHR, LSHL, LSHR, LUSHR),
       COMPARE(LCMP, FCMPG, FCMPL, DCMPG, DCMPL),
       BINARY(IADD, LADD, FADD, DADD,
                    ISUB, LSUB, FSUB, DSUB,
                    IMUL, LMUL, FMUL, DMUL,
                    IDIV, LDIV, FDIV, DDIV,
                    IREM, LREM, FREM, DREM,
                    IAND, LAND, IOR, LOR, IXOR, LXOR),
       ;
       
       private final EnumSet<Opcode> opcodes;

        private OperationType(Opcode opcode1, Opcode... opcodes) {
            this.opcodes = EnumSet.of(opcode1, opcodes);
        }
        
        public static OperationType of(OperatorInstruction opinst) {
            Opcode op = opinst.opcode();
            return Stream.of(values())
                    .filter(ot -> ot.opcodes.contains(op))
                    .findAny()
                    .orElseThrow();
        }
    }

    public enum BranchType {
        COMMAND(GOTO, GOTO_W),
        AUNARY(IFNULL, IFNONNULL),
        IUNARY(IFEQ, IFNE, IFLT, IFGT, IFLE, IFGE),
        ABINARY(IF_ACMPEQ, IF_ACMPNE),
        IBINARY(IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGT, IF_ICMPGE, IF_ICMPLE),
        ;

        private final EnumSet<Opcode> opcodes;

        private BranchType(Opcode opcode1, Opcode... opcodes) {
            this.opcodes = EnumSet.of(opcode1, opcodes);
        }
        
        public static BranchType of(BranchInstruction opinst) {
            Opcode op = opinst.opcode();
            return Stream.of(values())
                    .filter(ot -> ot.opcodes.contains(op))
                    .findAny()
                    .orElseThrow();
            
        }
        
    }
    
    private static final EnumSet<Opcode> UNCONDITIONAL = EnumSet.of(GOTO, GOTO_W,
            TABLESWITCH, LOOKUPSWITCH, ATHROW,
            ARETURN, IRETURN , LRETURN, FRETURN, DRETURN, RETURN);
    
    public static boolean isUnconditional(Opcode op) {
        return UNCONDITIONAL.contains(op);
    }
}
