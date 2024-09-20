package com.github.david32768.jynxto.jynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.MethodParameterInfo;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleOpenInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.reflect.AccessFlag;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static jynx.Message.M148;

import jvm.JvmVersion;
import jynx.ClassType;
import jynx.Directive;
import jynx.LogIllegalArgumentException;


public record AccessName(EnumSet<jvm.AccessFlag> flags, Optional<CharSequence> name) {

    public AccessName {
       Objects.requireNonNull(flags);
       Objects.requireNonNull(name);
       flags = flags.clone();
    }

    public EnumSet<jvm.AccessFlag> flags() {
        return flags.clone();
    }
    
    public static AccessName of(ClassModel cm) {
        String name = cm.thisClass().asInternalName();
        Optional<CharSequence> optname;
        if (cm.isModuleInfo()) {
            assert name.equals("module-info");
            optname = Optional.empty();
        } else {
            optname = Optional.of(name);
        }
        return of(cm.flags().flags(), optname).adjustForAttributes(cm.attributes());
    }

    public static AccessName of(FieldModel fm) {
        return of(fm.flags().flags(), fm.fieldName()).adjustForAttributes(fm.attributes());
    }

    public static AccessName of(InnerClassInfo inner) {
        return of(inner.flags(), inner.innerClass().asInternalName());
    }

    public static AccessName of(MethodParameterInfo parm) {
        Optional<CharSequence> name = parm.name().map(Utf8Entry::stringValue);
        return of(parm.flags(), name);
    }

    public static AccessName of(MethodModel mm) {
        String namedesc = mm.methodName().stringValue() + mm.methodTypeSymbol().descriptorString();
        return of(mm.flags().flags(), namedesc).adjustForAttributes(mm.attributes());
    }

    public static AccessName of(ModuleAttribute module) {
        return of(module.moduleFlags(), module.moduleName().name());
    }

    public static AccessName of(ModuleExportInfo exported) {
        return of(exported.exportsFlags(), exported.exportedPackage().name());
    }

    public static AccessName of(ModuleOpenInfo opened) {
        return of(opened.opensFlags(), opened.openedPackage().name());
    }

    public static AccessName of(ModuleRequireInfo required) {
        return of(required.requiresFlags(), required.requires().name());
    }

    private static AccessName of(Set<AccessFlag> flags, CharSequence name) {
        assert !name.isEmpty();
        return of(flags, Optional.of(name));
    } 

    private static AccessName of(Set<AccessFlag> flags, Optional<CharSequence> name) {
        return new AccessName(convertFlags(flags), name);
    } 

    private static EnumSet<jvm.AccessFlag> convertFlags(Set<AccessFlag> flags) {
        EnumSet<jvm.AccessFlag> result = EnumSet.noneOf(jvm.AccessFlag.class);
        for (var flag : flags) {
            String flagname = flag.name().toLowerCase();
            var jvmflag = jvm.AccessFlag.fromString(flagname);
            if (jvmflag.isEmpty()) {
                // "unknown access flag %s" M148
                throw new LogIllegalArgumentException(M148, flagname);
            }
            result.add(jvmflag.get());
        }
        return result;
    } 

    private AccessName adjustForAttributes(List<Attribute<?>> attributes) {
        for (var attribute: attributes) {
            switch(attribute) {
                case SyntheticAttribute _ -> {
                    flags.add(jvm.AccessFlag.acc_synthetic);
                }
                case DeprecatedAttribute _ -> {
                    flags.add(jvm.AccessFlag.acc_deprecated);
                }
                case RecordAttribute _ -> {
                    flags.add(jvm.AccessFlag.acc_record);
                }
                default -> {}
            }
        }
        return this;
    } 

}
