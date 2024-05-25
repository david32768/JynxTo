package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attribute;
import java.lang.classfile.FieldModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.DeprecatedAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.attribute.SyntheticAttribute;
import java.lang.reflect.AccessFlag;

import jynx.Directive;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

import static jynx.Message.M130;

public class FieldPrinter {

    private final JynxPrinter ptr;

    FieldPrinter(JynxPrinter ptr) {
        this.ptr = ptr;
    }


    void process(FieldModel fm) {
        var flags = ToJynx.convertFlags(fm.flags().flags(), fm.attributes());
        ptr.nl().print(Directive.dir_field).printAccessName(flags, fm.fieldName())
                .print(fm.fieldType());
        var fmflags = fm.flags();
        if (fmflags.has(AccessFlag.STATIC) && fmflags.has(AccessFlag.FINAL)) {
            for (var attribute: fm.attributes()) {
                switch(attribute) {
                    case ConstantValueAttribute attr -> {
                        var constant = attr.constant().constantValue();
                        switch(constant) {
//                            case Integer c -> {
//                                ptr.print(ReservedWord.equals_sign, c);
//                            }
//                            case Long c -> {
//                                ptr.print(ReservedWord.equals_sign, c + "L");
//                            }
//                            case Float c -> {
//                                ptr.print(ReservedWord.equals_sign, Float.toHexString(c) + "f");
//                            }
//                            case Double c -> {
//                                ptr.print(ReservedWord.equals_sign, Double.toHexString(c));
//                            }
                            case String c -> {
                                ptr.print(ReservedWord.equals_sign)
                                        .printQuoted(c);
                            }
                            default  -> {
                                ptr.print(ReservedWord.equals_sign, constant);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }
        ptr.nl().incrDepth();
        for (var element : fm.elementList()) {
            switch(element) {
                case AccessFlags _ -> {}
                case Attribute _ -> {}
                default -> {
                    ptr.print("; element", element).nl();            
                }
            }
        }
        int ignoredCount = 0;
        for (var attribute: fm.attributes()) {
            switch(attribute) {
                case SignatureAttribute attr -> {
                    ptr.print(Directive.dir_signature, attr.signature()).nl();
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
                case ConstantValueAttribute _ -> {
                    ++ignoredCount;
                }
                case SyntheticAttribute _ -> {
                    ++ignoredCount;
                }
                case DeprecatedAttribute _ -> {
                    ++ignoredCount;
                }
                default -> {
                    // "unknown attribute %s"
                    throw new LogIllegalArgumentException(M130);
                }
            }
        }
        ptr.decrDepth();
        if (fm.attributes().size() > ignoredCount) {
            ptr.print(Directive.end_field).nl();
        }
    }
}
