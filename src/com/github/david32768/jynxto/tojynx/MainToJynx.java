package com.github.david32768.jynxto.tojynx;

import java.io.PrintWriter;

import static jynx.Global.LOG;
import jynx.MainOption;
import jynx.MainOptionService;
import static jynx.Message.M237;

import com.github.david32768.jynxto.tojynx.ToJynx;

public class MainToJynx implements MainOptionService {

    @Override
    public MainOption main() {
        return MainOption.TOJYNX;
    }

    @Override
    public boolean call(String fname, PrintWriter pw) {
        try {
            return ToJynx.toJynx(pw, fname);
        } catch (Exception ex) {
             LOG(ex);
             LOG(M237,fname); // "error accepting class file: %s"
             return false;
         }        
    }

}
