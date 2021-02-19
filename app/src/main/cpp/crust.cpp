#include <android/log.h>
#include <string>
#include <cstring>
#include <jni.h>

#define LOGINFO(...) ((void)__android_log_print(ANDROID_LOG_INFO, "security", __VA_ARGS__))
#define LOGERROR(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "security", __VA_ARGS__))

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_OK;
}

static jobject getApplication(JNIEnv *env) {
    jobject application = nullptr;
    jclass activity_thread_clz = env->FindClass("android/app/ActivityThread");
    if (activity_thread_clz != nullptr) {
        jmethodID currentApplication = env->GetStaticMethodID(
                activity_thread_clz, "currentApplication", "()Landroid/app/Application;");
        if (currentApplication != nullptr) {
            application = env->CallStaticObjectMethod(activity_thread_clz, currentApplication);
        } else {
            LOGERROR("Cannot find method: currentApplication() in ActivityThread.");
        }
        env->DeleteLocalRef(activity_thread_clz);
    } else {
        LOGERROR("Cannot find class: android.app.ActivityThread");
    }
    return application;
}