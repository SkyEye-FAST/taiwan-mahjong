package top.skyeyefast.taiwan

object TaiwanScoring {
    /** Enumerates both decompositions and the winning tile's placement, taking maximum payable tai. */
    @JvmStatic
    fun score(hand: Hand, winningKind: Int, context: WinContext, profile: TaiwanScoringProfile): TaiwanScore? =
        scoreRegular(hand, winningKind, context, profile, false)

    private fun scoreRegular(
        hand: Hand, winningKind: Int, context: WinContext, profile: TaiwanScoringProfile, flowerWin: Boolean,
    ): TaiwanScore? {
        hand.requireSize(16)
        require(profile.flowers != FlowerPolicy.NONE || context.flowers.isEmpty())
        require(context.opening == OpeningWin.NONE || hand.melds.isEmpty())
        val complete = hand.withTile(winningKind)
        val waits = TaiwanMahjong.waitingTiles(hand)
        val candidates = ArrayList<TaiwanScore>()
        for (shape in TaiwanMahjong.winningShapes(complete)) {
            val positions = shape.groups.indices.filter { winningKind in shape.groups[it].tiles }.toMutableList()
            if (shape.pair == winningKind) positions.add(0, -1)
            for (position in positions) candidates += evaluate(hand, shape, position, context, profile, waits.size, flowerWin)
        }
        return candidates.maxWithOrNull(compareBy<TaiwanScore> { it.tai }.thenBy { it.rawTai })
    }

    private fun evaluate(
        hand: Hand, shape: WinningShape, position: Int, c: WinContext, p: TaiwanScoringProfile,
        waitCount: Int, flowerWin: Boolean,
    ): TaiwanScore {
        val found = linkedMapOf<Pattern, Int>()
        fun add(pattern: Pattern, condition: Boolean = true, units: Int = 1) {
            if (condition && units > 0 && p.values.getValue(pattern) > 0) found[pattern] = units
        }
        val allGroups = hand.melds + shape.groups
        val triplets = allGroups.filter { it.type != GroupType.SEQUENCE }.map { it.kind }.toSet()
        val closed = hand.melds.all { it.type == GroupType.CONCEALED_KONG }
        val selfDraw = c.method == WinMethod.SELF_DRAW
        val concealedTriplets = hand.melds.count { it.type == GroupType.CONCEALED_KONG } +
            shape.groups.withIndex().count { (index, group) ->
                group.type == GroupType.TRIPLET && (selfDraw || index != position)
            }
        val allKinds = allGroups.flatMap { it.tiles } + shape.pair
        val suits = allKinds.filter { it < 27 }.map { it / 9 }.toSet()
        val honors = allKinds.any { it >= 27 }
        add(Pattern.ALL_HONORS, suits.isEmpty())
        add(Pattern.FULL_FLUSH, suits.size == 1 && !honors)
        add(Pattern.HALF_FLUSH, suits.size == 1 && honors)
        add(Pattern.ALL_TRIPLETS, allGroups.none { it.type == GroupType.SEQUENCE })
        add(Pattern.BIG_FOUR_WINDS, (27..30).all { it in triplets })
        add(Pattern.SMALL_FOUR_WINDS, shape.pair in 27..30 && (27..30).count { it in triplets } == 3)
        add(Pattern.BIG_THREE_DRAGONS, (31..33).all { it in triplets })
        add(Pattern.SMALL_THREE_DRAGONS, shape.pair in 31..33 && (31..33).count { it in triplets } == 2)
        add(Pattern.THREE_CONCEALED_TRIPLETS, concealedTriplets >= 3)
        add(Pattern.FOUR_CONCEALED_TRIPLETS, concealedTriplets >= 4)
        add(Pattern.FIVE_CONCEALED_TRIPLETS, concealedTriplets == 5)
        add(Pattern.CONCEALED, closed)
        add(Pattern.SELF_DRAW, selfDraw)
        add(Pattern.CONCEALED_SELF_DRAW, closed && selfDraw)
        val allOpen = hand.melds.size == 5 && !hand.melds.any { it.type == GroupType.CONCEALED_KONG }
        add(Pattern.ALL_FROM_OTHERS, allOpen && !selfDraw)
        add(Pattern.HALF_FROM_OTHERS, allOpen && selfDraw)
        add(Pattern.PINFU, !selfDraw && !honors && c.flowers.isEmpty() &&
            allGroups.all { it.type == GroupType.SEQUENCE } &&
            (p.pinfu == PinfuPolicy.DISCARD_SEQUENCES || waitCount > 1 && position != -1))
        add(Pattern.SINGLE_WAIT, waitCount == 1)
        add(Pattern.WHITE_DRAGON, 31 in triplets)
        add(Pattern.GREEN_DRAGON, 32 in triplets)
        add(Pattern.RED_DRAGON, 33 in triplets)
        add(Pattern.SEAT_WIND, c.seatWind in triplets)
        add(Pattern.ROUND_WIND, c.roundWind in triplets)
        add(Pattern.REPLACEMENT_WIN, selfDraw && (c.drawOrigin == DrawOrigin.KONG_REPLACEMENT ||
            c.drawOrigin == DrawOrigin.FLOWER_REPLACEMENT && p.replacements == ReplacementPolicy.KONG_OR_FLOWER))
        add(Pattern.LAST_DRAW, selfDraw && c.lastTile)
        add(Pattern.LAST_DISCARD, c.method == WinMethod.DISCARD && c.lastTile)
        add(Pattern.ROBBING_KONG, c.method == WinMethod.ROBBING_KONG)
        add(Pattern.HEAVENLY_WIN, c.opening == OpeningWin.HEAVENLY)
        add(Pattern.EARTHLY_WIN, c.opening == OpeningWin.EARTHLY)
        add(Pattern.HUMAN_WIN, c.opening == OpeningWin.HUMAN)
        add(Pattern.DECLARED_READY, c.ready != ReadyDeclaration.NONE)
        add(Pattern.HEAVENLY_READY, c.ready == ReadyDeclaration.HEAVENLY)
        add(Pattern.EARTHLY_READY, c.ready == ReadyDeclaration.EARTHLY)
        if (!flowerWin && p.flowers != FlowerPolicy.NONE) {
            val completeFamilies = (0..1).filter { family -> c.flowers.count { it.family == family } == 4 }
            add(Pattern.FLOWER_SET, units = completeFamilies.size)
            add(Pattern.SEAT_FLOWER, units = c.flowers.count {
                it.number == c.flowerNumber && (p.flowerSets == FlowerSetPolicy.ADD_SEAT_FLOWER || it.family !in completeFamilies)
            })
        }
        // Exclusions are applied simultaneously: suppressed composite awards never resurrect components.
        val excluded = found.keys.flatMap { p.exclusions[it].orEmpty() }.toSet()
        val awards = found.filterKeys { it !in excluded }.map { (pattern, units) ->
            TaiAward(pattern, units, units * p.values.getValue(pattern), p.sources.getValue(pattern))
        }
        val raw = awards.sumOf { it.tai }
        return TaiwanScore(shape, position, awards, raw, minOf(raw, p.taiLimit ?: Int.MAX_VALUE))
    }

    /** Flower victory is not a 17-tile decomposition. Payment routing remains the host's concern. */
    @JvmStatic
    @JvmOverloads
    fun flowerWin(
        flowers: Set<Flower>, event: FlowerEvent, profile: TaiwanScoringProfile, replacement: ReplacementWin? = null,
    ): FlowerScore? {
        require(profile.flowers != FlowerPolicy.NONE) { "This profile has no flowers" }
        val pattern = if (event == FlowerEvent.EIGHT_AFTER_REPLACEMENT || event == FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT) Pattern.EIGHT_FLOWERS else Pattern.SEVEN_ROBS_ONE
        require(flowers.size == if (pattern == Pattern.EIGHT_FLOWERS) 8 else 7)
        val needsReplacementHand = event == FlowerEvent.EIGHT_AFTER_REPLACEMENT || event == FlowerEvent.SEVEN_AFTER_REPLACEMENT
        require((replacement != null) == needsReplacementHand)
        if (replacement != null) require(replacement.context.flowers == flowers)
        val value = profile.values.getValue(pattern)
        if (value == 0) return null
        val handScore = replacement?.let { scoreRegular(it.hand, it.winningKind, it.context, profile, true) }
        val rawTai = value + (handScore?.rawTai ?: 0)
        return FlowerScore(TaiAward(pattern, 1, value, profile.sources.getValue(pattern)), handScore,
            rawTai, minOf(rawTai, profile.taiLimit ?: Int.MAX_VALUE))
    }
}
