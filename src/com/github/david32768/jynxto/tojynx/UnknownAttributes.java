package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.attribute.UnknownAttribute;

import static com.github.david32768.jynxto.my.Message.M172;
import static com.github.david32768.jynxto.my.Message.M173;
import static com.github.david32768.jynxto.my.Message.M174;
import static com.github.david32768.jynxto.my.Message.M311;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.StandardAttribute;
import com.github.david32768.jynxfree.jynx.Global;

public class UnknownAttributes {

    private UnknownAttributes() {}
    
    public static boolean checkAttribute(JynxPrinter ptr, Attribute attribute, Context context) {
        switch(attribute) {
            case UnknownAttribute uattr -> {
                // "unknown attribute %s in context %s ignored"
                ptr.comment(M173, uattr, context);
                return false;
            }
            case SyntheticAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, com.github.david32768.jynxfree.jvm.AccessFlag.acc_synthetic);
                return false;
            }
            case DeprecatedAttribute attr -> {
                // "%s is omitted as pseudo_access flag %s is used"
                ptr.comment(M174, attr, com.github.david32768.jynxfree.jvm.AccessFlag.acc_deprecated);
                return false;
            }
            default -> {
                var jynxattr = StandardAttribute.getInstance(attribute.attributeName().stringValue());
                if (jynxattr == null || !jynxattr.inContext(context)) {
                    // "known attribute %s not catered for in context %s"
                    ptr.comment(M172, attribute, context);
                    return false;
                }                
                if (!Global.SUPPORTS(jynxattr) && jynxattr != StandardAttribute.StackMapTable) {
                    // "attribute %s not valid for version %s"
                    ptr.comment(M311, attribute, Global.JVM_VERSION());                
                    return false;
                }
            }
        }
        return true;
    }
}
