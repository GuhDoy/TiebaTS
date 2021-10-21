package gm.tieba.tabswitch.util

data class MutablePair<A, B>(var first: A, var second: B)

infix fun <A, B> A.to(that: B): MutablePair<A, B> = MutablePair(this, that)

fun <T> Collection<T>.most(): T {
    val map = mutableListOf<MutablePair<T, Int>>()
    val mid = size - 1 ushr 1
    run loop@{
        forEach { thiz ->
            val pair = map.firstOrNull { it.first == thiz }
            if (pair == null) {
                map.add(thiz to 1)
            } else {
                pair.second++
                if (pair.second > mid) return@loop
            }
        }
    }
    map.sortWith(Comparator.comparingInt { -it.second })
    return map[0].first
}
