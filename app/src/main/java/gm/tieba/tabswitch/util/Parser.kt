@file:JvmName("Parser")

package gm.tieba.tabswitch.util

import de.robv.android.xposed.XposedHelpers

fun parsePbContent(instance: Any?, fieldName: String?): String {
    val contents = XposedHelpers.getObjectField(instance, fieldName) as? List<*> ?: return ""
    return contents.mapNotNull { XposedHelpers.getObjectField(it, "text") as? String }.joinToString("")
}
