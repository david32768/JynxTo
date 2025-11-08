package com.github.david32768.jynxto.jynx;

import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.ClassModel;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import com.github.david32768.jynxfree.jvm.AccessFlag;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.ClassType;
import com.github.david32768.jynxfree.jynx.Directive;

public record DirectiveAccessName(Directive dir,
        EnumSet<AccessFlag> flags, Optional<? extends CharSequence> optionalName) {

    public DirectiveAccessName {
       Objects.requireNonNull(dir);
       Objects.requireNonNull(flags);
       Objects.requireNonNull(optionalName);
       flags = flags.clone();
    }
    
    public EnumSet<AccessFlag> flags() {
        return flags.clone();
    }
    
    public static DirectiveAccessName of(InnerClassInfo inner, JvmVersion jvmVersion) {
        var accessName = AccessName.ofInner(inner);
        var flags = accessName.flags();
        ClassType classtype = ClassType.from(flags, jvmVersion);
        flags.removeAll(classtype.getMustHave4Inner(jvmVersion));
        return new DirectiveAccessName(classtype.getInnerDir(), flags, accessName.optionalName());        
    }    

    private static final String PACKAGE_INFO = "/package-info";
    
    public static DirectiveAccessName of(ClassModel cm, JvmVersion jvmVersion) {
        var accessName = AccessName.ofClass(cm);
        var flags = accessName.flags();
        var name = accessName.optionalName().orElse(null);
        ClassType classtype = ClassType.from(flags, jvmVersion);
        if (classtype == ClassType.INTERFACE && name.toString().endsWith(PACKAGE_INFO)) {
            classtype = ClassType.PACKAGE;
            name = name.subSequence(0, name.length() - PACKAGE_INFO.length());
        }
        flags.removeAll(classtype.getMustHave4Class(jvmVersion));
        return new DirectiveAccessName(classtype.getDir(), flags, Optional.ofNullable(name));        
    }    

}
