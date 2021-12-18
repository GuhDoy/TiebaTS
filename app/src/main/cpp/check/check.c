#include <jni.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <sys/system_properties.h>
#include <dlfcn.h>
#include "check.h"
#include "classloader.h"
#include "inline.h"
#include "plt.h"

static bool check_hook_function(void *handle, const char *name) {
    void *symbol = dlsym(handle, name);
    if (symbol != NULL && setRead(symbol) && isInlineHooked(symbol)) {
        return true;
    }
    return false;
}

#define HOOK_SYMBOL(x, y) check_hook_function(x, y)

jboolean _inline(JNIEnv *env, jclass clazz, jstring jname) {
    const char *name = (*env)->GetStringUTFChars(env, jname, NULL);
    void *handle = dlopen("libc.so", RTLD_NOW);
    bool isInlineHooked = false;
    if (handle) isInlineHooked = HOOK_SYMBOL(handle, name);
    dlclose(handle);
    (*env)->ReleaseStringUTFChars(env, jname, name);
    return isInlineHooked;
}

jboolean FindClass_inline(JNIEnv *env, jclass clazz) {
    void *symbol = (*env)->FindClass;
    return isInlineHooked(symbol);
}

static inline void fill_ro_build_version_sdk(char v[]) {
    // ro.build.version.sdk
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 's';
    v[0x1] = 'm';
    v[0x2] = '-';
    v[0x3] = 'f';
    v[0x4] = 'p';
    v[0x5] = 'o';
    v[0x6] = 'k';
    v[0x7] = 'l';
    v[0x8] = '\'';
    v[0x9] = '|';
    v[0xa] = 'n';
    v[0xb] = '~';
    v[0xc] = '~';
    v[0xd] = 'g';
    v[0xe] = '`';
    v[0xf] = '~';
    v[0x10] = '?';
    v[0x11] = 'a';
    v[0x12] = 'd';
    v[0x13] = 'j';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

bool xposed_status = false;

jboolean findXposed(JNIEnv *env, jclass clazz) {
    static int sdk = 0;
    if (sdk == 0) {
        char v1[0x20];
        char prop[PROP_VALUE_MAX] = {0};
        fill_ro_build_version_sdk(v1);
        __system_property_get(v1, prop);
        sdk = (int) strtol(prop, NULL, 10);
    }

    checkClassLoader(env, sdk);
    return xposed_status;
}

jint _access(JNIEnv *env, jclass clazz, jstring jpath) {
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    int i = access(path, F_OK);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    return i;
}

static inline char *getCannotOpen() {
    // cannot open /proc/self/maps
    char v[] = "gdhig}*d|h`/?a`|w:eemd,idvt";
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    return strdup(v);
}

static inline char *getDataApp() {
    // /data/app
    char v[] = "-geqg/`rs";
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    for (unsigned int i = 0; i < 0x9; ++i) {
        v[i] ^= ((i + 0x9) % m);
    }
    return strdup(v);
}

static inline char *getComGoogleAndroid() {
    // com.google.android
    char v[] = "fij&nedkld,bjatham";
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    return strdup(v);
}

static inline char *getComBaiduTieba() {
    // com.baidu.tieba
    char v[] = "gjk)jhcdt,wm`df";
    static unsigned int m = 0;

    if (m == 0) {
        m = 11;
    } else if (m == 13) {
        m = 17;
    }

    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    return strdup(v);
}

jstring _fopen(JNIEnv *env, jclass clazz, jstring jpath) {
    const char *path = (*env)->GetStringUTFChars(env, jpath, NULL);
    FILE *f = fopen(path, "r");
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    if (f == NULL) {
        return (*env)->NewStringUTF(env, getCannotOpen());
    }

    char result[PATH_MAX];
    strcpy(result, "");
    char line[PATH_MAX];
    while (fgets(line, PATH_MAX - 1, f) != NULL) {
        if (strstr(line, getDataApp()) != NULL
            && strstr(line, getComGoogleAndroid()) == NULL
            && strstr(line, getComBaiduTieba()) == NULL) {
            if (strlen(result) + strlen(line) > PATH_MAX) break;
            strcat(result, line);
        }
    }
    fclose(f);
    return (*env)->NewStringUTF(env, result);
}
