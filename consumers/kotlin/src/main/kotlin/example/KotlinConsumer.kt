package example

import top.skyeyefast.taiwan.*

fun main() {
    val tiles = listOf(0, 1, 2, 3, 4, 5, 9, 10, 11, 18, 19, 20, 24, 25, 26, 33)
    val ready = Hand(tiles)
    val complete = Hand(tiles + 33)
    check(ready.size == 16 && TaiwanMahjong.shanten(ready) == 0)
    check(33 in TaiwanMahjong.waitingTiles(ready))
    check(TaiwanMahjong.analyze(ready).remaining > 0)
    check(TaiwanMahjong.discards(complete).isNotEmpty())
    check(TaiwanMahjong.winningShapes(complete).isNotEmpty())
    val context = WinContext(WinMethod.SELF_DRAW)
    val pocket = checkNotNull(TaiwanScoring.score(ready, 33, context, TaiwanProfiles.POCKET_COMMON))
    val southern = checkNotNull(TaiwanScoring.score(ready, 33, context, TaiwanProfiles.SOUTHERN_COMMON))
    check(pocket.tai > 0 && pocket.awards.isNotEmpty())
    check(southern.tai <= 4 && TaiwanProfiles.SOUTHERN_COMMON.flowers == FlowerPolicy.NONE)
    val flower = checkNotNull(TaiwanScoring.flowerWin(Flower.entries.toSet(), FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT, TaiwanProfiles.POCKET_COMMON))
    check(flower.tai == 8 && flower.handScore == null)
    println("Kotlin consumer PASS (JVM ${Runtime.version().feature()})")
}
