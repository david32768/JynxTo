package com.github.david32768.jynxto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import static jynx.Global.LOG;
import static jynx.Global.LOGGER;
import static jynx.Message.M12;
import static jynx.Message.M3;

import jynx.Global;
import jynx.MainOption;

import com.github.david32768.jynxto.tojynx.ToJynx;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            appUsage();
            System.exit(1);
        }
        if (args[0].equals(MainOption.TOJYNX.extname())) {
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        MainOption main = MainOption.TOJYNX; 
        Global.newGlobal(main);
        Optional<String> optpath = Global.setOptions(args);
        if (LOGGER().numErrors() != 0 || optpath.isEmpty()) {
            LOG(M3); // "program terminated because of errors"
            appUsage();
            System.exit(1);
        }
        String path = optpath.orElseThrow();
        ToJynx.toJynx(path);
        Global.END_MESSAGES(path);
    }
    
    private static void appUsage() {
        LOG(M12); // "%nUsage:%n"
        MainOption.TOJYNX.appUsage();
    }

}
