# AndroidX Startup initializer - discovered via manifest merge + reflection.
-keep class com.atruedev.kmpnfc.adapter.KmpNfcInitializer {
    public <init>();
    public * create(android.content.Context);
    public java.util.List dependencies();
}
