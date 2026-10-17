package com.griffin.solhome;

import android.service.dreams.DreamService;
import android.util.Log;
import android.widget.FrameLayout;

/** The system owns inactivity/playback policy and returns to the previously active app. */
public final class SolDreamService extends DreamService {
    private PixelSceneView scene;
    private AmbientWidgetsView widgets;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(0xff17242a);
        scene=new PixelSceneView(this);scene.setImmersion(1);
        widgets=new AmbientWidgetsView(this);
        root.addView(scene);root.addView(widgets);setContentView(root);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();scene.start();widgets.start();Log.i("SolAmbient","System screensaver started");
    }

    @Override public void onDreamingStopped() {
        scene.stop();widgets.stop();Log.i("SolAmbient","System screensaver stopped");super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if(scene!=null)scene.release();if(widgets!=null)widgets.stop();super.onDetachedFromWindow();
    }
}
