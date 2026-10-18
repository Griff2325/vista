package com.griffin.solhome;

/** Sparse sightings use shared monotonic time, so Home/Dream transitions keep their place. */
final class WorldMotion {
    static double phase(double seconds,double period,double offset){return ((seconds+offset)%period+period)%period;}
    static float fade(double age,double duration){return (float)Math.max(0,Math.min(1,Math.min(age,duration-age)/1.8));}
    static boolean calm(float rain,float snow,float wind){return rain<.18f&&snow<.16f&&wind<.75f;}
    static float foxX(double age){return 113+(float)(Math.min(12,Math.max(0,age))/12*23+Math.max(0,age-21)/13*24);}
}
