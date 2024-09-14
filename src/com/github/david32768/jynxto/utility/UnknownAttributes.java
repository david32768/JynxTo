package com.github.david32768.jynxto.utility;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.UnknownAttribute;

import static jynx.Global.LOG;
import static jynx.Message.M172;
import static jynx.Message.M173;

import jvm.Context;

public class UnknownAttributes {

    private UnknownAttributes() {}
    
    // known unknown or unknown known
    public static boolean unknown(Attribute attr, Context context) {
        if (attr instanceof UnknownAttribute uattr) {
            knowUnknown(uattr, context);
            return true;
        } else {
            unknownKnown(attr, context);
            return false;
        }
    }
 
    private static void unknownKnown(Attribute attr, Context context) {
        // "known attribute %s not catered for in context %s"
        LOG(M172, attr, context);
    }
 
    private static void knowUnknown(UnknownAttribute attr, Context context) {
        // "unknown attribute %s in context %s ignored"
        LOG(M173, attr, context);
    }
 
}
