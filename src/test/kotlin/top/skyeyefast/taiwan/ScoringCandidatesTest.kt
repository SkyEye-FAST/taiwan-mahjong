package top.skyeyefast.taiwan

import kotlin.test.*
import org.junit.jupiter.api.Test

class ScoringCandidatesTest {
    private val profile = TaiwanProfiles.POCKET_COMMON
    private val context = WinContext(WinMethod.DISCARD)

    private fun key(score: TaiwanScore) = Triple(score.shape.pair, score.shape.groups, score.winningGroup)

    @Test fun retainsEveryDecompositionAndWinningPlacementBeforeSelectingTheMaximum() {
        val complete = tiles("111222333m444p555s66z")
        val winning = 2
        val hand = Hand(complete.toMutableList().apply { remove(winning) })
        val candidates = TaiwanScoring.candidates(hand, winning, context, profile)
        val expected = TaiwanMahjong.winningShapes(Hand(complete)).flatMap { shape ->
            (shape.groups.indices.filter { winning in shape.groups[it].tiles } +
                if (shape.pair == winning) listOf(-1) else emptyList()).map { Triple(shape.pair, shape.groups, it) }
        }.toSet()
        assertTrue(expected.size > 1)
        assertEquals(expected, candidates.map(::key).toSet())
        val best = assertNotNull(TaiwanScoring.score(hand, winning, context, profile))
        val expectedBest = candidates.maxWith(compareBy<TaiwanScore> { it.tai }.thenBy { it.rawTai })
        assertEquals(key(expectedBest), key(best))
        assertEquals(expectedBest.awards, best.awards)
        assertEquals(expectedBest.tai, best.tai)
        assertEquals(expectedBest.rawTai, best.rawTai)

        // A host's sequence award must be able to make a previously losing interpretation win.
        val extended = candidates.maxBy { it.rawTai + 40 * it.shape.groups.count { group -> group.type == GroupType.SEQUENCE } }
        assertNotEquals(key(best), key(extended))
        assertFailsWith<UnsupportedOperationException> { (candidates as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (best.awards as MutableList).clear() }
    }

    @Test fun zeroTaiWinningShapesRemainCandidatesAndInvalidInputsStillFail() {
        val disabled = TaiwanScoringProfile("ZERO", profile.values.mapValues { 0 }, profile.exclusions, profile.sources,
            profile.flowers, profile.flowerSets, profile.pinfu, profile.replacements)
        val complete = tiles("111222333m444p555s66z")
        val hand = Hand(complete.dropLast(1))
        val candidates = TaiwanScoring.candidates(hand, complete.last(), context, disabled)
        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.awards.isEmpty() && it.tai == 0 && it.rawTai == 0 })
        assertNotNull(TaiwanScoring.score(hand, complete.last(), context, disabled))
        assertTrue(TaiwanScoring.candidates(Hand(tiles("147m147p147s1234567z")), 0, context, profile).isEmpty())
        assertFailsWith<IllegalArgumentException> { TaiwanScoring.candidates(Hand(complete), 0, context, profile) }
        assertFailsWith<IllegalArgumentException> {
            TaiwanScoring.candidates(hand, complete.last(), WinContext(WinMethod.DISCARD, flowers = setOf(Flower.SPRING)),
                TaiwanProfiles.SOUTHERN_COMMON)
        }
    }

    @Test fun flowerCandidatesRetainSeparateComponentsAndSuppressOrdinaryFlowerBonuses() {
        val flowers = Flower.entries.toSet()
        val complete = tiles("111222333m444p555s66z")
        val replacement = ReplacementWin(Hand(complete.dropLast(1)), complete.last(),
            WinContext(WinMethod.SELF_DRAW, flowers = flowers, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT))
        val capped = TaiwanScoringProfile("CAPPED", profile.values, profile.exclusions, profile.sources,
            profile.flowers, profile.flowerSets, profile.pinfu, profile.replacements, 4)
        val candidates = TaiwanScoring.flowerWinCandidates(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, capped, replacement)
        assertTrue(candidates.size > 1)
        for (candidate in candidates) {
            val ordinary = assertNotNull(candidate.handScore)
            assertEquals(Pattern.EIGHT_FLOWERS, candidate.award.pattern)
            assertTrue(ordinary.awards.none { it.pattern in setOf(Pattern.SEAT_FLOWER, Pattern.FLOWER_SET) })
            assertEquals(candidate.award.tai + ordinary.rawTai, candidate.rawTai)
            assertEquals(4, candidate.tai)
        }
        val best = assertNotNull(TaiwanScoring.flowerWin(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, capped, replacement))
        assertEquals(candidates.maxOf { it.rawTai }, best.rawTai)
        assertFailsWith<UnsupportedOperationException> { (candidates as MutableList).clear() }
        val initial = TaiwanScoring.flowerWinCandidates(flowers, FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT, profile).single()
        assertNull(initial.handScore)
        assertEquals(initial.award.tai, initial.tai)
        assertFailsWith<IllegalArgumentException> {
            TaiwanScoring.flowerWinCandidates(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, profile)
        }
        assertFailsWith<IllegalArgumentException> {
            TaiwanScoring.flowerWinCandidates(flowers, FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT, profile, replacement)
        }
        val disabled = TaiwanScoringProfile("NO_EIGHT", profile.values + (Pattern.EIGHT_FLOWERS to 0),
            profile.exclusions, profile.sources, profile.flowers, profile.flowerSets, profile.pinfu, profile.replacements)
        assertTrue(TaiwanScoring.flowerWinCandidates(flowers, FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT, disabled).isEmpty())
    }
}
