package com.griffin.solhome;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Focusable overlay without an Activity transition. Shell-only external entry. */
public final class PicturePanelService extends Service {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable expire=this::stopSelf;
    private WindowManager windows;
    private LinearLayout panel;
    private final BroadcastReceiver screenOff=new BroadcastReceiver(){public void onReceive(Context c,Intent i){stopSelf();}};
    public static void show(Context context){context.startForegroundService(new Intent(context,PicturePanelService.class));}
    public static void dismiss(Context context){context.stopService(new Intent(context,PicturePanelService.class));}
    public static void nativeSettings(Context context){context.startActivity(new Intent().setClassName("com.amazon.tv.launcher","com.amazon.tv.launcher.ui.MainSettingsActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    @Override public void onCreate(){
        super.onCreate();
        NotificationManager notifications=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notifications.createNotificationChannel(new NotificationChannel("vista_picture","Picture controls",NotificationManager.IMPORTANCE_LOW));
        startForeground(73,new Notification.Builder(this,"vista_picture").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Vista picture controls").setContentText("Close the panel to return to your app").build());
        registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&Settings.ACTION_SETTINGS.equals(intent.getAction())){
            stopSelf();try{nativeSettings(this);}catch(RuntimeException failed){Toast.makeText(this,"Could not open TV settings",Toast.LENGTH_SHORT).show();}return START_NOT_STICKY;
        }
        if(panel!=null){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"Vista picture controls need display-over-apps access",Toast.LENGTH_LONG).show();stopSelf();return START_NOT_STICKY;}
        try{
            open();
        }catch(RuntimeException failed){Toast.makeText(this,"Could not open picture controls. Hold Home for TV settings.",Toast.LENGTH_LONG).show();stopSelf();}
        return START_NOT_STICKY;
    }
    private void open(){
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        panel=new LinearLayout(this){
            @Override public boolean dispatchKeyEvent(KeyEvent e){
                arm();int code=e.getKeyCode();
                if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_SETTINGS||code==KeyEvent.KEYCODE_BUTTON_6){if(e.getAction()==KeyEvent.ACTION_UP)stopSelf();return true;}
                return super.dispatchKeyEvent(e);
            }
        };
        panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(22),dp(24),dp(18));panel.setBackground(surface(0xfa182825,18));panel.setElevation(dp(12));
        panel.addView(text("VISTA  /  PICTURE",12,0xffb5c1b7));
        TextView title=text("TV settings",25,0xfff2f0e7);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(0,dp(8),0,dp(4));panel.addView(title);
        TextView hint=text("Hold Settings for Panasonic’s picture menu. Hold Home for full TV settings.",14,0xffb5c1b7);hint.setPadding(0,dp(14),0,dp(10));panel.addView(hint);
        panel.addView(button("All TV settings  ›",()->{stopSelf();try{nativeSettings(this);}catch(RuntimeException failed){Toast.makeText(this,"Hold Home for TV settings",Toast.LENGTH_SHORT).show();}}));
        panel.addView(button("Close",this::stopSelf));
        WindowManager.LayoutParams layout=new WindowManager.LayoutParams(dp(320),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;layout.x=dp(24);layout.windowAnimations=0;layout.setTitle("Vista Picture");panel.setSystemUiVisibility(5894);windows.addView(panel,layout);panel.getChildAt(panel.getChildCount()-2).requestFocus();arm();
    }
    private void arm(){main.removeCallbacks(expire);main.postDelayed(expire,45000);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
    private GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String label,Runnable action){
        Button b=new Button(this);b.setText(label);b.setTextSize(15);b.setTextColor(0xfff2f0e7);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);b.setPadding(dp(14),0,dp(14),0);b.setStateListAnimator(null);
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_focused},surface(0xff596b59,10));states.addState(new int[]{android.R.attr.state_pressed},surface(0xff596b59,10));states.addState(new int[]{},surface(0xff293c33,10));states.setEnterFadeDuration(0);states.setExitFadeDuration(0);b.setBackground(states);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.topMargin=dp(8);b.setLayoutParams(p);b.setOnClickListener(v->{arm();action.run();});return b;
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public void onDestroy(){
        main.removeCallbacksAndMessages(null);if(panel!=null&&panel.isAttachedToWindow())windows.removeViewImmediate(panel);unregisterReceiver(screenOff);stopForeground(true);super.onDestroy();
    }
}
