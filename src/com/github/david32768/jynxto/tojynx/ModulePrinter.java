package com.github.david32768.jynxto.tojynx;

import com.github.david32768.jynxto.utility.UnknownAttributes;
import java.lang.classfile.Attribute;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleMainClassAttribute;
import java.lang.classfile.attribute.ModulePackagesAttribute;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static jynx.Directive.*;

import jvm.Context;
import jvm.JvmVersion;
import jvm.StandardAttribute;
import jynx.Directive;
import jynx.ReservedWord;


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
            String name = attribute.attributeName();
            var standard = StandardAttribute.getInstance(name);
            if (standard != null && standard.inContext(Context.CLASS) && standard.inContext(Context.MODULE)) {
                boolean processed = processClassAttribute(attribute);
                if(!processed) {
                    UnknownAttributes.unknown(attribute, Context.MODULE);
                }
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
        var name = module.moduleName().name();
        var flags = module.moduleFlags();
        var version = module.moduleVersion();
        ptr.print(dir_module).printAccessName(JynxAccessFlags.convert(flags), name)
                .print(version).nl().incrDepth();
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
                    ptr.print(Directive.dir_requires);
                    var flags = JynxAccessFlags.convert(required.requiresFlags());
                    ptr.printAccessName(flags, required.requires().name());
                    ptr.print(required.requiresVersion()).nl();
                }

                var exports = attr.exports();
                if (!exports.isEmpty()) {
                    for (var exported : exports) {
                        ptr.print(Directive.dir_exports);
                        var flags = JynxAccessFlags.convert(exported.exportsFlags());
                        ptr.printAccessName(flags, exported.exportedPackage().name());
                        var to = exported.exportsTo();
                        if (to.isEmpty()) {
                            ptr.nl();
                        } else {
                            ptr.print(ReservedWord.res_to, ReservedWord.dot_array).nl().incrDepth();
                            for (var tomod : to) {
                                ptr.print(tomod).nl();
                            }
                            ptr.decrDepth().print(end_array).nl();
                        }
                    }
                }

                var opens = attr.opens();
                if (!opens.isEmpty()) {
                    for (var opened : opens) {
                        ptr.print(Directive.dir_opens);
                        var flags = JynxAccessFlags.convert(opened.opensFlags());
                        ptr.printAccessName(flags, opened.openedPackage().name());
                        var to = opened.opensTo();
                        if (to.isEmpty()) {
                            ptr.nl();
                        } else {
                            ptr.print(ReservedWord.res_to, ReservedWord.dot_array).nl().incrDepth();
                            for (var tomod : to) {
                                ptr.print(tomod).nl();
                            }
                            ptr.decrDepth().print(end_array).nl();
                        }
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
                UnknownAttributes.unknown(attribute, Context.MODULE);
            }            
        }
    }
}
