package com.griffin.solhome;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Explicitly started, non-sticky LAN companion. One server thread; no wake lock. */
public final class CompanionService extends Service {
    private static volatile CompanionService instance;
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile CompanionServer server;
    private volatile String address="",error="Starting…";
    private WeakReference<Activity> visible=new WeakReference<>(null);
    private SpotifyPlayback playback;
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
        public void onActivityCreated(Activity a,Bundle b){} public void onActivityStarted(Activity a){}
        public void onActivityResumed(Activity a){visible=new WeakReference<>(a);}
        public void onActivityPaused(Activity a){if(visible.get()==a)visible.clear();}
        public void onActivityStopped(Activity a){} public void onActivitySaveInstanceState(Activity a,Bundle b){}
        public void onActivityDestroyed(Activity a){if(visible.get()==a)visible.clear();}
    };
    static CompanionService active(){return instance;}
    String address(){return address;}
    String error(){return error;}
    String pair(){return server==null?"":server.pair();}
    String pairingStatus(){return server==null?error:server.pairingStatus();}
    void attach(Activity a){visible=new WeakReference<>(a);}
    @Override public void onCreate(){
        super.onCreate();instance=this;getApplication().registerActivityLifecycleCallbacks(lifecycle);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("vista_companion","Phone companion",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,CompanionService.class).setAction("stop"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(71,new Notification.Builder(this,"vista_companion").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Vista phone companion is on").setContentText("Local network · turn off in Vista settings").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Turn off",stop).build()).build());
        new Thread(()->{
            try{
                InetAddress local=localAddress();if(local==null)throw new IOException("Connect the TV to a private Wi-Fi or Ethernet network.");
                byte[] html;try(InputStream input=getAssets().open("companion.html");ByteArrayOutputStream bytes=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);html=bytes.toByteArray();}
                CompanionServer created=new CompanionServer(local,8765,html,this::dispatch);
                synchronized(this){if(instance!=this){created.close();return;}server=created;address="http://"+local.getHostAddress()+":"+created.port();error="";}
                created.serve();
            }catch(IOException failed){error="Could not start companion. Check the TV network and try again.";}
            main.post(()->{if(instance==this){if(server==null)android.widget.Toast.makeText(this,error,android.widget.Toast.LENGTH_LONG).show();stopSelf();}});
        },"vista-companion").start();
    }
    private static InetAddress localAddress()throws SocketException{
        for(NetworkInterface network:Collections.list(NetworkInterface.getNetworkInterfaces())){
            if(!network.isUp()||network.isLoopback()||network.isVirtual())continue;
            for(InetAddress ip:Collections.list(network.getInetAddresses()))if(ip instanceof Inet4Address&&ip.isSiteLocalAddress())return ip;
        }return null;
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&"stop".equals(intent.getAction()))stopSelf();return START_NOT_STICKY;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){
        synchronized(this){if(instance==this)instance=null;if(server!=null)server.close();}
        getApplication().unregisterActivityLifecycleCallbacks(lifecycle);if(playback!=null)playback.close();main.removeCallbacksAndMessages(null);stopForeground(true);super.onDestroy();
    }
    private CompanionServer.Reply dispatch(String action,String value){
        FutureTask<CompanionServer.Reply> task=new FutureTask<>(()->instance==this?command(action,value):reply(503,"Companion is off"));main.post(task);
        try{return task.get(1200,TimeUnit.MILLISECONDS);}catch(Exception unavailable){task.cancel(false);return reply(503,"TV is busy. Try again.");}
    }
    private CompanionServer.Reply command(String action,String value){
        Activity a=visible.get();
        if(action.equals("status"))return reply(200,a instanceof StreamingActivity?"Explore is open":a instanceof MainActivity?"Vista Home is open":"An app or screensaver is open · press Home on the TV remote");
        if(action.equals("disconnect"))return reply(200,"Phone unpaired");
        if(action.matches("home|search|app|panel")&&(a==null||!a.hasWindowFocus()))return reply(409,"Press Home on the TV remote and close TV dialogs first. Fire OS blocks background app launches.");
        if(action.equals("home")){startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));return reply(200,"Returning to Vista");}
        if(action.equals("search")){
            if(value.trim().isEmpty())return reply(400,"Enter a title to find");
            if(a instanceof StreamingActivity)((StreamingActivity)a).companionSearch(value);
            else startActivity(new Intent(this,StreamingActivity.class).putExtra("vista.search",value).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
            return reply(200,"Search sent to Explore · a TMDB catalog connection is required for results");
        }
        if(action.equals("app")){
            String[] ids={"youtube","netflix","hulu","disney","spotify","apple"};String[] packages={"com.amazon.firetv.youtube","com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.spotify.tv.android","com.apple.atve.amazon.appletv"};
            for(int i=0;i<ids.length;i++)if(ids[i].equals(value)){Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(packages[i]);if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(packages[i]);if(intent==null)return reply(409,"This app is not installed");try{startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return reply(200,"Opening "+ids[i]);}catch(RuntimeException failed){return reply(409,"Could not open this app");}}
            return reply(400,"Unknown app");
        }
        if(action.equals("season")){
            if(!value.matches("AUTO|SPRING|SUMMER|AUTUMN|WINTER"))return reply(400,"Unknown season");
            SceneSettings.preferences(this).edit().putString("season",value).putString("occasion",value.equals("AUTO")?"AUTO":"NONE").apply();
            if(a instanceof MainActivity)((MainActivity)a).refreshCompanionScene();return reply(200,value.equals("AUTO")?"Automatic calendar restored":"Season preview saved · clock and weather stay live");
        }
        if(action.equals("wildlife")){
            if(!value.matches("on|off"))return reply(400,"Choose on or off");SceneSettings.preferences(this).edit().putBoolean("wildlife",value.equals("on")).apply();
            if(a instanceof MainActivity)((MainActivity)a).refreshCompanionScene();return reply(200,"Wildlife "+value);
        }
        if(action.equals("spotify")){
            if(!value.matches("toggle|previous|next"))return reply(400,"Unknown playback control");
            if(playback==null){playback=new SpotifyPlayback(this,state->{});playback.start();}
            boolean sent=value.equals("toggle")?playback.toggle():value.equals("next")?playback.next():playback.previous();
            return reply(sent?200:409,sent?"Spotify control sent":"Spotify has no available control. Start music in the TV app first.");
        }
        if(a==null||!a.hasWindowFocus())return reply(409,"Return to Vista Home and close TV dialogs first. Use the physical remote for dialogs.");
        if(action.equals("key")){
            int key=value.equals("up")?19:value.equals("down")?20:value.equals("left")?21:value.equals("right")?22:value.equals("ok")?23:value.equals("back")?4:0;
            if(key==0)return reply(400,"Unknown key");
            // Only dispatch inside a visible Vista Activity, never inject into other apps.
            Window.Callback callback=a.getWindow().getCallback();callback.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));callback.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));a.onUserInteraction();return reply(200,"Sent "+value);
        }
        if(action.equals("panel")&&a instanceof MainActivity)return ((MainActivity)a).companionPanel(value);
        return reply(400,"Unsupported command here");
    }
    private static CompanionServer.Reply reply(int status,String text){return new CompanionServer.Reply(status,text);}
}
