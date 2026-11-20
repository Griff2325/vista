package com.griffin.solhome;
import java.util.*;
public final class HomeNavigationTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        int id=0,scroll=0;int[] route={2,4,18,12,7,9,19,13,15};
        for(int next:route){id=HomeNavigation.move(id,HomeNavigation.DOWN,10);check(id==next,"down reaches every tile group then footer");scroll=HomeNavigation.reveal(id,scroll);}
        check(scroll==600,"final utility row is revealed");
        check(HomeNavigation.move(9,HomeNavigation.RIGHT,9)==10,"Watching shares the last row with Settings");
        check(HomeNavigation.move(13,HomeNavigation.UP,9)==19&&HomeNavigation.move(14,HomeNavigation.UP,10)==10,"shortcuts return to the matching last-row column");
        check(HomeNavigation.favoriteIndex(18)==6&&HomeNavigation.favoriteIndex(2)==2,"Prime and Hulu map to their favorite entries");
        check(HomeNavigation.favoriteIndex(6)==-1&&HomeNavigation.favoriteIndex(17)==-1,"controls are not app favorites");
        check(HomeNavigation.move(15,HomeNavigation.RIGHT,10)==16&&HomeNavigation.move(16,HomeNavigation.RIGHT,10)==17,"both coding cards and weather reachable");
        check(HomeNavigation.move(15,HomeNavigation.LEFT,9)==9,"left returns to prior grid focus");
        check(HomeNavigation.reveal(0,450)==0,"Back/app focus can reveal first row");
        check(HomeNavigation.hit(200,300,0)==0&&HomeNavigation.hit(200,300,450)==18,"touch resolves Prime after scrolling");
        check(HomeNavigation.hit(200,800,450)==9&&HomeNavigation.hit(450,800,450)==10,"last-row touch keeps Settings and Watching separate");
        check(HomeNavigation.hit(320,310,0)==-1&&HomeNavigation.hit(110,260,0)==-1,"gutters and header do not launch apps");
        check(HomeNavigation.move(9,HomeNavigation.DOWN,9)==19&&HomeNavigation.move(10,HomeNavigation.DOWN,10)==19,"both columns reach the final native Home tile");
        check(HomeNavigation.move(19,HomeNavigation.UP,10)==9&&HomeNavigation.move(19,HomeNavigation.DOWN,10)==13,"native Home shortcut exits to grid/footer");
        check(HomeNavigation.hit(200,800,600)==19&&HomeNavigation.hit(450,800,600)==-1&&HomeNavigation.reveal(19,0)==600&&HomeNavigation.shortcutHit(550,910)==-1&&HomeNavigation.shortcutHit(600,930)==-1,"shortcut touch respects visible bounds and gutters");
        check(HomeNavigation.favoriteIndex(19)==-1,"native Home is not an app favorite");
        Set<Integer> reachable=new HashSet<>();Queue<Integer> queue=new ArrayDeque<>();queue.add(0);
        while(!queue.isEmpty()){int current=queue.remove();if(!reachable.add(current))continue;for(int dir=0;dir<4;dir++)queue.add(HomeNavigation.move(current,dir,10));}
        check(reachable.contains(19),"Fire TV Home reachable");for(int tile:HomeNavigation.GRID)check(reachable.contains(tile),"every tile reachable");for(int control=13;control<=17;control++)check(reachable.contains(control),"every footer/reading control reachable");
        parkRat();
        System.out.println("Classic and Park Rat Home focus, touch and action reachability checks passed.");
    }

    private static void parkRat(){
        Set<Integer> main = new HashSet<>();
        for (ParkRatLayout.Tile tile : ParkRatLayout.TILES) {
            check(main.add(tile.id), "Park Rat tile IDs are unique");
            check(ParkRatLayout.hit(tile.centerX(),tile.centerY()) == tile.id, "Touch hits the drawn tile");
            check(tile.left >= 86 && tile.right <= 1834 && tile.top >= 54 && tile.bottom <= 1026,
                "Park Rat tiles leave space for focus borders inside TV safe margins");
            for (ParkRatLayout.Tile other : ParkRatLayout.TILES) {
                if (tile == other) continue;
                check(tile.right <= other.left || tile.left >= other.right || tile.bottom <= other.top || tile.top >= other.bottom,
                    "Park Rat tile touch targets do not overlap");
            }
        }
        check(ParkRatLayout.hit(960,500) == -1 && ParkRatLayout.hit(0,0) == -1, "Rider artwork and safe margin do not open a tile");
        check(ParkRatLayout.move(0,HomeNavigation.RIGHT) == 2 && ParkRatLayout.move(2,HomeNavigation.LEFT) == 0,
            "Right and Left cross the artwork between YouTube and Hulu");
        check(ParkRatLayout.move(0,HomeNavigation.UP) == 0 && ParkRatLayout.move(0,HomeNavigation.DOWN) == 1,
            "Up stays at the top; Down goes to Netflix");
        check(ParkRatLayout.move(11,HomeNavigation.RIGHT) == ParkRatLayout.MORE
            && ParkRatLayout.move(ParkRatLayout.MORE,HomeNavigation.LEFT) == 11, "More is directly reachable from Explore");
        for (int start : main) {
            Set<Integer> reachable = new HashSet<>();
            Queue<Integer> remaining = new ArrayDeque<>();
            remaining.add(start);
            while (!remaining.isEmpty()) {
                int id = remaining.remove();
                if (!reachable.add(id)) continue;
                for (int direction = 0; direction < 4; direction++) remaining.add(ParkRatLayout.move(id,direction));
            }
            check(reachable.equals(main), "Every Park Rat tile can reach every other tile with the D-pad");
        }

        Set<Integer> actions = new HashSet<>(main);
        actions.remove(ParkRatLayout.MORE);
        for (int item : ParkRatLayout.MORE_ACTIONS) check(actions.add(item), "More offers unique actions beyond the main tiles");
        Set<Integer> classicActions = new HashSet<>();
        for (int item : HomeNavigation.GRID) classicActions.add(item);
        classicActions.addAll(Arrays.asList(13,14,15,17)); // Both Classic usage cards open the same detail panel.
        check(actions.equals(classicActions), "Park Rat preserves every distinct Classic Home action");
    }
}
