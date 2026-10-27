package com.griffin.solhome;

import android.app.*;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.net.Uri;
import android.view.*;
import android.widget.Toast;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
  private HomeView home;
  private android.media.SoundPool menuSound;
  private int menuSample;
  private boolean menuSoundReady;
  private long lastMenuSound=-100;
  private PixelSceneView scene;
  private AmbientWidgetsView widgets;
  private ValueAnimator immersionAnimator;
  private boolean ambient=false;
  private int wakeKey=-1;
  private static final long IDLE_MS=120000, TRANSITION_MS=2000;
  private final Runnable idle=()->{if(this.foreground&&hasWindowFocus())setAmbient(true,true);};
  private final Handler handler=new Handler(Looper.getMainLooper());
  private boolean foreground=false;
  private JSONObject weather;
  private final Runnable widgetChanged=()->{weather=WidgetRepository.get(this).weather();if(home!=null)home.invalidate();};
  private final ArrayList<JSONObject> watching=new ArrayList<>();
  private final String[] favorites={"com.amazon.firetv.youtube","com.netflix.ninja","com.griffin.hululab","com.disney.disneyplus","com.spotify.tv.android","com.apple.atve.amazon.appletv","com.amazon.firebat"};
  private final String[] names={"YouTube","Netflix","Hulu","Disney+","Spotify","Apple TV","Prime Video"};
  private final Runnable minuteTick=new Runnable(){public void run(){if(foreground){home.invalidate();loadWeather();handler.postDelayed(this,60000);}}};
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);loadMenuSound();
    home=new HomeView();scene=new PixelSceneView(this);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff17242a);root.addView(scene);root.addView(home);widgets=new AmbientWidgetsView(this);widgets.setAlpha(0);root.addView(widgets);setContentView(root);home.requestFocus();
    new Thread(()->{
      Bitmap[] logos=new Bitmap[favorites.length];for(int n=0;n<logos.length;n++){try{PackageManager pm=getPackageManager();Intent intent=pm.getLeanbackLaunchIntentForPackage(favorites[n]);Drawable d=intent!=null?pm.getActivityBanner(intent.getComponent()):null;if(d==null)d=pm.getApplicationBanner(favorites[n]);if(d==null)d=pm.getApplicationIcon(favorites[n]);Bitmap icon=Bitmap.createBitmap(256,144,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(icon);float ratio=Math.min(256f/Math.max(1,d.getIntrinsicWidth()),144f/Math.max(1,d.getIntrinsicHeight()));int w=Math.round(d.getIntrinsicWidth()*ratio),h=Math.round(d.getIntrinsicHeight()*ratio);d.setBounds((256-w)/2,(144-h)/2,(256+w)/2,(144+h)/2);d.draw(c);logos[n]=icon;}catch(Exception ignored){}}
      runOnUiThread(()->{home.logos=logos;home.invalidate();});
    },"sol-logos").start();
  }
  private void loadMenuSound(){
    try{menuSound=new android.media.SoundPool.Builder().setMaxStreams(1).setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
    menuSound.setOnLoadCompleteListener((pool,id,status)->{if(pool==menuSound&&id==menuSample){menuSoundReady=status==0;android.util.Log.i("VistaAudio","Focus sample load status="+status);}});
    try(android.content.res.AssetFileDescriptor asset=getAssets().openFd("audio/focus.wav")){menuSample=menuSound.load(asset,1);}catch(IOException unavailable){menuSoundReady=false;}
    }catch(RuntimeException unavailable){menuSoundReady=false;if(menuSound!=null){menuSound.release();menuSound=null;}}
  }
  private void menuMoved(int before,int after){
    long now=SystemClock.uptimeMillis();
    if(foreground&&!ambient&&menuSoundReady&&menuSound!=null&&HomeNavigation.sound(before,after,now,lastMenuSound)){
      int stream=menuSound.play(menuSample,1f,1f,1,0,1);if(lastMenuSound<0)android.util.Log.i("VistaAudio","First focus sound stream="+stream);if(stream!=0)lastMenuSound=now;
    }
  }
  private void releaseMenuSound(){menuSoundReady=false;if(menuSound!=null){menuSound.setOnLoadCompleteListener(null);menuSound.release();menuSound=null;}}
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);PicturePanelService.dismiss(this);SpotifyPanel.dismiss();setIntent(intent);scene.setPreviewTime(intent.getLongExtra("sol.preview.epoch",0));scene.setWorldPreview(intent.getFloatExtra("sol.preview.world_seconds",-1));setAmbient(false,true);armIdle();}
  @Override protected void onResume(){super.onResume();if(menuSound==null)loadMenuSound();PicturePanelService.dismiss(this);foreground=true;setAmbient(false,true);home.requestFocus();home.revealSelection(false);handler.removeCallbacks(minuteTick);handler.post(minuteTick);scene.setPreviewTime(getIntent().getLongExtra("sol.preview.epoch",0));scene.setWorldPreview(getIntent().getFloatExtra("sol.preview.world_seconds",-1));scene.start();widgets.start();WidgetRepository.get(this).listen(widgetChanged);loadWatching();armIdle();}
  @Override protected void onPause(){foreground=false;releaseMenuSound();home.stopScroll();handler.removeCallbacks(minuteTick);handler.removeCallbacks(idle);if(immersionAnimator!=null)immersionAnimator.cancel();scene.stop();widgets.stop();WidgetRepository.get(this).unlisten(widgetChanged);super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);scene.release();releaseMenuSound();super.onDestroy();}
  @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)armIdle();else handler.removeCallbacks(idle);}
  @Override public void onUserInteraction(){super.onUserInteraction();armIdle();}
  private void armIdle(){handler.removeCallbacks(idle);if(foreground&&!ambient){long delay=Math.max(1000,Math.min(IDLE_MS,getIntent().getLongExtra("sol.preview.idle_ms",IDLE_MS)));handler.postDelayed(idle,Math.max(0,delay-TRANSITION_MS));}}
  private void setAmbient(boolean value,boolean animate){
    ambient=value;handler.removeCallbacks(idle);if(immersionAnimator!=null)immersionAnimator.cancel();
    float target=value?1:0;
    if(!animate||Math.abs(scene.immersion()-target)<.001f){setImmersion(target);return;}
    immersionAnimator=ValueAnimator.ofFloat(scene.immersion(),target);immersionAnimator.setDuration(value?TRANSITION_MS:650);immersionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    immersionAnimator.addUpdateListener(a->setImmersion((Float)a.getAnimatedValue()));immersionAnimator.start();
    android.util.Log.i("SolAmbient",value?"Expanding to screensaver":"Returning to Home");
  }
  private void setImmersion(float amount){scene.setImmersion(amount);home.setAlpha(1-amount);home.invalidate();widgets.setAlpha(Math.max(0,(amount-.55f)/.45f));}
  @Override public boolean dispatchKeyEvent(KeyEvent event){
    int key=event.getKeyCode();
    if(wakeKey==key){if(event.getAction()==KeyEvent.ACTION_UP)wakeKey=-1;return true;}
    if(event.getAction()==KeyEvent.ACTION_DOWN&&ambient){setAmbient(false,true);armIdle();
      if(key!=KeyEvent.KEYCODE_VOLUME_UP&&key!=KeyEvent.KEYCODE_VOLUME_DOWN&&key!=KeyEvent.KEYCODE_VOLUME_MUTE&&key!=KeyEvent.KEYCODE_POWER){wakeKey=key;return true;}}
    return super.dispatchKeyEvent(event);
  }
  @Override public boolean dispatchTouchEvent(MotionEvent event){if(ambient){if(event.getAction()==MotionEvent.ACTION_UP){setAmbient(false,true);armIdle();}return true;}return super.dispatchTouchEvent(event);}
  @Override public void onBackPressed(){setAmbient(false,true);home.selected=home.lastApp;home.revealSelection(false);home.invalidate();armIdle();}
  private String localTime(String pattern,Date date){SimpleDateFormat format=new SimpleDateFormat(pattern,Locale.US);format.setTimeZone(TimeZone.getTimeZone(CincinnatiSky.ZONE_ID));return format.format(date);}
  private void toast(String s){runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
  private void openApp(String pkg){try{
    Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
    if(i==null){toast("This app is not available.");return;}startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  }catch(Exception e){toast("Could not open this app.");}}
  private void allApps(){
    TreeMap<String,String> found=new TreeMap<>();
    for(String cat:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER})
      for(ResolveInfo r:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat),0))
        if(!r.activityInfo.packageName.equals(getPackageName()))found.put(r.loadLabel(getPackageManager()).toString(),r.activityInfo.packageName);
    String[] labels=found.keySet().toArray(new String[0]),packages=found.values().toArray(new String[0]);
    new AlertDialog.Builder(this).setTitle("All apps").setItems(labels,(d,n)->openApp(packages[n])).setNegativeButton("Close",null).show();
  }
  private boolean openingTvControl;
  private void inputs(){tvControl("inputs");}
  private void tvControl(String command){
    if(openingTvControl)return;openingTvControl=true;
    new Thread(()->{
      try{PictureBridgeProvider.request(command,0,null,null);}
      catch(Exception failed){toast("inputs".equals(command)?"Input helper unavailable. Open Inputs from the TV's native settings menu.":"Fire TV Home helper unavailable.");}
      finally{runOnUiThread(()->openingTvControl=false);}
    },"vista-inputs").start();
  }
  private void tvSettings(){try{PicturePanelService.nativeSettings(this);}catch(Exception e){toast("Hold Home to open TV settings.");}}
  void refreshCompanionScene(){scene.reloadTheme();home.invalidate();widgets.invalidate();}
  CompanionServer.Reply companionPanel(String panel){
    setAmbient(false,true);armIdle();
    if(panel.equals("ambient"))setAmbient(true,true);
    else if(panel.equals("spotify"))SpotifyPanel.show(this);
    else if(panel.equals("explore"))startActivity(new Intent(this,StreamingActivity.class));
    else return new CompanionServer.Reply(400,"Unknown panel");
    return new CompanionServer.Reply(200,"Opened "+panel);
  }
  private void settings(){new AlertDialog.Builder(this).setTitle("Vista settings").setItems(new String[]{"TV settings","Seasons & occasions","Phone companion","Wildlife · "+(SceneSettings.preferences(this).getBoolean("wildlife",true)?"On":"Off"),"Snow locations","This week · interests","Screensaver: 2 minutes · Sleep: 60 minutes","Coding usage","Hulu ad muting · "+(HuluAdMuteService.enabled(this)?"On":"Off")},(d,n)->{if(n==0)tvSettings();else if(n==1)ScenePanel.show(this,this::refreshCompanionScene);else if(n==2)CompanionPanel.show(this);else if(n==3){android.content.SharedPreferences p=SceneSettings.preferences(this);p.edit().putBoolean("wildlife",!p.getBoolean("wildlife",true)).apply();refreshCompanionScene();}else if(n==4)snowLocations();else if(n==5)EventsPanel.interests(this);else if(n==7)CodingUsageView.show(this);else if(n==8)HuluAdMuteService.show(this);else new AlertDialog.Builder(this).setTitle("Idle timing").setMessage("The scene expands after 2 minutes without input. The TV sleep setting is 60 minutes. Playback apps can keep the display awake.").setPositiveButton("OK",null).show();}).setNegativeButton("Close",null).show();}
  private void snowLocations(){WidgetRepository repo=WidgetRepository.get(this);JSONArray locations=repo.locations();String[] labels=new String[locations.length()+2];for(int i=0;i<locations.length();i++)labels[i]=locations.optJSONObject(i).optString("name");labels[locations.length()]="Add location";labels[locations.length()+1]="Restore default resorts";
    new AlertDialog.Builder(this).setTitle("Snow forecast locations").setItems(labels,(d,n)->{if(n==locations.length()+1){repo.resetLocations();snowLocations();}else if(n==locations.length()&&locations.length()>=2)toast("Edit or remove a location to replace it. Two snow locations fit beneath weather.");else editSnowLocation(locations,n);}).setNegativeButton("Close",null).show();}
  private void editSnowLocation(JSONArray locations,int index){
    JSONObject current=locations.optJSONObject(index);android.widget.LinearLayout form=new android.widget.LinearLayout(this);form.setOrientation(1);form.setPadding(32,12,32,12);
    android.widget.EditText name=new android.widget.EditText(this),point=new android.widget.EditText(this);name.setHint("Resort or location name");point.setHint("Forecast point: latitude, longitude (US)");name.setSingleLine(true);point.setSingleLine(true);form.addView(name);form.addView(point);
    if(current!=null){name.setText(current.optString("name"));point.setText(current.optDouble("lat")+", "+current.optDouble("lon"));}
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Snow location").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",null).setNeutralButton(current==null?"Defaults":"Remove",(d,w)->{if(current==null)WidgetRepository.get(this).resetLocations();else{locations.remove(index);if(locations.length()==0)WidgetRepository.get(this).resetLocations();else WidgetRepository.get(this).saveLocations(locations);}}).create();
    dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{String label=name.getText().toString().trim();String[] pair=point.getText().toString().split(",");double lat=Double.parseDouble(pair[0].trim()),lon=Double.parseDouble(pair[1].trim());if(label.isEmpty()||label.length()>40||pair.length!=2||!Double.isFinite(lat)||!Double.isFinite(lon)||lat<18||lat>72||lon< -180||lon> -60||(current==null&&locations.length()>=2))throw new IllegalArgumentException();locations.put(index,WidgetRepository.location(label,lat,lon));WidgetRepository.get(this).saveLocations(locations);dialog.dismiss();}catch(Exception error){point.setError("Enter a name and valid US latitude, longitude");}}));dialog.show();
  }
  private void loadWatching(){new Thread(()->{
    ArrayList<JSONObject> items=new ArrayList<>();
    try(Cursor c=getContentResolver().query(Uri.parse("content://android.media.tv/watch_next_program"),new String[]{"package_name","title","intent_uri","last_playback_position_millis","duration_millis"},null,null,"last_engagement_time_utc_millis DESC")){
      if(c!=null)while(c.moveToNext()&&items.size()<20){if(c.getString(1)==null||c.getString(2)==null)continue;
        items.add(new JSONObject().put("pkg",c.getString(0)).put("title",c.getString(1)).put("link",c.getString(2)).put("position",c.getLong(3)).put("duration",c.getLong(4)));
      }
    }catch(Exception ignored){}
    runOnUiThread(()->{watching.clear();watching.addAll(items);home.invalidate();});
  },"sol-progress").start();}
  private void continueWatching(){
    if(watching.isEmpty()){new AlertDialog.Builder(this).setTitle("Continue watching").setMessage("Your streaming apps have not shared any viewing progress with Vista. You can still resume inside each app.").setPositiveButton("OK",null).show();return;}
    final ArrayList<JSONObject> snapshot=new ArrayList<>(watching);String[] titles=new String[snapshot.size()];
    for(int n=0;n<titles.length;n++){JSONObject p=snapshot.get(n);long total=p.optLong("duration"),position=p.optLong("position");titles[n]=p.optString("title")+(total>0?"  ·  "+(position*100/total)+"%":"");}
    new AlertDialog.Builder(this).setTitle("Continue watching").setItems(titles,(d,n)->{
      JSONObject item=snapshot.get(n);try{Intent i=Intent.parseUri(item.getString("link"),Intent.URI_INTENT_SCHEME);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);i.setSelector(null);i.setClipData(null);i.setPackage(item.getString("pkg"));startActivity(i);}catch(Exception e){openApp(item.optString("pkg"));}
    }).setNegativeButton("Close",null).show();
  }
  private void loadWeather(){WidgetRepository.get(this).refresh();weather=WidgetRepository.get(this).weather();home.invalidate();}
  class HomeView extends View {
    Bitmap[] logos=new Bitmap[favorites.length];int selected=0,lastApp=0,lastGrid=0,gridScroll=0;
    final android.widget.OverScroller gridScroller=new android.widget.OverScroller(MainActivity.this);
    final Paint paint=new Paint(3);final int white=Color.rgb(242,240,231),muted=Color.rgb(168,174,173),gold=Color.rgb(207,182,132);
    final String[] controls={"All apps","Inputs","Ambient","Settings","Watching"};
    final CodingUsageView codingView=new CodingUsageView();
    final HomeWeatherView weatherView=new HomeWeatherView();
    final Shader wallLight=new LinearGradient(0,0,1920,900,0xff192b2c,0xff30403c,Shader.TileMode.CLAMP);
    final Shader railLight=new LinearGradient(72,0,577,0,0xff152323,0xff1c2e2c,Shader.TileMode.CLAMP);
    HomeView(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Vista. Use arrows to choose a tile, scroll down for TV controls, or move right to coding usage and weather. Press OK to open.");}
    void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif"+(bold?"-medium":"-light"),Typeface.NORMAL));c.drawText(s,x,y,paint);}
    void drawRoom(Canvas c){
      float l=scene.windowLeft(),t=scene.windowTop(),r=scene.windowRight(),b=scene.windowBottom();
      paint.setStyle(Paint.Style.FILL);
      // Room surfaces stay in this retained View; only the view outdoors animates.
      c.save();c.clipOutRect(l,t,r,b);paint.setShader(wallLight);c.drawRect(0,0,1920,1080,paint);paint.setShader(null);
      paint.setColor(0x183d5048);Path light=new Path();light.moveTo(l-25,t+40);light.lineTo(l-25,b+10);light.lineTo(580,940);light.lineTo(605,254);light.close();c.drawPath(light,paint);c.restore();
      // Recessed timber reveal: one open aperture, with no sash or crossbars.
      paint.setColor(0xff101e20);c.drawRect(l-22,t-22,r+22,t-8,paint);c.drawRect(l-22,t-8,l-8,b+16,paint);c.drawRect(r+8,t-8,r+22,b+16,paint);
      paint.setColor(0xff676957);c.drawRect(l-8,t-8,r+8,t,paint);c.drawRect(r,t,r+8,b,paint);
      paint.setColor(0xff424c40);c.drawRect(l-8,t,l,b,paint);
      paint.setColor(0xff89907a);c.drawRect(l-22,t-22,r+22,t-19,paint);c.drawRect(r+19,t-19,r+22,b+16,paint);
      Path sill=new Path();sill.moveTo(l-22,b);sill.lineTo(r+22,b);sill.lineTo(r+38,b+23);sill.lineTo(l-38,b+23);sill.close();paint.setColor(0xff7f8066);c.drawPath(sill,paint);
      paint.setColor(0xffa3a083);c.drawRect(l-22,b,r+22,b+3,paint);paint.setColor(0xff414b40);c.drawRect(l-38,b+23,r+38,b+32,paint);
      paint.setColor(0x550b1517);c.drawRect(l-30,b+32,r+35,b+39,paint);
      // The bottom information cards belong to a shallow console below the sill.
      paint.setColor(0xff101d1f);c.drawRect(0,944,1920,1080,paint);paint.setColor(0xff666a55);c.drawRect(0,942,1920,946,paint);
      paint.setColor(0xff8b8d70);c.drawRect(0,942,1920,943,paint);paint.setColor(0xff233631);c.drawRect(0,951,1920,953,paint);
      paint.setColor(0xff30413a);c.drawRect(714,970,716,1060,paint);c.drawRect(1388,970,1390,1060,paint);
    }
    void drawAppNiche(Canvas c){
      paint.setShader(null);paint.setColor(0x30081113);c.drawRoundRect(new RectF(74,46,584,938),18,18,paint);
      paint.setShader(railLight);c.drawRoundRect(new RectF(77,40,580,931),16,16,paint);paint.setShader(null);
    }
    void utilityIcon(Canvas c,int kind,float x,float y,int color){
      c.save();c.translate(x,y);paint.setShader(null);paint.setColor(color);paint.setStrokeWidth(2);paint.setStyle(Paint.Style.STROKE);
      if(kind==0){paint.setStyle(Paint.Style.FILL);for(int row=0;row<3;row++)for(int col=0;col<3;col++)c.drawRect(col*7,row*7,col*7+3,row*7+3,paint);}
      else if(kind==1){c.drawRect(1,3,23,18,paint);c.drawLine(4,21,20,21,paint);c.drawLine(7,7,17,7,paint);}
      else if(kind==2){Path path=new Path();path.moveTo(0,20);path.lineTo(8,6);path.lineTo(13,13);path.lineTo(17,3);path.lineTo(25,20);path.close();c.drawPath(path,paint);}
      else if(kind==3){c.drawCircle(11,11,7,paint);c.drawCircle(11,11,2,paint);for(int n=0;n<8;n++){double a=n*Math.PI/4;c.drawLine(11+(float)Math.cos(a)*8,11+(float)Math.sin(a)*8,11+(float)Math.cos(a)*11,11+(float)Math.sin(a)*11,paint);}}
      else{c.drawRect(0,2,24,21,paint);Path path=new Path();path.moveTo(9,7);path.lineTo(16,12);path.lineTo(9,17);path.close();c.drawPath(path,paint);}
      paint.setStyle(Paint.Style.FILL);c.restore();
    }
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);c.save();c.scale(getWidth()/1920f,getHeight()/1080f);
      drawRoom(c);
      drawAppNiche(c);
      text(c,"VISTA",108,82,25,white,true);text(c,"/",207,82,23,muted,false);text(c,"HOME",234,82,17,muted,false);
      Date now=new Date();text(c,localTime("h:mm",now),103,207,99,white,false);
      text(c,localTime("a",now),368,204,19,muted,false);
      text(c,localTime("EEEE, MMMM d",now),108,252,23,muted,false);
      String themeLabel=scene.themeLabel();paint.setTextSize(16);int count=paint.breakText(themeLabel,true,438,null);text(c,count<themeLabel.length()?themeLabel.substring(0,Math.max(0,count-1))+"…":themeLabel,108,274,16,0xffb7c9ab,false);
      c.save();c.clipRect(88,280,552,884);
      for(int i=0;i<HomeNavigation.GRID.length;i++){
        int item=HomeNavigation.GRID[i],favorite=HomeNavigation.favoriteIndex(item);String label=favorite>=0?names[favorite]:item==11?"Explore":item==12?"Spotify vibes":item==HomeNavigation.FIRE_TV_HOME?"Fire TV Home":controls[item-6]+(item==10&&!watching.isEmpty()?" · "+watching.size():"");
        float x=108+(i%2)*233,y=286+(i/2)*150-gridScroll,width=200;
        if(y+143<280||y>884)continue;
        boolean focus=selected==item;RectF tile=new RectF(x,y,x+width,y+113);
        paint.setColor(focus?0xffcfb684:0xff3c4c4b);c.drawRoundRect(new RectF(x-3,y-3,x+width+3,y+116),8,8,paint);
        paint.setColor(0xff192322);c.drawRoundRect(tile,6,6,paint);
        Bitmap logo=favorite>=0?logos[favorite]:null;
        if(item==11){
          paint.setColor(0xff405f54);c.drawRoundRect(new RectF(x+55,y+35,x+127,y+85),8,8,paint);
          paint.setColor(0xffb9d4a7);c.drawRoundRect(new RectF(x+73,y+23,x+145,y+73),8,8,paint);
          paint.setColor(0xff1b302a);Path play=new Path();play.moveTo(x+101,y+36);play.lineTo(x+122,y+48);play.lineTo(x+101,y+60);play.close();c.drawPath(play,paint);
          paint.setColor(gold);c.drawRect(x+151,y+25,x+154,y+42,paint);c.drawRect(x+144,y+32,x+161,y+35,paint);
        }
        else if(item==12){
          paint.setColor(0xffb9d4a7);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(6);paint.setStrokeCap(Paint.Cap.ROUND);
          Path arch=new Path();arch.moveTo(x+74,y+59);arch.cubicTo(x+74,y+16,x+126,y+16,x+126,y+59);c.drawPath(arch,paint);
          paint.setStyle(Paint.Style.FILL);c.drawRoundRect(new RectF(x+68,y+50,x+82,y+79),6,6,paint);c.drawRoundRect(new RectF(x+118,y+50,x+132,y+79),6,6,paint);
          paint.setStrokeWidth(3);c.drawLine(x+92,y+52,x+92,y+66,paint);c.drawLine(x+100,y+45,x+100,y+72,paint);c.drawLine(x+108,y+49,x+108,y+68,paint);
          paint.setStrokeCap(Paint.Cap.BUTT);paint.setColor(gold);c.drawRect(x+145,y+24,x+148,y+41,paint);c.drawRect(x+138,y+31,x+155,y+34,paint);
        }
        else if(item==HomeNavigation.FIRE_TV_HOME)text(c,"fire tv",x+47,y+67,32,focus?gold:0xffb9d4a7,true);
        else if(item>=6&&item<=10){c.save();c.translate(x+width/2-36,y+22);c.scale(3,3);utilityIcon(c,item-6,0,0,focus?gold:0xffb9d4a7);c.restore();}
        else if(logo!=null){paint.setColor(Color.WHITE);c.drawBitmap(logo,null,tile,paint);}else text(c,label,x+18,y+67,25,white,true);
        text(c,label,x,y+137,20,focus?white:muted,focus);
      }
      c.restore();
      paint.setColor(0xff354840);c.drawRoundRect(new RectF(561,286,564,878),1.5f,1.5f,paint);
      float thumbHeight=592*4f/((HomeNavigation.GRID.length+1)/2),thumbY=286+gridScroll/(float)HomeNavigation.MAX_SCROLL*(592-thumbHeight);paint.setColor(0xff8c9b80);c.drawRoundRect(new RectF(561,thumbY,564,thumbY+thumbHeight),1.5f,1.5f,paint);
      String[] shortcuts={"News briefs","This week"};
      for(int i=0;i<shortcuts.length;i++){float x=108+i*233;boolean focus=selected==13+i;paint.setColor(focus?0xff405549:0xff223832);c.drawRoundRect(new RectF(x,896,x+200,926),8,8,paint);text(c,shortcuts[i],x+16,918,18,focus?white:muted,focus);}
      codingView.home(c,WidgetRepository.get(MainActivity.this).codingUsage(),System.currentTimeMillis(),selected);
      weatherView.draw(c,weather,System.currentTimeMillis(),selected==17);
      c.restore();
    }
    void activate(){
      int favorite=HomeNavigation.favoriteIndex(selected);
      if(favorite>=0){lastApp=selected;openApp(favorites[favorite]);}
      else switch(selected){case 6:allApps();break;case 7:inputs();break;case 8:setAmbient(true,true);break;case 9:settings();break;case 10:continueWatching();break;case 11:startActivity(new Intent(MainActivity.this,StreamingActivity.class));break;case 12:SpotifyPanel.show(MainActivity.this);break;case HomeNavigation.FIRE_TV_HOME:tvControl("fire-home");break;case 13:NewsPanel.show(MainActivity.this);break;case 14:EventsPanel.show(MainActivity.this);break;case 15:case 16:CodingUsageView.show(MainActivity.this);break;case 17:forecast();break;}
    }
    void forecast(){new AlertDialog.Builder(MainActivity.this).setTitle("Cincinnati forecast").setMessage(weather==null?"Forecast unavailable":weather.optString("detail")+"\n\nNational Weather Service · Cincinnati city center").setPositiveButton("Close",null).show();}
    void stopScroll(){if(!gridScroller.isFinished())gridScroll=gridScroller.getFinalY();gridScroller.forceFinished(true);}
    void revealSelection(boolean animate){
      int target=HomeNavigation.reveal(selected,gridScroller.isFinished()?gridScroll:gridScroller.getFinalY());
      if(!animate){gridScroller.forceFinished(true);gridScroll=target;invalidate();return;}
      if(target==(gridScroller.isFinished()?gridScroll:gridScroller.getFinalY()))return;
      gridScroller.computeScrollOffset();if(!gridScroller.isFinished())gridScroll=gridScroller.getCurrY();
      gridScroller.startScroll(0,gridScroll,0,target-gridScroll,180);postInvalidateOnAnimation();
    }
    @Override public void computeScroll(){if(gridScroller.computeScrollOffset()){gridScroll=gridScroller.getCurrY();if(foreground)postInvalidateOnAnimation();}}
    @Override public boolean onKeyDown(int key,KeyEvent e){
      int direction;
      switch(key){
        case KeyEvent.KEYCODE_DPAD_UP:direction=HomeNavigation.UP;break;
        case KeyEvent.KEYCODE_DPAD_DOWN:direction=HomeNavigation.DOWN;break;
        case KeyEvent.KEYCODE_DPAD_LEFT:direction=HomeNavigation.LEFT;break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:direction=HomeNavigation.RIGHT;break;
        case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:if(e.getRepeatCount()==0)activate();return true;
        default:return super.onKeyDown(key,e);
      }
      int previous=selected;selected=HomeNavigation.move(selected,direction,lastGrid);menuMoved(previous,selected);
      if(HomeNavigation.position(selected)>=0)lastGrid=selected;if(HomeNavigation.favoriteIndex(selected)>=0)lastApp=selected;
      revealSelection(true);invalidate();return true;
    }
    float dragStart;int dragScroll;boolean dragArea,dragged;
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      float x=e.getX()*1920/getWidth(),y=e.getY()*1080/getHeight();
      if(e.getAction()==android.view.MotionEvent.ACTION_DOWN){dragArea=x>=88&&x<570&&y>=280&&y<884;dragStart=y;dragScroll=gridScroll;dragged=false;return true;}
      if(e.getAction()==android.view.MotionEvent.ACTION_MOVE&&dragArea&&Math.abs(y-dragStart)>18){dragged=true;gridScroller.forceFinished(true);gridScroll=Math.max(0,Math.min(HomeNavigation.MAX_SCROLL,dragScroll+Math.round(dragStart-y)));invalidate();return true;}
      if(e.getAction()!=android.view.MotionEvent.ACTION_UP)return true;
      if(dragged){int previous=selected,row=Math.round(gridScroll/150f);selected=HomeNavigation.GRID[Math.min(HomeNavigation.GRID.length-1,row*2)];menuMoved(previous,selected);lastGrid=selected;return true;}
      int item=HomeNavigation.hit(x,y,gridScroll);
      if(item>=0){selected=item;lastGrid=item;activate();}
      else if(HomeNavigation.shortcutHit(x,y)>=0){selected=HomeNavigation.shortcutHit(x,y);activate();}
      else if(y>954&&x>=96&&x<700){selected=15;activate();}
      else if(y>954&&x>=730&&x<1350){selected=16;activate();}
      else if(y>954&&x>=1400){selected=17;activate();}
      invalidate();return true;
    }
  }
}
