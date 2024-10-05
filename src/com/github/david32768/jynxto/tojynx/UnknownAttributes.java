package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.attribute.UnknownAttribute;

import static jynx.Message.M172;
import static jynx.Message.M173;

import jvm.Context;
import static jynx.Message.M174;

public class UnknownAttributes {

    private UnknownAttributes() {}
    
    // known unknown or ignore known or unknown known
    public static void unknown(JynxPrinter ptr, Attribute attribute, Context context) {
        switch(attribute) {
            case UnknownAttribute uattr -> {
                // "unknown attribute %s in context %s ignored"
                ptr.comment(M173, uattr, context);                
            }
            case SyntheticAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, jvm.AccessFlag.acc_synthetic);
            }
            case DeprecatedAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, jvm.AccessFlag.acc_deprecated);
            }
            default -> {
                // "known attribute %s not catered for in context %s"
                ptr.comment(M172, attribute, context);                
            }
        }
    }
 
}
