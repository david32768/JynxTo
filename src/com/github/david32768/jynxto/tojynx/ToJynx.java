package com.github.david32768.jynxto.tojynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.instruction.DiscontinuedInstruction;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jvm.JvmVersion;
import jynx.ClassType;
import jynx.ClassUtil;
import jynx.Directive;
import jynx.Global;
import jynx.LogIllegalArgumentException;

import static jynx.GlobalOption.SKIP_DEBUG;
import static jynx.Message.M148;

public class ToJynx {

    static EnumSet<jvm.AccessFlag> convertFlags(Set<AccessFlag> flags) {
        return convertFlags(flags, Collections.emptyList());
    }
    
    static EnumSet<jvm.AccessFlag> convertFlags(Set<AccessFlag> flags, List<Attribute<?>> attributes) {
        EnumSet<jvm.AccessFlag> result = EnumSet.noneOf(jvm.AccessFlag.class);
        for (var flag : flags) {
            String name = flag == AccessFlag.STRICT? "fpstrict": flag.name().toLowerCase();
            var jvmflag = jvm.AccessFlag.fromString(name);
            if (jvmflag.isEmpty()) {
                // "unknown access flag %s" M148
                throw new LogIllegalArgumentException(M148, name);
            }
            result.add(jvmflag.get());
        }
        for (var attribute: attributes) {
            switch(attribute) {
                case SyntheticAttribute _ -> {
                    result.add(jvm.AccessFlag.acc_synthetic);
                }
                case DeprecatedAttribute _ -> {
                    result.add(jvm.AccessFlag.acc_deprecated);
                }
                case RecordAttribute _ -> {
                    result.add(jvm.AccessFlag.acc_record);
                }
                default -> {}
            }
        }
        result.remove(jvm.AccessFlag.acc_super);
        return result;
    } 

    public static void toJynx(String file) throws IOException {
        ClassFile classfile;
        if (Global.OPTION(SKIP_DEBUG)) {
            classfile = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG,
                    ClassFile.LineNumbersOption.DROP_LINE_NUMBERS);
        } else {
            classfile = ClassFile.of();
        }
        byte[] bytes = ClassUtil.getClassBytes(file);
        ClassModel cm = classfile.parse(bytes);

        var version = getVersion(cm);

        try (PrintWriter pw = new PrintWriter(System.out)) {
            JynxPrinter ptr = new JynxPrinter(pw::print);
            Global.setJvmVersion(version);
            ptr.print(Directive.dir_version, version.name()).nl();
            var flags = convertFlags(cm.flags().flags(), cm.attributes());

            ClassType classtype = ClassType.from(flags);
            flags.removeAll(classtype.getMustHave4Class(version));
            Directive dir = classtype.getDir();
            String name = cm.thisClass().asInternalName();
            ptr.print(dir);
            if (cm.isModuleInfo()) {
                assert name.equals("module-info");
                ptr.printAccessName(flags, "").nl();
            } else {
                ptr.printAccessName(flags, name).nl();
            }
            ptr.incrDepth();
            ClassHeaderPrinter chp = new ClassHeaderPrinter(ptr, version);
            chp.process(cm);
            if (!cm.isModuleInfo()) {
                ptr.decrDepth();
                for (var fm : cm.fields()) {
                    FieldPrinter fp = new FieldPrinter(ptr);
                    fp.process(fm);
                }
                for (var mm : cm.methods()) {
                    MethodPrinter mp = new MethodPrinter(ptr, cm.thisClass().asSymbol());
                    mp.process(mm);
                }
            }
        }
    }

    private static JvmVersion getVersion(ClassModel cm) {
        int major = cm.majorVersion();
        int minor = cm.minorVersion();
        var version = JvmVersion.from(major, minor);

        if (version == JvmVersion.V1_6JSR) {
            var jsr = anyCodeElementMatch(cm, e -> e instanceof DiscontinuedInstruction);
            if (!jsr) {
                version = JvmVersion.V1_6;
            }
        }
        
        return version;
    }
    
    private static boolean anyCodeElementMatch(ClassModel cm, Predicate<CodeElement> matchfn) {
        return cm.methods().stream()
                    .map(m -> m.code())
                    .filter(c -> c.isPresent())
                    .flatMap(c -> c.orElseThrow().elementStream())
                    .anyMatch(matchfn);
    }
}
