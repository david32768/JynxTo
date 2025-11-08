package com.github.david32768.jynxto.jynx.classfile;

import java.lang.classfile.AttributedElement;
import java.lang.classfile.AttributeMapper;
import java.lang.classfile.BufWriter;
import java.lang.classfile.ClassReader;
import java.lang.classfile.constantpool.Utf8Entry;
import java.util.ArrayList;
import java.util.List;

public class CustomLoadableDescriptorsMapper implements AttributeMapper<CustomLoadableDescriptorsAttribute> {

    @Override
    public String name() {
        return "LoadableDescriptors";
    }

    @Override
    public CustomLoadableDescriptorsAttribute readAttribute(AttributedElement enclosing, ClassReader cf, int pos) {
        int length = cf.readInt(pos - 4);
        List<String> classes = new ArrayList<>();
        for (int i = 0; i < length; ++i) {
            int offset = cf.readU2(pos);
            var klass = cf.readEntry(offset, Utf8Entry.class);
            classes.add(klass.stringValue());
            pos+=2;
        }
        return new CustomLoadableDescriptorsAttribute(classes);
    }

    @Override
    public void writeAttribute(BufWriter buf, CustomLoadableDescriptorsAttribute attr) {
        buf.writeIndex(attr.attributeName());
        var list = attr.stringDescriptors();
        var pool = buf.constantPool();
        buf.writeInt(list.size());
        for (var str : list) {
            buf.writeIndex(pool.utf8Entry(str));
        }
    }

    @Override
    public AttributeStability stability() {
        return AttributeStability.CP_REFS;
    }
    
}
