package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationElement;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Label;
import java.lang.classfile.TypeAnnotation;
import java.lang.classfile.TypeAnnotation.TargetInfo;
import java.lang.classfile.TypeAnnotation.TypePathComponent;
import java.lang.classfile.attribute.AnnotationDefaultAttribute;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jvm.TypeRef;
import jynx.Directive;
import jynx.ReservedWord;

import static jynx.Global.OPTION;
import static jynx.GlobalOption.SKIP_ANNOTATIONS;

public class AnnotationPrinter {

    private final JynxPrinter ptr;
    private final boolean omit;

    AnnotationPrinter(JynxPrinter ptr) {
        this.ptr = ptr;
        this.omit = OPTION(SKIP_ANNOTATIONS);
    }
    
    void processRuntimeAnnotation(boolean visible, Annotation annotation) {
        if (omit) {
            return;
        }
        ReservedWord visibility = visible? ReservedWord.res_visible: ReservedWord.res_invisible;
        ptr.print(Directive.dir_annotation, visibility, annotation.className()).nl();
        processAnnotation(annotation);
        ptr.print(Directive.end_annotation).nl();
    }

    void processRuntimeTypeAnnotation(boolean visible, TypeAnnotation annotation) {
        if (omit) {
            return;
        }
        processTypeHeader(visible, annotation);
        ptr.nl();
        processAnnotation(annotation);
        ptr.print(Directive.end_annotation).nl();
    }

    void processDefaultAnnotation(AnnotationDefaultAttribute attr) {
        if (omit) {
            return;
        }
        ptr.print(Directive.dir_default_annotation).nl().incrDepth();
        var value = attr.defaultValue();
        processValue(value);
        ptr.decrDepth().print(Directive.end_annotation).nl();
    }

    void processParameterAnnotations(boolean visible, List<List<Annotation>> annotations) {
        if (omit) {
            return;
        }
        Directive dircount = visible? Directive.dir_visible_parameter_count: Directive.dir_invisible_parameter_count;
        ptr.print(dircount, annotations.size()).nl();
        int index = 0;
        for (var annolist : annotations) {
            for (var anno : annolist) {
                processParameterAnnotation(visible, index, anno);
            }
            ++index;
        }
    }
    
    private void processParameterAnnotation(boolean visible, int index, Annotation attr) {
        if (omit) {
            return;
        }
        String visibility = visible? "visible": "invisible";
        ptr.print(Directive.dir_parameter_annotation, visibility, index, attr.className()).nl();
        processAnnotation(attr);
        ptr.print(Directive.end_annotation).nl();
    }
    
    void processLocalVarAnnotation(boolean visible, TypeAnnotation annotation, Map<Label,String> labelNames) {
        if (omit) {
            return;
        }
        processTypeHeader(visible, annotation);
        ptr.print(ReservedWord.dot_array).nl().incrDepth().incrDepth();
        var target = (TypeAnnotation.LocalVarTarget)annotation.targetInfo();
        for (var info : target.table()) {
            var start = labelNames.get(info.startLabel());
            var end = labelNames.get(info.endLabel());
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            ptr.print(info.index(), start, end).nl();
        }
        ptr.decrDepth().print(Directive.end_array).nl().decrDepth();
        processAnnotation(annotation);
        ptr.print(Directive.end_annotation).nl();
    }
    
    private void processTypeHeader(boolean visible, TypeAnnotation annotation) {
        ReservedWord visibility = visible? ReservedWord.res_visible: ReservedWord.res_invisible;
        var className = annotation.className();
        var targetInfo = annotation.targetInfo();
        var targetPath = annotation.targetPath();
        var targetType = targetInfo.targetType();
        var type = TypeRef.fromJVM(targetType.targetTypeValue());
        ptr.print(type.getDirective(), visibility);
        target(targetInfo);
        ptr.print(ReservedWord.res_typepath, path(targetPath), className);
    }
    
    private Optional<String> path(List<TypePathComponent> list) {
        if (list.isEmpty()) {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder();
        for (var comp : list) {
            var kind = comp.typePathKind();
            assert kind == TypePathComponent.Kind.TYPE_ARGUMENT;
            sb.append(comp.typeArgumentIndex()).append(";");
        }
        return Optional.of(sb.toString());
    }
    
    private void target(TargetInfo target) {
        switch(target) {
            case TypeAnnotation.CatchTarget t -> {
                ptr.print(t.exceptionTableIndex());
            }
            case TypeAnnotation.EmptyTarget _ -> {}
            case TypeAnnotation.FormalParameterTarget t -> {
                ptr.print(t.formalParameterIndex());
            }
            case TypeAnnotation.LocalVarTarget _ -> {}
            case TypeAnnotation.OffsetTarget  _ -> {}
            case TypeAnnotation.SupertypeTarget  t -> {
                ptr.print(t.supertypeIndex());
            }
            case TypeAnnotation.ThrowsTarget t -> {
                ptr.print(t.throwsTargetIndex());
            }
            case TypeAnnotation.TypeArgumentTarget t -> {
                ptr.print(t.typeArgumentIndex());
            }
            case TypeAnnotation.TypeParameterBoundTarget t -> {
                ptr.print(t.typeParameterIndex(),t.boundIndex());
            }
            case TypeAnnotation.TypeParameterTarget t -> {
                ptr.print(t.typeParameterIndex());
            }
        }
    }

    private void processAnnotation(Annotation annotation) {
        ptr.incrDepth();
        for (var element : annotation.elements()) {
            processElement(element);
        }
        ptr.decrDepth();
    }
    
    private void processArrayAnnotation(Annotation annotation) {
        ptr.print(Directive.dir_annotation).nl();
        for (var element : annotation.elements()) {
            processElement(element);
        }
        ptr.print(Directive.end_annotation).nl();
    }
    
    private void processElement(AnnotationElement element) {
        ptr.print(element.name());
        processValue(element.value());
    }

    private void processValue(AnnotationValue value) {
        switch(value) {
            case AnnotationValue.OfAnnotation val -> {
                ptr.print(val.tag(),
                        val.annotation().className(),
                        ReservedWord.equals_sign,
                        ReservedWord.dot_annotation
                ).nl();
                processAnnotation(val.annotation());
                ptr.print(Directive.end_annotation).nl();
            }
            case AnnotationValue.OfArray val -> {
                processArray(val);
            }
            case AnnotationValue.OfConstant val -> {
                ptr.print(val.tag(),
                        ReservedWord.equals_sign,
                        val
                ).nl();
            }
            case AnnotationValue.OfClass val -> {
                ptr.print(val.tag(),
                        ReservedWord.equals_sign,
                        val.className()
                ).nl();
            }
            case AnnotationValue.OfEnum val -> {
                ptr.print(val.tag(),
                        val.className(),
                        ReservedWord.equals_sign,
                        val.constantName()
                ).nl();
            }
        }
    }
    
    private void processArrayValue(AnnotationValue value) {
        switch(value) {
            case AnnotationValue.OfAnnotation val -> {
                processArrayAnnotation(val.annotation());
            }
            case AnnotationValue.OfArray _ -> {
                throw new AssertionError();
            }
            case AnnotationValue.OfConstant val -> {
                ptr.print(val).nl();
            }
            case AnnotationValue.OfClass val -> {
                ptr.print(val.className()).nl();
            }
            case AnnotationValue.OfEnum val -> {
                ptr.print(val.constantName()).nl();
            }
        }
    }
    
    private void processArray(AnnotationValue.OfArray value) {
        var type = value.values().isEmpty()? AnnotationValue.of(0) : value.values().getFirst();
        switch(type) {
            case AnnotationValue.OfAnnotation _ -> {
                processAnnotationArray(value);
            }
            case AnnotationValue.OfEnum _ -> {
                processEnumArray(value);
            }
            default -> {
                ptr.print(ReservedWord.left_array.toString() + type.tag(),
                        ReservedWord.equals_sign,
                        ReservedWord.dot_array
                ).nl().incrDepth();
                for (var val : value.values()) {
                    assert type.tag() == val.tag();
                    processArrayValue(val);
                }
                ptr.decrDepth();
                ptr.print(Directive.end_array).nl();
            }
        }
    }

    private void processAnnotationArray(AnnotationValue.OfArray value) {
        var anno0 = (AnnotationValue.OfAnnotation)value.values().getFirst();
        ptr.print(ReservedWord.left_array.toString() + anno0.tag(),
                anno0.annotation().className(),
                ReservedWord.equals_sign,
                ReservedWord.dot_annotation_array
        ).nl().incrDepth();
        for (var val : value.values()) {
            assert anno0.tag() == val.tag();
            processArrayValue(val);
        }
        ptr.decrDepth().print(Directive.end_annotation_array).nl();
    }

    private void processEnumArray(AnnotationValue.OfArray value) {
        var enum0 = (AnnotationValue.OfEnum)value.values().getFirst();
        ptr.print(ReservedWord.left_array.toString() + enum0.tag(),
                enum0.className(),
                ReservedWord.equals_sign,
                ReservedWord.dot_array
        ).nl().incrDepth();
        for (var val : value.values()) {
            assert enum0.tag() == val.tag();
            assert enum0.className().toString().equals(((AnnotationValue.OfEnum)val).className().toString()):
                    String.format("%nenum0 = '%s'%nvalxx = '%s'%n",
                            enum0.className(), ((AnnotationValue.OfEnum)val).className());
            processArrayValue(val);
        }
        ptr.decrDepth().print(Directive.end_array).nl();
    }

}
