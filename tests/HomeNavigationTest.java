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
        for (HomeTheme theme : HomeTheme.values()) {
            if (theme == HomeTheme.CLASSIC) continue;
            verifyLayout(HomeThemeLayout.forTheme(theme), theme);
        }
        verifyLayout(HomeThemeLayout.porchlight(2), HomeTheme.PORCHLIGHT);
        verifyLayout(HomeThemeLayout.porchlight(3), HomeTheme.PORCHLIGHT);
        System.out.println("Classic and all five Home themes passed focus, touch and action reachability checks.");
    }

    private static void parkRat(){
        HomeThemeLayout layout = HomeThemeLayout.forTheme(HomeTheme.PARK_RAT);
        check(layout.hit(960,500) == -1 && layout.hit(0,0) == -1, "Rider artwork and safe margin do not open a tile");
        check(layout.move(0,HomeNavigation.RIGHT,0) == 2 && layout.move(2,HomeNavigation.LEFT,0) == 0,
            "Right and Left cross the artwork between YouTube and Hulu");
        check(layout.move(0,HomeNavigation.UP,0) == 0 && layout.move(0,HomeNavigation.DOWN,0) == 1,
            "Up stays at the top; Down goes to Netflix");
        check(layout.move(11,HomeNavigation.RIGHT,0) == HomeThemeLayout.MORE
            && layout.move(HomeThemeLayout.MORE,HomeNavigation.LEFT,0) == 11, "More is directly reachable from Explore");
    }

    private static void verifyLayout(HomeThemeLayout layout, HomeTheme theme){
        Set<Integer> main = new HashSet<>();
        for (HomeThemeLayout.Tile tile : layout.tiles) {
            check(main.add(tile.id), theme.label + " tile IDs are unique");
            check(layout.hit(tile.centerX(),tile.centerY()) == tile.id, "Touch hits the drawn tile");
            check(tile.left >= 86 && tile.right <= 1844 && tile.top >= 54 && tile.bottom <= 1037,
                theme.label + " tiles leave space for focus borders inside TV safe margins");
            for (HomeThemeLayout.Tile other : layout.tiles) {
                if (tile == other) continue;
                check(tile.right <= other.left || tile.left >= other.right || tile.bottom <= other.top || tile.top >= other.bottom,
                    theme.label + " tile touch targets do not overlap");
            }
        }
        for (int start : main) {
            Set<Integer> reachable = new HashSet<>();
            Queue<Integer> remaining = new ArrayDeque<>();
            remaining.add(start);
            while (!remaining.isEmpty()) {
                int id = remaining.remove();
                if (!reachable.add(id)) continue;
                for (int direction = 0; direction < 4; direction++) remaining.add(layout.move(id,direction,0));
            }
            check(reachable.equals(main), theme.label + " tile " + start + " cannot reach " + main.stream().filter(id -> !reachable.contains(id)).collect(java.util.stream.Collectors.toList()));
        }

        Set<Integer> actions = new HashSet<>(main);
        check(actions.remove(HomeThemeLayout.MORE), theme.label + " has More");
        actions.removeIf(id -> HomeThemeLayout.snowControl(id) || id >= HomeThemeLayout.FIRST_TRACK);
        for (int item : layout.moreActions) check(actions.add(item), "More offers unique actions beyond the main tiles");
        if (theme == HomeTheme.FIRST_CHAIR) {
            check(!main.contains(13) && !main.contains(14), "First Chair uses its own information strip");
            check(layout.move(11,HomeNavigation.UP,11) == HomeThemeLayout.SNOW_TOGGLE
                && layout.move(HomeThemeLayout.SNOW_TOGGLE,HomeNavigation.RIGHT,11) == HomeThemeLayout.SNOW_NEXT
                && layout.move(HomeThemeLayout.SNOW_NEXT,HomeNavigation.DOWN,11) == 11, "Snow controls return to the last dock tile");
        }
        Set<Integer> classicActions = new HashSet<>();
        for (int item : HomeNavigation.GRID) classicActions.add(item);
        classicActions.addAll(Arrays.asList(13,14,15,17)); // Both Classic usage cards open the same detail panel.
        check(actions.equals(classicActions), theme.label + " preserves every distinct Classic Home action");
    }
}
