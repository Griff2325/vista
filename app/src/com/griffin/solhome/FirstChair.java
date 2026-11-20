package com.griffin.solhome;

import java.util.Locale;

final class FirstChair {
    static final long ROTATION_MS = 8000;
    private static final long HOUR_MS = 3600000;
    static final Resort[] RESORTS = {
        new Resort("Perfect North", 39.1492, -84.8866),
        new Resort("Mad River Mountain", 40.3166, -83.6764),
        new Resort("Paoli Peaks", 38.5570, -86.5104),
        new Resort("Breckenridge", 39.4739, -106.0739),
        new Resort("Park City", 40.6503, -111.5097),
        new Resort("Stowe", 44.5310, -72.7813),
        new Resort("Mount Snow", 42.9680, -72.8943)
    };
    private int index;
    boolean paused;

    FirstChair(boolean paused) { this.paused = paused; }

    Resort resort() { return RESORTS[index]; }
    void next() { index = (index + 1) % RESORTS.length; }
    boolean rotates(boolean visible) { return visible && !paused; }
    void focusEntered() { paused = true; }

    static boolean validPoint(double lat, double lon) {
        return lat >= 18 && lat <= 72 && lon >= -180 && lon <= -60;
    }

    static final class Resort {
        final String name;
        final double lat, lon;

        Resort(String name, double lat, double lon) {
            if (!validPoint(lat, lon)) throw new IllegalArgumentException(name);
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }

    static final class Outlook {
        final String name, amount, horizon, source;

        Outlook(String name, double inches, int hours, long at, long end, long now) {
            this.name = name;
            boolean available = Double.isFinite(inches) && inches >= 0 && (hours == 24 || hours == 48 || hours == 72)
                && at > 0 && at <= now && end > now;
            amount = available ? String.format(Locale.US, "~%.1f″", inches) : "Snow forecast unavailable";
            horizon = available ? ((now - at > 2 * HOUR_MS ? "saved " : "next ") + hours + " h") : "";
            source = "NWS";
        }

        String description() { return name + " · " + amount + (horizon.isEmpty() ? "" : " · " + horizon) + " · " + source; }
    }
}
