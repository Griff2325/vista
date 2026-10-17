package com.griffin.solhome;
public final class GhostMotionTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){GhostMotion a=new GhostMotion(0),b=new GhostMotion(1),c=new GhostMotion(2);int hidden=0,visible=0,different=0;
        for(int i=0;i<18000;i++){double t=i*.1;a.update(t);b.update(t);c.update(t);check(a.x>=110&&a.x<=395&&a.y>=110&&a.y<=246,"Bounded roaming");check(a.opacity>=0&&a.opacity<=1,"Opacity range");if(a.opacity==0)hidden++;if(a.opacity==1)visible++;if(Math.abs(a.x-b.x)>10&&Math.abs(b.y-c.y)>10)different++;
            float x=a.x,y=a.y,opacity=a.opacity;a.update(t+.001);check(Math.abs(x-a.x)<.05&&Math.abs(y-a.y)<.05&&Math.abs(opacity-a.opacity)<.01,"No movement or opacity jumps across waypoints/cycles");}
        check(hidden>100&&visible>100&&different>100,"Independent paths and visible/hidden intervals");
        System.out.println("Ghost bounds, independent routes, fades and waypoint continuity checks passed.");
    }
}
