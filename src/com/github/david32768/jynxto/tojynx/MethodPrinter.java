package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.MethodModel;
import java.lang.classfile.TypeAnnotation;
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
import java.lang.constant.ClassDesc;

import jvm.Context;
import jynx.Directive;
import jynx.ReservedWord;

import com.github.david32768.jynxto.jynx.AccessName;
import com.github.david32768.jynxto.stack.StackMap;

public class MethodPrinter {
    
    private final JynxPrinter ptr;

    private final ClassDesc classDesc;
    
    private CodeAttribute codeAttribute;

    MethodPrinter(JynxPrinter ptr, ClassDesc classDesc) {
        this.ptr = ptr.copy();
        this.classDesc = classDesc;
    }

    void process(MethodModel mm) {
        var accessName = AccessName.of(mm);        
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
            StackMap stackmap = StackMap.of(classDesc,mm, codeAttribute);
            CodePrinter cp = new CodePrinter(ptr, stackmap);
            cp.process(cm.get(), codeAttribute);
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
                    var accessName = AccessName.of(parm);
                    ptr.print(Directive.dir_parameter, index, accessName).nl();
                    ++index;
                }
            }
            case CodeAttribute ca -> {
                this.codeAttribute = ca;
            }
            default -> {
                UnknownAttributes.unknown(ptr.copy(), attribute, Context.METHOD);
            }
        }
    }
    
}
