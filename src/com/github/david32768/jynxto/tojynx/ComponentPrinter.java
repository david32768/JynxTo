package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.attribute.RecordComponentInfo;
import java.lang.classfile.attribute.RuntimeInvisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeInvisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.classfile.attribute.RuntimeVisibleTypeAnnotationsAttribute;
import java.lang.classfile.attribute.SignatureAttribute;

import jvm.Context;
import jynx.Directive;

import com.github.david32768.jynxto.utility.UnknownAttributes;

public class ComponentPrinter {

    private final JynxPrinter ptr;

    ComponentPrinter(JynxPrinter ptr) {
        this.ptr = ptr.copy();
    }


    void process(RecordComponentInfo component) {
        ptr.nl().print(Directive.dir_component, component.name(), component.descriptorSymbol());
        ptr.nl().incrDepth();
        for (var attribute: component.attributes()) {
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
                default -> {
                    UnknownAttributes.unknown(attribute, Context.COMPONENT);
                }
            }
        }
        ptr.decrDepth();
        if (!component.attributes().isEmpty()) {
            ptr.print(Directive.end_component).nl();
        }
    }
}
