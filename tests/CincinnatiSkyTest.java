package com.griffin.solhome;

import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;

public final class CincinnatiSkyTest {
    private static CincinnatiSky.State sky(String time) {
        return CincinnatiSky.at(Instant.parse(time).toEpochMilli());
    }
    private static void require(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }
    public static void main(String[] arguments) {
        CincinnatiSky.State noon = sky("2026-10-17T17:00:00Z");
        require(noon.elevation > 38 && noon.elevation < 45, "Cincinnati autumn solar noon elevation");
        require(noon.daylight > .99 && noon.phase.equals("Day"), "Daylight during local afternoon");
        require(sky("2026-10-18T04:00:00Z").daylight == 0, "Dark at Cincinnati midnight");
        require(sky("2026-06-21T17:00:00Z").elevation > noon.elevation+20, "Summer sun higher than autumn");
        require(sky("2026-12-21T17:00:00Z").elevation < noon.elevation-10, "Winter sun lower than autumn");
        require(sky("2026-10-17T11:00:00Z").daylight < .15, "Before autumn sunrise");
        require(sky("2026-10-17T13:00:00Z").daylight > .9, "After autumn sunrise");
        require(sky("2026-10-17T22:00:00Z").daylight > .6, "Before autumn sunset");
        require(sky("2026-10-18T00:00:00Z").daylight == 0, "After autumn dusk");
        require(sky("2026-11-01T03:30:00Z").halloween, "Still October in Cincinnati before local midnight");
        require(!sky("2026-11-01T04:30:00Z").halloween, "Halloween off after Cincinnati November begins");
        long boundary = Instant.parse("2026-11-01T06:00:00Z").toEpochMilli();
        double before = CincinnatiSky.at(boundary-60000).elevation;
        double after = CincinnatiSky.at(boundary+60000).elevation;
        require(Math.abs(after-before) < .6, "Solar light continuous through DST fallback");
        TimeZone saved = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            require(Math.abs(sky("2026-10-17T17:00:00Z").elevation-noon.elevation) < .0001,
                    "Solar calculation independent of device timezone");
        } finally { TimeZone.setDefault(saved); }
        System.out.println("Cincinnati solar lighting, seasons, timezone and DST checks passed.");
    }
}
