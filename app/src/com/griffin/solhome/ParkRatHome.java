package com.griffin.solhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import java.util.function.IntFunction;

/** Retained native artwork and print treatment for Park Rat Home. */
final class ParkRatHome {
    private static final int PAPER = 0xffefe4c9, CREAM = 0xfffff2dc;
    private static final int WIDTH = ParkRatLayout.WIDTH, HEIGHT = ParkRatLayout.HEIGHT;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
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
    private Bitmap art;

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

    void setArt(Bitmap bitmap) {
        art = bitmap;
        float scale = Math.max(opening.width() / bitmap.getWidth(), opening.height() / bitmap.getHeight());
        float width = bitmap.getWidth() * scale, height = bitmap.getHeight() * scale;
        float left = opening.left - (width - opening.width()) * .67f;
        float top = opening.top - (height - opening.height()) * .5f;
        artwork.set(left, top, left + width, top + height);
    }

    void draw(Canvas canvas, int selected, Bitmap[] logos, IntFunction<String> label, String clock, String date) {
        paint.setShader(ink);
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint);
        paint.setShader(null);
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

        text(canvas, "VISTA", 96, 83, 31, CREAM, wordmark);
        text(canvas, "/", 188, 83, 26, 0xffffbb9c, medium);
        text(canvas, "HOME", 213, 83, 23, CREAM, medium);
        paint.setTextAlign(Paint.Align.RIGHT);
        text(canvas, clock, 1824, 84, 36, CREAM, medium);
        float clockWidth = paint.measureText(clock);
        text(canvas, date, 1808 - clockWidth, 84, 24, 0xffe6dcca, medium);
        paint.setTextAlign(Paint.Align.LEFT);

        for (ParkRatLayout.Tile tile : ParkRatLayout.TILES) {
            int favorite = HomeNavigation.favoriteIndex(tile.id);
            drawTile(canvas, tile, tile.id == selected, favorite < 0 ? null : logos[favorite], label.apply(tile.id));
        }
    }

    private void drawTile(Canvas canvas, ParkRatLayout.Tile tile, boolean focused, Bitmap logo, String label) {
        int background = 0xff494e68, foreground = CREAM, border = 0xffe7d8bb;
        switch (tile.id) {
            case 0: background = 0xfff5e7cb; foreground = 0xff36374b; break;
            case 1: background = 0xff8d4650; break;
            case 2: background = 0xffc4d4ad; foreground = 0xff2f4c3e; break;
            case 4: background = 0xff4f5d47; foreground = 0xffdae9b9; break;
            case HomeNavigation.PRIME_VIDEO: background = 0xff3a5774; foreground = 0xffb7eaff; break;
            case 11: background = 0xffefd9b0; foreground = 0xff47313d; break;
            case 13: case 14: case ParkRatLayout.MORE: background = 0xff333750; border = 0xffa59bac; break;
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
            float scale = Math.min((tile.right - tile.left) * .82f / logo.getWidth(),
                (tile.bottom - tile.top) * .82f / logo.getHeight());
            float halfWidth = logo.getWidth() * scale / 2, halfHeight = logo.getHeight() * scale / 2;
            banner.set(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
            paint.setAlpha(255);
            canvas.drawBitmap(logo, null, banner, paint);
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
        if (tile.id == ParkRatLayout.MORE) {
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

    private void text(Canvas canvas, String text, float x, float baseline, float size, int color, Typeface face) {
        paint.setTextSize(size);
        paint.setTypeface(face);
        paint.setColor(color);
        canvas.drawText(text, x, baseline, paint);
    }
}
