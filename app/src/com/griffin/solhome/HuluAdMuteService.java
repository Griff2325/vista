package com.griffin.solhome;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/** Optional local-only Hulu badge detection. No images, text or history are retained. */
public final class HuluAdMuteService extends AccessibilityService {
  private static HuluAdMuteService instance;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final HuluMutePolicy policy=new HuluMutePolicy();
  private AudioManager audio;
  private boolean running,pending;
  private int generation;
  private long delay=1000;
  private final Runnable tick=()->check();
  private final Runnable timeout=()->{if(pending){generation++;pending=false;release();schedule();}};
  private final BroadcastReceiver volume=new BroadcastReceiver(){
    @Override public void onReceive(Context context,Intent intent){if(intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE",-1)==AudioManager.STREAM_MUSIC)policy.volumeChanged(intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE",-1));}
  };

  static boolean enabled(Context context){
    String value=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    ComponentName component=new ComponentName(context,HuluAdMuteService.class);
    if(value!=null)for(String item:value.split(":"))if(component.equals(ComponentName.unflattenFromString(item)))return true;
    return false;
  }
  static void show(Activity activity){
    if(Build.VERSION.SDK_INT<30){new AlertDialog.Builder(activity).setTitle("Hulu ad muting").setMessage("Requires Android 11 or newer.").setPositiveButton("OK",null).show();return;}
    boolean on=enabled(activity);
    new AlertDialog.Builder(activity).setTitle("Hulu ad muting · "+(on?"On":"Off"))
      .setMessage("Mutes while Hulu shows its yellow Ad badge. Sound returns after the badge disappears. Ad breaks still take their normal time.\n\nAndroid accessibility grants broad screen access. Vista checks only the small badge area while Hulu is foreground, processes it locally, and saves or sends no screenshots. Volume changes override the current break. Hulu UI changes can stop detection.")
      .setPositiveButton(on?"Turn off":"Enable Android access",(dialog,which)->{
        if(on){activity.sendBroadcast(new Intent(activity,DisableReceiver.class));return;}
        try{activity.startActivity(new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME,new ComponentName(activity,HuluAdMuteService.class)));}
        catch(Exception e){try{activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Exception unavailable){new AlertDialog.Builder(activity).setMessage("This Fire TV does not expose Android's accessibility settings. Enable Vista — Hulu ad muting through the TV's authorized development setup.").setPositiveButton("OK",null).show();}}
      }).setNegativeButton("Close",null).show();
  }
  @Override protected void onServiceConnected(){instance=this;audio=(AudioManager)getSystemService(AUDIO_SERVICE);running=true;registerReceiver(volume,new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));handler.post(tick);}
  @Override public void onAccessibilityEvent(AccessibilityEvent event){
    if(!running)return;
    if(!huluForeground()){handler.removeCallbacks(tick);release();}
    else if(!pending){handler.removeCallbacks(tick);handler.post(tick);}
  }
  @Override public void onInterrupt(){release();}
  @Override public void onDestroy(){running=false;generation++;handler.removeCallbacksAndMessages(null);release();try{unregisterReceiver(volume);}catch(IllegalArgumentException ignored){}instance=null;super.onDestroy();}
  private boolean huluForeground(){
    AccessibilityNodeInfo root=getRootInActiveWindow();
    if(root==null)return false;
    try{return "com.hulu.plus".contentEquals(root.getPackageName());}finally{root.recycle();}
  }
  private void schedule(){if(running){handler.removeCallbacks(tick);handler.postDelayed(tick,delay);}}
  private void check(){
    if(!running)return;
    if(!huluForeground()){release();return;}
    if(pending)return;
    pending=true;
    final int request=++generation;
    handler.postDelayed(timeout,4000);
    try{takeScreenshot(0,getMainExecutor(),new TakeScreenshotCallback(){
      @Override public void onFailure(int error){if(request!=generation)return;handler.removeCallbacks(timeout);pending=false;if(delay!=5000)android.util.Log.w("VistaHuluMute","Badge capture unavailable: "+error);delay=5000;release();schedule();}
      @Override public void onSuccess(ScreenshotResult result){
        Bitmap hardware=null,crop=null,software=null;
        try{
          if(request!=generation)return;
          handler.removeCallbacks(timeout);delay=1000;
          if(!running||!huluForeground()){release();return;}
          hardware=Bitmap.wrapHardwareBuffer(result.getHardwareBuffer(),result.getColorSpace());
          if(hardware==null){release();return;}
          crop=Bitmap.createBitmap(hardware,HuluAdBadge.left(hardware.getWidth()),HuluAdBadge.top(hardware.getHeight()),HuluAdBadge.width(hardware.getWidth()),HuluAdBadge.height(hardware.getHeight()));
          software=crop.copy(Bitmap.Config.ARGB_8888,false);
          int w=software.getWidth(),h=software.getHeight();int[] pixels=new int[w*h];software.getPixels(pixels,0,w,0,0,w,h);
          apply(policy.sample(HuluAdBadge.matches(pixels,w,h),audio.isStreamMute(AudioManager.STREAM_MUSIC),audio.getStreamVolume(AudioManager.STREAM_MUSIC)));
        }catch(RuntimeException error){release();}
        finally{
          if(software!=null)software.recycle();if(crop!=null&&crop!=hardware)crop.recycle();if(hardware!=null)hardware.recycle();result.getHardwareBuffer().close();if(request==generation){pending=false;schedule();}
        }
      }
    });}catch(RuntimeException error){handler.removeCallbacks(timeout);pending=false;release();schedule();}
  }
  private void release(){if(audio!=null)apply(policy.stop(audio.isStreamMute(AudioManager.STREAM_MUSIC),audio.getStreamVolume(AudioManager.STREAM_MUSIC)));}
  private void apply(int action){
    if(action==HuluMutePolicy.NONE)return;
    try{audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,action==HuluMutePolicy.MUTE?AudioManager.ADJUST_MUTE:AudioManager.ADJUST_UNMUTE,0);android.util.Log.i("VistaHuluMute",action==HuluMutePolicy.MUTE?"Ad badge: mute":"Restore sound");}
    catch(SecurityException error){android.util.Log.w("VistaHuluMute","Audio control unavailable");}
  }
  public static final class DisableReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){if(instance!=null){instance.running=false;instance.handler.removeCallbacksAndMessages(null);instance.release();instance.disableSelf();}}
  }
}
