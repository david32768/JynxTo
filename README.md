Requires Java24-ea+17 preview (java.lang.classfile)

Uses java.lang.classfile to produce Jynx file from Java class file.
After each instruction there is a comment of the current stack state.

Uses module com.github.david32768x.Jynx (V23 branch) and hence also requires ASM.

#Limitation

Custom Attributes are not supported.

If a label after an unconditional branch is not previously mentioned
and no StackMap is present then an empty stack is assumed.
However a stackmap will be generated provided that
 no JSR or RET is present in the class methods
 and option --skip-frames is not specified.
