#ifdef __cplusplus
extern "C" {
#endif

jboolean _inline(JNIEnv *env, jclass clazz, jstring jname);

jint _access(JNIEnv *env, jclass clazz, jstring path);

jint sysaccess(JNIEnv *env, jclass clazz, jstring jpath);

jstring _fopen(JNIEnv *env, jclass clazz, jstring jpath);

jboolean findXposed(JNIEnv *env, jclass clazz);

#ifdef __cplusplus
}
#endif
