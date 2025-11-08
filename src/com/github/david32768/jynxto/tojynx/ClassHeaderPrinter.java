package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.attribute.*;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.TypeAnnotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.david32768.jynxfree.jynx.Directive.*;


import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.ReservedWord;

import com.github.david32768.jynxto.jynx.classfile.CustomLoadableDescriptorsAttribute;
import com.github.david32768.jynxto.jynx.DirectiveAccessName;

public class ClassHeaderPrinter {
    
    protected final JynxPrinter ptr;
    private final JvmVersion jvmVersion;
    private final List<RecordComponentInfo> components;

    ClassHeaderPrinter(JynxPrinter ptr, JvmVersion jvmversion) {
        this.ptr = ptr.copy();
        this.jvmVersion = jvmversion;
        this.components = new ArrayList<>();
    }

    List<RecordComponentInfo> components() {
        return List.copyOf(components);
    }

    void process(ClassModel cm) {
        ptr.incrDepth();
        processHeader(cm);
        for (var attribute : cm.attributes()) {
            processClassAttribute(attribute, cm.constantPool());
        }
        ptr.decrDepth();
    }
    
    protected void processClassAttribute(Attribute<?> attribute, ConstantPool pool) {
        switch(attribute) {
            case SourceFileAttribute attr -> {
                ptr.print(dir_source, attr.sourceFile()).nl();
            }
            case SignatureAttribute attr -> {
                ptr.print(dir_signature, attr.signature()).nl();
            }
            case NestHostAttribute attr -> {
                ptr.print(dir_nesthost, attr.nestHost()).nl();
            }
            case NestMembersAttribute attr -> {
                ptr.print(dir_nestmember, ReservedWord.dot_array).nl().incrDepth();
                for (var member : attr.nestMembers()) {
                    ptr.print(member).nl();
                }
                ptr.decrDepth().print(end_array).nl();
            }
            case PermittedSubclassesAttribute attr -> {
                ptr.print(dir_permittedSubclass).print(ReservedWord.dot_array).nl().incrDepth();
                for (var subclass : attr.permittedSubclasses()) {
                    ptr.print(subclass).nl();
                }
                ptr.decrDepth().print(end_array).nl();
            }
            case EnclosingMethodAttribute attr -> {
                var encClass = attr.enclosingClass();
                var encMethod = attr.enclosingMethodName();
                var encType = attr.enclosingMethodType();
                if (encMethod.isPresent()) {
                    String method = String.format("%s.%s%s",
                            encClass.asInternalName(),
                            encMethod.get().stringValue(),
                            encType.get().stringValue());
                    ptr.print(dir_enclosing_method, method).nl();
                } else {
                    ptr.print(dir_outer_class, encClass).nl();
                }
            }
            case InnerClassesAttribute attr -> {
                for (var inner : attr.classes()) {
                    var dirAccessName = DirectiveAccessName.of(inner, jvmVersion);
                    ptr.print(dirAccessName,
                        ReservedWord.res_outer, inner.outerClass(),
                        ReservedWord.res_innername, inner.innerName())
                        .nl();
                }
            }
            case SourceDebugExtensionAttribute attr -> {
                ptr.print(dir_debug).printQuoted(new String(attr.contents())).nl();
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
            case RecordAttribute attr -> {
                components.addAll(attr.components());
            }
            case BootstrapMethodsAttribute _ -> {}
            default -> {
                if (CustomLoadableDescriptorsAttribute.is(attribute)) {
                    processLoadableDescriptors(attribute, pool);
                } else {           
                    // default
                    UnknownAttributes.process(ptr, attribute, Context.CLASS);
                }
            }
        }
    }

    private void processLoadableDescriptors(Attribute<?> attribute, ConstantPool pool) { 
        Objects.requireNonNull(attribute);
        var attr = CustomLoadableDescriptorsAttribute.of(attribute, pool);
        ptr.print(dir_descriptors, ReservedWord.dot_array).nl()
                .incrDepth();
        for (var item: attr.stringDescriptors()) {
            ptr.print(item).nl();
        }
        ptr.decrDepth()
                .print(end_array).nl();        
    }
    
    private void processHeader(ClassModel cm) {
        cm.superclass().ifPresent(cd -> ptr.print(dir_super, cd).nl());
        var interfaces = cm.interfaces();
        if (!interfaces.isEmpty()) {
            ptr.print(dir_implements, ReservedWord.dot_array).nl().incrDepth();
            for (var itf : interfaces) {
                ptr.print(itf).nl();
            }
            ptr.decrDepth();
            ptr.print(end_array).nl();
        }
    }

}
