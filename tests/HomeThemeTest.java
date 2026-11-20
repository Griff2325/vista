package com.griffin.solhome;

import java.util.HashSet;
import java.util.Set;

public final class HomeThemeTest {
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(HomeTheme.saved(null, false) == HomeTheme.CLASSIC, "Blank or legacy Classic install starts in Classic");
        check(HomeTheme.saved(null, true) == HomeTheme.PARK_RAT, "Existing Park Rat selection survives an upgrade");
        check(HomeTheme.saved("unknown-theme", true) == HomeTheme.CLASSIC, "Unknown theme falls back safely");
        Set<String> ids = new HashSet<>();
        for (HomeTheme theme : HomeTheme.values()) {
            check(ids.add(theme.id), "Theme IDs are unique");
            check(HomeTheme.saved(theme.id, false) == theme && HomeTheme.saved(theme.id, true) == theme,
                "Saved theme survives a relaunch regardless of the legacy preference");
        }

        FirstChair rotation = new FirstChair(false);
        check(rotation.rotates(true) && !rotation.rotates(false), "Rotation only runs on visible Home");
        rotation.focusEntered();
        check(!rotation.rotates(true) && !new FirstChair(true).rotates(true), "Focus and disabled motion pause rotation");
        String[] resorts = {"Perfect North", "Mad River Mountain", "Paoli Peaks", "Breckenridge", "Park City", "Stowe", "Mount Snow"};
        Set<String> points = new HashSet<>();
        for (String resort : resorts) {
            FirstChair.Resort point = rotation.resort();
            check(point.name.equals(resort) && FirstChair.validPoint(point.lat, point.lon), "Forecast points match the requested order");
            check(points.add(point.lat + "," + point.lon), "Each resort has its own forecast point");
            rotation.next();
        }
        check(rotation.resort().name.equals(resorts[0]) && rotation.paused, "Manual Next wraps without resuming");
        check(!FirstChair.validPoint(Double.NaN, -80) && !FirstChair.validPoint(42, 80), "Reject malformed points");
        long hour = 3600000, now = 300 * hour;
        FirstChair.Outlook fresh = new FirstChair.Outlook("Stowe", 0.0, 72, now - hour, now + 71 * hour, now);
        FirstChair.Outlook saved = new FirstChair.Outlook("Stowe", 2.6, 24, now - 3 * hour, now + 21 * hour, now);
        check(fresh.amount.equals("~0.0″") && fresh.horizon.equals("next 72 h"), "An actual forecast can report zero snow");
        check(saved.horizon.equals("saved 24 h"), "Old forecast is visibly marked saved");
        check(new FirstChair.Outlook("Stowe", 0, 24, now - 24 * hour, now, now).horizon.isEmpty(), "Expired windows are unavailable");
        check(new FirstChair.Outlook("Stowe", Double.NaN, 0, 0, 0, now).amount.equals("Snow forecast unavailable"),
            "Missing forecast does not show an amount");

        RecentSpotifyTracks music = new RecentSpotifyTracks();
        String track = "spotify:track:0123456789abcdefghijAB";
        music.observe(true, false, "Paused", "", "", "");
        music.observe(true, true, "", "", "", "");
        check(music.size() == 0, "Missing metadata and paused-only observations do not create records");
        music.observe(true, true, "Song A", "Artist", "https://evil.example/track", track);
        music.observe(true, true, "Song B", "Artist", "spotify:playlist:0123456789abcdefghijAB", "");
        check(music.get(0).uri == null && music.get(1).uri.equals(track), "Only verified Spotify track links are retained");
        music.observe(true, true, "Song A", "Artist", "", "");
        check(music.size() == 2 && music.get(0).uri.equals(track), "Repeat moves to the most recent slot and retains its known link");
        music.observe(true, true, "Song C", "", "", "");
        music.observe(true, true, "Song D", "", "", "");
        check(music.size() == 3 && music.get(0).title.equals("Song D") && music.get(2).title.equals("Song A"), "History is bounded and ordered");
        music.observe(false, true, "Song E", "", "", track);
        check(music.size() == 0 && !music.access, "Revoked access clears recent tracks");
        System.out.println("Theme preference, First Chair forecast and Porchlight track policy checks passed.");
    }
}
