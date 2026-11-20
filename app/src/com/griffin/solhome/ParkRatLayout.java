package com.griffin.solhome;

/** One set of 1920×1080 coordinates for drawing, D-pad navigation and touch. */
final class ParkRatLayout {
    static final int WIDTH = 1920, HEIGHT = 1080;
    static final int MORE = 20;
    static final int[] MORE_ACTIONS = {9, 6, 7, 8, 10, 12, HomeNavigation.FIRE_TV_HOME, 15, 17};
    static final Tile[] TILES = {
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
    };

    static final class Tile {
        final int id;
        final float left, top, right, bottom;

        Tile(int id, int x, int y, int width, int height) {
            this.id = id;
            left = x * WIDTH / 100f;
            top = y * HEIGHT / 100f;
            right = (x + width) * WIDTH / 100f;
            bottom = (y + height) * HEIGHT / 100f;
        }

        float centerX() { return (left + right) / 2; }
        float centerY() { return (top + bottom) / 2; }
    }

    static boolean contains(int id) {
        return find(id) != null;
    }

    private static Tile find(int id) {
        for (Tile tile : TILES) if (tile.id == id) return tile;
        return null;
    }

    static int hit(float x, float y) {
        for (Tile tile : TILES) {
            if (x >= tile.left && x < tile.right && y >= tile.top && y < tile.bottom) return tile.id;
        }
        return -1;
    }

    static int move(int id, int direction) {
        Tile current = find(id);
        if (current == null) return 0;
        boolean vertical = direction == HomeNavigation.UP || direction == HomeNavigation.DOWN;
        boolean backward = direction == HomeNavigation.UP || direction == HomeNavigation.LEFT;
        int target = id;
        float best = Float.POSITIVE_INFINITY;
        boolean bestAligned = false;
        for (Tile tile : TILES) {
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
