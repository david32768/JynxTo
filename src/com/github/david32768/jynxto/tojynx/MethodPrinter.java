package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.AnnotationDefaultAttribute;
import java.lang.classfile.attribute.CodeAttribute;
import java.lang.classfile.attribute.ExceptionsAttribute;
import java.lang.classfile.attribute.MethodParametersAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleParameterAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;

import static com.github.david32768.jynxfree.jynx.Global.OPTION;
import static com.github.david32768.jynxfree.jynx.GlobalOption.SKIP_STACK;
import static com.github.david32768.jynxto.my.Message.M616;

import com.github.david32768.jynxfree.classfile.StackMap;
import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.ReservedWord;
import com.github.david32768.jynxfree.transform.SlotKind;

import com.github.david32768.jynxto.jynx.AccessName;


public class MethodPrinter {
    
    private final JynxPrinter ptr;
    private CodeAttribute codeAttribute;

    MethodPrinter(JynxPrinter ptr) {
        this.ptr = ptr.copy();
    }

    void process(MethodModel mm) {
        var accessName = AccessName.ofMethod(mm);        
        ptr.nl()
                .print(Directive.dir_method, accessName)
                .setLogContext()
                .nl()
                .incrDepth();
        for (var attribute : mm.attributes()) {
            processAttribute(attribute);
        }
        var cm = mm.code();
        if (cm.isPresent()) {
            StackMap stackmap = StackMap.of(mm);
            CodePrinter cp = new CodePrinter(ptr, stackmap, !OPTION(SKIP_STACK));
            cp.process(cm.get(), codeAttribute, SlotKind.ofParameters(mm));
        }
        ptr.decrDepth().print(Directive.end_method).nl();
    }

    private void processAttribute(Attribute<?> attribute) {
        switch(attribute) {
            case ExceptionsAttribute attr -> {
                if (attr.exceptions().isEmpty()) {
                    // "%s attribute is present but empty"
                    ptr.comment(M616, attr.attributeName());
                } else {
                    ptr.print(Directive.dir_throws, ReservedWord.dot_array)
                            .nl().incrDepth();
                    for (var except : attr.exceptions()) {
                        ptr.print(except.asInternalName()).nl();
                    }
                    ptr.decrDepth().print(Directive.end_array).nl();
                }
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
                    var accessName = AccessName.ofParameter(parm);
                    ptr.print(Directive.dir_parameter, index, accessName).nl();
                    ++index;
                }
            }
            case CodeAttribute ca -> {
                this.codeAttribute = ca;
            }
            default -> {
                UnknownAttributes.process(ptr, attribute, Context.METHOD);
            }
        }
    }
    
}
