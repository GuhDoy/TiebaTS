package gm.tieba.tabswitch.util

data class MutablePair<A, B>(var first: A, var second: B)

fun <T> Collection<T>.most(): T {
    val map = ArrayList<MutablePair<T, Int>>()
    val mid = size shr 1 - if (size and 1 == 0) 1 else 0
    run loop@{
        forEach { thiz ->
            val pair = map.firstOrNull { it.first == thiz }
            if (pair == null) {
                map.add(MutablePair(thiz, 0))
            } else {
                pair.second++
                if (pair.second > mid) return@loop
            }
        }
    }
    map.sortWith(Comparator.comparingInt { -it.second })
    return map[0].first
}
