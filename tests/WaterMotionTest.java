package com.griffin.solhome;

public final class WaterMotionTest {
    private static double distance(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a-b), Math.cos(a-b)));
    }
    public static void main(String[] args) {
        WaterMotion fresh = new WaterMotion(), old = new WaterMotion();
        long oldBoot = 23L*86400000;
        fresh.advance(0, 0); old.advance(oldBoot, 0);
        for (int frame=1; frame<=12000; frame++) {
            // A weather update must not multiply accumulated uptime into wave speed.
            float wind = frame<100 ? 0 : frame<8000 ? 1 : .2f;
            double before = old.phase();
            fresh.advance(frame*33L, wind); old.advance(oldBoot+frame*33L, wind);
            if (distance(fresh.phase(),old.phase())>1e-10)
                throw new AssertionError("Wave motion depends on device uptime");
            if (distance(before,old.phase())>.014)
                throw new AssertionError("Wind update or phase wrap jumped");
            for (int system=0;system<6;system++) for (int ring=0;ring<2;ring++) {
                float p=old.rippleProgress(system,ring);
                if(p<0 || p>=1) throw new AssertionError("Ripple outside its lifetime");
            }
        }
        double before = old.phase();
        old.advance(oldBoot+12000*33L+12*3600000L, 1);
        if (distance(before,old.phase())>.04)
            throw new AssertionError("Hidden interval fast-forwarded the lake");
        if (WaterMotion.rippleOpacity(0)>1e-6 || WaterMotion.rippleOpacity(1)>1e-6
                || WaterMotion.rippleOpacity(.5f)<.999f)
            throw new AssertionError("Ripple reset must be invisible, with a gradual fade");
        System.out.println("Ripples stay continuous across wind updates, long uptime and resume; resets fade out.");
    }
}
