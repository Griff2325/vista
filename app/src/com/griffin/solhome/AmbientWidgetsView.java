package com.griffin.solhome;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.view.View;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Text cards redraw on data changes or a 15-second clock, independently of the landscape. */
public final class AmbientWidgetsView extends View {
    private final Paint paint=new Paint(3);
    private final WidgetRepository repository;
    private final CodingUsageView codingView=new CodingUsageView();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Typeface light=Typeface.create("sans-serif-light",0),medium=Typeface.create("sans-serif-medium",0);
    private final Shader top=new LinearGradient(0,0,0,445,0x99081319,0x00081319,Shader.TileMode.CLAMP);
    private final Shader bottom=new LinearGradient(0,690,0,1080,0x00081319,0xc0081319,Shader.TileMode.CLAMP);
    private boolean active;
    private final Runnable changed=()->invalidate();
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;repository.refresh();invalidate();handler.postDelayed(this,15000);}};
    public AmbientWidgetsView(Context context){super(context);repository=WidgetRepository.get(context);setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void start(){if(active)return;active=true;repository.listen(changed);handler.post(tick);}
    public void stop(){active=false;repository.unlisten(changed);handler.removeCallbacks(tick);}
    private String date(String pattern,long at){SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setTimeZone(TimeZone.getTimeZone(CincinnatiSky.ZONE_ID));return f.format(new Date(at));}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean bold){paint.setShader(null);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(bold?medium:light);c.drawText(value,x,y,paint);}
    private void lines(Canvas c,String value,float x,float y,float width,int count,float size,float leading,int color){
        String rest=FeedParser.clean(value,650);paint.setTextSize(size);paint.setTypeface(light);
        for(int i=0;i<count&&!rest.isEmpty();i++){
            int end=paint.breakText(rest,true,width,null);if(end==0)break;
            if(end<rest.length()){int space=rest.lastIndexOf(' ',end);if(space>0)end=space;}
            String line=rest.substring(0,end);rest=rest.substring(end).trim();
            if(i==count-1&&!rest.isEmpty()){while(paint.measureText(line+"…")>width&&line.length()>1)line=line.substring(0,line.length()-1);line+="…";}
            text(c,line,x,y+i*leading,size,color,false);
        }
    }
    private void card(Canvas c,float x,String label){paint.setShader(null);paint.setColor(0xbd0d1e23);c.drawRoundRect(new RectF(x,775,x+552,1013),16,16,paint);text(c,label,x+24,808,17,0xffc7d0ba,true);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);c.save();c.scale(getWidth()/1920f,getHeight()/1080f);
        paint.setColor(Color.WHITE);paint.setShader(top);c.drawRect(0,0,1920,445,paint);paint.setShader(bottom);c.drawRect(0,690,1920,1080,paint);paint.setShader(null);
        long now=System.currentTimeMillis(),cycle=SystemClock.elapsedRealtime()/45000;
        text(c,date("h:mm",now),96,163,104,0xfff3f3eb,false);text(c,date("a",now),366,160,22,0xffdce2d4,false);text(c,date("EEEE, MMMM d",now),101,207,25,0xffdce2d4,false);
        text(c,WinterCountdown.label(now),101,253,23,0xffdce5ca,false);
        lines(c,SceneSettings.theme(getContext(),now).label(),101,284,950,1,19,24,0xffc4ceba);
        codingView.ambient(c,repository.codingUsage(),now);
        drawWeather(c,now);
        drawNews(c,cycle,now);drawEvents(c,cycle,now);drawSnow(c,cycle,now);
        text(c,"Press a remote button to return · News briefs and This week on Home",96,1050,16,0xffc4ceca,false);c.restore();
    }
    private void drawWeather(Canvas c,long now){
        JSONObject w=repository.weather();text(c,"CINCINNATI",1230,98,18,0xffdce2d4,true);
        if(w==null){text(c,"Weather unavailable",1230,150,28,0xfff3f3eb,false);return;}
        boolean observation=w.has("currentTemperature")&&now-w.optLong("observedAt")<3*3600000L;
        text(c,(observation?w.optInt("currentTemperature"):w.optInt("temperature"))+"°",1226,179,76,0xfff3f3eb,false);
        text(c,"°F · "+(observation?"Observed "+date("h:mm a",w.optLong("observedAt")):"Forecast"),1231,210,15,0xffc1cccd,false);
        String conditions=observation?w.optString("condition"):w.optString("summary");lines(c,conditions,1410,137,414,1,27,30,0xfff3f3eb);
        String comfort=(w.has("feelsLike")?"Feels "+w.optInt("feelsLike")+"°":"")+(w.has("humidity")?(w.has("feelsLike")?" · ":"")+"Humidity "+w.optInt("humidity")+"%":"");
        text(c,comfort,1410,170,20,0xffdce2df,false);
        String wind=w.has("windMph")?"Wind "+WeatherValues.bearing(w.optDouble("sceneWindDirection",Double.NaN))+" "+w.optInt("windMph")+" mph":"Wind unavailable";
        text(c,wind,1410,201,20,0xffdce2df,false);text(c,w.has("chance")?"Precipitation chance "+w.optInt("chance")+"%":"Precipitation chance unavailable",1410,232,20,0xffdce2df,false);
        JSONArray periods=w.optJSONArray("forecast");if(periods!=null)for(int i=0;i<Math.min(3,periods.length());i++){JSONObject p=periods.optJSONObject(i);if(p==null)continue;float x=1230+i*200;
            lines(c,p.optString("name"),x,279,180,1,17,20,0xffdce2d4);text(c,p.optInt("temperature")+"°"+(p.has("chance")?" · "+p.optInt("chance")+"%":""),x,316,29,0xfff3f3eb,false);lines(c,p.optString("summary"),x,345,180,2,18,22,0xffdce2df);
        }
        text(c,"National Weather Service"+(w.optBoolean("stale")?" · saved data":""),1230,397,15,0xffc1cccd,false);
    }
    private void drawNews(Canvas c,long cycle,long now){String key=cycle%2==0?"ai":"world",source=key.equals("ai")?"MIT News":"BBC News";JSONObject data=repository.data(key);JSONArray items=data.optJSONArray("items");
        card(c,96,key.equals("ai")?"AI RESEARCH · MIT NEWS":"WORLD NEWS · BBC NEWS");
        if(items==null||items.length()==0){lines(c,"News briefs unavailable",120,852,504,2,26,31,0xfff2f2e9);text(c,"Reconnect to refresh this feed",120,990,17,0xffbcc7c8,false);return;}
        JSONObject item=items.optJSONObject((int)((cycle/2)%items.length()));if(item==null)return;
        lines(c,item.optString("title"),120,850,504,2,26,31,0xfff2f2e9);
        String summary=item.optString("detail");if(summary.isEmpty())summary="Open News briefs on Home for the publisher's story.";
        lines(c,summary,120,916,504,3,21,25,0xffdce2df);
        text(c,"Publisher brief · "+source+(item.optLong("date")>0?" · "+date("MMM d",item.optLong("date")):"")+(now-data.optLong("at")>2*3600000L?" · saved":""),120,992,16,0xffbcc7c8,false);
    }
    private void drawEvents(Canvas c,long cycle,long now){card(c,684,"CINCINNATI · NEXT 7 DAYS");List<JSONObject> items=repository.weeklyEvents();
        if(items.isEmpty()){lines(c,"No current picks available",708,852,504,2,26,31,0xfff2f2e9);text(c,"Choose interests in This week on Home",708,990,17,0xffbcc7c8,false);return;}
        JSONObject item=items.get((int)(cycle%items.size()));lines(c,repository.eventWhen(item),708,841,504,1,20,25,0xffd2dcb9);
        lines(c,item.optString("title"),708,878,504,2,26,31,0xfff2f2e9);
        lines(c,item.optString("detail"),708,943,504,1,21,25,0xffdce2df);
        String venue=item.optString("venue"),source=item.optString("source");lines(c,venue.equals(source)?venue:venue+" · "+source,708,991,504,1,16,20,0xffbcc7c8);
    }
    private void drawSnow(Canvas c,long cycle,long now){card(c,1272,"SNOW FORECAST");JSONArray locations=repository.locations();int pages=Math.max(1,(locations.length()+2)/3),start=(int)(cycle%pages)*3;
        boolean stale=false;for(int row=0;row<3&&start+row<locations.length();row++){JSONObject loc=locations.optJSONObject(start+row);if(loc==null)continue;JSONObject snow=repository.data(WidgetRepository.snowKey(loc));float y=851+row*49;
            lines(c,loc.optString("name"),1296,y,320,1,23,28,0xfff2f2e9);String amount=snow.has("inches")?String.format(Locale.US,"~%.1f in / %dh",snow.optDouble("inches"),snow.optInt("hours",72)):"—";
            text(c,amount,1643,y,20,0xffd2dcb9,false);if(now-snow.optLong("at")>2*3600000L)stale=true;
        }
        text(c,"NWS grid · next 24–72h · — unavailable"+(stale?" / saved":""),1296,992,16,0xffbcc7c8,false);
    }
}
