Requires Java24-36 (java.lang.classfile)

Uses java.lang.classfile to produce Jynx file from Java class file.

if option SKIP_STACK is not present
 
*	After each instruction there is a comment of the current stack state and bci.
*	After each local variable instruction there is a comment of the name and type if attribute LocalVariableTable is present.
*	After each label there is a comment of the current exception handlers.


Uses module com.github.david32768x.Jynx and hence also requires ASM.

#Limitation

Custom Attributes are not supported.

If a label after an unconditional branch is not previously mentioned
and no StackMap is present then an empty stack is assumed.
However a stackmap will be generated provided that
 no JSR or RET is present in the class methods
 and option --SKIP-FRAMES is not specified.