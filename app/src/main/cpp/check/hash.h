//
// Created by Thom on 2020/9/6.
//

#ifndef BREVENT_HASH_H
#define BREVENT_HASH_H

#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

bool add(intptr_t hash);

bool clear();

#ifdef __cplusplus
}
#endif

#endif //BREVENT_HASH_H
