package com.github.david32768.jynxto.tojynx;

import java.io.PrintWriter;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.CodeTransform;
import java.lang.classfile.Opcode;

import java.util.Optional;

import static com.github.david32768.jynxto.my.Message.M600;

import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.GlobalOption;

public class ToJynx {

    public static boolean toJynx(byte[] bytes, PrintWriter pw) {
        ClassFile classfile = classFile();
        ClassModel cm = classfile.parse(bytes);
        if (Global.OPTION(GlobalOption.DEBUG)) {
            var debug = cm.toDebugString();
            System.err.println(debug);
        }
        boolean hasStackMap = hasStackMap(cm);        
        if (!hasStackMap && !Global.OPTION(GlobalOption.SKIP_FRAMES)) {
            var optcm = addStackMap(classfile, cm);
            if (optcm.isPresent()) {
                cm = optcm.get();
                hasStackMap = true;
            }
        }

        var version = versionOf(cm);
        if (version == JvmVersion.V1_6JSR && hasStackMap) {
            version = JvmVersion.V1_6;
        }
        toJynx(pw, cm, version);
	return true;
    }

    private static ClassFile classFile() {
        ClassFile classfile;
        if (Global.OPTION(GlobalOption.SKIP_DEBUG)) {
            classfile = ClassFile.of(
                    ClassFile.DebugElementsOption.DROP_DEBUG,
                    ClassFile.LineNumbersOption.DROP_LINE_NUMBERS
            );
        } else {
            classfile = ClassFile.of();
        }
        return classfile;
    }
    
    private static final AttributeMapper<StackMapTableAttribute> STACK_MAPPER = Attributes.stackMapTable();
    
    private static boolean hasStackMap(ClassModel cm) {
        return cm.methods().stream()
                .map(m -> m.code())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(c -> c.findAttribute(STACK_MAPPER).isPresent());
    }
    
    private static Optional<ClassModel> addStackMap(ClassFile classfile, ClassModel cm) {
        try {
            ClassTransform ct = ClassTransform.transformingMethodBodies(CodeTransform.ACCEPT_ALL);
            byte[] bytes = classfile.withOptions(
                        ClassFile.AttributesProcessingOption.PASS_ALL_ATTRIBUTES,
                        ClassFile.StackMapsOption.GENERATE_STACK_MAPS,
            // PATCH_DEAD_CODE is the default but as it alters the bytecode it is deliberately added
                        ClassFile.DeadCodeOption.PATCH_DEAD_CODE)
                    .transformClass(cm, ct);
            return Optional.of(classfile.parse(bytes));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            if (msg.contains(" jsr")) {
                // "%s not actioned as a method contains %s"
                Global.LOG(M600, ClassFile.StackMapsOption.GENERATE_STACK_MAPS, Opcode.JSR);
                return Optional.empty();
            } else {
                throw ex;
            }
        }
    }

    private static JvmVersion versionOf(ClassModel cm) {
        int major = cm.majorVersion();
        int minor = cm.minorVersion();
        return JvmVersion.from(major, minor);
    }
    
    public static void toJynx(ClassModel cm, JvmVersion version) {
        try (PrintWriter pw = new PrintWriter(System.out)) {
            toJynx(pw, cm, version);
        }
    }

    private static void toJynx(PrintWriter pw, ClassModel cm, JvmVersion version) {
        JynxPrinter ptr = new JynxPrinter(pw::print);
        Global.setJvmVersion(version);
        ClassPrinter cp = new ClassPrinter(ptr, version);
        cp.process(cm);
        pw.flush();
    }

}
