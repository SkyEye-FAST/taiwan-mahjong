package top.skyeyefast.taiwan

import kotlin.test.*
import org.junit.jupiter.api.Test

class TaiwanScoringTest {
    private val pocket = TaiwanProfiles.POCKET_COMMON
    private fun score(text: String, context: WinContext = WinContext(WinMethod.SELF_DRAW),
                      profile: TaiwanScoringProfile = pocket): TaiwanScore {
        val all = tiles(text)
        return assertNotNull(TaiwanScoring.score(Hand(all.dropLast(1)), all.last(), context, profile))
    }
    private fun TaiwanScore.patterns() = awards.map { it.pattern }.toSet()

    @Test fun exclusionsDoNotDoubleCountAndFiveTripletsStillAddAllTriplets() {
        val s = score("111m222p333s44455566z")
        assertTrue(Pattern.FIVE_CONCEALED_TRIPLETS in s.patterns())
        assertTrue(Pattern.ALL_TRIPLETS in s.patterns())
        assertTrue(Pattern.CONCEALED_SELF_DRAW in s.patterns())
        assertFalse(Pattern.FOUR_CONCEALED_TRIPLETS in s.patterns())
        assertFalse(Pattern.THREE_CONCEALED_TRIPLETS in s.patterns())
        assertFalse(Pattern.CONCEALED in s.patterns())
        assertFalse(Pattern.SELF_DRAW in s.patterns())
    }

    @Test fun dragonAndHonorExclusionsAreIndependentOfOtherAwards() {
        val s = score("11122255566677733z")
        assertTrue(Pattern.ALL_HONORS in s.patterns())
        assertTrue(Pattern.BIG_THREE_DRAGONS in s.patterns())
        assertFalse(Pattern.ALL_TRIPLETS in s.patterns())
        assertFalse(Pattern.RED_DRAGON in s.patterns())
        assertFalse(Pattern.GREEN_DRAGON in s.patterns())
        assertFalse(Pattern.WHITE_DRAGON in s.patterns())
        assertTrue(Pattern.SEAT_WIND in s.patterns())
        assertTrue(Pattern.ROUND_WIND in s.patterns())
    }

    @Test fun ambiguousWinningPlacementUsesBestScoreAndDiscardTripletIsNotConcealed() {
        val before = Hand(tiles("111222333m444p55s66z"))
        val discard = assertNotNull(TaiwanScoring.score(before, 22, WinContext(WinMethod.DISCARD), pocket))
        assertTrue(Pattern.FOUR_CONCEALED_TRIPLETS in discard.patterns())
        assertFalse(Pattern.FIVE_CONCEALED_TRIPLETS in discard.patterns())
        val self = assertNotNull(TaiwanScoring.score(before, 22, WinContext(WinMethod.SELF_DRAW), pocket))
        assertTrue(Pattern.FIVE_CONCEALED_TRIPLETS in self.patterns())
    }

    @Test fun southernProfileSharesDetectorButChangesFlowersWaitAndCap() {
        val north = score("111m222p333s44455566z")
        val south = score("111m222p333s44455566z", profile = TaiwanProfiles.SOUTHERN_COMMON)
        assertTrue(Pattern.SINGLE_WAIT in north.patterns())
        assertFalse(Pattern.SINGLE_WAIT in south.patterns())
        assertEquals(4, south.tai)
        assertTrue(south.rawTai > south.tai)
        assertFailsWith<IllegalArgumentException> {
            score("111m222p333s44455566z", WinContext(WinMethod.SELF_DRAW, flowers = setOf(Flower.SPRING)), TaiwanProfiles.SOUTHERN_COMMON)
        }
    }

    @Test fun flowerNumberIsExplicitAndSetsCanReplaceSeatFlowers() {
        val c = WinContext(WinMethod.DISCARD, flowerNumber = 3, flowers = setOf(Flower.CHRYSANTHEMUM, Flower.AUTUMN))
        val s = score("123456m123p123789s55z", c)
        assertEquals(2, s.awards.single { it.pattern == Pattern.SEAT_FLOWER }.tai)
        val custom = TaiwanScoringProfile("CUSTOM", pocket.values, pocket.exclusions, pocket.sources,
            pocket.flowers, FlowerSetPolicy.REPLACE_SEAT_FLOWER, pocket.pinfu, pocket.replacements)
        val allSeasons = WinContext(WinMethod.DISCARD, flowers = Flower.entries.filter { it.family == 0 }.toSet())
        val replaced = score("123456m123p123789s55z", allSeasons, custom)
        assertTrue(Pattern.FLOWER_SET in replaced.patterns())
        assertFalse(Pattern.SEAT_FLOWER in replaced.patterns())
    }

    @Test fun openingExclusionsAndInvalidContexts() {
        val s = score("111m222p333s44455566z", WinContext(WinMethod.SELF_DRAW, opening = OpeningWin.HEAVENLY))
        assertTrue(Pattern.HEAVENLY_WIN in s.patterns())
        assertFalse(Pattern.CONCEALED_SELF_DRAW in s.patterns())
        assertFalse(Pattern.CONCEALED in s.patterns())
        assertFalse(Pattern.SELF_DRAW in s.patterns())
        assertFailsWith<IllegalArgumentException> { WinContext(WinMethod.DISCARD, drawOrigin = DrawOrigin.KONG_REPLACEMENT) }
        assertFailsWith<IllegalArgumentException> { WinContext(WinMethod.SELF_DRAW, opening = OpeningWin.EARTHLY) }
        assertFailsWith<IllegalArgumentException> {
            TaiwanScoringProfile("BAD", pocket.values, mapOf(Pattern.CONCEALED to setOf(Pattern.CONCEALED)),
                pocket.sources, pocket.flowers, pocket.flowerSets, pocket.pinfu, pocket.replacements)
        }
    }

    @Test fun specialFlowerWinsNeedNoOrdinaryShapeAndRequireCorrectReplacementStage() {
        val flowers = Flower.entries.toSet()
        val input = ReplacementWin(Hand(tiles("147m147p147s1234567z")), 0,
            WinContext(WinMethod.SELF_DRAW, flowers = flowers, drawOrigin = DrawOrigin.FLOWER_REPLACEMENT))
        val special = assertNotNull(TaiwanScoring.flowerWin(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, pocket, input))
        assertEquals(8, special.award.tai)
        assertNull(special.handScore)
        assertNotNull(TaiwanScoring.flowerWin(flowers - Flower.BAMBOO, FlowerEvent.SEVEN_ON_OPPONENT_FLOWER, pocket))
        assertFailsWith<IllegalArgumentException> { TaiwanScoring.flowerWin(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, pocket) }
        assertFailsWith<IllegalArgumentException> { TaiwanScoring.flowerWin(flowers, FlowerEvent.SEVEN_ON_OPPONENT_FLOWER, pocket) }
        val ordinary = tiles("111m222p333s44455566z")
        val completed = ReplacementWin(Hand(ordinary.dropLast(1)), ordinary.last(), input.context)
        val both = assertNotNull(TaiwanScoring.flowerWin(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, pocket, completed))
        assertNotNull(both.handScore)
        assertFalse(Pattern.SEAT_FLOWER in both.handScore.patterns())
        assertFalse(Pattern.FLOWER_SET in both.handScore.patterns())
        val capped = TaiwanScoringProfile("CAPPED_FLOWERS", pocket.values, pocket.exclusions, pocket.sources,
            pocket.flowers, pocket.flowerSets, pocket.pinfu, pocket.replacements, 4)
        val cappedFlower = assertNotNull(TaiwanScoring.flowerWin(flowers, FlowerEvent.EIGHT_AFTER_REPLACEMENT, capped, completed))
        assertEquals(4, cappedFlower.tai)
        assertEquals(8 + cappedFlower.handScore!!.rawTai, cappedFlower.rawTai)
    }
}
