package com.github.david32768.jynxto.jynx;

import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.MethodParameterInfo;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleOpenInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;

import java.lang.classfile.AttributedElement;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jynx.Global;
import com.github.david32768.jynxfree.jynx.NameDesc;

public record AccessName(EnumSet<AccessFlag> flags, Optional<? extends CharSequence> optionalName) {

    public AccessName {
       Objects.requireNonNull(optionalName);
       Objects.requireNonNull(flags);
       flags = flags.clone();
    }

    private AccessName(EnumSet<AccessFlag> flags, CharSequence name) {
        this(flags, Optional.of(name));
    }
    
    public EnumSet<AccessFlag> flags() {
        return flags.clone();
    }
    
    public CharSequence name() {
        return optionalName.orElseThrow();
    }
    
    public static AccessName ofClass(ClassModel cm) {
        String name = cm.thisClass().asInternalName();
        Optional<CharSequence> optname;
        if (cm.isModuleInfo()) {
            assert name.equals("module-info");
            optname = Optional.empty();
        } else {
            optname = Optional.of(name);
        }
        var flags = flagsInContext(cm, cm.flags().flagsMask(), Context.CLASS);
        return new AccessName(flags, optname);
    }

    public static AccessName ofField(FieldModel fm) {
        var flags = flagsInContext(fm, fm.flags().flagsMask(), Context.FIELD);
        return new AccessName(flags, fm.fieldName());
    }

    public static AccessName ofInner(InnerClassInfo inner) {
        var flags = flagsInContext(inner, inner.flagsMask(), Context.INNER_CLASS);
        return new AccessName(flags, inner.innerClass().asInternalName());
    }

    public static AccessName ofParameter(MethodParameterInfo parm) {
        var flags = flagsInContext(parm, parm.flagsMask(), Context.PARAMETER);
        return new AccessName(flags, parm.name());
    }

    public static AccessName ofMethod(MethodModel mm) {
        Context context = NameDesc.INIT_NAME.isValid(mm.methodName().stringValue())?
                Context.INIT_METHOD
                : Context.METHOD;
        var flags = flagsInContext(mm, mm.flags().flagsMask(), context);
        String namedesc = mm.methodName().stringValue() + mm.methodType().stringValue();
        return new AccessName(flags, namedesc);
    }

    public static AccessName ofModule(ModuleAttribute module) {
        var flags = flagsInContext(module, module.moduleFlagsMask(),Context.MODULE);
        return new AccessName(flags, module.moduleName().name());
    }

    public static AccessName ofExport(ModuleExportInfo exported) {
        var flags = flagsInContext(exported, exported.exportsFlagsMask(), Context.EXPORT);
        return new AccessName(flags, exported.exportedPackage().name());
    }

    public static AccessName ofOpen(ModuleOpenInfo opened) {
        var flags = flagsInContext(opened, opened.opensFlagsMask(), Context.OPEN);
        return new AccessName(flags, opened.openedPackage().name());
    }

    public static AccessName ofRequire(ModuleRequireInfo required) {
        var flags = flagsInContext(required, required.requiresFlagsMask(), Context.REQUIRE);
        return new AccessName(flags, required.requires().name());
    }

    private static EnumSet<AccessFlag> flagsInContext(Object flagged, int flagsMask, Context context) {
        var flags = AccessFlag.getEnumSet(flagsMask, context, Global.JVM_VERSION());
        if (flagged instanceof AttributedElement attributed) {
            for (var attribute: attributed.attributes()) {
                switch(attribute) {
                    case SyntheticAttribute _ -> {
                        flags.add(AccessFlag.acc_synthetic);
                    }
                    case DeprecatedAttribute _ -> {
                        flags.add(AccessFlag.acc_deprecated);
                    }
                    case RecordAttribute _ when context == Context.CLASS -> {
                        flags.add(AccessFlag.acc_record);
                    }
                    default -> {}
                }
            }
        }
        return flags;
    }

}
