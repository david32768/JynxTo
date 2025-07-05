package com.github.david32768.jynxto.tojynx;


import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.instruction.ExceptionCatch;
import java.lang.classfile.Label;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.david32768.jynxfree.classfile.Comparators;

public class ExceptionCatcher {
    
    private final Map<Label,List<ExceptionCatch>> startCatch;
    private final Map<Label,List<ExceptionCatch>> endCatch;
    private final Map<Label,List<ExceptionCatch>> handleCatch;
    private final List<ExceptionCatch> currentCatch;

    public ExceptionCatcher() {
        this.startCatch = new HashMap<>();
        this.endCatch = new HashMap<>();
        this.handleCatch = new HashMap<>();
        this.currentCatch = new ArrayList<>();
    }

    void add(ExceptionCatch handler) {
        startCatch.computeIfAbsent(handler.tryStart(), k -> new ArrayList<>()).add(handler);
        endCatch.computeIfAbsent(handler.tryEnd(), k -> new ArrayList<>()).add(handler);
        handleCatch.computeIfAbsent(handler.handler(), k -> new ArrayList<>()).add(handler);
        
    }
    
    List<ExceptionCatch> update(Label label) {
        var exlist = endCatch.getOrDefault(label, Collections.emptyList());
        for (var ex : exlist) {
            currentCatch.remove(ex);
        }
        exlist = startCatch.getOrDefault(label, Collections.emptyList());
        for (var ex : exlist) {
            currentCatch.add(ex);
        }
        var handlers = handleCatch.getOrDefault(label, Collections.emptyList());
        return List.copyOf(handlers);
    }

    public List<ExceptionCatch> currentCatch() {
        Collections.sort(currentCatch, ExceptionCatcher::compare);
        return List.copyOf(currentCatch);
    }    
    
    public static int compare(ExceptionCatch o1, ExceptionCatch o2) {
        var opt1 = o1.catchType().map(ClassEntry::asInternalName);
        var opt2 = o2.catchType().map(ClassEntry::asInternalName);
        int result = Comparators.compareEmptyLast(opt1,opt2); // empty = all exceptions
        return result == 0?
                Comparators.compareHash(o1, o2):
                result;
    }
    
}
