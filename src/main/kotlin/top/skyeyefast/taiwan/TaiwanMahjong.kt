package top.skyeyefast.taiwan

/** Region-independent five-meld structure; flower wins have a separate scoring entry point. */
object TaiwanMahjong {
    @JvmStatic
    fun winningShapes(hand: Hand): List<WinningShape> {
        hand.requireSize(17)
        val c = counts(hand.concealed)
        val result = ArrayList<WinningShape>()
        fun visit(pair: Int, groups: MutableList<Group>) {
            val first = c.indexOfFirst { it != 0 }
            if (first < 0) {
                result += WinningShape(pair, groups)
                return
            }
            if (c[first] >= 3) {
                c[first] -= 3
                groups += Group(GroupType.TRIPLET, first)
                visit(pair, groups)
                groups.removeAt(groups.lastIndex)
                c[first] += 3
            }
            if (first < 27 && first % 9 <= 6 && c[first + 1] > 0 && c[first + 2] > 0) {
                for (k in first..first + 2) c[k]--
                groups += Group(GroupType.SEQUENCE, first)
                visit(pair, groups)
                groups.removeAt(groups.lastIndex)
                for (k in first..first + 2) c[k]++
            }
        }
        for (pair in 0..33) if (c[pair] >= 2) {
            c[pair] -= 2
            visit(pair, ArrayList())
            c[pair] += 2
        }
        return java.util.List.copyOf(result)
    }

    /** Exact minimum missing tiles minus one, respecting copies committed to fixed melds. */
    @JvmStatic
    fun shanten(hand: Hand): Int = Distance.calculate(counts(hand.concealed), hand.fixedCounts(), 5 - hand.melds.size)

    @JvmStatic
    fun waitingTiles(hand: Hand): Set<Int> {
        hand.requireSize(16)
        val owned = counts(hand.concealed + hand.melds.flatMap { it.tiles })
        return java.util.Collections.unmodifiableSet((0..33).filterTo(sortedSetOf()) {
            owned[it] < 4 && winningShapes(hand.withTile(it)).isNotEmpty()
        })
    }

    /** Visible tiles are other, disjoint known copies, not a second copy of this hand's melds. */
    @JvmStatic
    @JvmOverloads
    fun analyze(hand: Hand, visibleTiles: List<Int> = emptyList()): HandAnalysis {
        hand.requireSize(16)
        val known = counts(hand.concealed + hand.melds.flatMap { it.tiles } + visibleTiles)
        val distance = shanten(hand)
        val effective = (0..33).filter { known[it] < 4 && shanten(hand.withTile(it)) < distance }
            .map { EffectiveTile(it, 4 - known[it]) }
        return HandAnalysis(distance, effective)
    }

    @JvmStatic
    @JvmOverloads
    fun discards(hand: Hand, visibleTiles: List<Int> = emptyList()): List<DiscardAnalysis> {
        hand.requireSize(17)
        counts(hand.concealed + hand.melds.flatMap { it.tiles } + visibleTiles)
        return java.util.List.copyOf(hand.concealed.distinct().sorted().map { discard ->
            val after = hand.concealed.toMutableList().also { it.remove(discard) }
            DiscardAnalysis(discard, analyze(Hand(after, hand.melds), visibleTiles + discard))
        })
    }
}

/** Rank DP over complete target shapes, not incomplete fragments that may need a fifth copy. */
internal object Distance {
    private const val INF = 100
    // carry: tiles owed at this rank; next: tiles already owed at the following rank.
    // One sequence started at r produces (1,1) at r+1, (1,0) at r+2, then (0,0).
    private data class State(val carry: Int, val next: Int, val groups: Int, val pairs: Int)

    fun calculate(concealed: IntArray, fixed: IntArray, needed: Int): Int {
        var combined = Array(needed + 1) { IntArray(2) { INF } }
        combined[0][0] = 0
        for (suit in 0..3) {
            var states = mapOf(State(0, 0, 0, 0) to 0)
            val length = if (suit == 3) 7 else 9
            for (rank in 0 until length) {
                val kind = suit * 9 + rank
                val updated = HashMap<State, Int>()
                for ((state, cost) in states) {
                    val maxSequences = if (suit < 3 && rank <= 6) minOf(4, needed - state.groups) else 0
                    for (sequences in 0..maxSequences) for (triplet in 0..1) for (pair in 0..1) {
                        val groups = state.groups + sequences + triplet
                        val pairs = state.pairs + pair
                        val target = state.carry + sequences + 3 * triplet + 2 * pair
                        if (groups > needed || pairs > 1 || target > 4 - fixed[kind]) continue
                        val next = State(state.next + sequences, sequences, groups, pairs)
                        val value = cost + maxOf(0, target - concealed[kind])
                        if (value < (updated[next] ?: INF)) updated[next] = value
                    }
                }
                states = updated
            }
            val next = Array(needed + 1) { IntArray(2) { INF } }
            for (g in 0..needed) for (p in 0..1) {
                for ((state, value) in states) {
                    if (state.carry != 0 || state.next != 0 || g + state.groups > needed || p + state.pairs > 1) continue
                    val ng = g + state.groups
                    val np = p + state.pairs
                    next[ng][np] = minOf(next[ng][np], combined[g][p] + value)
                }
            }
            combined = next
        }
        check(combined[needed][1] < INF)
        return combined[needed][1] - 1
    }
}
