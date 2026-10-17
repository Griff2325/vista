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
  private PixelSceneView scene;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private boolean foreground=false;
  private JSONObject weather;
  private final ArrayList<JSONObject> watching=new ArrayList<>();
  private final String[] favorites={"com.amazon.firetv.youtube","com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.spotify.tv.android","com.apple.atve.amazon.appletv"};
  private final String[] names={"YouTube","Netflix","Hulu","Disney+","Spotify","Apple TV"};
  private final Runnable minuteTick=new Runnable(){public void run(){if(foreground){home.invalidate();loadWeather();handler.postDelayed(this,60000);}}};
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
    home=new HomeView();scene=new PixelSceneView(this);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff17242a);root.addView(scene);root.addView(home);setContentView(root);home.requestFocus();
    new Thread(()->{
      Bitmap[] logos=new Bitmap[6];for(int n=0;n<6;n++){try{PackageManager pm=getPackageManager();Intent intent=pm.getLeanbackLaunchIntentForPackage(favorites[n]);Drawable d=intent!=null?pm.getActivityBanner(intent.getComponent()):null;if(d==null)d=pm.getApplicationBanner(favorites[n]);if(d==null)d=pm.getApplicationIcon(favorites[n]);Bitmap icon=Bitmap.createBitmap(256,144,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(icon);float ratio=Math.min(256f/Math.max(1,d.getIntrinsicWidth()),144f/Math.max(1,d.getIntrinsicHeight()));int w=Math.round(d.getIntrinsicWidth()*ratio),h=Math.round(d.getIntrinsicHeight()*ratio);d.setBounds((256-w)/2,(144-h)/2,(256+w)/2,(144+h)/2);d.draw(c);logos[n]=icon;}catch(Exception ignored){}}
      runOnUiThread(()->{home.logos=logos;home.invalidate();});
    },"sol-logos").start();
  }
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);scene.setPreviewTime(intent.getLongExtra("sol.preview.epoch",0));}
  @Override protected void onResume(){super.onResume();foreground=true;home.ambient=false;home.requestFocus();home.invalidate();handler.removeCallbacks(minuteTick);handler.post(minuteTick);scene.setPreviewTime(getIntent().getLongExtra("sol.preview.epoch",0));scene.start();loadWatching();}
  @Override protected void onPause(){foreground=false;handler.removeCallbacks(minuteTick);scene.stop();super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);scene.release();super.onDestroy();}
  @Override public void onBackPressed(){home.ambient=false;home.selected=home.lastApp;home.invalidate();}
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
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","SolHome/0.4 (Cincinnati forecast)");c.setRequestProperty("Accept","application/geo+json");
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
  class HomeView extends View {
    Bitmap[] logos=new Bitmap[6];int selected=0,lastApp=0;boolean ambient=false;
    final Paint paint=new Paint(3);final int white=Color.rgb(242,240,231),muted=Color.rgb(168,174,173),gold=Color.rgb(207,182,132);
    final String[] controls={"All apps","Inputs","Ambient","Settings","Watching"};
    final float[] controlX={100,300,492,700,930};
    final Shader wallLight=new LinearGradient(0,0,1920,900,0xff192b2c,0xff30403c,Shader.TileMode.CLAMP);
    final Shader railLight=new LinearGradient(72,0,577,0,0xff152323,0xff1c2e2c,Shader.TileMode.CLAMP);
    final Shader linenLight=new LinearGradient(620,0,682,0,new int[]{0xff3c514c,0xff7e8c7c,0xff566b60,0xff8d9886},null,Shader.TileMode.CLAMP);
    HomeView(){super(MainActivity.this);setFocusable(true);setFocusableInTouchMode(true);setContentDescription("Sol Home. Use arrows to choose an app tile or bottom control, and OK to open.");}
    void text(Canvas c,String s,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif"+(bold?"-medium":"-light"),Typeface.NORMAL));c.drawText(s,x,y,paint);}
    void drawRoom(Canvas c){
      float l=PixelSceneView.WINDOW_LEFT,t=PixelSceneView.WINDOW_TOP,r=PixelSceneView.WINDOW_RIGHT,b=PixelSceneView.WINDOW_BOTTOM;
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
      // A gathered linen edge sits entirely beside the opening, preserving the view.
      Path curtain=new Path();curtain.moveTo(624,48);curtain.lineTo(681,48);curtain.cubicTo(683,220,662,440,660,578);curtain.cubicTo(657,700,665,840,674,914);curtain.cubicTo(654,923,635,910,617,914);curtain.cubicTo(630,735,625,648,628,568);curtain.cubicTo(633,338,620,178,624,48);curtain.close();paint.setShader(linenLight);c.drawPath(curtain,paint);paint.setShader(null);
      paint.setColor(0x35708372);paint.setStrokeWidth(2);paint.setStyle(Paint.Style.STROKE);Path fold=new Path();fold.moveTo(651,50);fold.cubicTo(660,250,642,445,642,581);fold.cubicTo(640,728,644,847,650,914);c.drawPath(fold,paint);paint.setStyle(Paint.Style.FILL);
      paint.setColor(0xff394c48);c.drawRoundRect(new RectF(621,40,686,48),4,4,paint);
      // The bottom controls belong to a continuous shallow console below the sill.
      paint.setColor(0xff101d1f);c.drawRect(0,944,1920,1080,paint);paint.setColor(0xff666a55);c.drawRect(0,942,1920,946,paint);
      paint.setColor(0xff8b8d70);c.drawRect(0,942,1920,943,paint);paint.setColor(0xff233631);c.drawRect(0,951,1920,953,paint);
      paint.setColor(0xff30413a);c.drawRect(1204,970,1206,1044,paint);
    }
    void drawAppNiche(Canvas c){
      paint.setShader(null);paint.setColor(0x30081113);c.drawRoundRect(new RectF(74,46,584,889),18,18,paint);
      paint.setShader(railLight);c.drawRoundRect(new RectF(77,40,580,880),16,16,paint);paint.setShader(null);
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
      super.onDraw(c);scene.setAmbient(ambient);c.save();c.scale(getWidth()/1920f,getHeight()/1080f);
      drawRoom(c);if(ambient){c.restore();return;}
      drawAppNiche(c);
      text(c,"SOL",108,82,25,white,true);text(c,"/",174,82,23,muted,false);text(c,"HOME",201,82,17,muted,false);
      Date now=new Date();text(c,localTime("h:mm",now),103,207,99,white,false);
      text(c,localTime("a",now),368,204,19,muted,false);
      text(c,localTime("EEEE, MMMM d",now),108,252,23,muted,false);
      for(int i=0;i<names.length;i++){
        float x=108+(i%2)*233,y=326+(i/2)*166;boolean focus=selected==i;RectF tile=new RectF(x,y,x+200,y+113);
        paint.setColor(focus?0xffcfb684:0xff3c4c4b);c.drawRoundRect(new RectF(x-3,y-3,x+203,y+116),8,8,paint);
        paint.setColor(0xff192322);c.drawRoundRect(tile,6,6,paint);
        if(logos[i]!=null){paint.setColor(Color.WHITE);c.drawBitmap(logos[i],null,tile,paint);}else text(c,names[i],x+18,y+67,25,white,true);
        text(c,names[i],x,y+144,21,focus?white:muted,focus);
      }
      for(int i=0;i<controls.length;i++){
        boolean focus=selected==6+i;float x=controlX[i];String label=controls[i]+(i==4&&!watching.isEmpty()?" · "+watching.size():"");
        utilityIcon(c,i,x,986,focus?gold:muted);text(c,label,x+34,1010,22,focus?white:muted,focus);
        if(focus){paint.setColor(gold);c.drawRect(x,1024,x+34+paint.measureText(label),1026,paint);}
      }
      String summary=weather==null?"Forecast unavailable":weather.optString("summary");if(summary.length()>32)summary=summary.substring(0,29)+"…";
      String temp=weather==null?"—":weather.optInt("temperature")+"°";
      text(c,"CINCINNATI",1244,982,16,muted,true);text(c,temp,1240,1036,46,white,false);
      text(c,summary,1403,1001,21,white,false);
      String detail=weather==null?"NWS forecast":weather.optString("period")+" · NWS"+(weather.optBoolean("stale")?" · saved":"");
      text(c,detail,1403,1031,16,muted,false);
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
      else if(y>954&&x<1180){for(int i=controls.length-1;i>=0;i--)if(x>=controlX[i]-20){selected=6+i;activate();break;}}
      else if(y>954&&x>1220){new AlertDialog.Builder(MainActivity.this).setTitle("Cincinnati forecast").setMessage(weather==null?"Forecast unavailable":weather.optString("detail")+"\n\nNational Weather Service · Cincinnati city center").setPositiveButton("Close",null).show();}
      invalidate();return true;
    }
  }
}
