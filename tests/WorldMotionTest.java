package com.griffin.solhome;
public final class WorldMotionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(WorldMotion.fade(0,34)==0&&WorldMotion.fade(34,34)==0,"sightings fade at both boundaries");
        check(WorldMotion.fade(-1,34)==0&&WorldMotion.fade(35,34)==0,"inactive sightings invisible");
        check(WorldMotion.foxX(12)==WorldMotion.foxX(21),"fox pauses on bank");
        check(Math.abs(WorldMotion.foxX(12.001)-WorldMotion.foxX(11.999))<.01,"arrival continuity");
        check(Math.abs(WorldMotion.foxX(21.001)-WorldMotion.foxX(20.999))<.01,"departure continuity");
        check(WorldMotion.foxX(0)>=113&&WorldMotion.foxX(34)<=160,"dry near-bank bounds");
        check(!WorldMotion.calm(.3f,0,0)&&!WorldMotion.calm(0,.3f,0)&&!WorldMotion.calm(0,0,.9f),"weather suppresses sightings");
        check(WorldMotion.phase(1000000000,229,13)>=0&&WorldMotion.phase(1000000000,229,13)<229,"long uptime bound");
        System.out.println("WorldMotionTest passed");
    }
}
