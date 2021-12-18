//
// Created by Thom on 2019/3/18.
//
#include <android/log.h>

#ifndef BREVENT_INLINE_H
#define BREVENT_INLINE_H

#ifndef APPLICATION_ID
#define APPLICATION_ID "gm.tieba.tabswitch"
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APPLICATION_ID, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APPLICATION_ID, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, APPLICATION_ID, __VA_ARGS__))
#endif

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

bool setRead(void *symbol);

bool isInlineHooked(void *symbol);

#ifdef DEBUG_HOOK_SELF
#if defined(__arm__) || defined(__aarch64__)

#include "hookzz/hookzz.h"

void check_inline_hook_hookzz();

void check_inline_hook_hookzz_b();

#endif

void check_inline_hook_whale();

#if defined(__arm__)

void check_inline_hook_substrate();

#endif
#endif

#ifdef __cplusplus
}
#endif

#endif //BREVENT_INLINE_H
