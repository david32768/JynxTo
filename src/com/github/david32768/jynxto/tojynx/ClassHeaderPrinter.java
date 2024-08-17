package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.*;
import java.lang.classfile.TypeAnnotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static jynx.Directive.*;
import static jynx.Message.M130;

import jvm.Context;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jynx.ClassType;
import jynx.LogIllegalArgumentException;
import jynx.ReservedWord;

public class ClassHeaderPrinter {
    
    private final JynxPrinter ptr;
    private final JvmVersion jvmVersion;
    private final List<RecordComponentInfo> components;
    private final Map<String, Attribute<?>> moduleAttributes;

    ClassHeaderPrinter(JynxPrinter ptr, JvmVersion jvmversion) {
        this.ptr = ptr.copy();
        this.jvmVersion = jvmversion;
        this.components = new ArrayList<>();
        this.moduleAttributes = new TreeMap<>();
    }

    Map<String, Attribute<?>> moduleAttributes() {
        return Map.copyOf(moduleAttributes);
    }

    List<RecordComponentInfo> components() {
        return List.copyOf(components);
    }

    
    void process(ClassModel cm) {
        processHeader(cm);
        for (var attribute : cm.attributes()) {
            String name = attribute.attributeName();
            var standard = StandardAttribute.getInstance(name);
            if (standard == null) {
                // "unknown attribute %s"
                throw new LogIllegalArgumentException(M130, attribute);
            } else if (standard.inContext(Context.CLASS)) {
                processClassAttribute(attribute);
            } else if (cm.isModuleInfo() && standard.inContext(Context.MODULE)) {
                moduleAttributes.put(name,attribute);
            } else {
                // "invalid attribute %s"
                throw new LogIllegalArgumentException(M130, attribute);
            }
        }
    }
    
    private void processClassAttribute(Attribute<?> attribute) {
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
                    var flags = JynxAccessFlags.convert(inner.flags());
                    ClassType classtype = ClassType.from(flags);
                    flags.removeAll(classtype.getMustHave4Inner(jvmVersion));
                    var dir = classtype.getInnerDir();
                    var innerClass = inner.innerClass();
                    var innerName = inner.innerName();
                    var outerClass = inner.outerClass();
                    ptr.print(dir).printAccessName(flags, innerClass.asInternalName())
                        .print(ReservedWord.res_outer, outerClass)
                        .print(ReservedWord.res_innername, innerName)
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
            case DeprecatedAttribute _ -> {}
            case RecordAttribute attr -> {
                components.addAll(attr.components());
            }
            case BootstrapMethodsAttribute _ -> {}
            default -> {
                // "invalid attribute %s"
                throw new LogIllegalArgumentException(M130, attribute);
            }
        }
    }
    
    private void processHeader(ClassModel cm) {
        ptr.println(dir_super, cm.superclass());
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
