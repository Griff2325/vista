package com.griffin.solhome;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/** Offline solar elevation for scene lighting; UTC math avoids DST discontinuities. */
public final class CincinnatiSky {
    public static final double LATITUDE = 39.1031;
    public static final double LONGITUDE = -84.5120;
    public static final String ZONE_ID = "America/New_York";

    private CincinnatiSky() {}

    public static final class State {
        public final double elevation;
        public final double hourAngle;
        public final float daylight;
        public final float twilight;
        public final boolean halloween;
        public final String phase;

        private State(double elevation, double hourAngle, boolean halloween) {
            this.elevation = elevation;
            this.hourAngle = hourAngle;
            this.daylight = smooth(-7, 8, elevation);
            this.twilight = (float) Math.max(0, 1 - Math.abs(elevation - 1) / 11);
            this.halloween = halloween;
            this.phase = elevation < -6 ? "Night" : elevation < 6
                    ? (hourAngle < 0 ? "Dawn" : "Dusk") : "Day";
        }
    }

    /** NOAA's approximate fractional-year solar-position equations.
     * Reference: https://gml.noaa.gov/grad/solcalc/solareqns.PDF
     */
    public static State at(long epochMillis) {
        Calendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(epochMillis);
        double hour = utc.get(Calendar.HOUR_OF_DAY) + utc.get(Calendar.MINUTE) / 60.0
                + utc.get(Calendar.SECOND) / 3600.0;
        double gamma = 2 * Math.PI / utc.getActualMaximum(Calendar.DAY_OF_YEAR)
                * (utc.get(Calendar.DAY_OF_YEAR) - 1 + (hour - 12) / 24);
        double equation = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma)
                - 0.032077 * Math.sin(gamma) - 0.014615 * Math.cos(2 * gamma)
                - 0.040849 * Math.sin(2 * gamma));
        double declination = 0.006918 - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma) - 0.006758 * Math.cos(2 * gamma)
                + 0.000907 * Math.sin(2 * gamma) - 0.002697 * Math.cos(3 * gamma)
                + 0.00148 * Math.sin(3 * gamma);
        double solarMinutes = ((hour * 60 + equation + 4 * LONGITUDE) % 1440 + 1440) % 1440;
        double hourAngle = solarMinutes / 4 - 180;
        double latitude = Math.toRadians(LATITUDE);
        double cosineZenith = Math.sin(latitude) * Math.sin(declination)
                + Math.cos(latitude) * Math.cos(declination) * Math.cos(Math.toRadians(hourAngle));
        double elevation = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, cosineZenith))));
        Calendar local = new GregorianCalendar(TimeZone.getTimeZone(ZONE_ID));
        local.setTimeInMillis(epochMillis);
        return new State(elevation, hourAngle, local.get(Calendar.MONTH) == Calendar.OCTOBER);
    }

    private static float smooth(double low, double high, double value) {
        double t = Math.max(0, Math.min(1, (value - low) / (high - low)));
        return (float) (t * t * (3 - 2 * t));
    }
}
