package top.skyeyefast.taiwan

import kotlin.test.*
import org.junit.jupiter.api.Test

/** Every published detector has a positive witness; context facts are supplied by the host. */
class PatternRegressionTest {
    private val p = TaiwanProfiles.POCKET_COMMON
    private val seen = mutableSetOf<Pattern>()
    private fun check(text: String, expected: Set<Pattern>, absent: Set<Pattern> = emptySet(),
                      c: WinContext = WinContext(WinMethod.SELF_DRAW), melds: List<Group> = emptyList()): TaiwanScore {
        val tiles = tiles(text)
        val result = assertNotNull(TaiwanScoring.score(Hand(tiles.dropLast(1), melds), tiles.last(), c, p))
        val patterns = result.awards.map { it.pattern }.toSet()
        assertTrue(patterns.containsAll(expected), "$expected missing from $patterns")
        assertTrue(patterns.intersect(absent).isEmpty(), "$absent present in $patterns")
        result.awards.forEach { assertEquals(it.units * p.values.getValue(it.pattern), it.tai); assertEquals(p.sources[it.pattern], it.source) }
        assertEquals(result.awards.sumOf { it.tai }, result.rawTai)
        seen += patterns
        return result
    }

    @Test fun everyPatternHasASourceBoundWitness() {
        check("11122233344455566z", setOf(Pattern.BIG_FOUR_WINDS, Pattern.ALL_HONORS, Pattern.FIVE_CONCEALED_TRIPLETS), setOf(Pattern.ALL_TRIPLETS))
        check("11122233355566644z", setOf(Pattern.SMALL_FOUR_WINDS))
        check("11122255566677733z", setOf(Pattern.BIG_THREE_DRAGONS), setOf(Pattern.WHITE_DRAGON, Pattern.GREEN_DRAGON, Pattern.RED_DRAGON))
        check("123456789m55566677z", setOf(Pattern.SMALL_THREE_DRAGONS, Pattern.HALF_FLUSH), setOf(Pattern.WHITE_DRAGON, Pattern.GREEN_DRAGON))
        check("11123445677788899m", setOf(Pattern.FULL_FLUSH, Pattern.THREE_CONCEALED_TRIPLETS))
        check("111m222p333s44455566z", setOf(Pattern.FIVE_CONCEALED_TRIPLETS, Pattern.ALL_TRIPLETS, Pattern.WHITE_DRAGON, Pattern.SINGLE_WAIT))
        check("111m222p333s44477766z", setOf(Pattern.RED_DRAGON))
        check("111m222p333s44466655z", setOf(Pattern.GREEN_DRAGON))
        check("123456m123p123789s55z", setOf(Pattern.CONCEALED), c = WinContext(WinMethod.DISCARD))
        check("123456m123p123789s55p", setOf(Pattern.PINFU), c = WinContext(WinMethod.DISCARD))
        check("11z", setOf(Pattern.ALL_FROM_OTHERS), c = WinContext(WinMethod.DISCARD),
            melds = listOf(0, 3, 9, 18, 24).map { Group(GroupType.SEQUENCE, it) })
        check("11z", setOf(Pattern.HALF_FROM_OTHERS, Pattern.SELF_DRAW),
            melds = listOf(0, 3, 9, 18, 24).map { Group(GroupType.SEQUENCE, it) })
        val base = "111m222p333s44455566z"
        check(base, setOf(Pattern.HEAVENLY_WIN), setOf(Pattern.CONCEALED_SELF_DRAW, Pattern.SELF_DRAW, Pattern.REPLACEMENT_WIN),
            WinContext(WinMethod.SELF_DRAW, opening = OpeningWin.HEAVENLY, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT))
        check(base, setOf(Pattern.EARTHLY_WIN), setOf(Pattern.CONCEALED_SELF_DRAW, Pattern.SELF_DRAW),
            WinContext(WinMethod.SELF_DRAW, seatWind = 28, opening = OpeningWin.EARTHLY))
        check(base, setOf(Pattern.HUMAN_WIN), setOf(Pattern.CONCEALED),
            WinContext(WinMethod.DISCARD, seatWind = 28, opening = OpeningWin.HUMAN))
        check(base, setOf(Pattern.HEAVENLY_READY, Pattern.CONCEALED_SELF_DRAW), setOf(Pattern.DECLARED_READY, Pattern.CONCEALED),
            WinContext(WinMethod.SELF_DRAW, ready = ReadyDeclaration.HEAVENLY))
        check(base, setOf(Pattern.EARTHLY_READY), setOf(Pattern.DECLARED_READY),
            WinContext(WinMethod.SELF_DRAW, seatWind = 28, ready = ReadyDeclaration.EARTHLY))
        check(base, setOf(Pattern.DECLARED_READY, Pattern.LAST_DRAW), c = WinContext(WinMethod.SELF_DRAW, ready = ReadyDeclaration.ORDINARY, lastTile = true))
        check(base, setOf(Pattern.LAST_DISCARD), setOf(Pattern.LAST_DRAW), WinContext(WinMethod.DISCARD, lastTile = true))
        check(base, setOf(Pattern.ROBBING_KONG), setOf(Pattern.LAST_DISCARD), WinContext(WinMethod.ROBBING_KONG))
        for (origin in listOf(DrawOrigin.KONG_REPLACEMENT, DrawOrigin.FLOWER_REPLACEMENT)) {
            check(base, setOf(Pattern.REPLACEMENT_WIN), c = WinContext(WinMethod.SELF_DRAW, drawOrigin = origin))
        }
        check(base, setOf(Pattern.FLOWER_SET, Pattern.SEAT_FLOWER), c = WinContext(WinMethod.SELF_DRAW, flowers = Flower.entries.take(4).toSet()))
        check("111m222p333s44455z", setOf(Pattern.FIVE_CONCEALED_TRIPLETS),
            melds = listOf(Group(GroupType.CONCEALED_KONG, 27)))
        val before = Hand(tiles("111222333m444p55s66z"))
        val ron = assertNotNull(TaiwanScoring.score(before, 22, WinContext(WinMethod.DISCARD), p))
        assertTrue(ron.awards.any { it.pattern == Pattern.FOUR_CONCEALED_TRIPLETS })
        assertFalse(ron.awards.any { it.pattern == Pattern.FIVE_CONCEALED_TRIPLETS })
        seen += ron.awards.map { it.pattern }
        for ((flowers, event) in listOf(Flower.entries.toSet() to FlowerEvent.EIGHT_AFTER_REPLACEMENT,
            Flower.entries.take(7).toSet() to FlowerEvent.SEVEN_AFTER_REPLACEMENT)) {
            val replacement = ReplacementWin(Hand(tiles(base).dropLast(1)), tiles(base).last(),
                WinContext(WinMethod.SELF_DRAW, flowers = flowers, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT))
            seen += assertNotNull(TaiwanScoring.flowerWin(flowers, event, p, replacement)).award.pattern
        }
        assertEquals(Pattern.entries.toSet(), seen)
    }

    @Test fun alternateDecompositionsCompeteBeforeAndAfterCap() {
        val hand = Hand(tiles("111222333m444p55s66z"))
        assertTrue(TaiwanMahjong.winningShapes(Hand(hand.concealed + 22)).size > 1)
        val score = assertNotNull(TaiwanScoring.score(hand, 22, WinContext(WinMethod.SELF_DRAW), p))
        assertEquals(5, score.shape.groups.count { it.type == GroupType.TRIPLET })
        val capped = TaiwanScoringProfile("CAP", p.values, p.exclusions, p.sources, p.flowers, p.flowerSets, p.pinfu, p.replacements, 1)
        val after = assertNotNull(TaiwanScoring.score(hand, 22, WinContext(WinMethod.SELF_DRAW), capped))
        assertEquals(1, after.tai)
        assertEquals(score.rawTai, after.rawTai)
        val placements = tiles("111123m222p333s44455z").toMutableList()
        placements.remove(0)
        val ron = assertNotNull(TaiwanScoring.score(Hand(placements), 0, WinContext(WinMethod.DISCARD), p))
        assertEquals(GroupType.SEQUENCE, ron.shape.groups[ron.winningGroup].type)
        assertTrue(ron.awards.any { it.pattern == Pattern.FOUR_CONCEALED_TRIPLETS })
        assertFalse(ron.awards.any { it.pattern == Pattern.THREE_CONCEALED_TRIPLETS })
    }

    @Test fun pinfuAndReplacementPoliciesDoNotChangeStructuralWinning() {
        val all = tiles("123456m123p123789s55p")
        val hand = Hand(all.dropLast(1))
        val strict = TaiwanScoringProfile("STRICT", p.values, p.exclusions, p.sources, p.flowers, p.flowerSets,
            PinfuPolicy.MULTIPLE_WAIT_DISCARD_SEQUENCES, ReplacementPolicy.KONG_ONLY)
        val ron = assertNotNull(TaiwanScoring.score(hand, all.last(), WinContext(WinMethod.DISCARD), strict))
        assertFalse(ron.awards.any { it.pattern == Pattern.PINFU })
        val draw = assertNotNull(TaiwanScoring.score(hand, all.last(), WinContext(WinMethod.SELF_DRAW, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT), strict))
        assertFalse(draw.awards.any { it.pattern == Pattern.REPLACEMENT_WIN || it.pattern == Pattern.PINFU })
        // Seven pairs plus a triplet is a historical optional form, absent from the selected profiles.
        assertTrue(TaiwanMahjong.winningShapes(Hand(tiles("1199m1199p1199s11555z"))).isEmpty())
    }

    @Test fun initialFlowerEventsHaveNoSeventeenthTileOrOrdinaryScore() {
        for ((event, flowers) in listOf(FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT to Flower.entries.toSet(),
            FlowerEvent.SEVEN_AFTER_INITIAL_REPLACEMENT to Flower.entries.take(7).toSet())) {
            val score = assertNotNull(TaiwanScoring.flowerWin(flowers, event, p))
            assertNull(score.handScore)
            assertEquals(8, score.rawTai)
            assertEquals(8, score.tai)
            val extra = ReplacementWin(Hand(tiles("123456m123p123789s5z")), 31,
                WinContext(WinMethod.SELF_DRAW, flowers = flowers, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT))
            assertFailsWith<IllegalArgumentException> { TaiwanScoring.flowerWin(flowers, event, p, extra) }
        }
    }

    @Test fun exactExclusionSetsAndWindUnitsStayStable() {
        val all = tiles("111m222p333s44455566z")
        val result = assertNotNull(TaiwanScoring.score(Hand(all.dropLast(1)), all.last(), WinContext(WinMethod.SELF_DRAW), p))
        assertEquals(setOf(Pattern.FIVE_CONCEALED_TRIPLETS, Pattern.ALL_TRIPLETS, Pattern.CONCEALED_SELF_DRAW,
            Pattern.SINGLE_WAIT, Pattern.WHITE_DRAGON), result.awards.map { it.pattern }.toSet())
        assertEquals(17, result.rawTai)
        val windHand = tiles("111m222p333s11155566z")
        val wind = assertNotNull(TaiwanScoring.score(Hand(windHand.dropLast(1)), windHand.last(), WinContext(WinMethod.DISCARD, seatWind = 28), p))
        assertEquals(listOf(Pattern.ROUND_WIND), wind.awards.filter { it.pattern == Pattern.ROUND_WIND || it.pattern == Pattern.SEAT_WIND }.map { it.pattern })
    }
}
