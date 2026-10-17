package com.griffin.solhome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.net.Uri;
import android.media.tv.*;
import android.view.*;
import android.widget.Toast;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
  private HomeView home;
  private SceneMotion motion;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private boolean foreground=false;
  private JSONObject weather;
  private final ArrayList<JSONObject> watching=new ArrayList<>();
  private final String[] favorites={"com.amazon.firetv.youtube","com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.spotify.tv.android","com.apple.atve.amazon.appletv"};
  private final String[] names={"YouTube","Netflix","Hulu","Disney+","Spotify","Apple TV"};
  private final Runnable minuteTick=new Runnable(){public void run(){if(foreground){home.invalidate();loadWeather();handler.postDelayed(this,60000);}}};
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
    home=new HomeView();motion=new SceneMotion();FrameLayout root=new FrameLayout(this);root.addView(home);root.addView(motion);setContentView(root);home.requestFocus();
    new Thread(()->{try(InputStream in=getAssets().open("fall-forest.png")){
      BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;o.inSampleSize=1;
      Bitmap decoded=BitmapFactory.decodeStream(in,null,o);Bitmap bitmap=Bitmap.createScaledBitmap(decoded,1536,864,true);if(bitmap!=decoded)decoded.recycle();
      Bitmap[] logos=new Bitmap[6];for(int n=0;n<6;n++){try{PackageManager pm=getPackageManager();Intent intent=pm.getLeanbackLaunchIntentForPackage(favorites[n]);Drawable d=intent!=null?pm.getActivityBanner(intent.getComponent()):null;if(d==null)d=pm.getApplicationBanner(favorites[n]);if(d==null)d=pm.getApplicationIcon(favorites[n]);Bitmap icon=Bitmap.createBitmap(256,144,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(icon);float ratio=Math.min(256f/Math.max(1,d.getIntrinsicWidth()),144f/Math.max(1,d.getIntrinsicHeight()));int w=Math.round(d.getIntrinsicWidth()*ratio),h=Math.round(d.getIntrinsicHeight()*ratio);d.setBounds((256-w)/2,(144-h)/2,(256+w)/2,(144+h)/2);d.draw(c);logos[n]=icon;}catch(Exception ignored){}}
      runOnUiThread(()->{home.background=bitmap;home.logos=logos;home.invalidate();});
    }catch(Exception ignored){}},"sol-art").start();
  }
  @Override protected void onResume(){super.onResume();foreground=true;home.ambient=false;home.requestFocus();home.invalidate();handler.removeCallbacks(minuteTick);handler.post(minuteTick);motion.start();loadWatching();}
  @Override protected void onPause(){foreground=false;handler.removeCallbacks(minuteTick);motion.stop();super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
  @Override public void onBackPressed(){home.ambient=false;home.selected=home.lastApp;home.invalidate();}
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
  private void inputs(){try{
    TvInputManager m=(TvInputManager)getSystemService(TV_INPUT_SERVICE);ArrayList<TvInputInfo> list=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
    for(TvInputInfo i:m.getTvInputList())if(i.isPassthroughInput()){list.add(i);CharSequence custom=i.loadCustomLabel(this);labels.add((custom!=null?custom:i.loadLabel(this)).toString());}
    new AlertDialog.Builder(this).setTitle("TV inputs").setItems(labels.toArray(new String[0]),(d,n)->{
      try{startActivity(new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(list.get(n).getId())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
      catch(Exception e){toast("Use the Input button on your TV remote.");}
    }).setNegativeButton("Close",null).show();
  }catch(Exception e){toast("Use the Input button on your TV remote.");}}
  private void settings(){try{startActivity(new Intent().setClassName("com.amazon.tv.launcher","com.amazon.tv.launcher.ui.MainSettingsActivity"));}catch(Exception e){toast("Hold Home to open TV settings.");}}
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
    if(watching.isEmpty()){new AlertDialog.Builder(this).setTitle("Continue watching").setMessage("Your streaming apps have not shared any viewing progress with Sol. You can still resume inside each app.").setPositiveButton("OK",null).show();return;}
    final ArrayList<JSONObject> snapshot=new ArrayList<>(watching);String[] titles=new String[snapshot.size()];
    for(int n=0;n<titles.length;n++){JSONObject p=snapshot.get(n);long total=p.optLong("duration"),position=p.optLong("position");titles[n]=p.optString("title")+(total>0?"  ·  "+(position*100/total)+"%":"");}
    new AlertDialog.Builder(this).setTitle("Continue watching").setItems(titles,(d,n)->{
      JSONObject item=snapshot.get(n);try{Intent i=Intent.parseUri(item.getString("link"),Intent.URI_INTENT_SCHEME);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);i.setSelector(null);i.setClipData(null);i.setPackage(item.getString("pkg"));startActivity(i);}catch(Exception e){openApp(item.optString("pkg"));}
    }).setNegativeButton("Close",null).show();
  }
  private JSONObject getJson(String url)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","SolHome/0.2 (personal Cincinnati forecast)");c.setRequestProperty("Accept","application/geo+json");
    try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){StringBuilder s=new StringBuilder();String line;while((line=r.readLine())!=null)s.append(line);return new JSONObject(s.toString());}finally{c.disconnect();}
  }
  private volatile boolean weatherLoading=false;
  private long lastWeatherAttempt=0;
  private void loadWeather(){
    SharedPreferences p=getSharedPreferences("weather",0);String cached=p.getString("value",null);long at=p.getLong("at",0);
    if(cached!=null)try{weather=new JSONObject(cached).put("stale",System.currentTimeMillis()-at>3600000);home.invalidate();}catch(Exception ignored){}
    if(weatherLoading||(cached!=null&&System.currentTimeMillis()-at<1800000)||System.currentTimeMillis()-lastWeatherAttempt<300000)return;
    weatherLoading=true;lastWeatherAttempt=System.currentTimeMillis();
    new Thread(()->{try{
      String endpoint=getJson("https://api.weather.gov/points/39.1031,-84.5120").getJSONObject("properties").getString("forecast");if(!endpoint.startsWith("https://api.weather.gov/"))throw new IOException();
      JSONObject first=getJson(endpoint).getJSONObject("properties").getJSONArray("periods").getJSONObject(0);
      JSONObject data=new JSONObject().put("temperature",first.getInt("temperature")).put("unit",first.getString("temperatureUnit")).put("summary",first.getString("shortForecast")).put("period",first.getString("name")).put("detail",first.getString("detailedForecast")).put("stale",false);
      p.edit().putString("value",data.toString()).putLong("at",System.currentTimeMillis()).apply();runOnUiThread(()->{weather=data;home.invalidate();});
    }catch(Exception ignored){}finally{weatherLoading=false;}},"sol-weather").start();
  }
  class SceneMotion extends View {
    final Paint p=new Paint(3);final Bitmap mist=Bitmap.createBitmap(640,160,Bitmap.Config.ARGB_8888);
    final Path leaf=new Path();final long epoch=SystemClock.elapsedRealtime();boolean active=false;
    final Runnable tick=new Runnable(){public void run(){if(active){invalidate();handler.postDelayed(this,83);}}};
    SceneMotion(){super(MainActivity.this);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);Canvas c=new Canvas(mist);for(int i=0;i<5;i++){p.setShader(new RadialGradient(60+i*130,80,110,new int[]{0x16e2e3d7,0x00e2e3d7},null,Shader.TileMode.CLAMP));c.drawRect(0,0,640,160,p);}p.setShader(null);leaf.moveTo(0,0);leaf.cubicTo(8,-8,15,-5,18,0);leaf.cubicTo(10,7,5,8,0,0);leaf.close();}
    void start(){active=true;handler.removeCallbacks(tick);handler.post(tick);}
    void stop(){active=false;handler.removeCallbacks(tick);}
    @Override protected void onDraw(Canvas c){if(home.background==null)return;c.save();c.scale(getWidth()/1920f,getHeight()/1080f);c.clipRect(home.ambient?new RectF(0,0,1920,1080):new RectF(632,32,1888,918));float t=(SystemClock.elapsedRealtime()-epoch)/1000f;float drift=(t*3)%1600;
      p.setColor(Color.WHITE);p.setAlpha(180);c.drawBitmap(mist,null,new RectF(450+drift,145,2050+drift,545),p);c.drawBitmap(mist,null,new RectF(-1150+drift,145,450+drift,545),p);
      float gust=t%42;if(gust<8){float fade=(float)Math.sin(gust/8*Math.PI);for(int i=0;i<5;i++){float phase=(gust+i*.85f)%8;c.save();c.translate(1850-phase*48+i*13,145+phase*60+i*18);c.rotate(phase*36+i*41);float scale=.5f+i*.1f;c.scale(scale,scale);p.setColor(0xffb97b37);p.setAlpha((int)(140*fade));c.drawPath(leaf,p);c.restore();}}p.setAlpha(255);c.restore();}
  }
  class HomeView extends View {
    Bitmap background;Bitmap[] logos=new Bitmap[6];int selected=0,lastApp=0;boolean ambient=false;
    final Paint paint=new Paint(3);final int white=Color.rgb(242,240,231),muted=Color.rgb(168,174,173),gold=Color.rgb(224,208,166);
    final String[] controls={"All apps","Inputs","Ambient","Settings","Watching"};
    final float[] controlX={108,300,478,682,893};
    HomeView(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Sol Home. Use arrows to choose an app tile or bottom control, and OK to open.");}
    void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif"+(bold?"-medium":"-light"),Typeface.NORMAL));c.drawText(s,x,y,paint);}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);c.save();c.scale(getWidth()/1920f,getHeight()/1080f);c.drawColor(Color.rgb(26,29,30));
      RectF window=ambient?new RectF(0,0,1920,1080):new RectF(632,32,1888,918);
      if(background!=null){paint.setColor(Color.WHITE);paint.setShader(null);float target=window.width()/window.height(),source=background.getWidth()/(float)background.getHeight();Rect src;if(source>target){int w=(int)(background.getHeight()*target);src=new Rect((background.getWidth()-w)/2,0,(background.getWidth()+w)/2,background.getHeight());}else{int h=(int)(background.getWidth()/target);src=new Rect(0,(background.getHeight()-h)/2,background.getWidth(),(background.getHeight()+h)/2);}c.drawBitmap(background,src,window,paint);}
      if(ambient){c.restore();return;}
      paint.setColor(0xff10191a);c.drawRect(0,0,612,1080,paint);
      paint.setColor(0xff63706b);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);c.drawRect(window,paint);paint.setStyle(Paint.Style.FILL);
      text(c,"SOL",108,82,25,white,true);text(c,"/",174,82,23,muted,false);text(c,"HOME",201,82,17,muted,false);
      Date now=new Date();text(c,new SimpleDateFormat("h:mm",Locale.US).format(now),103,207,99,white,false);
      text(c,new SimpleDateFormat("a",Locale.US).format(now),368,204,19,muted,false);
      text(c,new SimpleDateFormat("EEEE, MMMM d",Locale.US).format(now),108,252,23,muted,false);
      paint.setColor(0x60c4c6bf);c.drawRect(108,286,160,288,paint);
      for(int i=0;i<names.length;i++){
        float x=108+(i%2)*233,y=326+(i/2)*166;boolean focus=selected==i;RectF tile=new RectF(x,y,x+200,y+113);
        paint.setColor(focus?0xffe0d0a6:0xff344240);c.drawRoundRect(new RectF(x-3,y-3,x+203,y+116),8,8,paint);
        paint.setColor(0xff192322);c.drawRoundRect(tile,6,6,paint);
        if(logos[i]!=null){paint.setColor(Color.WHITE);c.drawBitmap(logos[i],null,tile,paint);}else text(c,names[i],x+18,y+67,25,white,true);
        text(c,names[i],x,y+144,21,focus?white:muted,focus);
      }
      paint.setShader(new LinearGradient(0,850,0,1080,0x00101418,0xc4101418,Shader.TileMode.CLAMP));c.drawRect(0,850,1920,1080,paint);paint.setShader(null);
      for(int i=0;i<controls.length;i++){
        boolean focus=selected==6+i;float x=controlX[i];String label=controls[i]+(i==4&&!watching.isEmpty()?" · "+watching.size():"");
        text(c,label,x,990,24,focus?white:muted,focus);
        if(focus){paint.setColor(gold);c.drawRect(x,1004,x+paint.measureText(label),1007,paint);}
      }
      String summary=weather==null?"Forecast unavailable":weather.optString("summary");if(summary.length()>32)summary=summary.substring(0,29)+"…";
      String temp=weather==null?"—":weather.optInt("temperature")+"°";
      text(c,"CINCINNATI",1443,949,17,muted,true);text(c,temp,1440,1011,48,white,false);
      text(c,summary,1553,985,21,white,false);
      String detail=weather==null?"NWS forecast":weather.optString("period")+" · NWS"+(weather.optBoolean("stale")?" · saved":"");
      text(c,detail,1553,1015,16,muted,false);
      c.restore();
    }
    void activate(){
      if(selected<6){lastApp=selected;openApp(favorites[selected]);}
      else switch(selected){case 6:allApps();break;case 7:inputs();break;case 8:ambient=true;invalidate();break;case 9:settings();break;case 10:continueWatching();break;}
    }
    @Override public boolean onKeyDown(int key,KeyEvent e){
      if(ambient){ambient=false;invalidate();return true;}
      switch(key){
        case KeyEvent.KEYCODE_DPAD_UP:if(selected>=6)selected=lastApp;else selected=Math.max(selected%2,selected-2);break;
        case KeyEvent.KEYCODE_DPAD_DOWN:if(selected<4)selected+=2;else if(selected<6){lastApp=selected;selected=6;}break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:if(selected<6){if(selected%2==0)selected++;else{lastApp=selected;selected=6;}}else selected=Math.min(10,selected+1);break;
        case KeyEvent.KEYCODE_DPAD_LEFT:if(selected>6)selected--;else if(selected==6)selected=lastApp;else if(selected%2==1)selected--;break;
        case KeyEvent.KEYCODE_DPAD_CENTER:case KeyEvent.KEYCODE_ENTER:if(e.getRepeatCount()==0)activate();return true;
        default:return super.onKeyDown(key,e);
      }
      if(selected<6)lastApp=selected;invalidate();return true;
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=android.view.MotionEvent.ACTION_UP)return true;
      if(ambient){ambient=false;invalidate();return true;}
      float x=e.getX()*1920/getWidth(),y=e.getY()*1080/getHeight();
      if(x>=88&&x<570&&y>=310&&y<824){selected=Math.min(5,Math.max(0,(int)((y-326)/166))*2+(x>=328?1:0));activate();}
      else if(y>920&&x<1150){for(int i=controls.length-1;i>=0;i--)if(x>=controlX[i]-20){selected=6+i;activate();break;}}
      else if(y>880&&x>1380){new AlertDialog.Builder(MainActivity.this).setTitle("Cincinnati forecast").setMessage(weather==null?"Forecast unavailable":weather.optString("detail")+"\n\nNational Weather Service · Cincinnati city center").setPositiveButton("Close",null).show();}
      invalidate();return true;
    }
  }
}
