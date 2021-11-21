-allowaccessmodification
-overloadaggressively

-keep class gm.tieba.tabswitch.XposedInit

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
