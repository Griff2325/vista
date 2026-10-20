package com.griffin.solhome;

import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional TMDB catalog. No service credentials, session scraping, or playback entitlement inference. */
final class StreamingCatalog implements AutoCloseable {
  static final String REGION="US";
  static final String[] SERVICES={"Netflix","Hulu","Disney+","Apple TV+","Prime Video","Max","Peacock","Paramount+","Tubi","Pluto TV"};
  static final String[] PROVIDER_NAMES={"Netflix","Hulu","Disney Plus","Apple TV Plus","Amazon Prime Video","HBO Max","Peacock Premium","Paramount Plus","Tubi TV","Pluto TV"};
  static final String[] PACKAGES={"com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.apple.atve.amazon.appletv","com.amazon.firebat","com.hbo.hbonow","com.peacock.peacockfiretv","com.cbs.ott","com.tubitv.ott","tv.pluto.android"};
  static final String[] INTERESTS={"Comedy","Drama","Animation","Documentary","Crime","Family","Mystery","Adventure","Action","Sci-Fi & fantasy"};
  private final String token,omdbKey;
  private final ExecutorService providerPool=Executors.newFixedThreadPool(4);
  private final Map<String,JSONObject> availability=Collections.synchronizedMap(new LinkedHashMap<String,JSONObject>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,JSONObject> e){return size()>120;}});
  private JSONArray providerRows;
  private final Map<String,Integer> pageCounts=Collections.synchronizedMap(new LinkedHashMap<String,Integer>(32,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Integer> e){return size()>120;}});
  private final Map<String,JSONObject> ratings=Collections.synchronizedMap(new LinkedHashMap<String,JSONObject>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,JSONObject> e){return size()>120;}});
  StreamingCatalog(String token){this(token,"");}
  StreamingCatalog(String token,String omdbKey){this.token=token==null?"":token.trim();this.omdbKey=omdbKey==null?"":omdbKey.trim();}
  static final class Query {
    String text=""; int type=0,sort=0,page=1,year=0,minRating=0; // type: both / movie / TV
    boolean allServices=true;
    boolean[] services=new boolean[SERVICES.length],interests=new boolean[INTERESTS.length];
    Query(){Arrays.fill(services,true);}
  }
  static final class Page {final ArrayList<JSONObject> items=new ArrayList<>();int totalPages=1;int unavailable;}
  JSONObject get(String path)throws Exception {
    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
    URL url=new URL("https://api.themoviedb.org/3/"+path+(token.matches("[a-fA-F0-9]{32}")?"&api_key="+token:""));
    HttpURLConnection c=(HttpURLConnection)url.openConnection();
    c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);
    c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","Vista/0.5");
    // Accept a v3 API key or a v4 read token; never log either.
    if(!token.matches("[a-fA-F0-9]{32}"))c.setRequestProperty("Authorization","Bearer "+token);
    try{
      int status=c.getResponseCode();
      if(status==401||status==403)throw new IOException("TMDB rejected the API credential. Check Catalog settings.");
      if(status==429)throw new IOException("TMDB request limit reached. Please try again shortly.");
      if(status!=200)throw new IOException("Catalog service unavailable (HTTP "+status+").");
      ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n,total=0;
      try(InputStream in=c.getInputStream()){while((n=in.read(buf))!=-1){total+=n;if(total>2000000)throw new IOException("Catalog response too large.");out.write(buf,0,n);}}
      return new JSONObject(new String(out.toByteArray(),"UTF-8"));
    }finally{c.disconnect();}
  }
  private void providers()throws Exception{
    if(providerRows!=null)return;
    JSONArray rows=new JSONArray();
    for(String media:new String[]{"movie","tv"}){
      JSONArray found=get("watch/providers/"+media+"?watch_region=US&language=en-US").optJSONArray("results");
      if(found!=null)for(int i=0;i<found.length();i++)if(found.optJSONObject(i)!=null)rows.put(found.optJSONObject(i));
    }
    providerRows=rows;
  }
  Page load(Query q)throws Exception{
    Page page=new Page();ArrayList<JSONObject> candidates=new ArrayList<>();
    boolean search=!q.text.trim().isEmpty();
    if(!search&&!q.allServices)providers();
    if(search){
      String type=q.type==0?"multi":q.type==1?"movie":"tv";
      String path="search/"+type+"?include_adult=false&language=en-US&query="+encode(q.text);
      if(q.year>0&&!type.equals("multi"))path+=(type.equals("movie")?"&primary_release_year=":"&first_air_date_year=")+q.year;
      appendPage(page,candidates,path,type.equals("multi")?null:type,q.page);
    }else{
      for(String type:new String[]{"movie","tv"}){
        if((q.type==1&&!type.equals("movie"))||(q.type==2&&!type.equals("tv")))continue;
        String p=q.allServices?"":selectedProviders(q.services);if(!q.allServices&&p.isEmpty())continue;
        String path="discover/"+type+"?include_adult=false&language=en-US&watch_region=US&with_watch_monetization_types=flatrate%7Cfree%7Cads"+(q.allServices?"":"&with_watch_providers="+encode(p))+"&sort_by="+StreamingPolicy.sort(type,q.sort)+"&vote_count.gte="+(q.sort==1?100:0);
        String genres=StreamingPolicy.genres(q.interests,type);if(!genres.isEmpty())path+="&with_genres="+encode(genres);
        if(q.sort==2){String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());path+=(type.equals("movie")?"&primary_release_date.lte=":"&first_air_date.lte=")+today;}
        if(q.year>0)path+=(type.equals("movie")?"&primary_release_year=":"&first_air_date_year=")+q.year;
        if(q.minRating>0)path+="&vote_average.gte="+q.minRating;
        appendPage(page,candidates,path,type,q.page);
      }
      if(q.type==0){final int sort=q.sort;candidates.sort((a,b)->sort==2?date(b).compareTo(date(a)):Double.compare(b.optDouble(sort==1?"vote_average":"popularity"),a.optDouble(sort==1?"vote_average":"popularity")));}
    }
    // Keep both source pages intact; availability requests run four at a time.
    ArrayList<Future<JSONObject>> pending=new ArrayList<>();
    ArrayList<JSONObject> submitted=new ArrayList<>();
    try{
      for(JSONObject candidate:candidates){
        if(!genreMatch(candidate,q))continue;
        if(q.year>0&&!date(candidate).startsWith(q.year+"-"))continue;
        if(q.minRating>0&&!(candidate.optDouble("vote_average",0)>=q.minRating))continue;
        candidate.put("sol_ratings_status",omdbKey.isEmpty()?"Optional free OMDb key not configured":"Ratings not checked yet");
        submitted.add(candidate);pending.add(providerPool.submit(()->enrich(candidate)));
      }
      long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(35);
      for(int i=0;i<pending.size();i++){
        JSONObject item=submitted.get(i);
        try{item=pending.get(i).get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);}
        catch(ExecutionException|TimeoutException e){pending.get(i).cancel(true);page.unavailable++;item.put("sol_availability_status","US availability could not be checked");}
        if(StreamingPolicy.keepResult(q.allServices,matches(item,q.services)||!item.has("sol_availability")))page.items.add(item);
      }
    }finally{for(Future<JSONObject> f:pending)if(!f.isDone())f.cancel(true);}
    return page;
  }
  private String selectedProviders(boolean[] selected){
    LinkedHashSet<Integer> ids=new LinkedHashSet<>();
    for(int j=0;j<providerRows.length();j++){JSONObject row=providerRows.optJSONObject(j);int i=providerService(row);if(i>=0&&i<selected.length&&selected[i]&&row.optInt("provider_id")>0)ids.add(row.optInt("provider_id"));}
    StringBuilder out=new StringBuilder();for(int id:ids){if(out.length()>0)out.append('|');out.append(id);}return out.toString();
  }
  private int providerService(JSONObject row){
    if(row.optInt("provider_id")==350)return 3;
    int index=serviceIndex(row.optString("provider_name"));if(index>=0)return index;
    if(providerRows!=null)for(int j=0;j<providerRows.length();j++){JSONObject known=providerRows.optJSONObject(j);if(known.optInt("provider_id")==row.optInt("provider_id"))return serviceIndex(known.optString("provider_name"));}
    return -1;
  }
  static int serviceIndex(JSONObject offer){return offer.optInt("provider_id")==350?3:serviceIndex(offer.optString("provider_name"));}
  private static int serviceIndex(String name){
    for(int i=0;i<PROVIDER_NAMES.length;i++)if(name.equals(PROVIDER_NAMES[i])||name.startsWith(PROVIDER_NAMES[i]+" "))return i;
    if(name.equals("Max")||name.startsWith("Max "))return 5;
    if(name.equals("Apple TV"))return 3;
    if(name.equals("Tubi"))return 8;
    if(name.equals("Peacock"))return 6;
    return -1;
  }
  private void appendPage(Page page,ArrayList<JSONObject> out,String path,String type,int requestedPage)throws Exception{
    int number=Math.max(1,Math.min(500,requestedPage));Integer known=pageCounts.get(path);
    if(known!=null&&number>known){page.totalPages=Math.max(page.totalPages,known);return;}
    JSONObject response=get(path+"&page="+number);
    int total=Math.max(1,Math.min(500,response.optInt("total_pages",1)));
    pageCounts.put(path,total);page.totalPages=Math.max(page.totalPages,total);
    append(out,response,type);
  }
  private void append(ArrayList<JSONObject> out,JSONObject response,String type)throws JSONException{
    JSONArray rows=response.optJSONArray("results");if(rows==null)return;
    for(int i=0;i<Math.min(20,rows.length());i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;String media=type==null?row.optString("media_type"):type;if(!media.equals("movie")&&!media.equals("tv"))continue;row.put("media_type",media);out.add(row);}
  }
  private boolean genreMatch(JSONObject item,Query q){String genres=StreamingPolicy.genres(q.interests,item.optString("media_type"));if(genres.isEmpty())return true;JSONArray ids=item.optJSONArray("genre_ids");if(ids==null)return false;for(String g:genres.split("\\|"))for(int j=0;j<ids.length();j++)if(ids.optInt(j)==Integer.parseInt(g))return true;return false;}
  private JSONObject enrich(JSONObject item)throws Exception{
    item=new JSONObject(item.toString());
    String key=item.optString("media_type")+"/"+item.optInt("id");JSONObject us=availability.get(key);
    if(us==null){us=get(key+"/watch/providers?language=en-US").optJSONObject("results");us=us==null?null:us.optJSONObject(REGION);if(us==null)us=new JSONObject();us.put("sol_checked",System.currentTimeMillis());availability.put(key,us);}
    item.put("sol_availability",us);item.put("sol_checked",us.optLong("sol_checked"));item.put("sol_availability_status","US availability checked");return item;
  }
  private boolean matches(JSONObject item,boolean[] selected){
    JSONObject us=item.optJSONObject("sol_availability");if(us==null)return false;
    for(String mode:new String[]{"flatrate","free","ads"}){JSONArray rows=us.optJSONArray(mode);if(rows==null)continue;for(int j=0;j<rows.length();j++){JSONObject offer=rows.optJSONObject(j);if(offer==null)continue;int i=providerService(offer);if(i>=0&&i<selected.length&&selected[i])return true;}}
    return false;
  }
  // Called off-main only after the initial results are rendered; the caller bounds concurrent work.
  JSONObject loadRatings(JSONObject item)throws JSONException{
    if(Thread.currentThread().isInterrupted())return item;
    if(omdbKey.isEmpty()){item.remove("sol_imdb");item.remove("sol_rt");item.put("sol_ratings_status","Optional free OMDb key not configured");return item;}
    String media=item.optString("media_type"),key=media+"/"+item.optInt("id");
    JSONObject found=ratings.get(key);
    if(found!=null&&System.currentTimeMillis()-found.optLong("sol_cached")>15*60*1000)found=null;
    if(found==null){
      found=new JSONObject();
      try{
        if((!media.equals("movie")&&!media.equals("tv"))||item.optInt("id")<=0)throw new IOException();
        String imdb=get(key+"/external_ids?language=en-US").optString("imdb_id");
        if(!StreamingPolicy.imdbIdValid(imdb))found.put("sol_ratings_status","No IMDb ID supplied by TMDB");
        else applyRatings(found,omdb(imdb),imdb);
      }catch(Exception e){found.put("sol_ratings_status","Ratings service unavailable; try again later");}
      if(Thread.currentThread().isInterrupted())return item;
      found.put("sol_cached",System.currentTimeMillis());ratings.put(key,found);
    }
    for(String field:new String[]{"sol_imdb","sol_rt","sol_ratings_status"}){item.remove(field);if(found.has(field))item.put(field,found.get(field));}
    return item;
  }
  JSONObject omdb(String imdb)throws Exception{
    if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
    HttpURLConnection c=(HttpURLConnection)new URL("https://www.omdbapi.com/?apikey="+encode(omdbKey)+"&i="+encode(imdb)).openConnection();
    c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setInstanceFollowRedirects(false);
    try{
      int code=c.getResponseCode();
      if(code==429)return new JSONObject().put("Response","False").put("Error","Request limit reached");
      if(code!=200)throw new IOException();
      ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n,total=0;
      try(InputStream in=c.getInputStream()){while((n=in.read(buffer))!=-1){total+=n;if(total>100000)throw new IOException();out.write(buffer,0,n);}}
      return new JSONObject(new String(out.toByteArray(),"UTF-8"));
    }finally{c.disconnect();}
  }
  static void applyRatings(JSONObject item,JSONObject response,String imdb)throws JSONException{
    item.remove("sol_imdb");item.remove("sol_rt");
    if(!"True".equals(response.optString("Response"))){
      boolean limit=response.optString("Error").toLowerCase(Locale.US).contains("limit");
      item.put("sol_ratings_status",limit?"Free OMDb request limit reached; try again later":"OMDb rating unavailable; check the optional free key");return;
    }
    if(!imdb.equals(response.optString("imdbID"))){item.put("sol_ratings_status","OMDb returned a different title ID; ratings omitted");return;}
    String value=response.optString("imdbRating");if(StreamingPolicy.imdbRatingValid(value))item.put("sol_imdb",value+"/10");
    JSONArray rows=response.optJSONArray("Ratings");
    if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&"Rotten Tomatoes".equals(row.optString("Source"))&&StreamingPolicy.rottenRatingValid(row.optString("Value")))item.put("sol_rt",row.optString("Value"));}
    item.put("sol_ratings_status",item.has("sol_imdb")&&item.has("sol_rt")?"Ratings via OMDb":item.has("sol_imdb")?"Rotten Tomatoes rating not supplied by OMDb":item.has("sol_rt")?"IMDb rating not supplied by OMDb":"IMDb and Rotten Tomatoes ratings not supplied by OMDb");
  }
  static String title(JSONObject item){return item.optString("title",item.optString("name","Untitled"));}
  static String date(JSONObject item){return item.optString("release_date",item.optString("first_air_date",""));}
  static String encode(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
  public void close(){providerPool.shutdownNow();availability.clear();ratings.clear();pageCounts.clear();}
}
