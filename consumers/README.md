# Published-library consumers

Run from the library root:

```sh
./gradlew build publishToMavenLocal verifyPublication --warning-mode fail
./gradlew -p consumers clean verify --warning-mode fail
```

These independent builds have no project/composite dependency. Java resolves the Maven POM; Kotlin resolves Gradle module metadata. Both consume `top.skyeyefast:taiwan-mahjong:0.1.0` from Maven Local and execute Hand, shanten, waitingTiles, analyze, discards, ordinary scoring, flowerWin and both profiles. Compile/API target and execution toolchains are Java 17.
