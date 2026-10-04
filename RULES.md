# Taiwan scoring sources and decisions

Checked 2026-10-04. Presets describe specific documented choices, not official
Taipei, Taichung, Tainan or southern standards. Source text governs semantics;
third-party code is only an algorithm cross-check.

## Source register

| ID | Primary/secondary source | Use |
| --- | --- | --- |
| P | [Funclub: 口袋麻將牌型與台數](https://pocket.funclub.com.tw/rule) | Primary publisher's own complete scoring table; baseline. |
| S | [EASTKING: 南北差異, 2025-01-21](https://www.eastking.com.tw/blog/posts/mahjong-south-north-differences) | Mahjong-table manufacturer's regional description; no flowers, no wait tai, dealer distinction and four-tai convention. Not a tournament authority. |
| W | [Wanin: 麻將之星規則](https://mahjongstar.waningames.com/Games/Game_Rule) | Independent primary rules: documents alternative flower-set, pinfu, passing and claim policies. Not the selected preset. |
| G | [Gametower: 台數計算](https://www.gametower.com.tw/Games/Freeplay/MJ/Star31/Data/count.aspx) | Primary comparison; distinct 13/16-tile sections must not be mixed. |
| A | [PTT backup of atawmj.org.tw/mjking.htm](https://www.pttweb.cc/bbs/heart/M.1710649783.A.8F3) | Secondary association-page transcription only; original association page could not be verified. Not adopted. |
| H | [2004 Taiwanese mahjong discussion](https://groups.google.com/g/tw.bbs.rec.mj/c/VOefaRnFPPQ) | Historical personal compilation of optional eight-and-a-half pairs; not a current rule authority. |
| T | [2004 NTU mahjong tournament post](https://www.ptt.cc/bbs/NTU-MJ/M.1096995006.A.50A.html) | Event-specific optional form and value; not evidence for P or S. |

An association or competition title alone is insufficient: some Taiwanese
tournament documents govern MCR, not Taiwanese sixteen-tile play. P is selected
for directly inspectable definitions and explicit exclusions. Its own introduction
disclaims a uniform Taiwanese rule. Regional names are withheld where the source
does not establish the region; there is no `TAIPEI_COMMON` or `TAICHUNG_COMMON`
alias for the same rules.

## Pattern ledger

Every row below uses P's identically named scoring-table clause. Award records
also retain the source URL and pattern identifier. Values apply before any cap.

| Patterns (P clause) | Tai |
| --- | --- |
| 天胡 | 24 |
| 地胡、天聽、大四喜、字一色 | 16 each |
| 人胡、地聽、八仙過海、七搶一、清一色、小四喜、大三元、五暗刻 | 8 each |
| 四暗刻 | 5 |
| 碰碰胡、混一色、小三元 | 4 each |
| 門清自摸 | 3 |
| 平胡、三暗刻、全求人 | 2 each |
| 門清、自摸、半求人、槓上開花、海底撈月、河底撈魚、搶槓、聽牌、獨聽、紅中、青發、白板、花槓、圈風、門風、門花 | 1 each |

Source P's 莊家/連莊 rows belong to the host's payment contract, not this table.
Flower sets and matching seat flowers count by occurrence. Flower numbers are
explicit: seasons 1–4; plum/orchid/chrysanthemum/bamboo 1–4. This differs from
MChjong's physical flower enum order, so the adapter maps names, never ordinals.

## Combination ledger

P explicitly replaces individual dragon awards with big/small three dragons;
all honors replaces all triplets; concealed self-draw replaces its two components.
Heavenly/earthly wins exclude concealed self-draw; heavenly win also excludes
replacement win. Human win excludes concealed. Heavenly ready excludes concealed
and ordinary ready; earthly ready excludes ordinary ready. Five concealed triplets
still adds all triplets. Source W explicitly documents the descending concealed
triplet exclusions; this project uses the highest applicable tier rather than
adding three-, four- and five-triplet awards together.

The profile stores a directed exclusion graph and rejects cycles. Suppression is
simultaneous: removing a composite award cannot resurrect its components. Zero
value disables an award. Incompatible shapes (full/half flush, big/small winds,
big/small dragons) are mutually exclusive by definition.

## Explicit interpretation choices

These are project decisions where the selected source is not sufficiently exact;
they are not presented as additional published rules.

* All valid decompositions and winning placements compete by payable tai then
  raw tai. This avoids adopting G's implementation-specific triplet-first search.
* The headline “24 highest tai” in P describes its largest listed pattern, not a
  total cap: its heavenly-win clause explicitly permits other awards. P has no
  total cap here.
* P's pinfu uses its stated conditions; W's stricter non-single-wait condition is
  a separate custom policy. A flower set adds matching flower tai under P's
  general additive interpretation; W's replacement interpretation is available
  as `REPLACE_SEAT_FLOWER`.
* Heavenly ready combined with concealed self-draw retains the three-tai composite
  under the literal named-award exclusion; it does not also add the separate
  concealed award. This is an explicit interpretation, not an independently
  verified combination from P.
* A flower victory receives its flower award, with ordinary replacement-hand
  awards separately if that hand wins. Ordinary flower/flower-set bonuses are
  suppressed for this event, following W's explicit treatment. P does not spell
  out that exclusion. The library returns the two components separately and does
  not infer payer distribution from their sum.
* Seven-versus-one on an opponent's flower requires the host to certify that
  external flower; no invented ordinary winning tile is supplied. Replacement
  events require a completed replacement, including when the ordinary hand loses.
  `EIGHT_AFTER_INITIAL_REPLACEMENT` / `SEVEN_AFTER_INITIAL_REPLACEMENT` distinguish
  a nondealer's completed initial sixteen-tile hand: they accept no `ReplacementWin`
  and award no ordinary hand score. The host certifies completion and timing;
  the library does not decide opening procedure or invent a seventeenth tile.

## Special structural forms: evidence and admission

Rechecked H and T rather than transplanting seven pairs or thirteen orphans.
Their optional 八對半 / 嚦咕嚦咕 means **seven pairs and one triplet**, seventeen
tiles, not an ordinary sixteen-tile eight-pair hand. H lists eight tai and allows
four identical tiles to supply two pairs; T's event lists six tai and excludes
the ordinary concealed award. Combination rules and values differ. These sources
establish a historical optional form, not its inclusion in P or a complete
southern profile. Neither selected preset lists it, so no detector or award is
enabled. A nonstandard seven-pair-plus-triplet example is explicitly rejected;
a hand that independently has five melds and a pair can still win normally.
Eight ordinary pairs alone have only sixteen tiles and are not silently accepted
as a completed seventeen-tile hand. Original association rules remain unverified.

## Scoring audit coverage

`PatternRegressionTest` provides a positive witness for every `Pattern`, checking
source and unit values. Focused assertions cover exact award sets, dragon/honor
and concealed-tier exclusions, independent seat/round winds, opening/ready facts,
open five-meld hands, concealed kongs, ron versus self-draw triplets, alternative
decompositions, raw-score tie-breaking after cap, and pinfu/replacement policies.
The original scoring tests additionally cover flower-set replacement, southern
cap and disabled flowers, opponent-triggered flowers, invalid contexts and a
single aggregate flower/ordinary cap. Opening event timing, passing and payment
remain host responsibilities, not facts reconstructed from a hand.

## Southern composition and custom rules

`SOUTHERN_COMMON` reuses P's detector definitions and award values, changing:

| Choice | Source/decision |
| --- | --- |
| No flowers, 136-tile stock | S, 牌牆; engine flow consequence, no flower scoring. |
| No single-wait tai | S, 台數; value zero. |
| Four-tai hand cap | S mentions the four-tai convention before continuation. Project interpretation: cap the hand subtotal, exclude dealer/continuation transfers. |
| Kong-only replacement | Consequence of no flowers. |

This is a documented composition, not a claim that every southern table shares
P's remaining values or exclusions. S does not supply a complete southern
scoring table. Its dealer-tai omission and continuation description belong to
the separate match rules design. `rawTai` and `tai` expose the cap transparently.

Custom profiles can change values, exclusions, flower use/set treatment, pinfu,
replacement origin and cap. Values and sources must cover every pattern. Callers
should attach their own source/house-rule clause when changing a value. There
are no speculative toggles for every incidental implementation condition.
