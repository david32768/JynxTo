module com.github.david32768.JynxTo {
    requires com.github.david32768x.jynx;
    exports com.github.david32768.jynxto.tojynx;
    provides jynx.MainOptionService with com.github.david32768.jynxto.tojynx.MainToJynx;
}
