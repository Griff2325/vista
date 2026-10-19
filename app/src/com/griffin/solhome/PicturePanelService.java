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
import java.util.concurrent.*;

/** Focusable overlay without an Activity transition. Shell-only external entry. */
public final class PicturePanelService extends Service {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable expire=this::stopSelf;
    private WindowManager windows;
    private LinearLayout panel;
    private ScrollView scroll;
    private TextView status;
    private final Button[] adjustments=new Button[3];
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Bundle values,opening;
    private boolean busy;
    private IBinder source;
    private boolean sourceBound;
    private final ServiceConnection sourceConnection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder remote){source=remote;if(panel!=null)read();}
        public void onServiceDisconnected(ComponentName name){source=null;if(status!=null)status.setText("Picture source unavailable; reopen the panel");}
    };
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
        try{sourceBound=bindService(new Intent().setClassName("com.mediatek.customerpq","com.mediatek.customerpq.CustomerPqService"),sourceConnection,BIND_AUTO_CREATE);}catch(RuntimeException unavailable){sourceBound=false;}
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
        TextView title=text("Picture controls",25,0xfff2f0e7);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));title.setPadding(0,dp(8),0,dp(4));panel.addView(title);
        TextView hint=text("Left / right adjusts by 5. These apply to current playback; native picture modes can replace them.",13,0xffb5c1b7);hint.setPadding(0,dp(8),0,dp(6));panel.addView(hint);
        status=text("Reading the TV…",13,0xffb5c1b7);panel.addView(status);
        String[] labels={"Panel brightness","Contrast","Color"};
        for(int i=0;i<adjustments.length;i++){
            final int index=i;Button row=button(labels[i]+"  ···",()->Toast.makeText(this,"Use left / right to adjust",Toast.LENGTH_SHORT).show());
            row.setOnKeyListener((v,key,event)->{
                if(key!=KeyEvent.KEYCODE_DPAD_LEFT&&key!=KeyEvent.KEYCODE_DPAD_RIGHT)return false;
                if(event.getAction()==KeyEvent.ACTION_DOWN&&!busy&&values!=null&&values.getBoolean("writable")){
                    String name=PicturePolicy.KEYS[index];int value=Math.max(0,Math.min(100,values.getInt(name)+(key==KeyEvent.KEYCODE_DPAD_LEFT?-5:5)));
                    if(value!=values.getInt(name))change(name,value);
                }
                arm();return true;
            });adjustments[i]=row;panel.addView(row);
        }
        panel.addView(button("Restore opening values",()->{if(!busy&&opening!=null&&values!=null)adjust(null,0);}));
        TextView modes=text("Settings opens Panasonic picture modes. Hold Home for full TV settings.",13,0xffb5c1b7);modes.setPadding(0,dp(12),0,0);panel.addView(modes);
        panel.addView(button("All TV settings  ›",()->{stopSelf();try{nativeSettings(this);}catch(RuntimeException failed){Toast.makeText(this,"Hold Home for TV settings",Toast.LENGTH_SHORT).show();}}));
        panel.addView(button("Close",this::stopSelf));
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(panel);
        WindowManager.LayoutParams layout=new WindowManager.LayoutParams(dp(350),Math.min(dp(510),getResources().getDisplayMetrics().heightPixels-dp(48)),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;layout.x=dp(24);layout.windowAnimations=0;layout.setTitle("Vista Picture");panel.setSystemUiVisibility(5894);windows.addView(scroll,layout);adjustments[0].requestFocus();arm();read();
    }
    private void read(){if(source!=null)adjust("read",0);else status.setText(sourceBound?"Connecting to the TV…":"Picture source service unavailable");}
    private void change(String key,int value){adjust(key,value);}
    private void adjust(String key,int value){
        if(busy)return;busy=true;status.setText(key==null?"Restoring opening values…":"read".equals(key)?"Reading the TV…":"Applying…");
        IBinder remote=source;String mode=values==null?null:values.getString("mode");Bundle saved=opening==null?null:new Bundle(opening);
        worker.execute(()->{
            Bundle result=null;String error=null;
            try{
                if(key==null){
                    if(saved==null||!saved.getString("mode").equals(mode))throw new IllegalStateException("Picture mode changed; reopen the panel");
                    for(String name:PicturePolicy.KEYS)result=PictureBridgeProvider.request(name,saved.getInt(name),mode,remote);
                }else result=PictureBridgeProvider.request(key,value,mode,remote);
            }catch(Exception failed){error=failed.getMessage();}
            final Bundle response=result;final String failure=error;
            main.post(()->{
                busy=false;if(panel==null)return;
                if(failure!=null){status.setText(failure);return;}
                values=response;if(opening==null)opening=new Bundle(response);
                String[] labels={"Panel brightness","Contrast","Color"};
                for(int i=0;i<adjustments.length;i++)adjustments[i].setText(labels[i]+"     ‹  "+response.getInt(PicturePolicy.KEYS[i])+"  ›");
                status.setText(response.getBoolean("writable")?"Live TV values · current playback":"Use Panasonic settings for this input or HDR");
            });
        });
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
        worker.shutdownNow();if(sourceBound)unbindService(sourceConnection);source=null;main.removeCallbacksAndMessages(null);if(scroll!=null&&scroll.isAttachedToWindow())windows.removeViewImmediate(scroll);panel=null;unregisterReceiver(screenOff);stopForeground(true);super.onDestroy();
    }
}
