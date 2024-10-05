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
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jvm.Context;
import jynx.Global;

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
        return of(cm.flags().flagsMask(), Context.CLASS, optname).adjustForAttributes(cm.attributes());
    }

    public static AccessName of(FieldModel fm) {
        return of(fm.flags().flagsMask(), Context.FIELD, fm.fieldName()).adjustForAttributes(fm.attributes());
    }

    public static AccessName of(InnerClassInfo inner) {
        return of(inner.flagsMask(), Context.INNER_CLASS, inner.innerClass().asInternalName());
    }

    public static AccessName of(MethodParameterInfo parm) {
        Optional<CharSequence> name = parm.name().map(Utf8Entry::stringValue);
        return of(parm.flagsMask(), Context.PARAMETER, name);
    }

    public static AccessName of(MethodModel mm) {
        String namedesc = mm.methodName().stringValue() + mm.methodType().stringValue();
        return of(mm.flags().flagsMask(), Context.METHOD, namedesc).adjustForAttributes(mm.attributes());
    }

    public static AccessName of(ModuleAttribute module) {
        return of(module.moduleFlagsMask(),Context.MODULE, module.moduleName().name());
    }

    public static AccessName of(ModuleExportInfo exported) {
        return of(exported.exportsFlagsMask(), Context.EXPORT, exported.exportedPackage().name());
    }

    public static AccessName of(ModuleOpenInfo opened) {
        return of(opened.opensFlagsMask(), Context.OPEN, opened.openedPackage().name());
    }

    public static AccessName of(ModuleRequireInfo required) {
        return of(required.requiresFlagsMask(), Context.REQUIRE, required.requires().name());
    }

    private static AccessName of(int flagsMask, Context context, CharSequence name) {
        assert !name.isEmpty();
        return of(flagsMask, context, Optional.of(name));
    } 

    private static AccessName of(int flagsMask, Context context, Optional<CharSequence> name) {
        var flags = jvm.AccessFlag.getEnumSet(flagsMask, context, Global.JVM_VERSION());
        return new AccessName(flags, name);
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
