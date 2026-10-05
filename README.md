# taiwan-mahjong

An independent Kotlin/JVM 17 library for Taiwanese sixteen-tile hand structure
and tai scoring. It contains no Minecraft types, match state machine, networking,
persistence, UI, dealer progression or payment execution.

Source: [SkyEye-FAST/taiwan-mahjong](https://github.com/SkyEye-FAST/taiwan-mahjong).
Maven coordinate: `top.skyeyefast:taiwan-mahjong:0.1.0`.
MChjong can pin this repository as a Git submodule for composite source development;
the published library resolves independently from its Maven coordinate.
This repository builds independently:

```sh
git clone https://github.com/SkyEye-FAST/taiwan-mahjong.git
cd taiwan-mahjong
./gradlew build --warning-mode fail
```

Build with JDK 21; production bytecode and JDK API usage target Java 17.
Runtime dependency: Kotlin standard library. `mcr-mahjong` is a test-only oracle
for four-meld remainders, not a scoring source or production dependency.

## Dependency

```kotlin
repositories { mavenCentral() }
dependencies { implementation("top.skyeyefast:taiwan-mahjong:0.1.0") }
```

The public API is the Kotlin-public declarations in `top.skyeyefast.taiwan`,
including JVM static analysis/scoring entry points and the two profile fields.
`internal` count validation, Hand helpers and distance DP are implementation details;
the checked-in `api/` ABI dump records the release boundary. RULES.md is the
0.1.0 semantic baseline. Review intentional API changes before `updateKotlinAbi`.

Sources and generated Dokka HTML ship in standard `sources` and `javadoc` JARs.

## Contracts

* `Hand`: 34 ordinary kinds, four copies maximum including declared kongs.
  `concealed.size + 3 * melds.size` is 16 before a draw, 17 after it.
  Flowers are separate `Flower` identities. A kong is four physical tiles and
  one structural meld; concealed triplets stay in the concealed hand.
* `TaiwanMahjong.winningShapes`: all five-meld-and-pair decompositions of a
  17-tile hand; returned groups exclude declared melds.
* `shanten`: minimum missing ordinary tiles minus one; complete = −1, ready = 0.
  Both 16/17 structural tiles are accepted. Rank dynamic programming enumerates
  complete target shapes, constraining copies left after fixed melds.
* `waitingTiles`: structural winning kinds for a 16-tile hand, excluding fifth
  copies. `analyze`: shanten and improving kinds with remaining known copies.
  Additional visible tiles must be disjoint from owned tiles; public exhaustion
  affects availability, not structural shanten.
* `discards`: one analysis per distinct concealed kind from a 17-tile hand.
  The discarded copy stays visible when counting remaining tiles.
* `TaiwanScoring.score`: before-win hand, separate winning kind, `WinContext`,
  and `TaiwanScoringProfile`; null means no ordinary winning shape. Scoring
  enumerates every decomposition and winning placement, maximizing capped tai,
  then raw tai. A discard completing a triplet does not make that triplet concealed.
* `flowerWin`: eight flowers or seven-versus-one, without requiring an ordinary
  winning shape. Replacement-triggered events require a `ReplacementWin` with
  validated 16+1 tile input. An opponent's eighth flower requires no replacement.
  Explicit initial-replacement events accept no seventeenth tile and return only
  the flower award; the host certifies the nondealer's completed initial replacement.
  The flower award and optional replacement hand score remain separate, because
  their payers can differ. `rawTai` sums raw components and `tai` caps that sum
  once; this is not an allocation between payers. The host certifies the event
  and external flower owner.

Inputs are defensively copied; retained collections and result collections are
unmodifiable. Objects contain immutable values and no global mutable caches.
`WinContext` contains host-certified chronology, not a reconstructed game history.
Structural readiness does not imply permission to win after passing a claim.

## Scoring profiles

See [source ledger and interpretation decisions](RULES.md).
`POCKET_COMMON` describes a documented online game's common rules;
`SOUTHERN_COMMON` is an explicitly composed southern-style preset. Neither is an
official regional standard. Custom profiles provide a complete pattern value
map, source map and acyclic exclusion graph, plus the few documented semantic
policies. Scoring never branches on the profile name.

Supported special wins are flower wins. Seven pairs, thirteen orphans and
eight-and-a-half pairs are not silently imported from other variants. The historical
optional seven-pair-plus-triplet form and conflicting values are recorded in RULES.md;
neither selected profile adopts it.
Opening wins and ready declarations are scoring facts certified by the host.
Dealer/continuation tai and base-plus-tai transfers are outside hand scoring.

## Verification

Tests cover ambiguous five-meld decomposition, all fixed-meld counts, kong/copy
bounds, impossible fifth-copy waits, availability after discard, immutability,
source-backed scoring examples, exclusions, custom policies, southern capping,
and flower-trigger boundaries. Seeded legal five-meld hands cross-check the
independent decomposition and target-distance algorithms. Seeded four-meld
remainders compare with `mcr-mahjong:0.1.0` regular-form analysis.

Local release verification:

```sh
./gradlew build publishToMavenLocal verifyPublication --warning-mode fail
./gradlew -p consumers clean verify --warning-mode fail
```

See [consumer verification](consumers/README.md). `build` also checks the public
ABI, JVM 17 class versions, Apache-2.0 license, sources/docs and POM/module metadata.
The only published dependency is Kotlin stdlib in API/compile and runtime scopes.

## Publishing

Set `CENTRAL_PORTAL_USERNAME`, `CENTRAL_PORTAL_PASSWORD`,
`MAVEN_CENTRAL_SIGNING_KEY` (ASCII-armored OpenPGP private key) and
`MAVEN_CENTRAL_SIGNING_PASSWORD` in protected environment variables, or their
Gradle property equivalents `centralPortalUsername`, `centralPortalPassword`,
`signingKey`, `signingPassword`. Never commit credentials.

After local verification, run:

```sh
./gradlew publishAggregationToCentralPortal --warning-mode fail
```

This signs the Maven publication and uploads a USER_MANAGED deployment; inspect
validation and publish that deployment in Central Portal. The POM carries this
project's developer, SCM and Apache-2.0 license only.
Release notes are in [CHANGELOG.md](CHANGELOG.md).

License: Apache-2.0; see [LICENSE](LICENSE).
