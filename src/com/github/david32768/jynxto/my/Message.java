package com.github.david32768.jynxto.my;

import static com.github.david32768.jynxfree.jynx.LogMsgType.*;

import com.github.david32768.jynxfree.jynx.JynxMessage;
import com.github.david32768.jynxfree.jynx.LogMsgType;

public enum Message implements JynxMessage {

    M22(WARNING,"value required (%d) for %s is more than limit value (%d); %d used"),
    M137(ERROR,"local variable name %s is different from name in type %s"),
    M167(WARNING,"type annotation %s not known"),
    M172(ERROR,"known attribute %s not catered for in context %s"),
    M173(WARNING,"unknown attribute %s in context %s ignored"),
    M174(INFO,"%s is omitted as pseudo_access flag %s is used"),
    M193(LINE,"value required (%d) for %s is less than limit value (%d); %d used"),
    M311(WARNING,"attribute %s not valid for version %s"),
    M600(INFO,"%s not actioned as a method contains %s"),
    M603(INFO,"duplicate case %d in %s dropped"),
    M604(ERROR,"lowest case value (%d) is lower than low (%d)"),
    M605(ERROR,"highest case value (%d) is higher than high (%d)"),
    M606(ERROR,"high (%d) is less than low (%d) in %s"),
    M607(ERROR,"number of entries in %s (%d) exceeds maximum possible %d"),
    M608(BLANK,"bci -> %d stack -> %s"),
    M610(ERROR,"ambiguous case %d in %s dropped"),
    M611(INFO,"case %d with branch to default label in %s could be dropped"),
    M612(BLANK,"%s ; line %d"),
    M613(BLANK,"slot %d name = %s type = %s"),
    M614(BLANK,"%s handled at %s"),
    M615(BLANK,"%s handler %s - %s"),
    M616(BLANK,"%s attribute is present but empty"),
    M911(ERROR,"indent depth would be below lower limit %d"),
    ;

    private final LogMsgType logtype;
    private final String format;
    private final String msg;

    private Message(LogMsgType logtype, String format) {
        this.logtype = logtype;
        this.format = logtype.prefix(name()) + format;
        this.msg = format;
    }
    
    
    private Message(String format) {
        this(ERROR,format);
    }

    @Override
    public String format(Object... objs) {
        return String.format(format,objs);
    }

    @Override
    public String getFormat() {
        return msg;
    }
    
    @Override
    public LogMsgType getLogtype() {
        return logtype;
    }
    
}
