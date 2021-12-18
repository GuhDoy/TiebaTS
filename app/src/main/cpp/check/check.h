#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

jboolean _inline(JNIEnv *env, jclass clazz, jstring jname);

jboolean FindClass_inline(JNIEnv *env, jclass clazz);

jboolean findXposed(JNIEnv *env, jclass clazz);

jint _access(JNIEnv *env, jclass clazz, jstring path);

jstring _fopen(JNIEnv *env, jclass clazz, jstring jpath);

#ifdef __cplusplus
}
#endif
