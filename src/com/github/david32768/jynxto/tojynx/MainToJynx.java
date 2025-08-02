package com.github.david32768.jynxto.tojynx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static com.github.david32768.jynxfree.jynx.Global.LOG;

import com.github.david32768.jynxfree.jynx.ClassUtil;
import com.github.david32768.jynxfree.jynx.MainOption;
import com.github.david32768.jynxfree.jynx.MainOptionService;

public class MainToJynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.DISASSEMBLY;
    }

    @Override
    public boolean call(PrintWriter pw, String fname) {
        byte[] bytes;
        try {
            bytes = ClassUtil.getClassBytes(fname);
        } catch (IOException ex) {
            LOG(ex);
            return false;
        }
        return ToJynx.toJynx(bytes, pw);
    }
    

    @Override
    public String callToString(byte[] bytes) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        boolean success = ToJynx.toJynx(bytes, pw);
        pw.flush();
        return success? sw.toString(): null;
    }

}
