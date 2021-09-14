#include <jni.h>
#include <dlfcn.h>
#include "hide.h"

static HookFunType hook_func = nullptr;

jclass (*backup_FindClass)(JNIEnv *env, const char *name);

jclass fake_FindClass(JNIEnv *env, const char *name) {
    if (!strcmp(name, "dalvik/system/BaseDexClassLoader")) return nullptr;
    return backup_FindClass(env, name);
}

void on_library_loaded(const char *name, void *handle) {
//    if (std::string(name).ends_with("libcheck.so")) {
//        // TODO
//    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;

    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    hook_func((void *) env->functions->FindClass, (void *) fake_FindClass,
              (void **) &backup_FindClass);
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    return on_library_loaded;
}
