package com.github.david32768.jynxto.tojynx;

import java.io.PrintWriter;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxto.my.Message.M621;

import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.transform.ClassModels;
import com.github.david32768.jynxfree.transform.Transforms;

public class ToJynx {

    public static boolean toJynx(byte[] bytes, PrintWriter pw) {
        ClassFile classfile = classFile();
        ClassModel cm = classfile.parse(bytes);
        boolean hasStackMap = ClassModels.hasStackMap(cm);        
        if (OPTION(GlobalOption.UPGRADE_TO_V7)) {
            var upgrade = MainOption.UPGRADE.mainOptionService();
            byte[] smbytes = upgrade.callToBytes(cm);
            cm =  classfile.parse(smbytes);
            hasStackMap = true;
        } else if (!hasStackMap && !Global.OPTION(GlobalOption.SKIP_FRAMES)) {
            try {
                byte[] smbytes= Transforms.addStackMap(classfile, cm);
                cm =  classfile.parse(smbytes);
                hasStackMap = true;
            } catch (UnsupportedOperationException | IllegalArgumentException ex) { 
                // "%s not actioned as exception occured: %s"
                Global.LOG(M621, ClassFile.StackMapsOption.GENERATE_STACK_MAPS, ex.getMessage());
            }
        }

        var version = versionOf(cm);
        if (version == JvmVersion.V1_6JSR && hasStackMap) {
            version = JvmVersion.V1_6;
        }
        toJynx(pw, cm, version);
        String classname = cm.thisClass().asInternalName();
        return Global.END_MESSAGES(classname);
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
