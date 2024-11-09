package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attributes;
import java.lang.classfile.FieldModel;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.ConstantValueAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;


import jvm.Context;
import jynx.Directive;
import jynx.ReservedWord;

import com.github.david32768.jynxto.jynx.AccessName;

public class FieldPrinter {

    private final JynxPrinter ptr;

    FieldPrinter(JynxPrinter ptr) {
        this.ptr = ptr.copy();
    }


    void process(FieldModel fm) {
        var accessName = AccessName.of(fm);
        ptr.nl().print(Directive.dir_field, accessName, fm.fieldType());
        var cva = fm.findAttribute(Attributes.constantValue());
        if (cva.isPresent()) {
            var constant = cva.get().constant().constantValue();
            switch(constant) {
                case String c -> {
                    ptr.print(ReservedWord.equals_sign)
                            .printQuoted(c);
                }
                default  -> {
                    ptr.print(ReservedWord.equals_sign, constant);
                }
            }
        }
        ptr.setLogContext().nl().incrDepth();

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
                default -> {
                    UnknownAttributes.unknown(ptr.copy(), attribute, Context.FIELD);
                }
            }
        }
        ptr.decrDepth();
        if (fm.attributes().size() > ignoredCount) {
            ptr.print(Directive.end_field).nl();
        }
    }
}
