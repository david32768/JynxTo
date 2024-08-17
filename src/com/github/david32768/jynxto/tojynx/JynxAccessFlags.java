package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.RecordAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static jynx.Message.M148;

import jynx.LogIllegalArgumentException;

public class JynxAccessFlags {
    
    static EnumSet<jvm.AccessFlag> convert(Set<AccessFlag> flags) {
        return convert(flags, Collections.emptyList());
    }
    
    static EnumSet<jvm.AccessFlag> convert(Set<AccessFlag> flags, List<Attribute<?>> attributes) {
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

}
