package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleMainClassAttribute;
import java.lang.classfile.attribute.ModulePackagesAttribute;
import java.lang.classfile.ClassModel;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.github.david32768.jynxfree.jynx.Directive.*;

import com.github.david32768.jynxfree.jvm.Context;
import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jvm.StandardAttribute;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.ReservedWord;
import com.github.david32768.jynxto.jynx.AccessName;

public class ModulePrinter extends ClassHeaderPrinter {
    
    private final Map<String, Attribute<?>> moduleAttributes;

    ModulePrinter(JynxPrinter ptr, JvmVersion jvmversion) {
        super(ptr, jvmversion);
        this.moduleAttributes = new TreeMap<>();
    }
    
    @Override
    void process(ClassModel cm) {
        ptr.incrDepth();
        for (var attribute : cm.attributes()) {
            String name = attribute.attributeName().stringValue();
            var standard = StandardAttribute.getInstance(name);
            if (standard != null && standard.inContext(Context.CLASS) && standard.inContext(Context.MODULE)) {
                processClassAttribute(attribute);
            } else {
                moduleAttributes.put(name,attribute);
            }                
        }
        ptr.decrDepth();
        processModuleInfo();
    }
    
    private void processModuleInfo() {
        var module = (ModuleAttribute)moduleAttributes.get("Module");
        Objects.nonNull(module);
        var version = module.moduleVersion();
        var accessName = AccessName.ofModule(module);
        ptr.print(dir_module, accessName, version).nl()
                .incrDepth();
        for( var attribute : moduleAttributes.values()) {
            processAttribute(attribute);
        }
        ptr.decrDepth().print(end_module).nl();        
    }

    void processAttribute(Attribute<?> attribute) {
        switch(attribute) {
            case ModuleAttribute attr -> {

                var requires = attr.requires();
                for (var required: requires) {
                    var accessName = AccessName.ofRequire(required);
                    ptr.print(Directive.dir_requires, accessName, required.requiresVersion()).nl();
                }

                var exports = attr.exports();
                if (!exports.isEmpty()) {
                    for (var exported : exports) {
                        var accessName = AccessName.ofExport(exported);
                        ptr.print(Directive.dir_exports, accessName);
                        var to = exported.exportsTo();
                        if (!to.isEmpty()) {
                            ptr.print(ReservedWord.res_to, ReservedWord.dot_array).nl().incrDepth();
                            for (var tomod : to) {
                                ptr.print(tomod).nl();
                            }
                            ptr.decrDepth().print(end_array);
                        }
                        ptr.nl();
                    }
                }

                var opens = attr.opens();
                if (!opens.isEmpty()) {
                    for (var opened : opens) {
                        var accessName = AccessName.ofOpen(opened);
                        ptr.print(Directive.dir_opens, accessName);
                        var to = opened.opensTo();
                        if (!to.isEmpty()) {
                            ptr.print(ReservedWord.res_to, ReservedWord.dot_array).nl().incrDepth();
                            for (var tomod : to) {
                                ptr.print(tomod).nl();
                            }
                            ptr.decrDepth().print(end_array);
                        }
                        ptr.nl();
                    }
                }

                var uses = attr.uses();
                if (!uses.isEmpty()) {
                    for (var used : uses) {
                        ptr.print(Directive.dir_uses,used).nl();
                    }
                }

                var provides = attr.provides();
                if (!provides.isEmpty()) {
                    for (var provided : provides) {
                        ptr.print(Directive.dir_provides, provided.provides());
                        var with = provided.providesWith();
                        if (with.isEmpty()) {
                            ptr.nl();
                        } else {
                            ptr.print(ReservedWord.res_with, ReservedWord.dot_array).nl().incrDepth();
                            for (var withClass : with) {
                                ptr.print(withClass).nl();
                            }
                            ptr.decrDepth().print(end_array).nl();
                        }
                    }
                }
                
            }
            case ModuleMainClassAttribute attr -> {
                ptr.print(dir_main, attr.mainClass()).nl();
            }
            case ModulePackagesAttribute attr -> {
                var packages = attr.packages();
                ptr.print(dir_packages, ReservedWord.dot_array).nl().incrDepth();
                for (var pack : packages) {
                    ptr.print(pack).nl();
                }
                ptr.decrDepth().print(end_array).nl();
            }
            default -> {
                UnknownAttributes.process(ptr, attribute, Context.MODULE);
            }            
        }
    }
}
