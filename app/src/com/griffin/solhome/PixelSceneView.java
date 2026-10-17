package com.griffin.solhome;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Choreographer;
import java.io.InputStream;
import java.util.Random;

/** Small reusable textures and native mesh motion, with no decoded frame sequence. */
public final class PixelSceneView extends View {
    private static final int WIDTH = 480, HEIGHT = 336;
    static final float WINDOW_LEFT=694, WINDOW_TOP=88, WINDOW_RIGHT=1858, WINDOW_BOTTOM=900;
    private static final int WATER_COLS = 12, WATER_ROWS = 24;
    private static final int TREE_COLS = 18, TREE_ROWS = 24;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint();
    private final RectF destination = new RectF();
    private final Path waterMask = new Path();
    private final float[] waterMesh = new float[(WATER_COLS + 1) * (WATER_ROWS + 1) * 2];
    private final float[] treeMesh = new float[(TREE_COLS + 1) * (TREE_ROWS + 1) * 2];
    private final float[] stars = new float[72];
    private final long epoch = SystemClock.elapsedRealtime();
    private Bitmap terrain, leftTree, rightTree, cloud, halloween;
    private final Bitmap ghost = makeGhost(false);
    private final Bitmap ghostBlink = makeGhost(true);
    private long lastFrameNanos;
    private Shader skyGradient;
    private ColorFilter terrainLight, cloudLight;
    private CincinnatiSky.State sky;
    private boolean active, ambient, released;
    private long skyMinute = Long.MIN_VALUE;
    private long previewTime;

    private final Choreographer.FrameCallback tick = new Choreographer.FrameCallback() {
        @Override public void doFrame(long frameTimeNanos) {
            if (!active) return;
            // Every other 60 Hz vsync: continuous motion at about 30 fps.
            if (frameTimeNanos-lastFrameNanos >= 30000000L) {
                lastFrameNanos = frameTimeNanos;
                refreshSky();
                invalidate();
            }
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    public PixelSceneView(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);
        Random random = new Random(314159);
        for (int i = 0; i < stars.length; i += 2) {
            stars[i] = random.nextInt(WIDTH);
            stars[i + 1] = 8 + random.nextInt(100);
        }
        waterMask.moveTo(185, 248);
        waterMask.lineTo(330, 238);
        waterMask.lineTo(475, 249);
        waterMask.lineTo(475, 294);
        waterMask.lineTo(407, 312);
        waterMask.lineTo(348, 326);
        waterMask.lineTo(285, 312);
        waterMask.lineTo(225, 292);
        waterMask.close();
        refreshSky();
        new Thread(this::loadAssets, "sol-pixel-art").start();
    }

    private Bitmap load(String name, int width, int height) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContext().getAssets().open("pixel-autumn/" + name + ".png")) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 1;
        while (bounds.outWidth / (options.inSampleSize * 2) >= width
                && bounds.outHeight / (options.inSampleSize * 2) >= height) options.inSampleSize *= 2;
        try (InputStream input = getContext().getAssets().open("pixel-autumn/" + name + ".png")) {
            Bitmap decoded = BitmapFactory.decodeStream(input, null, options);
            if (decoded == null) throw new IllegalStateException("Cannot decode " + name);
            Bitmap scaled = Bitmap.createScaledBitmap(decoded, width, height, false);
            if (scaled != decoded) decoded.recycle();
            return scaled;
        }
    }

    private Bitmap movingSprite(String name, int width, int height) throws Exception {
        Bitmap pixels = load(name, width, height);
        // Preserve chunky pixels first; bilinear filtering then only softens subpixel travel.
        Bitmap padded = Bitmap.createScaledBitmap(pixels, width*3, height*3, false);
        pixels.recycle();
        return padded;
    }

    private static Bitmap makeGhost(boolean blink) {
        Bitmap pixels = Bitmap.createBitmap(20, 24, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(pixels);
        Paint p = new Paint();
        p.setColor(0xffb8cbdc);
        canvas.drawRect(6, 0, 14, 2, p); canvas.drawRect(3, 2, 17, 5, p);
        canvas.drawRect(1, 5, 19, 20, p); canvas.drawRect(0, 12, 20, 21, p);
        canvas.drawRect(1, 20, 5, 23, p); canvas.drawRect(8, 20, 12, 24, p);
        canvas.drawRect(15, 20, 19, 23, p);
        p.setColor(0xffeef4f6);
        canvas.drawRect(6, 2, 14, 4, p); canvas.drawRect(3, 4, 17, 8, p);
        canvas.drawRect(2, 8, 18, 20, p); canvas.drawRect(1, 13, 19, 20, p);
        canvas.drawRect(2, 20, 4, 22, p); canvas.drawRect(9, 20, 11, 23, p);
        canvas.drawRect(16, 20, 18, 22, p);
        p.setColor(0xff35435d);
        canvas.drawRect(5, blink?11:9, 7, 12, p);
        canvas.drawRect(12, blink?11:9, 14, 12, p);
        canvas.drawRect(9, 15, 11, 17, p);
        Bitmap result = Bitmap.createScaledBitmap(pixels, 60, 72, false);
        pixels.recycle();
        return result;
    }

    private void loadAssets() {
        try {
            final Bitmap ground = load("terrain", WIDTH, HEIGHT);
            final Bitmap left = movingSprite("tree-left", 144, 192);
            final Bitmap right = movingSprite("tree-right", 112, 152);
            final Bitmap clouds = movingSprite("cloud", 192, 64);
            final Bitmap pumpkins = load("halloween", 100, 67);
            handler.post(() -> {
                if (released) return;
                terrain = ground; leftTree = left; rightTree = right;
                cloud = clouds; halloween = pumpkins;
                long bytes = ground.getAllocationByteCount() + left.getAllocationByteCount()
                        + right.getAllocationByteCount() + clouds.getAllocationByteCount()
                        + pumpkins.getAllocationByteCount() + ghost.getAllocationByteCount()
                        + ghostBlink.getAllocationByteCount();
                Log.i("SolScene", "Pixel assets ready; decoded bytes=" + bytes);
                invalidate();
            });
        } catch (Exception error) {
            Log.e("SolScene", "Pixel art load failed", error);
        }
    }

    public void start() {
        active = true;
        skyMinute = Long.MIN_VALUE;
        lastFrameNanos = 0;
        Choreographer.getInstance().removeFrameCallback(tick);
        Choreographer.getInstance().postFrameCallback(tick);
    }

    public void stop() {
        active = false;
        Choreographer.getInstance().removeFrameCallback(tick);
    }

    public void release() {
        stop();
        released = true;
        terrain = leftTree = rightTree = cloud = halloween = null;
    }

    public void setAmbient(boolean value) {
        if (ambient != value) { ambient = value; invalidate(); }
    }

    /** Developer preview changes scene lighting only; a normal Home intent clears it. */
    public void setPreviewTime(long value) {
        previewTime = Math.max(0, value);
        skyMinute = Long.MIN_VALUE;
        refreshSky();
        invalidate();
    }

    private void refreshSky() {
        long now = previewTime > 0 ? previewTime : System.currentTimeMillis();
        long minute = now / 60000;
        if (skyMinute == minute) return;
        skyMinute = minute;
        sky = CincinnatiSky.at(now);
        float day = sky.daylight, warm = sky.twilight * .55f;
        int top = blend(blend(0xff091630, 0xff5a92b6, day), 0xff88627e, warm);
        int bottom = blend(blend(0xff29395c, 0xfff1d7b0, day), 0xffefa57c, warm);
        skyGradient = new LinearGradient(0, 0, 0, 180, top, bottom, Shader.TileMode.CLAMP);
        terrainLight = new ColorMatrixColorFilter(new float[] {
                .24f + .76f * day + warm * .12f, 0, 0, 0, (1-day)*4,
                0, .32f + .68f * day, 0, 0, (1-day)*7,
                0, 0, .54f + .46f * day - warm*.08f, 0, (1-day)*14,
                0, 0, 0, 1, 0 });
        cloudLight = new ColorMatrixColorFilter(new float[] {
                .38f+.62f*day, 0, 0, 0, 0,
                0, .47f+.53f*day, 0, 0, 0,
                0, 0, .72f+.28f*day, 0, 0,
                0, 0, 0, 1, 0 });
        Log.i("SolScene", "Lighting=" + sky.phase + "; October accents=" + sky.halloween
                + "; preview=" + (previewTime > 0));
    }

    private static int blend(int from, int to, float amount) {
        return Color.rgb(Math.round(Color.red(from) + (Color.red(to)-Color.red(from))*amount),
                Math.round(Color.green(from)+(Color.green(to)-Color.green(from))*amount),
                Math.round(Color.blue(from)+(Color.blue(to)-Color.blue(from))*amount));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float time = (SystemClock.elapsedRealtime() - epoch) / 1000f;
        canvas.save();
        canvas.scale(getWidth()/1920f, getHeight()/1080f);
        canvas.translate(WINDOW_LEFT, WINDOW_TOP);
        canvas.scale((WINDOW_RIGHT-WINDOW_LEFT)/WIDTH, (WINDOW_BOTTOM-WINDOW_TOP)/HEIGHT);
        canvas.clipRect(0, 0, WIDTH, HEIGHT);
        paint.setColorFilter(null); paint.setAlpha(255); paint.setShader(skyGradient);
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint); paint.setShader(null);
        drawSky(canvas, time);
        drawCloud(canvas, (time*.9f + 64) % 760 - 190, 16, 206, 69);
        drawCloud(canvas, (time*.55f + 360) % 760 - 190, 57, 168, 56);
        drawCloud(canvas, (time*.7f + 580) % 760 - 190, 6, 132, 44);
        paint.setColorFilter(terrainLight);
        if (terrain != null) {
            destination.set(0, 0, WIDTH, HEIGHT);
            canvas.drawBitmap(terrain, null, destination, paint);
            drawWater(canvas, time);
        }
        if (sky.halloween) drawGhost(canvas, time);
        drawTree(canvas, leftTree, -74, 1, 244, 330, time, 0);
        drawTree(canvas, rightTree, 387, 130, 140, 205, time, 2.3f);
        paint.setColorFilter(null);
        drawLeaves(canvas, time);
        if (sky.halloween && halloween != null) drawHalloween(canvas, time);
        paint.setColorFilter(null); paint.setAlpha(255);
        canvas.restore();
    }

    private void drawSky(Canvas canvas, float time) {
        paint.setColorFilter(null);
        float dark = 1-sky.daylight;
        if (dark > .1f) {
            for (int i = 0; i < stars.length; i += 2) {
                paint.setColor(Color.argb((int)(dark*(135+45*Math.sin(time*.8+i))), 227, 233, 246));
                canvas.drawRect(stars[i], stars[i+1], stars[i]+1, stars[i+1]+1, paint);
            }
            // Decorative crescent, not an astronomical lunar-phase calculation.
            paint.setColor(Color.argb((int)(dark*255), 237, 234, 208));
            canvas.drawRect(386, 25, 391, 27, paint);
            canvas.drawRect(383, 27, 388, 30, paint);
            canvas.drawRect(382, 30, 386, 37, paint);
            canvas.drawRect(383, 37, 390, 41, paint);
            canvas.drawRect(386, 41, 393, 44, paint);
            canvas.drawRect(390, 38, 395, 41, paint);
        }
        if (sky.elevation > -3) {
            float x = (float) Math.max(30, Math.min(450, (sky.hourAngle+100)/200*WIDTH));
            float y = (float) Math.max(12, 115-Math.max(0, sky.elevation)*1.7);
            paint.setColor(0xffffe4a6);
            int xx = Math.round(x), yy = Math.round(y);
            canvas.drawRect(xx-7, yy-4, xx+8, yy+5, paint);
            canvas.drawRect(xx-4, yy-7, xx+5, yy+8, paint);
        }
    }

    private void drawCloud(Canvas canvas, float x, float y, float width, float height) {
        if (cloud == null) return;
        paint.setAlpha(255); paint.setColorFilter(cloudLight);
        destination.set(x, y, x+width, y+height);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(cloud, null, destination, paint);
        paint.setFilterBitmap(false);
        paint.setColorFilter(null);
    }

    private void drawWater(Canvas canvas, float time) {
        canvas.save(); canvas.clipPath(waterMask);
        int at = 0;
        for (int row = 0; row <= WATER_ROWS; row++) {
            float y = row * HEIGHT / (float) WATER_ROWS;
            float displacement = y > 235 ? (float) Math.sin(row*1.6-time*1.2)*1.8f : 0;
            for (int column = 0; column <= WATER_COLS; column++) {
                waterMesh[at++] = column*WIDTH/(float)WATER_COLS + displacement;
                waterMesh[at++] = y;
            }
        }
        paint.setFilterBitmap(true);
        canvas.drawBitmapMesh(terrain, WATER_COLS, WATER_ROWS, waterMesh, 0, null, 0, paint);
        paint.setFilterBitmap(false);
        paint.setColorFilter(null);
        for (int i = 0; i < 32; i++) {
            int y = 244 + (i*17)%79;
            float x = 172 + (i*37 + time*(3+i%3))%310;
            int length = 3 + i%6*2;
            paint.setColor(Color.argb(40+(i%4)*12, 130, 170, 191));
            canvas.drawRect(x, y, x+length, y+1, paint);
        }
        paint.setAlpha(255); paint.setColorFilter(terrainLight);
        canvas.restore();
    }

    private void drawTree(Canvas canvas, Bitmap bitmap, float x, float y, float width,
                          float height, float time, float phase) {
        if (bitmap == null) return;
        float gust = (float)Math.pow(Math.max(0, Math.sin(time*.18+phase)), 6)*2.2f;
        int at = 0;
        for (int row = 0; row <= TREE_ROWS; row++) {
            float position = row/(float)TREE_ROWS;
            float flex = (1-position)*(1-position);
            float sway = (float)(Math.sin(time*.68+phase)*1.8 + Math.sin(time*1.15+phase)*.5);
            for (int column = 0; column <= TREE_COLS; column++) {
                treeMesh[at++] = column*bitmap.getWidth()/(float)TREE_COLS
                        + (sway+gust)*flex*bitmap.getWidth()/144f;
                treeMesh[at++] = position*bitmap.getHeight();
            }
        }
        canvas.save(); canvas.translate(x, y);
        canvas.scale(width/bitmap.getWidth(), height/bitmap.getHeight());
        paint.setColorFilter(terrainLight);
        paint.setFilterBitmap(true);
        canvas.drawBitmapMesh(bitmap, TREE_COLS, TREE_ROWS, treeMesh, 0, null, 0, paint);
        paint.setFilterBitmap(false);
        canvas.restore();
    }

    private void drawLeaves(Canvas canvas, float time) {
        for (int i = 0; i < 7; i++) {
            float life = (time*.032f+i*.143f)%1;
            int x = (int)(25+i*71-life*70+Math.sin(life*15+i)*12);
            int y = (int)(life*310);
            paint.setColor(i%2==0 ? 0xffc8792e : 0xffb94d27);
            paint.setAlpha((int)((.45f+.55f*sky.daylight)*200));
            canvas.drawRect(x, y, x+2, y+1, paint);
            canvas.drawRect(x+1, y-1, x+3, y+2, paint);
        }
        paint.setAlpha(255);
    }

    private void drawGhost(Canvas canvas, float time) {
        // A bounded, continuous wandering route; no teleport or per-frame bitmap creation.
        float x = 246 + (float)Math.sin(time*.055+.4)*142
                + (float)Math.sin(time*.13)*24;
        float y = 153 + (float)Math.sin(time*.075+1.6)*62
                + (float)Math.sin(time*1.8)*2.5f;
        paint.setColorFilter(null);
        paint.setAlpha((int)(176+50*(1-sky.daylight)));
        paint.setFilterBitmap(true);
        destination.set(x, y, x+16, y+19);
        canvas.drawBitmap(time%11>10.8?ghostBlink:ghost, null, destination, paint);
        paint.setFilterBitmap(false);
        paint.setAlpha(255);
    }

    private void drawHalloween(Canvas canvas, float time) {
        float night = 1-sky.daylight;
        if (night > .1f) {
            paint.setColor(Color.argb((int)(night*(35+7*Math.sin(time*2.1))), 255, 165, 49));
            canvas.drawRect(104, 298, 143, 326, paint);
        }
        // The pumpkin cutout keeps its candle warmth while the surrounding scene darkens.
        paint.setColorFilter(null);
        paint.setAlpha((int)(205+50*sky.daylight));
        destination.set(78, 275, 163, 332);
        canvas.drawBitmap(halloween, null, destination, paint);
        paint.setAlpha(255);
        if (night > .4f && time%53 < 9) {
            float phase = time%53;
            for (int i = 0; i < 2; i++) {
                int x = (int)(480-phase*26-i*17);
                int y = (int)(72+Math.sin(phase*1.5+i)*8+i*10);
                paint.setColor(0xff11182d);
                canvas.drawRect(x, y, x+3, y+2, paint);
                int flap = ((int)(time*4+i)%2==0) ? -2 : 1;
                canvas.drawRect(x-4, y+flap, x, y+flap+1, paint);
                canvas.drawRect(x+3, y+flap, x+7, y+flap+1, paint);
            }
        }
    }
}
