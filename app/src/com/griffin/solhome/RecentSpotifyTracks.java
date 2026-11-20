package com.griffin.solhome;

import java.util.ArrayList;

/** Three tracks observed playing while Porchlight is on screen, kept only in memory. */
final class RecentSpotifyTracks {
    private final ArrayList<Track> tracks = new ArrayList<>(3);
    boolean access;

    void observe(boolean access, boolean playing, String title, String artist, String mediaUri, String mediaId) {
        this.access = access;
        if (!access) {
            tracks.clear();
            return;
        }
        title = label(title);
        if (!playing || title.isEmpty()) return;
        artist = label(artist);
        String uri = SpotifyLinks.track(mediaUri);
        if (uri == null) uri = SpotifyLinks.track(mediaId);
        for (int i = 0; i < tracks.size(); i++) {
            Track old = tracks.get(i);
            boolean same = uri != null && old.uri != null ? uri.equals(old.uri)
                : title.equals(old.title) && artist.equals(old.artist);
            if (!same) continue;
            tracks.remove(i);
            if (uri == null) uri = old.uri;
            break;
        }
        if (tracks.size() == 3) tracks.remove(2);
        tracks.add(0, new Track(title, artist, uri));
    }

    private static String label(String value) {
        String trimmed = value.substring(0, Math.min(value.length(), 200)).replaceAll("[\\p{Cc}\\p{Cf}]", " ").trim();
        return trimmed;
    }

    int size() { return tracks.size(); }
    Track get(int index) { return index >= tracks.size() ? null : tracks.get(index); }

    static final class Track {
        final String title, artist, uri;

        Track(String title, String artist, String uri) {
            this.title = title;
            this.artist = artist;
            this.uri = uri;
        }
    }
}
