//
// Created by Thom on 2020/9/6.
//

#include <stdlib.h>
#include "hash.h"

static int size;
static int index;
static intptr_t *container;

bool add(intptr_t hash) {
    if (hash == 0) {
        return clear();
    }
    for (int i = 0; i < index; ++i) {
        if (container[i] == hash) {
            return false;
        }
    }
    if (index >= size) {
        size += 4;
        container = (intptr_t *) (realloc(container, size * sizeof(intptr_t)));
    }
    container[index++] = hash;
    return true;
}

bool clear() {
    if (container) {
        free(container);
        size = 0;
        index = 0;
        container = NULL;
        return true;
    } else {
        return false;
    }
}

#ifdef MAIN
#include <stdio.h>
int main(int argc, char **argv) {
    for (int i = 1; i < argc; ++i) {
        printf("%s: %d\n", argv[i], add(atoi(argv[i])));
    }
    return 0;
}
#endif
