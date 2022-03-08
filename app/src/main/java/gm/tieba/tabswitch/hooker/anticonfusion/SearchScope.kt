package gm.tieba.tabswitch.hooker.anticonfusion

data class SearchScope @JvmOverloads constructor(
    val most: String,
    var numberOfClassesNeedToSearch: IntArray = IntArray(0)
) {
    fun isInScope(classDef: String) = classDef.startsWith(most)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchScope

        if (most != other.most) return false
        if (!numberOfClassesNeedToSearch.contentEquals(other.numberOfClassesNeedToSearch)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = most.hashCode()
        result = 31 * result + numberOfClassesNeedToSearch.contentHashCode()
        return result
    }
}
