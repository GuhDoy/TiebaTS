#ifndef XPOSEDTEMPLATE_NATIVE_API_H
#define XPOSEDTEMPLATE_NATIVE_API_H

#include <cstdint>
#include <string>

typedef int (*HookFunType)(void *func, void *replace, void **backup);

typedef int (*UnhookFunType)(void *func);

typedef void (*NativeOnModuleLoaded)(const char *name, void *handle);

typedef struct {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
} NativeAPIEntries;

typedef NativeOnModuleLoaded (*NativeInit)(const NativeAPIEntries *entries);

#endif //XPOSEDTEMPLATE_NATIVE_API_H
