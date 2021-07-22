#include <android/log.h>

#ifndef NELEM
#define NELEM(x) (sizeof(x) / sizeof((x)[0]))
#endif

#ifndef APPLICATION_ID
#define APPLICATION_ID "gm.tieba.tabswitch"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APPLICATION_ID, __VA_ARGS__))
#endif

#ifdef __cplusplus
extern "C" {
#endif

jboolean _inline(JNIEnv *env, jclass clazz, jstring jname);

jboolean findXposed(JNIEnv *env, jclass clazz);

jint _access(JNIEnv *env, jclass clazz, jstring path);

jint sysaccess(JNIEnv *env, jclass clazz, jstring jpath);

jstring _fopen(JNIEnv *env, jclass clazz, jstring jpath);

jstring _openat(JNIEnv *env, jclass clazz, jstring jpath);

#ifdef __cplusplus
}
#endif
