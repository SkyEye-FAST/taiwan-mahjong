package top.skyeyefast.taiwan

/** Kinds: 1-9m=0..8, 1-9p=9..17, 1-9s=18..26, ESWN=27..30, white/green/red=31..33. */
enum class GroupType { SEQUENCE, TRIPLET, OPEN_KONG, CONCEALED_KONG, ADDED_KONG }

data class Group(val type: GroupType, val kind: Int) {
    init {
        require(kind in 0..33)
        require(type != GroupType.SEQUENCE || kind < 27 && kind % 9 <= 6)
    }
    val tiles: List<Int>
        get() = java.util.List.copyOf(when (type) {
            GroupType.SEQUENCE -> listOf(kind, kind + 1, kind + 2)
            GroupType.TRIPLET -> List(3) { kind }
            else -> List(4) { kind }
        })
}

/** Declared sequences/triplets are open; only a concealed kong preserves concealment. */
class Hand(concealed: List<Int>, melds: List<Group> = emptyList()) {
    val concealed: List<Int> = java.util.List.copyOf(concealed)
    val melds: List<Group> = java.util.List.copyOf(melds)
    val size: Int get() = concealed.size + 3 * melds.size
    init {
        require(melds.size <= 5)
        require(size == 16 || size == 17) { "Expected 16/17 structural tiles, counting each fixed kong as three" }
        counts(this.concealed + this.melds.flatMap { it.tiles })
    }
    internal fun requireSize(expected: Int) { require(size == expected) { "Expected $expected structural tiles" } }
    internal fun fixedCounts(): IntArray = counts(melds.flatMap { it.tiles })
    internal fun withTile(kind: Int): Hand = Hand(concealed + kind, melds)
}

/** Concealed groups only. Fixed melds remain in Hand. */
class WinningShape(val pair: Int, groups: List<Group>) {
    val groups: List<Group> = java.util.List.copyOf(groups)
    init { require(pair in 0..33) }
}

internal fun counts(tiles: List<Int>): IntArray = IntArray(34).also { result ->
    tiles.forEach {
        require(it in 0..33) { "Flowers are not ordinary tile kinds" }
        require(++result[it] <= 4) { "A kind has more than four physical copies" }
    }
}

data class EffectiveTile(val kind: Int, val remaining: Int)
class HandAnalysis(val shanten: Int, effectiveTiles: List<EffectiveTile>) {
    val effectiveTiles: List<EffectiveTile> = java.util.List.copyOf(effectiveTiles)
    val remaining: Int get() = effectiveTiles.sumOf { it.remaining }
}
data class DiscardAnalysis(val kind: Int, val analysis: HandAnalysis)
