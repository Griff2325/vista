package com.griffin.solhome;

import android.content.*;
import android.os.*;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** One bounded cache shared by Home and the system dream; no account credentials. */
public final class WidgetRepository {
    private static WidgetRepository instance;
    private final SharedPreferences cache,settings;
    private final CodingUsageStore coding;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newFixedThreadPool(2);
    private final Set<String> loading=new HashSet<>();
    private final Map<String,Long> attempts=new HashMap<>();
    private final Set<Runnable> listeners=new HashSet<>();
    public static synchronized WidgetRepository get(Context context){if(instance==null)instance=new WidgetRepository(context.getApplicationContext());return instance;}
    private WidgetRepository(Context context){cache=context.getSharedPreferences("widgets",0);settings=context.getSharedPreferences("sol-settings",0);coding=new CodingUsageStore(context);}
    CodingUsage codingUsage(){return coding.get();}
    void importCodingUsage(String json)throws JSONException{coding.importSnapshot(json);main.post(this::notifyListeners);}
    public void listen(Runnable listener){listeners.add(listener);}
    public void unlisten(Runnable listener){listeners.remove(listener);}
    public JSONObject data(String key){try{return new JSONObject(cache.getString(key,"{}"));}catch(Exception e){return new JSONObject();}}
    public JSONObject weather(){JSONObject d=data("weather");try{d.put("stale",System.currentTimeMillis()-d.optLong("at")>3600000);}catch(Exception ignored){}return d.has("temperature")?d:null;}
    public JSONArray locations(){try{return new JSONArray(settings.getString("snow",defaults().toString()));}catch(Exception e){return defaults();}}
    private JSONArray defaults(){JSONArray list=new JSONArray();list.put(location("Perfect North",39.1492,-84.8866));list.put(location("Breckenridge",39.4750,-106.0600));list.put(location("Alyeska",60.9703,-149.0982));return list;}
    public static JSONObject location(String name,double lat,double lon){JSONObject d=new JSONObject();try{d.put("name",name).put("lat",lat).put("lon",lon);}catch(Exception ignored){}return d;}
    public void saveLocations(JSONArray list){settings.edit().putString("snow",list.toString()).apply();refresh();notifyListeners();}
    public void resetLocations(){saveLocations(defaults());}
    public Set<String> eventInterests(){return new HashSet<>(settings.getStringSet("event-interests",new HashSet<>(Arrays.asList("music","markets","food","sports"))));}
    public void saveEventInterests(Set<String> interests){settings.edit().putStringSet("event-interests",new HashSet<>(interests)).apply();notifyListeners();}
    public String eventWhen(JSONObject item){if(item.optBoolean("foodNews")){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("EEE, MMM d",Locale.US);f.setTimeZone(TimeZone.getTimeZone(CincinnatiSky.ZONE_ID));return "Food discovery · reported "+f.format(new Date(item.optLong("date")));}return EventPolicy.when(item.optLong("date"),item.optLong("end"),item.optBoolean("dateOnly"),System.currentTimeMillis());}
    public List<JSONObject> weeklyEvents(){
        List<JSONObject> result=new ArrayList<>();Set<String> interests=eventInterests(),seen=new HashSet<>();long now=System.currentTimeMillis();
        for(String key:new String[]{"park","square","concerts","bengals","fcc","reds","food"}){
            JSONArray items=data(key).optJSONArray("items");if(items==null)continue;
            for(int i=0;i<items.length();i++){JSONObject item=items.optJSONObject(i);if(item==null)continue;
                try{if(key.equals("concerts"))item.put("venue","Memorial Hall").put("source","Memorial Hall").put("category","music").put("dateOnly",true);
                    if(key.equals("food"))item.put("venue","Cincinnati food & drink").put("source","WCPO").put("category","food").put("foodNews",true);
                }catch(Exception ignored){}
                long at=item.optLong("date");boolean relevant=item.optBoolean("foodNews")?at<=now+300000&&at>=now-7*86400000L:EventPolicy.upcoming(at,item.optLong("end"),now)&&(item.optBoolean("dateOnly")||item.optLong("end")>0||at>=now);
                if(relevant&&interests.contains(item.optString("category"))&&!EventPolicy.routine(item.optString("title"))&&seen.add(item.optString("title").toLowerCase(Locale.US)+":"+at))result.add(item);
            }
        }
        // Upcoming events first, then explicitly dated food reporting. All sources remain one list.
        result.sort((a,b)->{if(a.optBoolean("foodNews")!=b.optBoolean("foodNews"))return a.optBoolean("foodNews")?1:-1;return a.optBoolean("foodNews")?Long.compare(b.optLong("date"),a.optLong("date")):Long.compare(a.optLong("date"),b.optLong("date"));});
        return result.size()>40?new ArrayList<>(result.subList(0,40)):result;
    }
    private void park(String key,String host,String venue){request(key,21600000,()->{
        java.time.LocalDate today=java.time.Instant.now().atZone(java.time.ZoneId.of(CincinnatiSky.ZONE_ID)).toLocalDate();
        JSONArray raw=new JSONArray(http(host+"/wp-json/events-widgets-cs/v1/events?start_date="+today+"&end_date="+today.plusDays(7)+"&per_page=100")),items=new JSONArray();
        for(int i=0;i<raw.length()&&i<100;i++){JSONObject row=raw.getJSONObject(i);try{String title=FeedParser.clean(row.optString("title")),detail=FeedParser.clean(row.optString("description_text",row.optString("excerpt")),650),link=row.optString("permalink");long start=java.time.OffsetDateTime.parse(row.getString("start")).toInstant().toEpochMilli(),end=java.time.OffsetDateTime.parse(row.getString("end")).toInstant().toEpochMilli();
            if(link.startsWith(host+"/")&&!EventPolicy.routine(title))items.put(new JSONObject().put("title",title).put("detail",detail).put("link",link).put("date",start).put("end",end).put("dateOnly",row.optBoolean("all_day")).put("category",EventPolicy.category(title+" "+detail)).put("venue",venue).put("source",venue));
        }catch(Exception ignored){}}
        return new JSONObject().put("items",items);
    });}
    private void sports(String key,String league,String team){request(key,21600000,()->{
        int year=java.time.LocalDate.now(java.time.ZoneId.of(CincinnatiSky.ZONE_ID)).getYear();JSONObject root=json("https://site.api.espn.com/apis/site/v2/sports/"+league+"/teams/"+team+"/schedule?season="+year);JSONArray raw=root.optJSONArray("events"),items=new JSONArray();
        if(raw==null)throw new IOException("Schedule unavailable");
        for(int i=0;i<raw.length()&&i<150;i++){JSONObject event=raw.getJSONObject(i);try{JSONObject game=event.getJSONArray("competitions").getJSONObject(0);JSONArray teams=game.getJSONArray("competitors");boolean home=false;for(int j=0;j<teams.length();j++){JSONObject t=teams.getJSONObject(j);if(team.equals(t.optString("id"))&&"home".equals(t.optString("homeAway")))home=true;}
            String status=game.optJSONObject("status")==null?"":game.getJSONObject("status").getJSONObject("type").optString("name");if(!home||game.optBoolean("neutralSite")||status.contains("CANCEL")||status.contains("POSTPON"))continue;
            long at=java.time.Instant.parse(event.getString("date")).toEpochMilli();if(!EventPolicy.upcoming(at,0,System.currentTimeMillis()))continue;
            String link=key.equals("bengals")?"https://www.bengals.com/schedule/":"https://www.fccincinnati.com/schedule/";
            items.put(new JSONObject().put("title",event.getString("name")).put("date",at).put("dateOnly",!event.optBoolean("timeValid",false)).put("venue",game.getJSONObject("venue").optString("fullName")).put("category","sports").put("source","ESPN schedule").put("link",link).put("detail","Home game. Confirm the schedule and tickets with the team; times may change."));
        }catch(Exception ignored){}}
        return new JSONObject().put("items",items);
    });}
    private void baseball(){request("reds",21600000,()->{
        java.time.LocalDate today=java.time.LocalDate.now(java.time.ZoneId.of(CincinnatiSky.ZONE_ID));JSONObject root=json("https://statsapi.mlb.com/api/v1/schedule?sportId=1&teamId=113&startDate="+today+"&endDate="+today.plusDays(7));JSONArray days=root.getJSONArray("dates"),items=new JSONArray();
        for(int i=0;i<days.length();i++){JSONArray games=days.getJSONObject(i).getJSONArray("games");for(int j=0;j<games.length();j++){JSONObject game=games.getJSONObject(j),teams=game.getJSONObject("teams"),home=teams.getJSONObject("home").getJSONObject("team");if(home.getInt("id")!=113)continue;String state=game.getJSONObject("status").optString("detailedState");if(state.contains("Cancelled")||state.contains("Postponed"))continue;
            items.put(new JSONObject().put("title",teams.getJSONObject("away").getJSONObject("team").getString("name")+" at Cincinnati Reds").put("date",java.time.Instant.parse(game.getString("gameDate")).toEpochMilli()).put("dateOnly",game.optBoolean("startTimeTBD")).put("venue",game.getJSONObject("venue").getString("name")).put("category","sports").put("source","MLB schedule").put("link","https://www.mlb.com/reds/schedule").put("detail","Home game. Confirm the schedule and tickets with the Reds."));
        }}return new JSONObject().put("items",items);
    });}
    public static String snowKey(JSONObject loc){return "snow:"+loc.optDouble("lat")+","+loc.optDouble("lon");}
    public void refresh(){
        request("weather",600000,()->{
            JSONObject point=json(nwsPoint(39.1031,-84.5120)).getJSONObject("properties");JSONArray periods=json(nws(point.getString("forecast"))).getJSONObject("properties").getJSONArray("periods");JSONObject first=periods.getJSONObject(0);
            JSONObject result=new JSONObject().put("temperature",first.getInt("temperature")).put("unit",first.getString("temperatureUnit")).put("summary",first.getString("shortForecast")).put("period",first.getString("name")).put("detail",first.getString("detailedForecast"));
            JSONArray outlook=new JSONArray();for(int i=0;i<Math.min(4,periods.length());i++){JSONObject period=periods.getJSONObject(i),entry=new JSONObject().put("name",period.getString("name")).put("temperature",period.getInt("temperature")).put("unit",period.getString("temperatureUnit")).put("summary",period.getString("shortForecast"));JSONObject chance=period.optJSONObject("probabilityOfPrecipitation");if(chance!=null&&!chance.isNull("value"))entry.put("chance",chance.getInt("value"));outlook.put(entry);}result.put("forecast",outlook).put("weatherSchema",2);
            JSONObject chance=first.optJSONObject("probabilityOfPrecipitation");if(chance!=null&&!chance.isNull("value"))result.put("chance",chance.getInt("value"));
            result.put("sceneSummary",first.getString("shortForecast")).put("sceneSource","NWS Cincinnati forecast").put("sceneObservedAt",0).put("sceneStale",true);
            try {
                JSONObject observed=json("https://api.weather.gov/stations/KLUK/observations/latest").getJSONObject("properties");
                long at=java.time.OffsetDateTime.parse(observed.getString("timestamp")).toInstant().toEpochMilli();
                String description=observed.optString("textDescription","");
                if(!description.isEmpty()&&System.currentTimeMillis()-at<3*3600000L&&at<System.currentTimeMillis()+300000){
                    result.put("sceneSummary",description).put("sceneSource","NWS KLUK observation").put("sceneObservedAt",at).put("sceneStale",false);
                    result.put("observedAt",at).put("condition",description);double current=temperatureF(observed.optJSONObject("temperature")),feels=temperatureF(observed.optJSONObject("windChill"));if(!Double.isFinite(feels))feels=temperatureF(observed.optJSONObject("heatIndex"));if(Double.isFinite(current))result.put("currentTemperature",Math.round(current));if(Double.isFinite(feels))result.put("feelsLike",Math.round(feels));
                    JSONObject humidity=observed.optJSONObject("relativeHumidity");if(humidity!=null&&!humidity.isNull("value")){double value=humidity.getDouble("value");if(value>=0&&value<=100)result.put("humidity",Math.round(value));}
                    JSONObject speed=observed.optJSONObject("windSpeed"),direction=observed.optJSONObject("windDirection");
                    if(speed!=null&&!speed.isNull("value")){double mps=WeatherValues.metersPerSecond(speed.getDouble("value"),speed.optString("unitCode"));if(Double.isFinite(mps))result.put("sceneWindMps",mps).put("windMph",Math.round(mps*2.236936));}
                    if(direction!=null&&!direction.isNull("value"))result.put("sceneWindDirection",direction.getDouble("value"));
                }
            }catch(Exception ignored){}
            return result;
        });
        feed("ai","https://news.mit.edu/rss/topic/artificial-intelligence2",false);
        feed("world","https://feeds.bbci.co.uk/news/world/rss.xml",false);
        park("park","https://washingtonpark.org","Washington Park");
        park("square","https://myfountainsquare.com","Fountain Square");
        feed("concerts","https://www.memorialhallotr.com/cincinnati-shows/",true);
        feed("food","https://www.wcpo.com/news/our-community/food.rss",false);
        sports("bengals","football/nfl","4");sports("fcc","soccer/usa.1","18267");
        baseball();
        JSONArray locations=locations();for(int i=0;i<locations.length();i++){JSONObject loc=locations.optJSONObject(i);if(loc==null)continue;
            request(snowKey(loc),3600000,()->{
                JSONObject point=json(nwsPoint(loc.getDouble("lat"),loc.getDouble("lon"))).getJSONObject("properties");JSONObject raw=json(nws(point.getString("forecastGridData"))).getJSONObject("properties");JSONObject snow=raw.getJSONObject("snowfallAmount");JSONArray rows=snow.getJSONArray("values");List<SnowForecast.Interval> values=new ArrayList<>();
                for(int n=0;n<rows.length();n++){JSONObject row=rows.getJSONObject(n);if(!row.isNull("value"))values.add(new SnowForecast.Interval(row.getString("validTime"),row.getDouble("value")));}
                long now=System.currentTimeMillis();JSONObject result=new JSONObject().put("name",loc.getString("name")).put("start",now).put("issued",raw.optString("updateTime")).put("schema",4);double inches=Double.NaN;
                for(int hours:new int[]{72,48,24}){inches=SnowForecast.inches(values,snow.getString("uom"),now,now+hours*3600000L);if(Double.isFinite(inches)){result.put("hours",hours).put("end",now+hours*3600000L);break;}}
                if(Double.isFinite(inches))result.put("inches",inches);else result.put("unavailable",true);return result;
            });
        }
    }
    private interface Fetch {JSONObject run() throws Exception;}
    private void request(String key,long ttl,Fetch fetch){
        long now=System.currentTimeMillis();if((now-data(key).optLong("at")<ttl&&(!key.equals("weather")||data(key).optInt("weatherSchema")==2)&&(!key.startsWith("snow:")||data(key).optInt("schema")==4)&&(!(key.equals("ai")||key.equals("world")||key.equals("concerts")||key.equals("food"))||data(key).optInt("schema")==4))||loading.contains(key)||now-attempts.getOrDefault(key,0L)<300000)return;
        loading.add(key);attempts.put(key,now);io.execute(()->{
            try{JSONObject value=fetch.run().put("at",System.currentTimeMillis());cache.edit().putString(key,value.toString()).apply();Log.i("SolWidgets",key.split(":")[0]+" refreshed");}
            catch(Exception e){Log.w("SolWidgets",key.split(":")[0]+" unavailable: "+e.getClass().getSimpleName());}
            finally{main.post(()->{loading.remove(key);notifyListeners();});}
        });
    }
    private void feed(String key,String url,boolean concerts){request(key,concerts||key.equals("events")?21600000:1800000,()->{
        String raw=http(url);List<FeedParser.Item> items=concerts?FeedParser.concerts(raw,System.currentTimeMillis()):FeedParser.rss(raw,key.equals("ai"));JSONArray list=new JSONArray();
        for(FeedParser.Item item:items)list.put(new JSONObject().put("title",item.title).put("link",item.link).put("detail",item.detail).put("date",item.date));
        if(list.length()==0)throw new IOException("No usable entries");return new JSONObject().put("items",list).put("schema",4);
    });}
    private void notifyListeners(){for(Runnable listener:new ArrayList<>(listeners))listener.run();}
    private static double temperatureF(JSONObject quantity){return quantity==null||quantity.isNull("value")?Double.NaN:WeatherValues.fahrenheit(quantity.optDouble("value",Double.NaN),quantity.optString("unitCode"));}
    private static String nwsPoint(double lat,double lon){return String.format(Locale.US,"https://api.weather.gov/points/%.4f,%.4f",lat,lon);}
    private static String nws(String url)throws IOException{URI u=URI.create(url);if(!"https".equals(u.getScheme())||!"api.weather.gov".equals(u.getHost()))throw new IOException("Unexpected forecast host");return url;}
    private static JSONObject json(String url)throws Exception{return new JSONObject(http(url));}
    private static String http(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Vista/0.5 (https://github.com/Griff2325/vista)");c.setRequestProperty("Accept","application/geo+json, application/rss+xml, text/html");
        try(InputStream input=c.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(output.size()+count>2500000)throw new IOException("Response too large");output.write(buffer,0,count);}return output.toString("UTF-8");
        }finally{c.disconnect();}
    }
}
