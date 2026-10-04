package top.skyeyefast.taiwan

import kotlin.test.*
import org.junit.jupiter.api.Test
import kotlin.random.Random
import top.skyeyefast.mcr.McrMahjong
import top.skyeyefast.mcr.HandForm

internal fun tiles(text: String): List<Int> {
    val result = ArrayList<Int>()
    val digits = ArrayList<Int>()
    for (c in text) when {
        c.isDigit() -> digits += c.digitToInt() - 1
        c in "mpsz" -> {
            result += digits.map { "mpsz".indexOf(c) * 9 + it }
            digits.clear()
        }
        c.isWhitespace() -> Unit
        else -> error("Invalid fixture")
    }
    check(digits.isEmpty())
    return result
}

class TaiwanMahjongTest {
    @Test fun fiveMeldsAndAmbiguousDecompositions() {
        val hand = Hand(tiles("111222333m456p789s55z"))
        val shapes = TaiwanMahjong.winningShapes(hand)
        assertEquals(2, shapes.size)
        assertTrue(shapes.all { it.groups.size == 5 && it.pair == 31 })
        assertEquals(-1, TaiwanMahjong.shanten(hand))
        val waiting = Hand(hand.concealed.dropLast(1))
        assertEquals(setOf(31), TaiwanMahjong.waitingTiles(waiting))
        assertEquals(0, TaiwanMahjong.shanten(waiting))
        assertEquals(listOf(EffectiveTile(31, 3)), TaiwanMahjong.analyze(waiting).effectiveTiles)
    }

    @Test fun everyFixedMeldCountAndKongPhysicalCount() {
        val groups = listOf(Group(GroupType.SEQUENCE, 0), Group(GroupType.SEQUENCE, 9),
            Group(GroupType.SEQUENCE, 18), Group(GroupType.TRIPLET, 27), Group(GroupType.CONCEALED_KONG, 32))
        for (fixed in 0..5) {
            val declared = groups.takeLast(fixed)
            val concealed = groups.dropLast(fixed).flatMap { group ->
                if (group.type == GroupType.CONCEALED_KONG) List(3) { group.kind } else group.tiles
            } + 33
            val hand = Hand(concealed, declared)
            assertEquals(0, TaiwanMahjong.shanten(hand))
            assertEquals(setOf(33), TaiwanMahjong.waitingTiles(hand))
            assertEquals(5 - fixed, TaiwanMahjong.winningShapes(hand.withTile(33)).first().groups.size)
        }
        assertFailsWith<IllegalArgumentException> {
            Hand(tiles("123m123p123s1112z"), listOf(Group(GroupType.CONCEALED_KONG, 27)))
        }
    }

    @Test fun fifthCopiesExhaustionAndDiscardVisibility() {
        val blocked = Hand(listOf(0, 0, 0, 0), listOf(Group(GroupType.SEQUENCE, 9),
            Group(GroupType.SEQUENCE, 18), Group(GroupType.TRIPLET, 27), Group(GroupType.TRIPLET, 31)))
        assertEquals(1, TaiwanMahjong.shanten(blocked))
        assertTrue(TaiwanMahjong.waitingTiles(blocked).isEmpty())
        val melds = listOf(Group(GroupType.CONCEALED_KONG, 27), Group(GroupType.SEQUENCE, 0),
            Group(GroupType.SEQUENCE, 9), Group(GroupType.SEQUENCE, 18), Group(GroupType.TRIPLET, 31))
        val hand = Hand(listOf(32), melds)
        assertEquals(setOf(32), TaiwanMahjong.waitingTiles(hand))
        assertTrue(TaiwanMahjong.analyze(hand, List(3) { 32 }).effectiveTiles.isEmpty())
        assertEquals(0, TaiwanMahjong.analyze(hand, List(3) { 32 }).shanten)
        assertFailsWith<IllegalArgumentException> { TaiwanMahjong.analyze(hand, listOf(27)) }
        val discard = TaiwanMahjong.discards(Hand(listOf(32, 32), melds)).single()
        assertEquals(2, discard.analysis.effectiveTiles.single().remaining)
        assertFailsWith<IllegalArgumentException> { Hand(List(16) { 34 }) }
        assertFailsWith<IllegalArgumentException> { Hand(List(14) { it }) }
    }

    @Test fun randomizedFourMeldRemainderMatchesMatureRegularOracle() {
        val random = Random(1705)
        val pool = (0..33).filter { it != 27 }.flatMap { k -> List(4) { k } }
        fun oracleTile(k: Int) = top.skyeyefast.mcr.Tile.parse(
            if (k < 27) "${k % 9 + 1}${"mps"[k / 9]}" else listOf("E", "S", "W", "N", "P", "F", "C")[k - 27]
        )
        repeat(80) {
            val concealed = pool.shuffled(random).take(13)
            val hand = Hand(concealed, listOf(Group(GroupType.TRIPLET, 27)))
            val expected = McrMahjong.analyze(top.skyeyefast.mcr.Hand(concealed.map(::oracleTile)))
                .forms.single { it.form == HandForm.REGULAR }.shanten
            assertEquals(expected, TaiwanMahjong.shanten(hand), concealed.toString())
        }
    }

    @Test fun generatedFiveMeldWinsAgreeAcrossDecomposerDistanceAndWaits() {
        val random = Random(16017)
        var checked = 0
        while (checked < 40) {
            val groups = List(5) {
                if (random.nextBoolean()) Group(GroupType.TRIPLET, random.nextInt(34))
                else Group(GroupType.SEQUENCE, random.nextInt(3) * 9 + random.nextInt(7))
            }
            val pair = random.nextInt(34)
            val complete = groups.flatMap { it.tiles } + listOf(pair, pair)
            if (complete.groupingBy { it }.eachCount().values.any { it > 4 }) continue
            val hand = Hand(complete)
            assertEquals(-1, TaiwanMahjong.shanten(hand))
            assertTrue(TaiwanMahjong.winningShapes(hand).isNotEmpty())
            val before = Hand(complete.dropLast(1))
            assertEquals(0, TaiwanMahjong.shanten(before))
            assertTrue(pair in TaiwanMahjong.waitingTiles(before))
            checked++
        }
    }

    @Test fun snapshotsAreDefensiveAndUnmodifiable() {
        val source = tiles("111222333m456p789s5z").toMutableList()
        val hand = Hand(source)
        source.clear()
        assertEquals(16, hand.concealed.size)
        assertFailsWith<UnsupportedOperationException> { (hand.concealed as MutableList).clear() }
        val shapes = TaiwanMahjong.winningShapes(hand.withTile(31))
        assertFailsWith<UnsupportedOperationException> { (shapes[0].groups as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (TaiwanProfiles.POCKET_COMMON.values as MutableMap).clear() }
    }
}
