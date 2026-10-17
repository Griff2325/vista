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
import org.json.JSONObject;

/** Small reusable textures and native mesh motion, with no decoded frame sequence. */
public final class PixelSceneView extends View {
    private static final int WIDTH = 480, HEIGHT = 336;
    static final float WINDOW_LEFT=694, WINDOW_TOP=88, WINDOW_RIGHT=1858, WINDOW_BOTTOM=900;
    private static final int TREE_COLS = 18, TREE_ROWS = 24;
    private static final int FOREST_COLS = 48, FOREST_ROWS = 42;
    private static Bitmap[] cachedAssets;
    private static Bitmap[] cloudVariants;
    private static float cloudTravel, leafTravel;
    private static long weatherFrameAt;
    private WeatherScene weather=WeatherScene.neutral();
    private float cloudCover=.35f, wind=.14f, windDrift=1, rain, snow, haze;
    private long previousDrawAt;
    private long weatherReadMinute=Long.MIN_VALUE;
    private Shader hazeGradient;
    private final Paint precipitationPaint=new Paint();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint();
    private final RectF destination = new RectF();
    private final Path waterMask = new Path();
    private static final WaterMotion waterMotion = new WaterMotion();
    private static final float[] RIPPLE_CENTERS = {261,268, 302,266, 344,279, 389,274, 313,290, 369,296};
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rippleBounds = new RectF();
    private final Path silkWeb=makeSilkWeb();
    private final float[] treeMesh = new float[(TREE_COLS + 1) * (TREE_ROWS + 1) * 2];
    private final float[] forestMesh = new float[(FOREST_COLS + 1) * (FOREST_ROWS + 1) * 2];
    private final float[] stars = new float[72];
    private Bitmap terrain, leftTree, rightTree, cloud, halloween;
    private static final Bitmap ghost = makeGhost(false);
    private static final Bitmap ghostBlink = makeGhost(true);
    private static final Bitmap witch = makeWitch();
    private static final Bitmap cat = makeCat();
    private final GhostMotion[] ghosts={new GhostMotion(0),new GhostMotion(1),new GhostMotion(2)};
    private final ColorFilter ghostBlue=new LightingColorFilter(0xffdbeaff,0),ghostWarm=new LightingColorFilter(0xffffe8e0,0);
    private long lastFrameNanos;
    private Shader skyGradient;
    private ColorFilter terrainLight, cloudLight;
    private CincinnatiSky.State sky;
    private boolean active, released;
    private float immersion;
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
        ripplePaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStrokeWidth(.65f);
        ripplePaint.setStrokeCap(Paint.Cap.ROUND);
        Random random = new Random(314159);
        for (int i = 0; i < stars.length; i += 2) {
            stars[i] = random.nextInt(WIDTH);
            stars[i + 1] = 8 + random.nextInt(100);
        }
        // Keep ripple strokes inside the lake, with a dry margin at both banks.
        waterMask.moveTo(217, 254);
        waterMask.lineTo(326, 250);
        waterMask.lineTo(418, 261);
        waterMask.lineTo(447, 276);
        waterMask.lineTo(414, 294);
        waterMask.lineTo(348, 312);
        waterMask.lineTo(285, 300);
        waterMask.lineTo(238, 282);
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

    private static Bitmap sprite(Bitmap pixels){Bitmap result=Bitmap.createScaledBitmap(pixels,pixels.getWidth()*3,pixels.getHeight()*3,false);pixels.recycle();return result;}
    private static Path makeSilkWeb(){
        // Suspension points sampled from existing woody forks in the original tree sprite.
        float[] ax={44.63f,65.08f,95.86f,91.59f,55.86f,38.56f},ay={153.47f,137.06f,134.78f,186.97f,192.44f,172.84f};
        float cx=69,cy=165;float[] x=new float[12],y=new float[12];Path web=new Path();
        for(int i=0;i<12;i++){int a=i/2,b=(a+1)%6;if(i%2==0){x[i]=ax[a];y[i]=ay[a];}else{x[i]=(ax[a]+ax[b])*.5f*.92f+cx*.08f;y[i]=(ay[a]+ay[b])*.5f*.92f+cy*.08f;}}
        for(int i=0;i<12;i++){web.moveTo(cx,cy);web.quadTo((cx+x[i])*.5f+.4f*(float)Math.sin(i),(cy+y[i])*.5f,x[i],y[i]);}
        for(int ring=1;ring<=5;ring++){float radius=ring/5f;float firstX=0,firstY=0,previousX=0,previousY=0;
            for(int i=0;i<=12;i++){int n=i%12;float r=radius+(ring==5?0:.012f*(float)Math.sin(n*2.3+ring)),px=cx+(x[n]-cx)*r,py=cy+(y[n]-cy)*r;
                if(i==0){web.moveTo(px,py);firstX=px;firstY=py;}else{if(i==12){px=firstX;py=firstY;}web.quadTo((previousX+px)*.47f+cx*.06f,(previousY+py)*.47f+cy*.06f,px,py);}previousX=px;previousY=py;}
        }
        return web;
    }

    private static Bitmap makeWitch(){
        Bitmap b=Bitmap.createBitmap(48,30,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint();
        // A bent pointed hat, copper hair, plum cloak and a straw broom, facing right.
        p.setColor(0xffd7a35d);c.drawRect(3,23,45,25,p);p.setColor(0xff8e683d);c.drawRect(0,22,12,28,p);p.setColor(0xffc39450);c.drawRect(1,21,11,23,p);c.drawRect(0,27,9,29,p);c.drawRect(3,25,12,26,p);
        p.setColor(0xff473153);c.drawRect(24,3,28,5,p);c.drawRect(22,5,27,8,p);c.drawRect(20,8,29,12,p);c.drawRect(17,12,33,14,p);
        p.setColor(0xffb08449);c.drawRect(21,10,29,12,p);p.setColor(0xffb46a41);c.drawRect(18,14,25,20,p);
        p.setColor(0xffc0c899);c.drawRect(25,14,31,18,p);c.drawRect(30,15,33,17,p);p.setColor(0xff25303b);c.drawRect(29,14,30,15,p);
        p.setColor(0xff684e78);c.drawRect(20,18,28,21,p);c.drawRect(15,21,29,24,p);c.drawRect(10,23,27,25,p);c.drawRect(27,19,34,21,p);
        p.setColor(0xffc0c899);c.drawRect(33,20,35,23,p);p.setColor(0xff252c3b);c.drawRect(26,24,29,28,p);c.drawRect(28,27,34,29,p);
        return sprite(b);
    }
    private static Bitmap makeCat(){
        Bitmap b=Bitmap.createBitmap(18,17,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint();p.setColor(0xff273038);
        c.drawRect(5,1,7,6,p);c.drawRect(12,1,14,6,p);c.drawRect(4,5,15,11,p);c.drawRect(6,11,14,16,p);c.drawRect(4,15,16,17,p);
        p.setColor(0xff4a494d);c.drawRect(7,11,12,16,p);return sprite(b);
    }

    /** Four shared, tiny stepped silhouettes; textures are cached, never allocated per frame. */
    private static Bitmap makeCloud(int type) {
        Bitmap pixels=Bitmap.createBitmap(64,24,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(pixels);Paint p=new Paint();
        p.setColor(0xffe5e8df);
        if(type==0){
            c.drawRect(4,13,59,20,p);c.drawRect(9,9,55,19,p);c.drawRect(15,5,32,18,p);c.drawRect(20,2,29,17,p);c.drawRect(36,8,48,18,p);
        }else if(type==1){
            c.drawRect(3,12,61,18,p);c.drawRect(8,8,19,17,p);c.drawRect(23,6,37,17,p);c.drawRect(43,9,55,17,p);c.drawRect(27,3,33,16,p);
        }else if(type==2){
            c.drawRect(4,10,28,12,p);c.drawRect(17,8,48,10,p);c.drawRect(34,6,60,8,p);c.drawRect(8,15,37,17,p);c.drawRect(28,13,55,15,p);
        }else{
            c.drawRect(0,9,64,20,p);c.drawRect(4,7,25,19,p);c.drawRect(20,5,48,19,p);c.drawRect(43,8,62,19,p);
        }
        p.setColor(type==3?0xffb7c2c4:0xffc8d2d0);
        c.drawRect(11,17,54,20,p);if(type!=2)c.drawRect(5,19,59,21,p);
        Bitmap result=Bitmap.createScaledBitmap(pixels,192,72,false);pixels.recycle();return result;
    }

    private void loadAssets() {
        try {
            final Bitmap[] assets;
            synchronized (PixelSceneView.class) {
                if (cachedAssets == null) cachedAssets = new Bitmap[] {movingSprite("terrain", WIDTH, HEIGHT),
                        movingSprite("tree-left", 144, 192), movingSprite("tree-right", 112, 152),
                        movingSprite("cloud", 192, 64), load("halloween", 100, 67)};
                if(cloudVariants==null)cloudVariants=new Bitmap[]{makeCloud(0),makeCloud(1),makeCloud(2),makeCloud(3)};
                assets = cachedAssets;
            }
            final Bitmap ground=assets[0], left=assets[1], right=assets[2], clouds=assets[3], pumpkins=assets[4];
            handler.post(() -> {
                if (released) return;
                terrain = ground; leftTree = left; rightTree = right;
                cloud = clouds; halloween = pumpkins;
                long bytes = ground.getAllocationByteCount() + left.getAllocationByteCount()
                        + right.getAllocationByteCount() + clouds.getAllocationByteCount()
                        + pumpkins.getAllocationByteCount() + ghost.getAllocationByteCount()
                        + ghostBlink.getAllocationByteCount()+witch.getAllocationByteCount()+cat.getAllocationByteCount();
                for(Bitmap variant:cloudVariants)bytes+=variant.getAllocationByteCount();
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

    public void setImmersion(float value) { immersion=Math.max(0,Math.min(1,value)); invalidate(); }
    public float immersion() { return immersion; }
    public float windowLeft() { return WINDOW_LEFT*(1-immersion); }
    public float windowTop() { return WINDOW_TOP*(1-immersion); }
    public float windowRight() { return WINDOW_RIGHT+(1920-WINDOW_RIGHT)*immersion; }
    public float windowBottom() { return WINDOW_BOTTOM+(1080-WINDOW_BOTTOM)*immersion; }

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
        // Consume the shared cache at most once/minute. This View performs no networking.
        long weatherMinute=System.currentTimeMillis()/60000;
        boolean firstWeather=weatherReadMinute==Long.MIN_VALUE;
        if(weatherReadMinute!=weatherMinute){
        weatherReadMinute=weatherMinute;
        JSONObject cached=WidgetRepository.get(getContext()).weather();
        weather=cached==null?WeatherScene.neutral():WeatherScene.from(
                cached.optString("sceneSummary",cached.optString("summary")),
                cached.optDouble("sceneWindMps",Double.NaN),cached.optDouble("sceneWindDirection",Double.NaN),
                cached.optLong("sceneObservedAt",cached.optLong("at")),
                cached.optBoolean("sceneStale",cached.optBoolean("stale",true)),
                cached.optString("sceneSource","forecast"),System.currentTimeMillis());
        if(firstWeather){cloudCover=weather.clouds;wind=weather.wind;windDrift=weather.drift;rain=weather.rain;snow=weather.snow;haze=weather.haze;}
        }
        float day = sky.daylight, warm = sky.twilight * .55f*(1-weather.clouds*.7f);
        int top = blend(blend(0xff091630, 0xff5a92b6, day), 0xff88627e, warm);
        int bottom = blend(blend(0xff29395c, 0xfff1d7b0, day), 0xffefa57c, warm);
        float overcast=weather.clouds*.46f;
        top=blend(top,blend(0xff182337,0xff8d9fa7,day),overcast);
        bottom=blend(bottom,blend(0xff293648,0xffbbc4bf,day),overcast);
        skyGradient = new LinearGradient(0, 0, 0, 180, top, bottom, Shader.TileMode.CLAMP);
        int mist=Color.argb((int)((weather.haze+weather.clouds*.07f)*85),
                (int)(53+86*day),(int)(69+88*day),(int)(90+77*day));
        hazeGradient=new LinearGradient(0,140,0,265,new int[]{0x008b9da7,mist,0x008b9da7},new float[]{0,.6f,1},Shader.TileMode.CLAMP);
        terrainLight = new ColorMatrixColorFilter(new float[] {
                .24f + .76f * day*(1-weather.clouds*.13f) + warm * .12f, 0, 0, 0, (1-day)*4,
                0, .32f + .68f * day*(1-weather.clouds*.1f), 0, 0, (1-day)*7,
                0, 0, .54f + .46f * day*(1-weather.clouds*.06f) - warm*.08f, 0, (1-day)*14,
                0, 0, 0, 1, 0 });
        cloudLight = new ColorMatrixColorFilter(new float[] {
                .38f+.62f*day, 0, 0, 0, 0,
                0, .47f+.53f*day, 0, 0, 0,
                0, 0, .72f+.28f*day, 0, 0,
                0, 0, 0, 1, 0 });
        Log.i("SolScene", "Lighting=" + sky.phase + "; October accents=" + sky.halloween
                + "; preview=" + (previewTime > 0)+"; weather="+weather.source+"; stale="+weather.stale);
    }

    private static int blend(int from, int to, float amount) {
        return Color.rgb(Math.round(Color.red(from) + (Color.red(to)-Color.red(from))*amount),
                Math.round(Color.green(from)+(Color.green(to)-Color.green(from))*amount),
                Math.round(Color.blue(from)+(Color.blue(to)-Color.blue(from))*amount));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Shared monotonic phase makes the Activity -> system dream handoff continuous.
        long monotonic=SystemClock.elapsedRealtime();
        float time = (monotonic%86400000L) / 1000f;
        float delta=previousDrawAt==0?0:Math.min(.1f,(monotonic-previousDrawAt)/1000f);previousDrawAt=monotonic;
        float ease=Math.min(1,delta*.22f);
        cloudCover+=(weather.clouds-cloudCover)*ease;wind+=(weather.wind-wind)*ease;
        windDrift+=(weather.drift-windDrift)*ease;rain+=(weather.rain-rain)*ease;
        snow+=(weather.snow-snow)*ease;haze+=(weather.haze-haze)*ease;
        waterMotion.advance(monotonic, wind);
        // One shared phase keeps cloud positions continuous across Home and Dream views.
        if(weatherFrameAt==0){cloudTravel=(monotonic%86400000L)/1000f*.7f;leafTravel=(monotonic%86400000L)/1000f*.02f;}
        float travelDelta=Math.min(.1f,(monotonic-weatherFrameAt)/1000f);
        if(weatherFrameAt>0){cloudTravel+=travelDelta*(.45f+wind*2.5f)*windDrift;leafTravel=wrap(leafTravel+travelDelta*(.014f+wind*.024f),1);}
        weatherFrameAt=monotonic;
        canvas.save();
        canvas.scale(getWidth()/1920f, getHeight()/1080f);
        float l=windowLeft(),t=windowTop(),w=windowRight()-l,h=windowBottom()-t;
        canvas.clipRect(l,t,l+w,t+h);
        // Cover without stretching the pixel artwork; keep the foreground bank in view.
        float scale=Math.max(w/WIDTH,h/HEIGHT);
        canvas.translate(l+(w-WIDTH*scale)*.5f,t+h-HEIGHT*scale);
        canvas.scale(scale,scale);
        paint.setColorFilter(null); paint.setAlpha(255); paint.setShader(skyGradient);
        canvas.drawRect(0, 0, WIDTH, HEIGHT, paint); paint.setShader(null);
        drawSky(canvas, time);
        drawClouds(canvas);

        paint.setColorFilter(terrainLight);
        if (terrain != null) {
            destination.set(0, 0, WIDTH, HEIGHT);
            if(immersion>.01f) drawForest(canvas,time);
            else canvas.drawBitmap(terrain, null, destination, paint);
            drawWater(canvas);
            drawHaze(canvas);
        }
        if (sky.halloween) {drawGhost(canvas, monotonic/1000.0);drawHauntings(canvas,time);}
        drawTree(canvas, leftTree, -74, 1, 244, 330, time, 0);
        drawTree(canvas, rightTree, 387, 130, 140, 205, time, 2.3f);
        paint.setColorFilter(null);
        drawLeaves(canvas, time);
        if(immersion>.01f) drawFireflies(canvas,time);
        if (sky.halloween && halloween != null) drawHalloween(canvas, time);
        drawPrecipitation(canvas,time);
        paint.setColorFilter(null); paint.setAlpha(255);
        canvas.restore();
    }

    private void drawSky(Canvas canvas, float time) {
        canvas.save();canvas.translate(0,66*immersion);
        paint.setColorFilter(null);
        float dark = (1-sky.daylight)*(1-cloudCover*.95f);
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
        if (sky.elevation > -3 && cloudCover<.8f) {
            float x = (float) Math.max(30, Math.min(450, (sky.hourAngle+100)/200*WIDTH));
            float y = (float) Math.max(12, 115-Math.max(0, sky.elevation)*1.7);
            paint.setColor(Color.argb((int)(255*(1-cloudCover*.85f)),255,228,166));
            int xx = Math.round(x), yy = Math.round(y);
            canvas.drawRect(xx-7, yy-4, xx+8, yy+5, paint);
            canvas.drawRect(xx-4, yy-7, xx+5, yy+8, paint);
        }
        canvas.restore();
    }

    private void drawForest(Canvas canvas,float time) {
        int at=0;
        for(int row=0;row<=FOREST_ROWS;row++) {
            float y=row*HEIGHT/(float)FOREST_ROWS;
            float band=Math.max(0,Math.min(1,(y-182)/57f));
            float flex=y>182&&y<239?(float)Math.pow(Math.sin(band*Math.PI),2):0;
            for(int col=0;col<=FOREST_COLS;col++) {
                float x=col*WIDTH/(float)FOREST_COLS;
                // Confine distant-tree flex to the wooded center; keep rocky side banks still.
                float edge=Math.max(0,Math.min(1,(x-100)/65f))*Math.max(0,Math.min(1,(392-x)/48f));
                forestMesh[at++]=x+(float)Math.sin(time*.62+x*.065)*flex*edge*immersion*(.3f+wind*1.7f);
                forestMesh[at++]=y;
            }
        }
        paint.setFilterBitmap(true);
        canvas.drawBitmapMesh(terrain,FOREST_COLS,FOREST_ROWS,forestMesh,0,null,0,paint);
        paint.setFilterBitmap(false);
    }

    private void drawFireflies(Canvas canvas,float time) {
        float dark=1-sky.daylight;
        if(dark<.3f)return;
        for(int i=0;i<12;i++) {
            float x=30+i*37+(float)Math.sin(time*.35+i)*9;
            float y=258+(i*23)%65+(float)Math.sin(time*.48+i*2)*5;
            int alpha=(int)(Math.max(0,Math.sin(time*.8+i*1.7))*130*dark*immersion);
            paint.setColor(Color.argb(alpha,227,221,141));canvas.drawRect(x,y,x+1,y+1,paint);
        }
        paint.setAlpha(255);
    }

    private void drawClouds(Canvas canvas) {
        if(cloud==null||cloudVariants==null)return;
        paint.setColorFilter(cloudLight);paint.setFilterBitmap(true);
        // Different silhouettes, sizes, altitudes and layer speeds; bounded at eleven sprites.
        for(int i=0;i<11;i++){
            float visibility=Math.max(0,Math.min(1,(cloudCover*12-i)*.9f));
            if(visibility<.01f)continue;
            int type=i%5;
            if(cloudCover>.8f&&i>4)type=4;
            Bitmap texture=type==0?cloud:cloudVariants[type-1];
            float width=type==3?110+i%3*31:type==4?235+i%2*65:70+(i*47)%133;
            float height=type==3?width*.17f:type==4?width*.23f:width*.34f;
            float speed=.5f+(i%4)*.22f;
            float x=wrap(68+i*137+cloudTravel*speed,850)-220;
            float y=4+(i*29)%85+36*immersion;
            paint.setAlpha((int)(visibility*(type==3?178:218)));
            destination.set(x,y,x+width,y+height);canvas.drawBitmap(texture,null,destination,paint);
        }
        paint.setFilterBitmap(false);paint.setColorFilter(null);paint.setAlpha(255);
    }

    private static float wrap(float value,float range){return value-(float)Math.floor(value/range)*range;}

    private void drawHaze(Canvas canvas){
        float amount=haze+cloudCover*.07f;
        if(amount<.015f)return;
        paint.setColorFilter(null);paint.setShader(hazeGradient);paint.setAlpha(255);
        canvas.drawRect(0,140,WIDTH,265,paint);paint.setShader(null);paint.setColorFilter(terrainLight);
    }

    private void drawPrecipitation(Canvas canvas,float time){
        if(rain<.015f&&snow<.015f)return;
        precipitationPaint.setAntiAlias(false);precipitationPaint.setStrokeWidth(.65f);
        int count=(int)(rain*65+snow*44);
        float weatherTime=time;
        for(int i=0;i<count;i++){
            boolean flake=snow>.015f&&(rain<.015f||i%3!=0);
            float speed=flake?10+(i%4)*2:114+(i%5)*13;
            float y=wrap(i*37+weatherTime*speed,HEIGHT+20)-10;
            float x=wrap(i*97+weatherTime*windDrift*(flake?5+wind*10:14+wind*25)+(flake?(float)Math.sin(weatherTime*.8+i)*4:0),WIDTH+30)-15;
            precipitationPaint.setColor(Color.argb((int)((flake?125:75)+sky.daylight*55),flake?236:173,flake?240:195,flake?243:208));
            if(flake)canvas.drawRect(x,y,x+1.3f,y+1.3f,precipitationPaint);
            else canvas.drawLine(x,y,x+windDrift*(1+wind*3),y+5+wind*3,precipitationPaint);
        }
    }

    private void drawWater(Canvas canvas) {
        canvas.save(); canvas.clipPath(waterMask);
        // Stable artwork underneath: only shallow, broken rings expand and fade.
        // Six systems / two rings maximum, no bitmap resampling or frame allocations.
        for (int i = 0; i < RIPPLE_CENTERS.length/2; i++) {
            float visibility = i<4 ? 1 : immersion;
            if (visibility<.01f) continue;
            float x=RIPPLE_CENTERS[i*2], y=RIPPLE_CENTERS[i*2+1];
            float perspective=.7f+(y-260)/65;
            for (int ring=0; ring<2; ring++) {
                float progress=waterMotion.rippleProgress(i,ring);
                int alpha=(int)(WaterMotion.rippleOpacity(progress)*visibility*(58+sky.daylight*74));
                if(alpha<1) continue;
                float radius=(4+progress*(18+i%3*5))*perspective;
                float height=radius*.20f;
                rippleBounds.set(x-radius,y-height,x+radius,y+height);
                ripplePaint.setColor(Color.argb(alpha,212,228,219));
                canvas.drawArc(rippleBounds,190+i*9,128,false,ripplePaint);
                ripplePaint.setColor(Color.argb(alpha*2/3,114,156,170));
                canvas.drawArc(rippleBounds,15+i*7,112,false,ripplePaint);
            }
        }
        canvas.restore();
    }

    private void drawTree(Canvas canvas, Bitmap bitmap, float x, float y, float width,
                          float height, float time, float phase) {
        if (bitmap == null) return;
        float gust = (float)Math.pow(Math.max(0, Math.sin(time*.18+phase)), 6)*wind*4*windDrift;
        int at = 0;
        for (int row = 0; row <= TREE_ROWS; row++) {
            float position = row/(float)TREE_ROWS;
            float flex = (1-position)*(1-position);
            float sway = (float)(Math.sin(time*.68+phase)*1.8 + Math.sin(time*1.15+phase)*.5);
            for (int column = 0; column <= TREE_COLS; column++) {
                treeMesh[at++] = column*bitmap.getWidth()/(float)TREE_COLS
                        + (sway*(.18f+wind*1.6f)+gust)*(1+.35f*immersion)*flex*bitmap.getWidth()/144f;
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
        // Integrate speed, rather than multiplying absolute uptime by changing wind.
        // Float positions and feathered lifetime edges avoid pixel jumps and respawn pops.
        paint.setAntiAlias(true);
        for (int i = 0; i < 7+(int)(9*immersion); i++) {
            float life=wrap(leafTravel+i*.143f,1),edge=Math.min(1,Math.min(life/.08f,(1-life)/.12f));
            float x=(25+i*71)%WIDTH+life*70*windDrift+(float)Math.sin(life*12+i)*(4+wind*8);
            float y=-12+life*360+(float)Math.sin(time*.7+i)*1.2f;
            paint.setColor(i%2==0?0xffc8792e:0xffb94d27);paint.setAlpha((int)((.45f+.55f*sky.daylight)*200*edge));
            canvas.save();canvas.translate(x,y);canvas.rotate((float)Math.sin(time*1.1+i*2)*28+i*41);
            canvas.drawRect(-1.5f,-.5f,1.5f,.5f,paint);canvas.drawRect(-.5f,-1,1,1,paint);canvas.restore();
        }
        paint.setAlpha(255);paint.setAntiAlias(false);
    }

    private void drawGhost(Canvas canvas,double seconds){
        for(int i=0;i<ghosts.length;i++){
            GhostMotion spirit=ghosts[i];spirit.update(seconds);if(spirit.opacity<=0)continue;
            float width=i==1?13.5f:i==2?17:16,height=i==1?18:i==2?20:19;
            paint.setColorFilter(i==1?ghostBlue:i==2?ghostWarm:null);paint.setAlpha((int)(spirit.opacity*(176+50*(1-sky.daylight))));paint.setFilterBitmap(true);
            destination.set(spirit.x,spirit.y,spirit.x+width,spirit.y+height);
            canvas.drawBitmap((seconds+i*1.7)%11.3>11.05?ghostBlink:ghost,null,destination,paint);
        }
        paint.setFilterBitmap(false);paint.setColorFilter(null);paint.setAlpha(255);
    }

    private void drawHauntings(Canvas canvas,float time){
        // One crossing every 2m 30s. Smooth bob and bank; no frame sequence or video.
        float fly=time%150;
        if(fly<24){float x=-58+fly/24*590,y=57+immersion*58+(float)Math.sin(fly*.27)*7;boolean reverse=((int)(time/150)%2)==1;
            if(reverse)x=WIDTH-x-42;
            // A sparse trail follows earlier broom positions and fades toward the tail.
            for(int i=1;i<=12;i++){float past=fly-i*.13f;if(past<0)continue;float sx=-58+past/24*590+3,sy=57+immersion*58+(float)Math.sin(past*.27)*7+24+(float)Math.sin(time*2+i)*2;
                if(reverse)sx=WIDTH-sx;float radius=i%3==0?.9f:.55f;paint.setColor(i%2==0?0xffdbeac0:0xfff7d893);paint.setAlpha((int)((1-i/13f)*155*(.65f+.35f*Math.sin(time*4+i))));
                canvas.drawRect(sx-radius,sy,sx+radius,sy+.6f,paint);canvas.drawRect(sx-.3f,sy-radius,sx+.3f,sy+radius,paint);}
            paint.setAlpha(255);canvas.save();canvas.translate(x+21,y+13);if(reverse)canvas.scale(-1,1);canvas.rotate((float)Math.sin(fly*.45)*3);
            paint.setColorFilter(null);paint.setAlpha((int)(Math.min(1,Math.min(fly,(24-fly)))*245));paint.setFilterBitmap(true);destination.set(-24,-15,24,15);canvas.drawBitmap(witch,null,destination,paint);paint.setFilterBitmap(false);paint.setAlpha(255);canvas.restore();}
    }

    private void drawSpider(Canvas canvas,float time){
        float gust=(float)Math.pow(Math.max(0,Math.sin(time*.18)),6)*wind*4*windDrift;
        float sway=(float)(Math.sin(time*.68)*1.8+Math.sin(time*1.15)*.5);
        float shift=(sway*(.18f+wind*1.6f)+gust)*(1+.35f*immersion)*.24f*244/144f;
        float anchor=91.59f+shift,x=anchor+(float)Math.sin(time*.48)*2.4f,y=218+(float)Math.sin(time*.13)*7;
        paint.setColorFilter(null);paint.setColor(0xffd3ded8);paint.setAlpha((int)(105+50*sky.daylight));paint.setStrokeWidth(.22f);paint.setAntiAlias(true);paint.setStyle(Paint.Style.STROKE);
        canvas.save();canvas.translate(shift,0);canvas.drawPath(silkWeb,paint);canvas.restore();
        canvas.drawLine(anchor,186.97f,x,y-6,paint);paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
        canvas.save();canvas.translate(x,y);canvas.scale(1.9f,1.9f);
        paint.setColor(0xff18212c);paint.setStrokeWidth(.8f);
        for(int side=-1;side<=1;side+=2)for(int leg=0;leg<3;leg++){float bend=(float)Math.sin(time*2+leg)*.6f,dy=(leg-1)*2;
            canvas.drawLine(side*1.5f,dy*.5f,side*4,dy+bend,paint);canvas.drawLine(side*4,dy+bend,side*6,dy+2,paint);}
        paint.setAntiAlias(false);canvas.drawRect(-2,-3,2,2,paint);canvas.drawRect(-1,2,1,3,paint);
        paint.setColor(0xff515062);canvas.drawRect(-1,-2,1,0,paint);paint.setColor(0xffe3c890);canvas.drawRect(-1.3f,-1,-.5f,0,paint);canvas.drawRect(.5f,-1,1.3f,0,paint);canvas.restore();
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
        drawSpider(canvas,time);
        // A sleepy black cat rests on the near-bank rocks.
        paint.setColor(0xff273038);paint.setStrokeWidth(1.5f);float tail=(float)Math.sin(time*.42)*2;canvas.drawLine(191,324,197+tail,321,paint);canvas.drawLine(197+tail,321,198+tail,317,paint);
        destination.set(175,308,193,325);paint.setFilterBitmap(true);canvas.drawBitmap(cat,null,destination,paint);paint.setFilterBitmap(false);
        if(time%13<12.6f){paint.setColor(0xffc5cc81);canvas.drawRect(181,315,183,316,paint);canvas.drawRect(186,315,188,316,paint);}
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
