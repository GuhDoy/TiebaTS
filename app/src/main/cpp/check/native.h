#ifdef __cplusplus
extern "C" {
#endif

jboolean _inline(JNIEnv *env, jclass clazz, jstring jname);

jboolean findXposed(JNIEnv *env, jclass clazz);

jint _access(JNIEnv *env, jclass clazz, jstring path);

jint sysaccess(JNIEnv *env, jclass clazz, jstring jpath);

jstring _fopen(JNIEnv *env, jclass clazz, jstring jpath);

#ifdef __cplusplus
}
#endif
