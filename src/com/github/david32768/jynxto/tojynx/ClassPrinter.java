package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.ClassModel;

import static jynx.Global.OPTIONS;

import jvm.JvmVersion;
import jynx.ClassType;
import jynx.Directive;
import jynx.GlobalOption;
import jynx.MainOption;

public class ClassPrinter {
    
    private final JynxPrinter ptr;
    private final JvmVersion jvmVersion;

    ClassPrinter(JynxPrinter ptr, JvmVersion jvmVersion) {
        this.ptr = ptr.copy();
        this.jvmVersion = jvmVersion;
    }
    
    void process(ClassModel cm) {
        ptr.print(Directive.dir_version, jvmVersion.asJava());
        OPTIONS().stream()
                .filter(GlobalOption::isExternal)
                .filter(opt -> MainOption.ASSEMBLY.usesOption(opt))
                .filter(opt -> opt != GlobalOption.SYSIN)
                .forEach(ptr::print);
        ptr.nl();

        var flags = JynxAccessFlags.convert(cm.flags().flags(), cm.attributes());
        ClassType classtype = ClassType.from(flags);
        flags.removeAll(classtype.getMustHave4Class(jvmVersion));
        Directive dir = classtype.getDir();
        String name = cm.thisClass().asInternalName();
        if (cm.isModuleInfo()) {
            assert name.equals("module-info");
            ptr.print(dir).printAccessName(flags, "").nl();
            processModuleInfo(cm);
        } else {
            ptr.print(dir).printAccessName(flags, name).nl();
            processClass(cm);
        }
    }
    
    private void processClass(ClassModel cm) {
        ClassHeaderPrinter chp = new ClassHeaderPrinter(ptr, jvmVersion);
        chp.process(cm);
        for (var component : chp.components()) {
            var cp = new ComponentPrinter(ptr);
            cp.process(component);
        }
        for (var fm : cm.fields()) {
            var fp = new FieldPrinter(ptr);
            fp.process(fm);
        }
        for (var mm : cm.methods()) {
            var mp = new MethodPrinter(ptr, cm.thisClass().asSymbol());
            mp.process(mm);
        }        
    }

    private void processModuleInfo(ClassModel cm) {
        var modptr = new ModulePrinter(ptr, jvmVersion);
        modptr.process(cm);
    }
}
