package com.griffin.solhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import java.util.function.IntFunction;

import static com.griffin.solhome.HomeThemeLayout.*;

/** Native artwork and shared tile rendering for the selectable Home themes. */
final class ThemedHome {
    private static final int PAPER = 0xffefe4c9, CREAM = 0xfffff2dc;
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private final Typeface wordmark = Typeface.create("sans-serif-black", Typeface.ITALIC);
    private final Shader ink = new LinearGradient(0, 0, WIDTH, HEIGHT,
        new int[]{0xff444860, 0xff343650, 0xff24283e}, new float[]{0, .6f, 1}, Shader.TileMode.CLAMP);
    private final float[] grain = ruledInk();
    private final RectF poster = new RectF(WIDTH * .27f, HEIGHT * .118f, WIDTH * .89f, HEIGHT * .86f);
    private final RectF opening = new RectF(poster.left + 13, poster.top + 13, poster.right - 13, poster.bottom - 13);
    private final RectF artwork = new RectF();
    private final RectF banner = new RectF();
    private final Path exploreMark = new Path();
    private final Typeface serif = Typeface.create("serif", Typeface.NORMAL);
    private final Typeface serifBold = Typeface.create("serif", Typeface.BOLD);
    private final HomeTheme theme;
    private final Shader backdrop, scrim;
    private final RectF screen = new RectF(0, 0, WIDTH, HEIGHT);
    private final RectF tuner = new RectF(WIDTH * .045f, HEIGHT * .64f, WIDTH * .39f, HEIGHT * .92f);
    private final Path sleeveShape = new Path();
    private Bitmap art, inset;
    private boolean artworkUnavailable;

    ThemedHome(HomeTheme theme) {
        this.theme = theme;
        switch (theme) {
            case FIRST_CHAIR:
                backdrop = vertical(new int[]{0xff42536a, 0xff102239}, null);
                scrim = vertical(new int[]{0xda13223c, 0x3b102039, 0x00102039, 0x74122540, 0xf514283e, 0xff102239},
                    new float[]{0, .23f, .44f, .57f, .81f, 1});
                break;
            case PORCHLIGHT:
                backdrop = new RadialGradient(WIDTH * .54f, HEIGHT * .58f, WIDTH * .58f,
                    new int[]{0xff573c30, 0xff312320, 0xff211a1b}, new float[]{0, .52f, 1}, Shader.TileMode.CLAMP);
                scrim = null;
                break;
            case RIVER_CITY:
                backdrop = vertical(new int[]{0xff283e58, 0xff1d293b}, null);
                scrim = vertical(new int[]{0xeb101b30, 0x7615233d, 0x0015233d, 0x0015233d, 0x21151d2a, 0xd4101a2c},
                    new float[]{0, .14f, .32f, .55f, .64f, 1});
                break;
            case GTA_VI:
                backdrop = vertical(new int[]{0xff674456, 0xff26303e}, null);
                scrim = vertical(new int[]{0xb3202337, 0x00202337, 0x00202337, 0x5c202333},
                    new float[]{0, .24f, .86f, 1});
                break;
            default:
                backdrop = ink;
                scrim = null;
        }
    }

    private static Shader vertical(int[] colors, float[] stops) {
        return new LinearGradient(0, 0, 0, HEIGHT, colors, stops, Shader.TileMode.CLAMP);
    }

    private static float[] ruledInk() {
        float drift = HEIGHT * .58f;
        float[] lines = new float[((int) (WIDTH + drift) / 10 + 1) * 4];
        for (int i = 0; i < lines.length; i += 4) {
            float x = i / 4 * 10;
            lines[i] = x;
            lines[i + 2] = x - drift;
            lines[i + 3] = HEIGHT;
        }
        return lines;
    }

    void setArt(Bitmap bitmap, Bitmap inset) {
        art = bitmap;
        this.inset = inset;
        if (theme == HomeTheme.PARK_RAT) cover(opening, .67f, .5f);
        else cover(screen, .5f, theme == HomeTheme.FIRST_CHAIR ? .60f : theme == HomeTheme.RIVER_CITY ? .56f : .5f);
        if (inset != null) tuner.top = tuner.bottom - tuner.width() * inset.getHeight() / inset.getWidth();
    }

    void setArtworkUnavailable() { artworkUnavailable = true; }

    private void cover(RectF bounds, float alignX, float alignY) {
        float scale = Math.max(bounds.width() / art.getWidth(), bounds.height() / art.getHeight());
        float width = art.getWidth() * scale, height = art.getHeight() * scale;
        float left = bounds.left - (width - bounds.width()) * alignX;
        float top = bounds.top - (height - bounds.height()) * alignY;
        artwork.set(left, top, left + width, top + height);
    }

    void draw(Canvas canvas, HomeThemeLayout layout, int selected, Bitmap[] logos, IntFunction<String> label,
              String clock, String date, FirstChair.Outlook outlook, boolean paused, RecentSpotifyTracks music) {
        paint.setShader(backdrop);
        canvas.drawRect(screen, paint);
        paint.setShader(null);
        if (theme == HomeTheme.PARK_RAT) drawParkRat(canvas);
        else {
            if (art != null) {
                paint.setAlpha(255);
                canvas.drawBitmap(art, null, artwork, paint);
                paint.setShader(scrim);
                canvas.drawRect(screen, paint);
                paint.setShader(null);
            }
            switch (theme) {
                case FIRST_CHAIR: drawSnow(canvas, outlook, layout); break;
                case PORCHLIGHT: drawCrate(canvas, selected, music); break;
                case RIVER_CITY:
                    rect(canvas, 62, 1044, 517, 1046, 0xb5dfad71);
                    rect(canvas, 1178, 1026, 1858, 1028, 0xb5dfad71);
                    break;
                case GTA_VI: drawGtaArtwork(canvas); break;
            }
        }

        header(canvas, clock, date);
        for (Tile tile : layout.tiles) {
            boolean focused = tile.id == selected;
            if (tile.id >= FIRST_TRACK) continue;
            if (snowControl(tile.id)) {
                drawSnowControl(canvas, tile, focused, paused);
                continue;
            }
            int favorite = HomeNavigation.favoriteIndex(tile.id);
            Bitmap logo = favorite < 0 ? null : logos[favorite];
            if (theme == HomeTheme.PARK_RAT) drawParkTile(canvas, tile, focused, logo, label.apply(tile.id));
            else drawTile(canvas, tile, focused, logo, label.apply(tile.id));
        }
    }

    private void drawParkRat(Canvas canvas) {
        paint.setColor(0x11ffffff);
        paint.setStrokeWidth(1);
        canvas.drawLines(grain, paint);

        canvas.save();
        canvas.rotate(-3, poster.centerX(), poster.centerY());
        paint.setColor(0xff823d44);
        canvas.drawRect(poster.left + 13, poster.top + 17, poster.right + 13, poster.bottom + 17, paint);
        paint.setColor(PAPER);
        canvas.drawRect(poster, paint);
        if (art != null) {
            canvas.clipRect(opening);
            canvas.drawBitmap(art, null, artwork, paint);
        }
        canvas.restore();

    }

    private void header(Canvas canvas, String clock, String date) {
        boolean parkRat = theme == HomeTheme.PARK_RAT, porchlight = theme == HomeTheme.PORCHLIGHT;
        int color = theme == HomeTheme.FIRST_CHAIR ? 0xfff0f8ff : CREAM;
        int accent;
        switch (theme) {
            case FIRST_CHAIR: accent = 0xffbce5ff; break;
            case PORCHLIGHT: accent = 0xffe9ac78; break;
            case RIVER_CITY: accent = 0xfffac87b; break;
            case GTA_VI: accent = 0xffcbe3d8; break;
            default: accent = 0xffffbb9c;
        }
        Typeface heading = porchlight ? serifBold : parkRat ? wordmark : bold;
        Typeface body = porchlight ? serif : medium;
        text(canvas, "VISTA", 96, 83, 31, color, heading);
        text(canvas, "/", 188, 83, 26, accent, body);
        text(canvas, "HOME", 213, 83, 23, color, body);
        paint.setTextAlign(Paint.Align.RIGHT);
        text(canvas, clock, 1824, 84, 36, color, body);
        float clockWidth = paint.measureText(clock);
        text(canvas, date, 1808 - clockWidth, 84, 24, parkRat ? 0xffe6dcca : color, body);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawParkTile(Canvas canvas, Tile tile, boolean focused, Bitmap logo, String label) {
        int background = 0xff494e68, foreground = CREAM, border = 0xffe7d8bb;
        switch (tile.id) {
            case 0: background = 0xfff5e7cb; foreground = 0xff36374b; break;
            case 1: background = 0xff8d4650; break;
            case 2: background = 0xffc4d4ad; foreground = 0xff2f4c3e; break;
            case 4: background = 0xff4f5d47; foreground = 0xffdae9b9; break;
            case HomeNavigation.PRIME_VIDEO: background = 0xff3a5774; foreground = 0xffb7eaff; break;
            case 11: background = 0xffefd9b0; foreground = 0xff47313d; break;
            case 13: case 14: case MORE: case THEMES: background = 0xff333750; border = 0xffa59bac; break;
        }

        paint.setColor(tile.id == 0 ? 0xff743940 : 0xff232238);
        canvas.drawRoundRect(tile.left + 7, tile.top + 9, tile.right + 7, tile.bottom + 9, 4, 4, paint);
        paint.setColor(background);
        canvas.drawRoundRect(tile.left, tile.top, tile.right, tile.bottom, 4, 4, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(border);
        canvas.drawRoundRect(tile.left, tile.top, tile.right, tile.bottom, 4, 4, paint);
        if (focused) {
            paint.setStrokeWidth(5);
            paint.setColor(0xff27273a);
            canvas.drawRoundRect(tile.left - 4, tile.top - 4, tile.right + 4, tile.bottom + 4, 7, 7, paint);
            paint.setColor(tile.id == 0 ? 0xffffb69f : 0xffffefad);
            canvas.drawRoundRect(tile.left - 8, tile.top - 8, tile.right + 8, tile.bottom + 8, 9, 9, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        float centerX = tile.centerX(), centerY = tile.centerY();
        if (logo != null) {
            drawLogo(canvas, tile, logo, .82f, .82f);
            return;
        }

        if (tile.id == 11) {
            float markX = centerX - 55;
            exploreMark.rewind();
            exploreMark.moveTo(markX, centerY - 12);
            exploreMark.quadTo(markX + 4, centerY - 4, markX + 12, centerY);
            exploreMark.quadTo(markX + 4, centerY + 4, markX, centerY + 12);
            exploreMark.quadTo(markX - 4, centerY + 4, markX - 12, centerY);
            exploreMark.quadTo(markX - 4, centerY - 4, markX, centerY - 12);
            exploreMark.close();
            paint.setColor(foreground);
            canvas.drawPath(exploreMark, paint);
            centerX += 15;
        }
        if (tile.id == MORE) {
            paint.setColor(foreground);
            for (int i = 0; i < 3; i++) canvas.drawCircle(centerX - 60 + i * 11, centerY, 3, paint);
            centerX += 15;
        }
        boolean app = HomeNavigation.favoriteIndex(tile.id) >= 0;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(app ? bold : medium);
        paint.setTextSize(app ? (tile.id == 0 ? 59 : 45) : 28);
        paint.setColor(foreground);
        canvas.drawText(label, centerX, centerY - (paint.ascent() + paint.descent()) / 2, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawLogo(Canvas canvas, Tile tile, Bitmap logo, float fractionWidth, float fractionHeight) {
        float scale = Math.min((tile.right - tile.left) * fractionWidth / logo.getWidth(),
            (tile.bottom - tile.top) * fractionHeight / logo.getHeight());
        float halfWidth = logo.getWidth() * scale / 2, halfHeight = logo.getHeight() * scale / 2;
        float x = tile.centerX(), y = tile.centerY();
        banner.set(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight);
        paint.setAlpha(255);
        canvas.drawBitmap(logo, null, banner, paint);
    }

    private void drawTile(Canvas c, Tile tile, boolean focused, Bitmap logo, String label) {
        int bg, stroke, focus;
        float radius, markWidth = .78f;
        int id = tile.id;
        boolean utility = id == 13 || id == 14 || id == MORE || id == THEMES;
        switch (theme) {
            case FIRST_CHAIR:
                bg = id == 0 ? 0xff355c7a : id == 11 ? 0xff345872 : 0xe8243e56;
                stroke = 0xff7a9dbb;
                focus = 0xffe2f8ff;
                radius = 7;
                break;
            case PORCHLIGHT:
                switch (id) {
                    case 0: bg = 0xff815749; break;
                    case 1: bg = 0xff713d3b; break;
                    case 2: bg = 0xff514d3d; break;
                    case 3: bg = 0xff4d3c3a; break;
                    case 4: bg = 0xff59442e; break;
                    case 5: bg = 0xff765743; break;
                    case HomeNavigation.PRIME_VIDEO: bg = 0xff41494b; break;
                    case 11: bg = 0xff6b4035; break;
                    default: bg = 0xff2d2221;
                }
                stroke = id == 4 ? 0xffc1a16d : 0xffab7a5b;
                focus = 0xffffe4b6;
                radius = id == 4 ? 14 : 3;
                break;
            case RIVER_CITY:
                bg = id == 0 ? 0xff674935 : id == 11 ? 0xff785334 : 0xee1b2737;
                stroke = utility ? 0xff7e8191 : 0xffb09a78;
                focus = 0xffffda9c;
                radius = 6;
                break;
            default:
                bg = utility ? 0xe829363d : id == 0 ? 0xff8a496b : id == 11 ? 0xff633751 : 0xef322639;
                stroke = utility ? 0xffa3b9b8 : id == 0 ? 0xffeca4ba : 0xffa58a9c;
                focus = 0xffffd1d3;
                radius = 10;
                if (id == 0) markWidth = .56f;
        }
        box(c, tile, radius, bg, stroke, focus, focused);
        if (logo != null) {
            drawLogo(c, tile, logo, markWidth, .81f);
            return;
        }

        boolean app = HomeNavigation.favoriteIndex(id) >= 0;
        if (id == 11 && (theme == HomeTheme.FIRST_CHAIR || theme == HomeTheme.GTA_VI || theme == HomeTheme.RIVER_CITY))
            label = "✦ " + label;
        int color = CREAM;
        if (app && id == 1) color = 0xffff7e86;
        if (app && (id == 2 || id == 4) && !(id == 4 && theme == HomeTheme.PORCHLIGHT)) color = 0xff82dcb1;
        if (id == HomeNavigation.PRIME_VIDEO) color = 0xffb7eaff;
        Typeface face = app ? bold : theme == HomeTheme.PORCHLIGHT ? serif : medium;
        float size = app ? (tile.right - tile.left > 300 ? 45 : 33) : 25;
        paint.setTypeface(face);
        paint.setTextSize(size);
        float fit = size * (tile.right - tile.left - 24) / Math.max(1, paint.measureText(label));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Math.min(size, fit));
        paint.setColor(color);
        c.drawText(label, tile.centerX(), tile.centerY() - (paint.ascent() + paint.descent()) / 2, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void box(Canvas c, Tile tile, float radius, int background, int stroke, int focus, boolean focused) {
        paint.setColor(background);
        c.drawRoundRect(tile.left, tile.top, tile.right, tile.bottom, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(stroke);
        c.drawRoundRect(tile.left, tile.top, tile.right, tile.bottom, radius, radius, paint);
        if (focused) {
            paint.setStrokeWidth(3.5f);
            paint.setColor(focus);
            c.drawRoundRect(tile.left - 5, tile.top - 5, tile.right + 5, tile.bottom + 5, radius + 3, radius + 3, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSnow(Canvas c, FirstChair.Outlook outlook, HomeThemeLayout layout) {
        float baseline = 716, x = 96;
        paint.setShadowLayer(8, 0, 2, 0xff091423);
        text(c, outlook.name, x, baseline, 29, 0xfff4faff, bold);
        x += paint.measureText(outlook.name) + 18;
        text(c, outlook.amount, x, baseline, outlook.horizon.isEmpty() ? 25 : 33, 0xfff4faff, bold);
        x += paint.measureText(outlook.amount) + 16;
        if (!outlook.horizon.isEmpty()) {
            text(c, outlook.horizon, x, baseline, 24, 0xffe3f0fa, medium);
            x += paint.measureText(outlook.horizon) + 16;
        }
        rect(c, x, baseline - 20, x + 1, baseline + 4, 0x94c5ddf1);
        text(c, outlook.source, x + 14, baseline, 21, 0xffe3f0fa, medium);
        paint.clearShadowLayer();

        float rail = HEIGHT * .6965f;
        rect(c, 96, rail, 1826, rail + 1.4f, 0xb0add9f9);
        for (Tile tile : layout.tiles) {
            if (HomeNavigation.favoriteIndex(tile.id) < 0 && tile.id != 11) continue;
            rect(c, tile.centerX(), rail, tile.centerX() + 1, tile.top - 1, 0xffafd1e7);
        }
    }

    private void drawSnowControl(Canvas c, Tile tile, boolean focused, boolean paused) {
        box(c, tile, 7, 0x2513263a, 0x91c9e5fc, 0xffe2f8ff, focused);
        float x = tile.centerX(), y = tile.centerY();
        if (tile.id == SNOW_TOGGLE && !paused) {
            rect(c, x - 9, y - 10, x - 4, y + 10, 0xffe2f8ff);
            rect(c, x + 4, y - 10, x + 9, y + 10, 0xffe2f8ff);
            return;
        }
        paint.setColor(0xffe2f8ff);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        exploreMark.rewind();
        exploreMark.moveTo(x - 5, y - 10);
        exploreMark.lineTo(x + 7, y);
        exploreMark.lineTo(x - 5, y + 10);
        if (tile.id == SNOW_TOGGLE) exploreMark.close();
        c.drawPath(exploreMark, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawGtaArtwork(Canvas c) {
        paint.setColor(0x63422d46);
        c.drawRoundRect(1286, 348, 1863, 988, 12, 12, paint);
        if (artworkUnavailable) {
            text(c, "Rockstar artwork unavailable", 96, 795, 40, CREAM, bold);
            text(c, "Add both JPEGs to .local/home-art and rebuild Vista.", 96, 845, 27, CREAM, medium);
        }
        if (inset == null) return;
        rect(c, tuner.left - 2, tuner.top - 2, tuner.right + 2, tuner.bottom + 2, 0xffecc3c5);
        paint.setAlpha(255);
        c.drawBitmap(inset, null, tuner, paint);
        rect(c, 1480, 1003, 1836, 1041, 0xdd202f31);
        text(c, "Rockstar Games imagery", 1492, 1030, 22, CREAM, medium);
    }

    private void drawCrate(Canvas c, int selected, RecentSpotifyTracks music) {
        paint.setColor(0xff3d2923);
        c.drawRoundRect(445, 248, 1544, 918, 9, 9, paint);
        rect(c, 445, 850, 1544, 918, 0xff7f4a34);
        rect(c, 449, 248, 1540, 255, 0xff966649);
        paint.setColor(0x14e9a770);
        paint.setStrokeWidth(1);
        for (int y = 263; y < 918; y += 7) c.drawLine(447, y, 1542, y - 8, paint);
        text(c, "Recently played", 480, 317, 43, CREAM, serif);
        paint.setTextAlign(Paint.Align.RIGHT);
        text(c, "Spotify on this TV", 1515, 315, 21, 0xffedc8a7, medium);
        paint.setTextAlign(Paint.Align.LEFT);
        if (music.size() == 0) {
            text(c, music.access ? "Play something in Spotify, then return Home." : "Open Spotify vibes to enable local playback access.",
                480, 367, 25, 0xffedc8a7, medium);
        }
        for (int i = 0; i < SLEEVES.length; i++) drawSleeve(c, i, selected, music.get(i));
    }

    private void drawSleeve(Canvas c, int index, int selected, RecentSpotifyTracks.Track track) {
        Tile t = SLEEVES[index];
        float left = t.left, top = t.top, width = t.right - left, bottom = top + width;
        int paper = index == 0 ? 0xffedc6a3 : index == 1 ? 0xffe9dcba : 0xffd9d9ce;
        int ink = index == 2 ? 0xff2f3940 : 0xff483129;
        rect(c, left + 6, top + 8, t.right + 6, t.bottom + 8, 0xff241716);
        box(c, t, 4, paper, 0xffbca089, 0xffffe4b6, t.id == selected);
        c.save();
        c.clipRect(left, top, t.right, bottom);
        c.translate(left, top);
        if (index == 0) {
            rect(c, 0, 0, width, width, 0xffad6859);
            paint.setColor(0xfff2cb95);
            c.drawCircle(width * .56f, width * .49f, width * .16f, paint);
            triangle(c, 0, width * .53f, width, width, 0, width, 0xff713938);
            triangle(c, width, width * .38f, 0, width * .86f, 0, width, 0xff2e2125);
        } else if (index == 1) {
            rect(c, 0, 0, width, width, 0xffb78955);
            for (int stripe = 0; stripe < 5; stripe++) {
                float x = stripe * width / 5;
                rect(c, x, 0, x + width * .04f, width, 0xff634035);
                rect(c, x + width * .04f, 0, x + width * .09f, width, 0xffdfba78);
            }
            rect(c, width * .11f, width * .1f, width * .89f, width * .9f, 0xffc9a17a);
            rect(c, width * .15f, width * .14f, width * .85f, width * .86f, 0xff362825);
            rect(c, width * .17f, width * .16f, width * .83f, width * .84f, 0xffd18c5d);
            triangle(c, width * .17f, width * .16f, width * .68f, width * .16f, width * .17f, width * .67f, 0xfff8e0a5);
            rect(c, width * .48f, width * .16f, width * .52f, width * .84f, 0xff362825);
            rect(c, width * .17f, width * .48f, width * .83f, width * .52f, 0xff362825);
        } else {
            rect(c, 0, 0, width, width, 0xff59616a);
            rect(c, 0, width * .57f, width, width, 0xff404250);
            rect(c, 0, width * .75f, width, width, 0xff253941);
            paint.setColor(0xffeee0c4);
            c.drawCircle(width * .65f, width * .37f, width * .115f, paint);
        }
        paint.setColor(0x22fff1da);
        for (int y = 4; y <= width + 45; y += 7) c.drawLine(0, y, width, index == 2 ? y - 44 : y, paint);
        c.restore();
        if (track != null || index == 0) {
            paint.setTextAlign(Paint.Align.CENTER);
            String title = track != null ? track.title : "Spotify vibes";
            String artist = track != null ? track.artist : "";
            sleeveLabel(c, title, t.centerX(), bottom + (artist.isEmpty() ? 54 : 39), width - 22, 34, ink, serifBold);
            if (!artist.isEmpty()) sleeveLabel(c, artist, t.centerX(), bottom + 69, width - 22, 22, ink, medium);
            paint.setTextAlign(Paint.Align.LEFT);
        } else {
            rect(c, left, top, t.right, t.bottom, 0x6a231a19);
        }
    }

    private void sleeveLabel(Canvas c, String value, float x, float y, float width, float size, int color, Typeface face) {
        paint.setTypeface(face);
        paint.setTextSize(size);
        text(c, TextUtils.ellipsize(value, paint, width, TextUtils.TruncateAt.END).toString(), x, y, size, color, face);
    }

    private void triangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        sleeveShape.rewind();
        sleeveShape.moveTo(x1, y1);
        sleeveShape.lineTo(x2, y2);
        sleeveShape.lineTo(x3, y3);
        sleeveShape.close();
        paint.setColor(color);
        c.drawPath(sleeveShape, paint);
    }

    private void rect(Canvas c, float left, float top, float right, float bottom, int color) {
        paint.setColor(color);
        c.drawRect(left, top, right, bottom, paint);
    }

    private void text(Canvas canvas, String text, float x, float baseline, float size, int color, Typeface face) {
        paint.setTextSize(size);
        paint.setTypeface(face);
        paint.setColor(color);
        canvas.drawText(text, x, baseline, paint);
    }
}
