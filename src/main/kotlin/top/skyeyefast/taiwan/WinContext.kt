package top.skyeyefast.taiwan

enum class WinMethod { DISCARD, SELF_DRAW, ROBBING_KONG }
enum class DrawOrigin { ORDINARY, KONG_REPLACEMENT, FLOWER_REPLACEMENT }
enum class OpeningWin { NONE, HEAVENLY, EARTHLY, HUMAN }
enum class ReadyDeclaration { NONE, ORDINARY, HEAVENLY, EARTHLY }

/** Number is explicit: engine flower enumeration order must not imply flower position. */
enum class Flower(val number: Int, val family: Int) {
    SPRING(1, 0), SUMMER(2, 0), AUTUMN(3, 0), WINTER(4, 0),
    PLUM(1, 1), ORCHID(2, 1), CHRYSANTHEMUM(3, 1), BAMBOO(4, 1)
}

/** Facts supplied by the host, which owns chronology and legal claim permission. No payment state. */
class WinContext(
    val method: WinMethod,
    val seatWind: Int = 27,
    val roundWind: Int = 27,
    val flowerNumber: Int = seatWind - 26,
    flowers: Set<Flower> = emptySet(),
    val drawOrigin: DrawOrigin = DrawOrigin.ORDINARY,
    val lastTile: Boolean = false,
    val opening: OpeningWin = OpeningWin.NONE,
    val ready: ReadyDeclaration = ReadyDeclaration.NONE,
) {
    val flowers: Set<Flower> = java.util.Set.copyOf(flowers)
    init {
        require(seatWind in 27..30 && roundWind in 27..30 && flowerNumber in 1..4)
        require(method == WinMethod.SELF_DRAW || drawOrigin == DrawOrigin.ORDINARY)
        require(method != WinMethod.ROBBING_KONG || !lastTile)
        require(opening != OpeningWin.HEAVENLY || method == WinMethod.SELF_DRAW && seatWind == 27)
        require(opening != OpeningWin.EARTHLY || method == WinMethod.SELF_DRAW && seatWind != 27)
        require(opening != OpeningWin.HUMAN || method == WinMethod.DISCARD && seatWind != 27)
        require(opening == OpeningWin.NONE || ready == ReadyDeclaration.NONE)
        require(ready != ReadyDeclaration.HEAVENLY || seatWind == 27)
        require(ready != ReadyDeclaration.EARTHLY || seatWind != 27)
    }
}

data class TaiAward(val pattern: Pattern, val units: Int, val tai: Int, val source: RuleSource)
class TaiwanScore(
    val shape: WinningShape,
    /** -1 denotes the pair; otherwise index into concealed shape groups. */
    val winningGroup: Int,
    awards: List<TaiAward>,
    val rawTai: Int,
    val tai: Int,
) {
    val awards: List<TaiAward> = java.util.List.copyOf(awards)
}

enum class FlowerEvent {
    EIGHT_AFTER_REPLACEMENT, SEVEN_AFTER_REPLACEMENT, SEVEN_ON_OPPONENT_FLOWER,
    EIGHT_AFTER_INITIAL_REPLACEMENT, SEVEN_AFTER_INITIAL_REPLACEMENT
}
class ReplacementWin(val hand: Hand, val winningKind: Int, val context: WinContext) {
    init {
        hand.requireSize(16)
        hand.withTile(winningKind)
        require(context.method == WinMethod.SELF_DRAW && context.drawOrigin == DrawOrigin.FLOWER_REPLACEMENT)
    }
}
/** Components retain raw awards; tai is the aggregate cap, not a payment allocation. */
class FlowerScore(val award: TaiAward, val handScore: TaiwanScore?, val rawTai: Int, val tai: Int)
