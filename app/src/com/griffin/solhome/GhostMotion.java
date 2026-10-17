package com.griffin.solhome;

/** Independent deterministic wandering paths: no allocations while a frame is drawn. */
public final class GhostMotion {
    private final int seed;
    private final double step,period,offset;
    public float x,y,opacity;
    public GhostMotion(int number){seed=number*7919+137;step=19+number*7;period=48+number*13;offset=number*23.7;}
    private double random(int point,int salt){int n=point*374761393+seed+salt*668265263;n=(n^(n>>>13))*1274126177;return ((n^(n>>>16))&0x7fffffff)/(double)0x7fffffff;}
    public void update(double seconds){
        double travel=(seconds+offset)/step;int point=(int)Math.floor(travel);double t=travel-point,ease=t*t*(3-2*t);
        x=(float)(110+285*(random(point,1)*(1-ease)+random(point+1,1)*ease));
        y=(float)(112+132*(random(point,2)*(1-ease)+random(point+1,2)*ease)+Math.sin(seconds*1.5+seed)*2);
        double phase=(seconds+offset)%period,visible=period*.68;
        opacity=(float)Math.max(0,Math.min(1,Math.min(phase/3,(visible-phase)/3)));
    }
}
