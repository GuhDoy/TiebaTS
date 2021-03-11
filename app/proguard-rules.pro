-repackageclasses "gm.tieba.tabswitch"

-keep class gm.tieba.tabswitch.Hook

-keepclassmembers class gm.tieba.tabswitch.ui.MainActivity {
    public static boolean isModuleActive();
}

-keepclassmembers class gm.tieba.tabswitch.util.TbProtoParser* {
     public *;
}

-overloadaggressively