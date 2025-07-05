package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.attribute.UnknownAttribute;
import java.lang.classfile.CustomAttribute;

import static com.github.david32768.jynxto.my.Message.M172;
import static com.github.david32768.jynxto.my.Message.M173;
import static com.github.david32768.jynxto.my.Message.M174;

import com.github.david32768.jynxfree.jvm.Context;

public class UnknownAttributes {

    private UnknownAttributes() {}
    
    public static void process(JynxPrinter ptr, Attribute attribute, Context context) {
        switch(attribute) {
            case UnknownAttribute uattr -> {
                // "unknown attribute %s in context %s ignored"
                ptr.comment(M173, uattr, context);
            }
            case CustomAttribute cattr -> {
                // "unknown attribute %s in context %s ignored"
                ptr.comment(M173, cattr, context);
            }
            case SyntheticAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, com.github.david32768.jynxfree.jvm.AccessFlag.acc_synthetic);
            }
            case DeprecatedAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, com.github.david32768.jynxfree.jvm.AccessFlag.acc_deprecated);
            }
            default -> {
                // "known attribute %s not catered for in context %s"
                ptr.comment(M172, attribute, context);
            }
        }
    }
}
