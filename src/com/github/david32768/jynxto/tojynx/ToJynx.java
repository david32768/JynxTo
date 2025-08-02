package com.github.david32768.jynxto.tojynx;

import java.io.PrintWriter;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

import static com.github.david32768.jynxto.my.Message.M600;
import static com.github.david32768.jynxto.my.Message.M621;

import com.github.david32768.jynxfree.classfile.ClassModels;
import com.github.david32768.jynxfree.classfile.Transforms;
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
        boolean hasStackMap = ClassModels.hasStackMap(cm);        
        if (!hasStackMap && !Global.OPTION(GlobalOption.SKIP_FRAMES)) {
        var jsropcode = ClassModels.findDiscontinuedOpcode(cm);
            if (jsropcode.isPresent()) {
                // "%s not actioned as a method contains %s"
                Global.LOG(M600, ClassFile.StackMapsOption.GENERATE_STACK_MAPS, jsropcode.get());
            } else {
                try {
                    byte[] smbytes = Transforms.addStackMap(classfile, cm);
                    cm =  classfile.parse(smbytes);
                    hasStackMap = true;
                } catch (IllegalArgumentException ex) {
                    // "%s not actioned as exception occured: %s"
                    Global.LOG(M621, ClassFile.StackMapsOption.GENERATE_STACK_MAPS, ex.getMessage());
                }
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
