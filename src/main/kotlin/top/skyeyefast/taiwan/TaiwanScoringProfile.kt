package top.skyeyefast.taiwan

enum class Pattern {
    HEAVENLY_WIN, EARTHLY_WIN, HUMAN_WIN, HEAVENLY_READY, EARTHLY_READY,
    EIGHT_FLOWERS, SEVEN_ROBS_ONE, BIG_FOUR_WINDS, ALL_HONORS, FULL_FLUSH,
    SMALL_FOUR_WINDS, BIG_THREE_DRAGONS, FIVE_CONCEALED_TRIPLETS, FOUR_CONCEALED_TRIPLETS,
    ALL_TRIPLETS, HALF_FLUSH, SMALL_THREE_DRAGONS, PINFU, THREE_CONCEALED_TRIPLETS,
    CONCEALED, SELF_DRAW, CONCEALED_SELF_DRAW, ALL_FROM_OTHERS, HALF_FROM_OTHERS,
    REPLACEMENT_WIN, LAST_DRAW, LAST_DISCARD, ROBBING_KONG, DECLARED_READY, SINGLE_WAIT,
    RED_DRAGON, GREEN_DRAGON, WHITE_DRAGON, FLOWER_SET, ROUND_WIND, SEAT_WIND, SEAT_FLOWER
}

enum class FlowerPolicy { NONE, SEAT_NUMBER }
enum class FlowerSetPolicy { ADD_SEAT_FLOWER, REPLACE_SEAT_FLOWER }
enum class PinfuPolicy { DISCARD_SEQUENCES, MULTIPLE_WAIT_DISCARD_SEQUENCES }
enum class ReplacementPolicy { KONG_ONLY, KONG_OR_FLOWER }

data class RuleSource(val url: String, val clause: String) {
    init { require(url.startsWith("https://") && clause.isNotBlank()) }
}

/** Complete immutable values and explicit directed exclusions; names never control scoring. */
class TaiwanScoringProfile(
    val name: String,
    values: Map<Pattern, Int>,
    exclusions: Map<Pattern, Set<Pattern>>,
    sources: Map<Pattern, RuleSource>,
    val flowers: FlowerPolicy,
    val flowerSets: FlowerSetPolicy,
    val pinfu: PinfuPolicy,
    val replacements: ReplacementPolicy,
    val taiLimit: Int? = null,
) {
    val values: Map<Pattern, Int> = java.util.Map.copyOf(values)
    val exclusions: Map<Pattern, Set<Pattern>> = java.util.Map.copyOf(
        exclusions.mapValues { java.util.Set.copyOf(it.value) }
    )
    val sources: Map<Pattern, RuleSource> = java.util.Map.copyOf(sources)
    init {
        require(name.isNotBlank())
        require(values.keys == Pattern.entries.toSet() && sources.keys == values.keys)
        require(values.values.all { it in 0..1000 })
        require(taiLimit == null || taiLimit in 1..1000)
        fun visit(pattern: Pattern, path: Set<Pattern>) {
            require(pattern !in path) { "Cyclic scoring exclusions" }
            this.exclusions[pattern].orEmpty().forEach { visit(it, path + pattern) }
        }
        Pattern.entries.forEach { visit(it, emptySet()) }
    }
}

/** Source-bound common presets, never official regional standards. See RULES.md. */
object TaiwanProfiles {
    private const val POCKET = "https://pocket.funclub.com.tw/rule"
    private const val SOUTH = "https://www.eastking.com.tw/blog/posts/mahjong-south-north-differences"
    private val clauses = mapOf(
        Pattern.HEAVENLY_WIN to "天胡", Pattern.EARTHLY_WIN to "地胡", Pattern.HUMAN_WIN to "人胡",
        Pattern.HEAVENLY_READY to "天聽", Pattern.EARTHLY_READY to "地聽",
        Pattern.EIGHT_FLOWERS to "八仙過海", Pattern.SEVEN_ROBS_ONE to "七搶一",
        Pattern.BIG_FOUR_WINDS to "大四喜", Pattern.ALL_HONORS to "字一色", Pattern.FULL_FLUSH to "清一色",
        Pattern.SMALL_FOUR_WINDS to "小四喜", Pattern.BIG_THREE_DRAGONS to "大三元",
        Pattern.FIVE_CONCEALED_TRIPLETS to "五暗刻", Pattern.FOUR_CONCEALED_TRIPLETS to "四暗刻",
        Pattern.ALL_TRIPLETS to "碰碰胡", Pattern.HALF_FLUSH to "混一色", Pattern.SMALL_THREE_DRAGONS to "小三元",
        Pattern.PINFU to "平胡", Pattern.THREE_CONCEALED_TRIPLETS to "三暗刻",
        Pattern.CONCEALED to "門清", Pattern.SELF_DRAW to "自摸", Pattern.CONCEALED_SELF_DRAW to "門清自摸",
        Pattern.ALL_FROM_OTHERS to "全求人", Pattern.HALF_FROM_OTHERS to "半求人", Pattern.REPLACEMENT_WIN to "槓上開花",
        Pattern.LAST_DRAW to "海底撈月", Pattern.LAST_DISCARD to "河底撈魚", Pattern.ROBBING_KONG to "搶槓",
        Pattern.DECLARED_READY to "聽牌", Pattern.SINGLE_WAIT to "獨聽", Pattern.RED_DRAGON to "紅中",
        Pattern.GREEN_DRAGON to "青發", Pattern.WHITE_DRAGON to "白板", Pattern.FLOWER_SET to "花槓",
        Pattern.ROUND_WIND to "圈風", Pattern.SEAT_WIND to "門風", Pattern.SEAT_FLOWER to "門花",
    )
    private val values = Pattern.entries.associateWith {
        when (it) {
            Pattern.HEAVENLY_WIN -> 24
            Pattern.EARTHLY_WIN, Pattern.HEAVENLY_READY, Pattern.BIG_FOUR_WINDS, Pattern.ALL_HONORS -> 16
            Pattern.HUMAN_WIN, Pattern.EARTHLY_READY, Pattern.EIGHT_FLOWERS, Pattern.SEVEN_ROBS_ONE,
            Pattern.FULL_FLUSH, Pattern.SMALL_FOUR_WINDS, Pattern.BIG_THREE_DRAGONS,
            Pattern.FIVE_CONCEALED_TRIPLETS -> 8
            Pattern.FOUR_CONCEALED_TRIPLETS -> 5
            Pattern.ALL_TRIPLETS, Pattern.HALF_FLUSH, Pattern.SMALL_THREE_DRAGONS -> 4
            Pattern.CONCEALED_SELF_DRAW -> 3
            Pattern.PINFU, Pattern.THREE_CONCEALED_TRIPLETS, Pattern.ALL_FROM_OTHERS -> 2
            else -> 1
        }
    }
    private val exclusions = mapOf(
        Pattern.HEAVENLY_WIN to setOf(Pattern.CONCEALED_SELF_DRAW, Pattern.CONCEALED, Pattern.SELF_DRAW, Pattern.REPLACEMENT_WIN),
        Pattern.EARTHLY_WIN to setOf(Pattern.CONCEALED_SELF_DRAW, Pattern.CONCEALED, Pattern.SELF_DRAW),
        Pattern.HUMAN_WIN to setOf(Pattern.CONCEALED),
        Pattern.HEAVENLY_READY to setOf(Pattern.CONCEALED, Pattern.DECLARED_READY),
        Pattern.EARTHLY_READY to setOf(Pattern.DECLARED_READY),
        Pattern.CONCEALED_SELF_DRAW to setOf(Pattern.CONCEALED, Pattern.SELF_DRAW),
        Pattern.ALL_HONORS to setOf(Pattern.ALL_TRIPLETS),
        Pattern.BIG_THREE_DRAGONS to setOf(Pattern.RED_DRAGON, Pattern.GREEN_DRAGON, Pattern.WHITE_DRAGON),
        Pattern.SMALL_THREE_DRAGONS to setOf(Pattern.RED_DRAGON, Pattern.GREEN_DRAGON, Pattern.WHITE_DRAGON),
        Pattern.FIVE_CONCEALED_TRIPLETS to setOf(Pattern.FOUR_CONCEALED_TRIPLETS, Pattern.THREE_CONCEALED_TRIPLETS),
        Pattern.FOUR_CONCEALED_TRIPLETS to setOf(Pattern.THREE_CONCEALED_TRIPLETS),
    )

    @JvmField
    val POCKET_COMMON = TaiwanScoringProfile(
        "POCKET_COMMON", values, exclusions,
        Pattern.entries.associateWith { RuleSource(POCKET, clauses.getValue(it)) },
        FlowerPolicy.SEAT_NUMBER, FlowerSetPolicy.ADD_SEAT_FLOWER,
        PinfuPolicy.DISCARD_SEQUENCES, ReplacementPolicy.KONG_OR_FLOWER,
    )

    /** Project composition: Pocket base, no flowers/single wait, four-tai hand cap. */
    @JvmField
    val SOUTHERN_COMMON = TaiwanScoringProfile(
        "SOUTHERN_COMMON", values + mapOf(Pattern.SINGLE_WAIT to 0), exclusions,
        POCKET_COMMON.sources + (Pattern.SINGLE_WAIT to RuleSource(SOUTH, "2. 台數")),
        FlowerPolicy.NONE, FlowerSetPolicy.ADD_SEAT_FLOWER,
        PinfuPolicy.DISCARD_SEQUENCES, ReplacementPolicy.KONG_ONLY, 4,
    )
}
