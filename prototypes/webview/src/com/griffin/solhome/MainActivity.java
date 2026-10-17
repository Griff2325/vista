package com.griffin.solhome;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputInfo;
import android.media.tv.TvContract;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  private WebView web;
  private boolean ready=false;
  private final Map<String,String> resumeLinks = new HashMap<>();
  private final String[] favorites = {"com.amazon.firetv.youtube","com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.spotify.tv.android","com.apple.atve.amazon.appletv"};
  private final String[] names = {"YouTube","Netflix","Hulu","Disney+","Spotify","Apple TV"};
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(5894);
    web = new WebView(this);
    web.setBackgroundColor(0xfff2f0e4);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(false);
    web.getSettings().setAllowFileAccess(true);
    web.getSettings().setAllowFileAccessFromFileURLs(false);
    web.getSettings().setAllowUniversalAccessFromFileURLs(false);
    web.addJavascriptInterface(new Bridge(), "TV");
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView w,String url){return true;}
      @Override public void onPageFinished(WebView w,String url){ready=true; w.requestFocus(); refresh(); loadWeather();}
    });
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }
  @Override protected void onResume(){super.onResume(); if(web!=null){web.onResume();web.resumeTimers();} if(ready){refresh();loadWeather();}}
  @Override protected void onPause(){if(web!=null){web.onPause();web.pauseTimers();}super.onPause();}
  @Override public void onBackPressed(){web.evaluateJavascript("closePanel()",null);}
  private void send(String function, Object value){runOnUiThread(()->{if(!isFinishing()&&ready) web.evaluateJavascript(function+"("+value.toString()+")",null);});}
  private void toast(String s){runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
  private void openApp(String pkg){
    try { Intent i=getPackageManager().getLeanbackLaunchIntentForPackage(pkg); if(i==null)i=getPackageManager().getLaunchIntentForPackage(pkg);
      if(i==null){toast("This app is not available."); return;} i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
    }catch(Exception e){toast("Could not open this app.");}
  }
  private void refresh(){
    new Thread(()->{
      JSONArray apps=new JSONArray();
      for(int k=0;k<favorites.length;k++){try{getPackageManager().getApplicationInfo(favorites[k],0);apps.put(new JSONObject().put("name",names[k]).put("pkg",favorites[k]));}catch(Exception ignored){}}
      send("setFavorites",apps);
      JSONArray programs=new JSONArray();
      synchronized(resumeLinks){resumeLinks.clear();}
      try(Cursor c=getContentResolver().query(Uri.parse("content://android.media.tv/watch_next_program"),new String[]{"_id","package_name","title","intent_uri","last_playback_position_millis","duration_millis"},null,null,"last_engagement_time_utc_millis DESC")){
        if(c!=null) while(c.moveToNext()&&programs.length()<12){
          String id=c.getString(0),pkg=c.getString(1),title=c.getString(2),link=c.getString(3);
          if(title==null||link==null)continue;
          synchronized(resumeLinks){resumeLinks.put(id,link);}
          String label=pkg;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
          programs.put(new JSONObject().put("id",id).put("title",title).put("app",label).put("position",c.getLong(4)).put("duration",c.getLong(5)));
        }
        send("setContinue",new JSONObject().put("available",true).put("items",programs));
      }catch(Exception e){try{send("setContinue",new JSONObject().put("available",false).put("items",programs));}catch(Exception ignored){}}
    },"sol-data").start();
  }
  private JSONObject getJson(String url) throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setRequestProperty("User-Agent","SolHome/0.1 (personal Cincinnati weather display)");c.setRequestProperty("Accept","application/geo+json");
    try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"))){StringBuilder s=new StringBuilder();String l;while((l=r.readLine())!=null)s.append(l);return new JSONObject(s.toString());}finally{c.disconnect();}
  }
  private void loadWeather(){
    SharedPreferences p=getSharedPreferences("weather",0);
    String cached=p.getString("value",null);long at=p.getLong("at",0);
    if(cached!=null)try{send("setWeather",new JSONObject(cached).put("stale",System.currentTimeMillis()-at>3600000));}catch(Exception ignored){}
    if(cached!=null&&System.currentTimeMillis()-at<1800000)return;
    new Thread(()->{try{
      String endpoint=getJson("https://api.weather.gov/points/39.1031,-84.5120").getJSONObject("properties").getString("forecast");
      if(!endpoint.startsWith("https://api.weather.gov/"))throw new IOException("Unexpected forecast source");
      JSONObject first=getJson(endpoint).getJSONObject("properties").getJSONArray("periods").getJSONObject(0);
      JSONObject data=new JSONObject().put("temperature",first.getInt("temperature")).put("unit",first.getString("temperatureUnit")).put("summary",first.getString("shortForecast")).put("period",first.getString("name")).put("detail",first.getString("detailedForecast")).put("stale",false);
      p.edit().putString("value",data.toString()).putLong("at",System.currentTimeMillis()).apply();send("setWeather",data);
    }catch(Exception e){if(cached==null)send("weatherUnavailable",new JSONObject());}},"sol-weather").start();
  }
  class Bridge {
    @JavascriptInterface public void launch(String pkg){runOnUiThread(()->openApp(pkg));}
    @JavascriptInterface public void settings(){runOnUiThread(()->{try{startActivity(new Intent("android.settings.SETTINGS"));}catch(Exception e){toast("Hold Home to open TV settings.");}});}
    @JavascriptInterface public void inputs(){
      JSONArray items=new JSONArray();
      try{TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);
        for(TvInputInfo input:manager.getTvInputList())if(input.isPassthroughInput()){
          CharSequence custom=input.loadCustomLabel(MainActivity.this);CharSequence label=custom!=null?custom:input.loadLabel(MainActivity.this);
          items.put(new JSONObject().put("id",input.getId()).put("name",label==null?"TV input":label.toString()));
        }
      }catch(Exception e){toast("Use the Input button on your remote.");}
      send("showInputs",items);
    }
    @JavascriptInterface public void openInput(String id){runOnUiThread(()->{
      try{TvInputManager manager=(TvInputManager)getSystemService(TV_INPUT_SERVICE);TvInputInfo input=manager.getTvInputInfo(id);
        if(input==null||!input.isPassthroughInput())return;
        startActivity(new Intent(Intent.ACTION_VIEW,TvContract.buildChannelUriForPassthroughInput(id)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      }catch(Exception e){toast("Use the Input button on your remote to switch to this input.");}
    });}
    @JavascriptInterface public void allApps(){
      JSONArray out=new JSONArray();TreeMap<String,String> found=new TreeMap<>();
      for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(category);
        for(ResolveInfo r:getPackageManager().queryIntentActivities(intent,0)) if(!r.activityInfo.packageName.equals(getPackageName()))found.put(r.loadLabel(getPackageManager()).toString(),r.activityInfo.packageName);
      }
      for(Map.Entry<String,String> a:found.entrySet())try{out.put(new JSONObject().put("name",a.getKey()).put("pkg",a.getValue()));}catch(Exception ignored){}
      send("showApps",out);
    }
    @JavascriptInterface public void resume(String id){runOnUiThread(()->{
      String link; synchronized(resumeLinks){link=resumeLinks.get(id);}if(link==null)return;
      try{Intent i=Intent.parseUri(link,Intent.URI_INTENT_SCHEME);i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);i.setSelector(null);i.setClipData(null);startActivity(i);}catch(Exception e){toast("Please resume this title inside its app.");}
    });}
  }
}
