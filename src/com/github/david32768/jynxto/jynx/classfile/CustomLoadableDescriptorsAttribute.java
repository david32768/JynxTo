package com.github.david32768.jynxto.jynx.classfile;

import java.lang.classfile.Attribute;
import java.lang.classfile.attribute.UnknownAttribute;
import java.lang.classfile.constantpool.ConstantPool;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.CustomAttribute;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.github.david32768.jynxfree.jynx.Global;

public class CustomLoadableDescriptorsAttribute extends CustomAttribute<CustomLoadableDescriptorsAttribute> {

    private static final String NAME = "LoadableDescriptors";
    private static final String METHOD_NAME = "l" + NAME.substring(1);
    private static final String CLASSFILE_NAME = "java.lang.classfile.attribute." + NAME + "Attribute";
        
    private final List<String> classes;
    
    public CustomLoadableDescriptorsAttribute(List<String> classes) {
        super(new CustomLoadableDescriptorsMapper());
        this.classes = classes;
    }

    public List<String> stringDescriptors() {
        return classes;
    }
    
    public static boolean is(Attribute<?> attribute) {
        return attribute.attributeName().stringValue().equals(NAME);
    }
    
    public static CustomLoadableDescriptorsAttribute of(Attribute<?> attribute, ConstantPool cp) {
        if (attribute instanceof CustomLoadableDescriptorsAttribute me) {
            return me;
        }
        List<String> classes = new ArrayList<>();
        if (attribute instanceof UnknownAttribute uattr) {
            ByteBuffer bb = ByteBuffer.wrap(uattr.contents());
            int ct = bb.getShort();
            while (bb.hasRemaining()) {
                int index = bb.getShort();
                Utf8Entry klass = cp.entryByIndex(index, Utf8Entry.class);
                classes.add(klass.stringValue());
                --ct;
            }
            assert ct == 0;
        } else {
            try {
                var lookup = MethodHandles.lookup();
                var mt = MethodType.methodType(List.class);
                var klass = Class.forName(CLASSFILE_NAME);
                var mh = lookup.findVirtual(klass, METHOD_NAME, mt);
                var listutf8 = (List)mh.invoke(klass.cast(attribute));
                for (var item: listutf8) {
                    classes.add(((Utf8Entry)item).stringValue());
                }
            } catch (Throwable ex) {
                Global.LOG(ex);
            }
         }
        return new CustomLoadableDescriptorsAttribute(classes);
    }
}
