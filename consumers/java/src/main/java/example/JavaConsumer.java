package example;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import top.skyeyefast.taiwan.*;

public final class JavaConsumer {
    public static void main(String[] args) {
        var tiles = List.of(0, 1, 2, 3, 4, 5, 9, 10, 11, 18, 19, 20, 24, 25, 26, 33);
        var ready = new Hand(tiles, List.of());
        var complete = new Hand(java.util.stream.Stream.concat(tiles.stream(), java.util.stream.Stream.of(33)).toList(), List.of());
        require(ready.getSize() == 16 && TaiwanMahjong.shanten(ready) == 0);
        require(TaiwanMahjong.waitingTiles(ready).contains(33));
        require(TaiwanMahjong.analyze(ready).getRemaining() > 0);
        require(!TaiwanMahjong.discards(complete).isEmpty());
        require(!TaiwanMahjong.winningShapes(complete).isEmpty());
        var context = new WinContext(WinMethod.SELF_DRAW, 27, 27, 1, Set.of(), DrawOrigin.ORDINARY, false, OpeningWin.NONE, ReadyDeclaration.NONE);
        var pocket = TaiwanScoring.score(ready, 33, context, TaiwanProfiles.POCKET_COMMON);
        var southern = TaiwanScoring.score(ready, 33, context, TaiwanProfiles.SOUTHERN_COMMON);
        require(pocket != null && pocket.getTai() > 0 && !pocket.getAwards().isEmpty());
        require(southern != null && southern.getTai() <= 4 && TaiwanProfiles.SOUTHERN_COMMON.getFlowers() == FlowerPolicy.NONE);
        var flower = TaiwanScoring.flowerWin(EnumSet.allOf(Flower.class), FlowerEvent.EIGHT_AFTER_INITIAL_REPLACEMENT, TaiwanProfiles.POCKET_COMMON);
        require(flower != null && flower.getTai() == 8 && flower.getHandScore() == null);
        // Access Kotlin-generated public enum API: stdlib must arrive transitively from the POM.
        require(Pattern.getEntries().size() > 0);
        System.out.println("Java consumer PASS (JVM " + Runtime.version().feature() + ")");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
