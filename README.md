# Jynx
[Jynx(bird)](https://en.wikipedia.org/wiki/Wryneck)

## JynxTo

Requires Java V24

Uses java.lang.classfile to produce Jynx file from Java class file.

Accessed via module com.github.david32768.JynxFree.

Usage:

```
 {JynxFree} tojynx {options}  class-name|class_file > .jx_file
   (produces a .jx file from a class)
   (any JYNX options are added to .version directive)


Options are:

 --VALHALLA Valhalla - limited support; may change
 --SKIP_CODE do not produce code
 --SKIP_DEBUG do not produce debug info
 --SKIP_FRAMES do not produce stack map
 --SKIP_ANNOTATIONS do not produce annotations
 --DOWN_CAST if necessary reduces JVM release to maximum supported by ASM version
 --SKIP_STACK do not print stack after each instruction
 --DEBUG print stack trace(s)
 --INCREASE_MESSAGE_SEVERITY treat warnings as errors etc.
```
if option SKIP_STACK is not present
 
*	After each instruction there is a comment of the current stack state and bci.
*	After each local variable instruction there is a comment of the name and type if attribute LocalVariableTable is present.
*	After each label there is a comment of the current exception handlers.


#Limitation

Custom Attributes are not supported.

If a label after an unconditional branch is not previously mentioned
and no StackMap is present then an empty stack is assumed.
However a stackmap will be generated provided that
 no JSR or RET is present in the class methods
 and option --SKIP-FRAMES is not specified.
