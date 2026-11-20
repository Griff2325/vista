package com.griffin.solhome;

/** One set of 1920×1080 coordinates for drawing, D-pad navigation and touch. */
final class HomeThemeLayout {
    static final int WIDTH = 1920, HEIGHT = 1080;
    static final int MORE = 20;
    static final int SNOW_TOGGLE = 21, SNOW_NEXT = 22, FIRST_TRACK = 23;
    private static final int[] MORE_ORDER = {9, 6, 7, 8, 10, 12, HomeNavigation.FIRE_TV_HOME, 15, 17, 13, 14};
    private static final HomeThemeLayout PARK_RAT = new HomeThemeLayout(
        new Tile(0, 5, 22, 28, 23),
        new Tile(1, 7, 48, 20, 15),
        new Tile(3, 5, 66, 25, 17),
        new Tile(2, 79, 23, 16, 16),
        new Tile(HomeNavigation.PRIME_VIDEO, 81, 41, 14, 17),
        new Tile(5, 77, 61, 18, 17),
        new Tile(4, 33, 76, 23, 17),
        new Tile(11, 58, 82, 17, 13),
        new Tile(13, 5, 86, 13, 9),
        new Tile(14, 19, 86, 11, 9),
        new Tile(MORE, 79, 86, 16, 9)
    );
    private static final HomeThemeLayout FIRST_CHAIR = new HomeThemeLayout(
        new Tile(0, 5, 73, 11, 17),
        new Tile(1, 16.3f, 73, 11, 17),
        new Tile(2, 27.6f, 73, 11, 17),
        new Tile(3, 38.9f, 73, 11, 17),
        new Tile(4, 50.2f, 73, 11, 17),
        new Tile(5, 61.5f, 73, 11, 17),
        new Tile(HomeNavigation.PRIME_VIDEO, 72.8f, 73, 11, 17),
        new Tile(11, 84.1f, 73, 11, 17),
        new Tile(SNOW_TOGGLE, 42, 62.3f, 3.15f, 5.5f),
        new Tile(SNOW_NEXT, 45.8f, 62.3f, 3.15f, 5.5f),
        new Tile(MORE, 84.1f, 91, 11, 5)
    );
    private static final HomeThemeLayout PORCHLIGHT = new HomeThemeLayout(
        new Tile(4, 5, 39, 17, 23),
        new Tile(0, 83, 17, 12, 9),
        new Tile(1, 84, 27, 11, 9),
        new Tile(2, 83, 37, 12, 9),
        new Tile(3, 82, 47, 13, 9),
        new Tile(5, 84, 57, 11, 9),
        new Tile(HomeNavigation.PRIME_VIDEO, 83, 67, 12, 9),
        new Tile(11, 82, 77, 13, 9),
        new Tile(13, 5, 67, 17, 9),
        new Tile(14, 5, 78, 17, 9),
        new Tile(MORE, 5, 89, 17, 6)
    );
    static final Tile[] SLEEVES = {
        new Tile(FIRST_TRACK, 24.8f, 38, 16.7f, 38),
        new Tile(FIRST_TRACK + 1, 43.6f, 38, 16.7f, 38),
        new Tile(FIRST_TRACK + 2, 62.4f, 38, 16.7f, 38)
    };
    private static final HomeThemeLayout RIVER_CITY = new HomeThemeLayout(
        new Tile(13, 42, 5.5f, 11, 8),
        new Tile(14, 54, 5.5f, 8, 8),
        new Tile(MORE, 63, 5.5f, 7.5f, 8),
        new Tile(0, 5, 69, 20, 15),
        new Tile(1, 26, 75, 14, 13),
        new Tile(4, 5, 86, 12, 9),
        new Tile(11, 18, 86, 7, 9),
        new Tile(3, 63, 82, 12, 12),
        new Tile(2, 76, 75, 9, 17),
        new Tile(5, 86, 67, 9, 12),
        new Tile(HomeNavigation.PRIME_VIDEO, 86, 81, 9, 13)
    );
    private static final HomeThemeLayout GTA_VI = new HomeThemeLayout(
        new Tile(MORE, 85, 14, 11, 6),
        new Tile(13, 70, 22, 14, 9),
        new Tile(14, 85, 22, 11, 9),
        new Tile(0, 68, 34, 28, 15),
        new Tile(1, 68, 51, 16, 11),
        new Tile(2, 85, 51, 11, 11),
        new Tile(3, 68, 64, 13, 12),
        new Tile(4, 82, 64, 14, 12),
        new Tile(5, 68, 78, 9, 12),
        new Tile(HomeNavigation.PRIME_VIDEO, 78, 78, 10, 12),
        new Tile(11, 89, 78, 7, 12)
    );

    final Tile[] tiles;
    final int[] moreActions;

    private HomeThemeLayout(Tile... tiles) {
        this.tiles = tiles;
        moreActions = java.util.Arrays.stream(MORE_ORDER).filter(id -> !contains(id)).toArray();
    }

    static HomeThemeLayout forTheme(HomeTheme theme) {
        switch (theme) {
            case FIRST_CHAIR: return FIRST_CHAIR;
            case PORCHLIGHT: return porchlight(0);
            case RIVER_CITY: return RIVER_CITY;
            case PARK_RAT: return PARK_RAT;
            case GTA_VI: return GTA_VI;
            default: throw new IllegalArgumentException(theme.id);
        }
    }

    static HomeThemeLayout porchlight(int trackCount) {
        int sleeves = Math.max(1, trackCount);
        Tile[] plates = java.util.Arrays.copyOf(PORCHLIGHT.tiles, PORCHLIGHT.tiles.length + sleeves);
        System.arraycopy(SLEEVES, 0, plates, PORCHLIGHT.tiles.length, sleeves);
        return new HomeThemeLayout(plates);
    }

    static boolean snowControl(int id) { return id == SNOW_TOGGLE || id == SNOW_NEXT; }

    static final class Tile {
        final int id;
        final float left, top, right, bottom;

        Tile(int id, float x, float y, float width, float height) {
            this.id = id;
            left = x * WIDTH / 100f;
            top = y * HEIGHT / 100f;
            right = (x + width) * WIDTH / 100f;
            bottom = (y + height) * HEIGHT / 100f;
        }

        float centerX() { return (left + right) / 2; }
        float centerY() { return (top + bottom) / 2; }
    }

    boolean contains(int id) {
        return find(id) != null;
    }

    private Tile find(int id) {
        for (Tile tile : tiles) if (tile.id == id) return tile;
        return null;
    }

    int hit(float x, float y) {
        for (Tile tile : tiles) {
            if (x >= tile.left && x < tile.right && y >= tile.top && y < tile.bottom) return tile.id;
        }
        return -1;
    }

    int move(int id, int direction, int lastDockTile) {
        if (this == FIRST_CHAIR) {
            if (snowControl(id) && direction == HomeNavigation.DOWN) return lastDockTile;
            if (!snowControl(id) && id != MORE && direction == HomeNavigation.UP) return SNOW_TOGGLE;
        }
        Tile current = find(id);
        if (current == null) return 0;
        boolean vertical = direction == HomeNavigation.UP || direction == HomeNavigation.DOWN;
        boolean backward = direction == HomeNavigation.UP || direction == HomeNavigation.LEFT;
        int target = id;
        float best = Float.POSITIVE_INFINITY;
        boolean bestAligned = false;
        for (Tile tile : tiles) {
            float dx = tile.centerX() - current.centerX();
            float dy = tile.centerY() - current.centerY();
            float advance = vertical ? dy : dx;
            float edgeDelta = edge(tile, vertical, backward) - edge(current, vertical, backward);
            if (backward ? advance >= 0 || edgeDelta >= 0 : advance <= 0 || edgeDelta <= 0) continue;
            boolean aligned = vertical ? tile.left < current.right && tile.right > current.left
                : tile.top < current.bottom && tile.bottom > current.top;
            // A tile intersecting the direction of travel comes before diagonal neighbors.
            if (!aligned && bestAligned) continue;
            float score = Math.abs(advance) + 2 * Math.abs(vertical ? dx : dy);
            if ((aligned && !bestAligned) || score < best) {
                target = tile.id;
                best = score;
                bestAligned = aligned;
            }
        }
        return target;
    }

    private static float edge(Tile tile, boolean vertical, boolean backward) {
        if (vertical) return backward ? tile.top : tile.bottom;
        return backward ? tile.left : tile.right;
    }
}
