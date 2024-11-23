package com.github.david32768.jynxto.stack;

import java.lang.classfile.Attributes;
import java.lang.classfile.Label;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.SimpleVerificationTypeInfo;
import java.lang.classfile.attribute.StackMapFrameInfo.VerificationTypeInfo;
import java.lang.classfile.attribute.StackMapTableAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StackMap {
    
    public final static StackMap NONE = new StackMap(Collections.emptyMap(), Collections.emptyList());
    
    private final Map<Label,StackMapFrameInfo> stackMapInfo;
    private final List<StackMapFrameInfo.VerificationTypeInfo> initialLocals;
    

    private StackMap(Map<Label,StackMapFrameInfo> stackMapInfo, List<StackMapFrameInfo.VerificationTypeInfo> initialLocals) {
        this.stackMapInfo = stackMapInfo;
        this.initialLocals = initialLocals;
    }
    
    public static StackMap of(ClassDesc classDesc, MethodModel mm, CodeAttribute codeAttribute) {
        Objects.requireNonNull(codeAttribute);
        var info = getStackMap(codeAttribute.findAttribute(Attributes.stackMapTable()).orElse(null));
        var init = locals(classDesc, mm);
        return new StackMap(info, init);
    }
    
    private static Map<Label,StackMapFrameInfo> getStackMap(StackMapTableAttribute attr) {
        Map<Label,StackMapFrameInfo> stackmapinfo = new HashMap<>();
        if (attr != null) {
            for (var info : attr.entries()) {
                var mustBeNull = stackmapinfo.put(info.target(), info);
                assert mustBeNull == null;
            }
        }
        return stackmapinfo;
    }
    
    private  static List<VerificationTypeInfo> locals(ClassDesc classDesc, MethodModel mm) {
        var parms = mm.methodTypeSymbol().parameterList();
        List<StackMapFrameInfo.VerificationTypeInfo> result = new ArrayList<>();
        if (!mm.flags().has(AccessFlag.STATIC)) {
            String mname = mm.methodName().stringValue();
            if (mname.equals("<init>")) {
                result.add(SimpleVerificationTypeInfo.UNINITIALIZED_THIS);
            } else {
                result.add(verificationTypeInfoOf(classDesc));
            }
        }
        for (var parm : parms) {
            result.add(verificationTypeInfoOf(parm));
        }
        return result;
    }

    private static VerificationTypeInfo verificationTypeInfoOf(ClassDesc desc) {
        if (desc.isPrimitive()) {
            String primitive = desc.descriptorString();
            assert primitive.length() == 1;
            char first = primitive.charAt(0);
            return switch (first) {
                case 'J' -> SimpleVerificationTypeInfo.LONG;
                case 'F' -> SimpleVerificationTypeInfo.FLOAT;
                case 'D' -> SimpleVerificationTypeInfo.DOUBLE;
                default -> SimpleVerificationTypeInfo.INTEGER;
            };
        } else {
            return StackMapFrameInfo.ObjectVerificationTypeInfo.of(desc);
        }        
    }

    public List<VerificationTypeInfo> initialLocals() {
        return initialLocals;
    }
    
    public List<VerificationTypeInfo> stackFrameFor(Label label) {
        var info = stackMapInfo.get(label);
        return info == null? null: info.stack();
    }
        
    public List<VerificationTypeInfo> localsFrameFor(Label label) {
        var info = stackMapInfo.get(label);
        return info == null? null: info.locals();
    }
        
}
