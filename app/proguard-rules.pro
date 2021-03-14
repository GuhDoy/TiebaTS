-repackageclasses "gm.tieba.tabswitch"

-overloadaggressively

-keep class gm.tieba.tabswitch.Hook

-keepclassmembers class gm.tieba.tabswitch.ui.MainActivity {
    public static boolean isModuleActive();
}