package com.griffin.solhome;
import java.util.*;
public final class HomeNavigationTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        int id=0,scroll=0;int[] route={2,4,11,6,8,10,13,15};
        for(int next:route){id=HomeNavigation.move(id,HomeNavigation.DOWN,10);check(id==next,"down reaches every tile group then footer");scroll=HomeNavigation.reveal(id,scroll);}
        check(scroll==450,"final utility row is revealed");
        check(HomeNavigation.move(9,HomeNavigation.DOWN,9)==10,"last wide tile reached from right column");
        check(HomeNavigation.move(15,HomeNavigation.RIGHT,10)==16&&HomeNavigation.move(16,HomeNavigation.RIGHT,10)==17,"both coding cards and weather reachable");
        check(HomeNavigation.move(15,HomeNavigation.LEFT,9)==9,"left returns to prior grid focus");
        check(HomeNavigation.reveal(0,450)==0,"Back/app focus can reveal first row");
        check(HomeNavigation.hit(200,300,0)==0&&HomeNavigation.hit(200,300,450)==11,"touch resolves scrolled tile");
        check(HomeNavigation.hit(450,800,450)==10,"wide Watching tile accepts both columns");
        check(HomeNavigation.hit(320,310,0)==-1&&HomeNavigation.hit(110,260,0)==-1,"gutters and header do not launch apps");
        Set<Integer> reachable=new HashSet<>();Queue<Integer> queue=new ArrayDeque<>();queue.add(0);
        while(!queue.isEmpty()){int current=queue.remove();if(!reachable.add(current))continue;for(int dir=0;dir<4;dir++)queue.add(HomeNavigation.move(current,dir,10));}
        for(int tile:HomeNavigation.GRID)check(reachable.contains(tile),"every tile reachable");for(int control=13;control<=17;control++)check(reachable.contains(control),"every footer/reading control reachable");
        System.out.println("Home grid, footer focus, scroll reveal and touch mapping checks passed.");
    }
}
