package com.github.david32768.jynxto.tojynx;

import static java.lang.classfile.attribute.StackMapFrameInfo.SimpleVerificationTypeInfo.*;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.AnnotationDefaultAttribute;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.MethodParametersAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.attribute.StackMapFrameInfo;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.List;

import jynx.Directive;
import jynx.ReservedWord;

public class MethodPrinter {
    
    private final JynxPrinter ptr;

    private final ClassDesc classDesc;

    MethodPrinter(JynxPrinter ptr, ClassDesc classDesc) {
        this.ptr = ptr.copy();
        this.classDesc = classDesc;
    }

    void process(MethodModel mm) {
        String namedesc = mm.methodName().stringValue() + mm.methodTypeSymbol().descriptorString();
        ptr.nl().print(Directive.dir_method).printAccessName(
                JynxAccessFlags.convert(mm.flags().flags(), mm.attributes()), namedesc).nl();
        ptr.incrDepth();
        for (var attribute : mm.attributes()) {
            processAttribute(attribute);
        }
        var cm = mm.code();
        if (cm.isPresent()) {
            CodePrinter cp = new CodePrinter(ptr, locals(mm));
            cp.process(cm.get());
        }
        ptr.decrDepth().print(Directive.end_method).nl();
    }

    private void processAttribute(Attribute<?> attribute) {
        switch(attribute) {
            case ExceptionsAttribute attr -> {
                ptr.print(Directive.dir_throws, ReservedWord.dot_array)
                        .nl().incrDepth();
                for (var except : attr.exceptions()) {
                    ptr.print(except.asInternalName()).nl();
                }
                ptr.decrDepth().print(Directive.end_array).nl();

            }
            case SignatureAttribute attr -> {
                ptr.print(Directive.dir_signature, attr.signature()).nl();
            }
            case RuntimeVisibleParameterAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                ap.processParameterAnnotations(true, attr.parameterAnnotations());
            }
            case RuntimeInvisibleParameterAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                ap.processParameterAnnotations(false, attr.parameterAnnotations());
            }
            case RuntimeInvisibleAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                for (var annotation : attr.annotations()) {
                    ap.processRuntimeAnnotation(false, annotation);
                }
            }
            case RuntimeVisibleAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                for (var annotation : attr.annotations()) {
                    ap.processRuntimeAnnotation(true, annotation);
                }
            }
            case RuntimeInvisibleTypeAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                for (var annotation : attr.annotations()) {
                    ap.processRuntimeTypeAnnotation(false, (TypeAnnotation)annotation);
                }
            }
            case RuntimeVisibleTypeAnnotationsAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                for (var annotation : attr.annotations()) {
                    ap.processRuntimeTypeAnnotation(true, (TypeAnnotation)annotation);
                }
            }
            case AnnotationDefaultAttribute attr -> {
                AnnotationPrinter ap = new AnnotationPrinter(ptr);
                ap.processDefaultAnnotation(attr);
            }
            case MethodParametersAttribute attr -> {
                int index = 0;
                for (var parm : attr.parameters()) {
                    String name = parm.name().map(Utf8Entry::stringValue).orElse("");
                    ptr.print(Directive.dir_parameter, index)
                            .printAccessName(JynxAccessFlags.convert(parm.flags()), name).nl();
                    ++index;
                }
            }
            case CodeAttribute _ -> {}
            case SyntheticAttribute _ -> {}
            case DeprecatedAttribute _ -> {}
            default -> {
                ptr.print("; attribute", attribute).nl();
                assert false;
            }
        }
    }
    
    private  List<StackMapFrameInfo.VerificationTypeInfo>  locals(MethodModel mm) {
        var parms = mm.methodTypeSymbol().parameterList();
        List<StackMapFrameInfo.VerificationTypeInfo> result = new ArrayList<>();
        if (!mm.flags().has(AccessFlag.STATIC)) {
            String mname = mm.methodName().stringValue();
            if (mname.equals("<init>")) {
                result.add(ITEM_UNINITIALIZED_THIS);
            } else {
                result.add(verificationTypeInfoOf(classDesc));
            }
        }
        for (var parm : parms) {
            result.add(verificationTypeInfoOf(parm));
        }
        return result;
    }
    
    public static  StackMapFrameInfo.VerificationTypeInfo verificationTypeInfoOf(ClassDesc desc) {
        if (desc.isPrimitive()) {
            String primitive = desc.descriptorString();
            assert primitive.length() == 1;
            char first = primitive.charAt(0);
            return switch (first) {
                case 'J' -> ITEM_LONG;
                case 'F' -> ITEM_FLOAT;
                case 'D' -> ITEM_DOUBLE;
                default -> ITEM_INTEGER;
            };
        } else {
            return StackMapFrameInfo.ObjectVerificationTypeInfo.of(desc);
        }        
    }
}
