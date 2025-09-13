package com.github.david32768.jynxto.tojynx;

import java.lang.classfile.ClassModel;

import static com.github.david32768.jynxfree.jynx.Global.OPTIONS;

import com.github.david32768.jynxfree.jvm.JvmVersion;
import com.github.david32768.jynxfree.jynx.Directive;
import com.github.david32768.jynxfree.jynx.GlobalOption;
import com.github.david32768.jynxfree.jynx.MainOption;

import com.github.david32768.jynxto.jynx.DirectiveAccessName;

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

        var dirAccessName = DirectiveAccessName.of(cm,jvmVersion);

        if (cm.isModuleInfo()) {
            ptr.print(dirAccessName).setLogContext().nl();
            processModuleInfo(cm);
        } else {
            ptr.print(dirAccessName).setLogContext().nl();
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
            var mp = new MethodPrinter(ptr);
            mp.process(mm);
        }        
    }

    private void processModuleInfo(ClassModel cm) {
        var modptr = new ModulePrinter(ptr, jvmVersion);
        modptr.process(cm);
    }
}
