#include <jni.h>
#include <string.h>
#include "main.h"
#include "check/native.h"

jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    jclass clazz;

    if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if ((clazz = (*env)->FindClass(env, "gm/tieba/tabswitch/hooker/extra/Native")) == NULL) {
        return JNI_ERR;
    }

    JNINativeMethod methods[] = {
            {"inline",     "(Ljava/lang/String;)Z",                  _inline},
            {"findXposed", "()Z",                                    findXposed},
            {"access",     "(Ljava/lang/String;)I",                  _access},
            {"sysaccess",  "(Ljava/lang/String;)I",                  sysaccess},
            {"fopen",      "(Ljava/lang/String;)Ljava/lang/String;", _fopen},
            {"openat",     "(Ljava/lang/String;)Ljava/lang/String;", _openat},
    };
    if ((*env)->RegisterNatives(env, clazz, methods, NELEM(methods)) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
