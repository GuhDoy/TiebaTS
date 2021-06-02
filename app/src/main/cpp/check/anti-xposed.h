#ifndef BREVENT_ANTI_XPOSED_H
#define BREVENT_ANTI_XPOSED_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

jclass findXposedBridge(JNIEnv *env, jobject classLoader);

jclass findLoadedClass(JNIEnv *env, jobject classLoader, const char *name);

#ifdef __cplusplus
}
#endif

#endif //BREVENT_ANTI_XPOSED_H
