package com.griffin.solhome;

/** Shared, bounded ripple phase. Wind changes speed without rephasing old uptime. */
final class WaterMotion {
    private long previousAt = -1;
    private double phase;

    void advance(long now, float wind) {
        if (previousAt >= 0) {
            double seconds = Math.max(0, Math.min(.1, (now - previousAt) / 1000.0));
            double breeze = Float.isNaN(wind) ? 0 : Math.max(0, Math.min(1, wind));
            phase = (phase + seconds * (.20 + .18 * breeze)) % (Math.PI * 2);
        }
        previousAt = now;
    }

    double phase() { return phase; }

    float rippleProgress(int system, int ring) {
        double cycle=phase/(Math.PI*2)+system*.271828+ring*.18;
        return (float)(cycle-Math.floor(cycle));
    }

    static float rippleOpacity(float progress) {
        double fade=Math.sin(Math.PI*progress);
        return (float)(fade*fade);
    }
}
