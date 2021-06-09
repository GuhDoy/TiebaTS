#include <jni.h>
#include <string.h>
#include "native.h"
#include <android/log.h>

#define APPLICATION_ID "gm.tieba.tabswitch"
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APPLICATION_ID, __VA_ARGS__))

static inline char *getNativeClassName() {
    // gm/tieba/tabswitch/util/Native
    char v[] = "`e&~bioo dpp`c|bci-vplj(Fh~bzh";
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    for (unsigned int i = 0; i < 0x1e; ++i) {
        v[i] ^= ((i + 0x1e) % m);
    }
    return strdup(v);
}

static inline char *getInline() {
    // inline
    char v[] = "hlomnd";
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    return strdup(v);
}

static inline char *getInlineSignature() {
    // (Ljava/lang/String;)Z
    char v[] = "*Ondpf'ekek\"]{bx|g:+Y";
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    return strdup(v);
}

static inline char *getFindXposed() {
    // findXposed
    char v[] = "emkbXqmpaa";
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    return strdup(v);
}

static inline char *getFindXposedSignature() {
    // ()Z
    char v[] = "))[";
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    for (unsigned int i = 0; i < 0x3; ++i) {
        v[i] ^= ((i + 0x3) % m);
    }
    return strdup(v);
}

static inline char *getProp() {
    // prop
    char v[] = "qpoq";
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    return strdup(v);
}

static inline char *getPropSignature() {
    // ()Ljava/lang/String;
    char v[] = ")+Ondpf'ekek\"]{bx|g:";
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    return strdup(v);
}

static inline char *getAccess() {
    // access
    char v[] = "`a`asr";
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    return strdup(v);
}

static inline char *getSysaccess() {
    // sysaccess
    char v[] = "qzwdecdqp";
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

static inline char *getAccessSignature() {
    // (Ljava/lang/String;)I
    char v[] = "*Ondpf'ekek\"]{bx|g:+J";
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    for (unsigned int i = 0; i < 0x15; ++i) {
        v[i] ^= ((i + 0x15) % m);
    }
    return strdup(v);
}

static inline char *getFopen() {
    // fopen
    char v[] = "fnrfj";
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    for (unsigned int i = 0; i < 0x5; ++i) {
        v[i] ^= ((i + 0x5) % m);
    }
    return strdup(v);
}

static inline char *getFopenSignature() {
    // (Ljava/lang/String;)Ljava/lang/String;
    char v[] = "'\\{seu:zaoe,Wqtnfn1\"@goyq>~rzr9Supjjb=";
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    for (unsigned int i = 0; i < 0x26; ++i) {
        v[i] ^= ((i + 0x26) % m);
    }
    return strdup(v);
}

jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    jclass clazz;

    if ((*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if ((clazz = (*env)->FindClass(env, getNativeClassName())) == NULL) {
        return JNI_ERR;
    }

    JNINativeMethod methods[] = {
            {getInline(),     getInlineSignature(),     _inline},
            {getFindXposed(), getFindXposedSignature(), findXposed},
            {getProp(),       getPropSignature(),       prop},
            {getAccess(),     getAccessSignature(),     _access},
            {getSysaccess(),  getAccessSignature(),     sysaccess},
            {getFopen(),      getFopenSignature(),      _fopen},
    };
    if ((*env)->RegisterNatives(env, clazz, methods, 6) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
