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
    M192(ERROR,"unknown code element %s ignored"),
    M193(LINE,"value required (%d) for %s is less than limit value (%d); %d used"),
    M608(BLANK,"bci -> %d stack -> %s"),
    M612(BLANK,"%s ; line %d"),
    M613(BLANK,"slot %d name = %s, type = %s"),
    M614(BLANK,"%s handled at %s"),
    M615(BLANK,"%s handler %s - %s"),
    M616(BLANK,"%s attribute is present but empty"),
    M621(INFO,"%s not actioned as exception occured: %s"),

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
