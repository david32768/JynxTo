package com.github.david32768.jynxto.jynx;

import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.InnerClassInfo;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import jvm.JvmVersion;
import jynx.ClassType;

import jynx.Directive;

public record DirectiveAccessName(Directive dir, EnumSet<jvm.AccessFlag> flags, Optional<CharSequence> name) {

    public DirectiveAccessName {
       Objects.requireNonNull(dir);
       Objects.requireNonNull(flags);
       Objects.requireNonNull(name);
       flags = flags.clone();
    }
    
    public EnumSet<jvm.AccessFlag> flags() {
        return flags.clone();
    }
    
    public static DirectiveAccessName of(InnerClassInfo inner, JvmVersion jvmVersion) {
        var accessName = AccessName.of(inner);
        var flags = accessName.flags();
        ClassType classtype = ClassType.from(flags);
        flags.removeAll(classtype.getMustHave4Inner(jvmVersion));
        return new DirectiveAccessName(classtype.getInnerDir(), flags, accessName.name());        
    }    

    public static DirectiveAccessName of(ClassModel cm, JvmVersion jvmVersion) {
        var accessName = AccessName.of(cm);
        var flags = accessName.flags();
        ClassType classtype = ClassType.from(flags);
        flags.removeAll(classtype.getMustHave4Class(jvmVersion));
        return new DirectiveAccessName(classtype.getDir(), flags, accessName.name());        
    }    

}
