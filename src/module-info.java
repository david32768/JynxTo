module com.github.david32768.JynxTo {
    requires com.github.david32768.JynxFree;
    exports com.github.david32768.jynxto.tojynx;
    provides com.github.david32768.jynxfree.jynx.MainOptionService 
            with com.github.david32768.jynxto.tojynx.MainToJynx;
}
