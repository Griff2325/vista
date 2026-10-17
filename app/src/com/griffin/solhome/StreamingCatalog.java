package com.griffin.solhome;

import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** Optional TMDB catalog. No service credentials, session scraping, or playback entitlement inference. */
final class StreamingCatalog implements AutoCloseable {
  static final String REGION="US";
  static final String[] SERVICES={"Netflix","Hulu","Disney+","Apple TV+"};
  static final String[] PROVIDER_NAMES={"Netflix","Hulu","Disney Plus","Apple TV Plus"};
  static final String[] PACKAGES={"com.netflix.ninja","com.hulu.plus","com.disney.disneyplus","com.apple.atve.amazon.appletv"};
  static final String[] INTERESTS={"Comedy","Drama","Animation","Documentary","Crime","Family","Mystery","Adventure"};
  private final String token;
  private final ExecutorService providerPool=Executors.newFixedThreadPool(4);
  private final Map<String,JSONObject> availability=Collections.synchronizedMap(new LinkedHashMap<String,JSONObject>(128,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,JSONObject> e){return size()>120;}});
  private int[] providerIds;
  StreamingCatalog(String token){this.token=token.trim();}
  static final class Query {
    String text=""; int type=0,sort=0,page=1; // type: both / movie / TV
    boolean[] services=new boolean[]{true,true,true,true},interests=new boolean[8];
  }
  static final class Page {final ArrayList<JSONObject> items=new ArrayList<>();int totalPages=1;int unavailable;}
  JSONObject get(String path)throws Exception {
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
    if(providerIds!=null)return;
    int[] ids=new int[4];JSONArray rows=get("watch/providers/movie?watch_region=US&language=en-US").optJSONArray("results");
    if(rows!=null)for(int j=0;j<rows.length();j++){JSONObject row=rows.optJSONObject(j);if(row==null)continue;for(int i=0;i<4;i++)if(PROVIDER_NAMES[i].equals(row.optString("provider_name"))||(i==3&&row.optInt("provider_id")==350))ids[i]=row.optInt("provider_id");}
    providerIds=ids;
  }
  Page load(Query q)throws Exception{
    providers();Page page=new Page();ArrayList<JSONObject> candidates=new ArrayList<>();
    boolean search=!q.text.trim().isEmpty();
    if(search){
      String type=q.type==0?"multi":q.type==1?"movie":"tv";
      JSONObject response=get("search/"+type+"?include_adult=false&language=en-US&page="+q.page+"&query="+encode(q.text));
      page.totalPages=Math.max(1,response.optInt("total_pages",1));append(candidates,response,type.equals("multi")?null:type);
    }else{
      for(String type:new String[]{"movie","tv"}){
        if((q.type==1&&!type.equals("movie"))||(q.type==2&&!type.equals("tv")))continue;
        String p=selectedProviders(q.services);if(p.isEmpty())continue;
        String path="discover/"+type+"?include_adult=false&language=en-US&watch_region=US&with_watch_monetization_types=flatrate%7Cfree%7Cads&with_watch_providers="+encode(p)+"&page="+q.page+"&sort_by="+StreamingPolicy.sort(type,q.sort)+"&vote_count.gte="+(q.sort==1?100:0);
        String genres=StreamingPolicy.genres(q.interests,type);if(!genres.isEmpty())path+="&with_genres="+encode(genres);
        if(q.sort==2){String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());path+=(type.equals("movie")?"&primary_release_date.lte=":"&first_air_date.lte=")+today;}
        JSONObject response=get(path);page.totalPages=Math.max(page.totalPages,response.optInt("total_pages",1));append(candidates,response,type);
      }
      if(q.type==0){final int sort=q.sort;candidates.sort((a,b)->sort==2?date(b).compareTo(date(a)):Double.compare(b.optDouble(sort==1?"vote_average":"popularity"),a.optDouble(sort==1?"vote_average":"popularity")));}
    }
    // Bound work: one catalog page, at most 20 titles; availability requests run four at a time.
    ArrayList<Future<JSONObject>> pending=new ArrayList<>();
    for(JSONObject candidate:candidates){if(pending.size()>=20)break;if(!genreMatch(candidate,q))continue;pending.add(providerPool.submit(()->enrich(candidate)));}
    for(Future<JSONObject> f:pending){try{JSONObject item=f.get(45,TimeUnit.SECONDS);if(matches(item,q.services))page.items.add(item);}catch(ExecutionException e){page.unavailable++;}catch(TimeoutException e){f.cancel(true);page.unavailable++;}}
    return page;
  }
  private String selectedProviders(boolean[] selected){StringBuilder out=new StringBuilder();for(int i=0;i<4;i++)if(selected[i]&&providerIds[i]>0){if(out.length()>0)out.append('|');out.append(providerIds[i]);}return out.toString();}
  private void append(ArrayList<JSONObject> out,JSONObject response,String type)throws JSONException{
    JSONArray rows=response.optJSONArray("results");if(rows==null)return;
    for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;String media=type==null?row.optString("media_type"):type;if(!media.equals("movie")&&!media.equals("tv"))continue;row.put("media_type",media);out.add(row);}
  }
  private boolean genreMatch(JSONObject item,Query q){String genres=StreamingPolicy.genres(q.interests,item.optString("media_type"));if(genres.isEmpty())return true;JSONArray ids=item.optJSONArray("genre_ids");if(ids==null)return false;for(String g:genres.split("\\|"))for(int j=0;j<ids.length();j++)if(ids.optInt(j)==Integer.parseInt(g))return true;return false;}
  private JSONObject enrich(JSONObject item)throws Exception{
    String key=item.optString("media_type")+"/"+item.optInt("id");JSONObject us=availability.get(key);
    if(us==null){us=get(key+"/watch/providers?language=en-US").optJSONObject("results");us=us==null?null:us.optJSONObject(REGION);if(us==null)us=new JSONObject();us.put("sol_checked",System.currentTimeMillis());availability.put(key,us);}
    for(String mode:new String[]{"flatrate","free","ads"}){JSONArray offers=us.optJSONArray(mode);if(offers!=null)for(int j=0;j<offers.length();j++){JSONObject offer=offers.optJSONObject(j);if(offer!=null)for(int i=0;i<4;i++)if(providerIds[i]>0&&offer.optInt("provider_id")==providerIds[i])offer.put("provider_name",PROVIDER_NAMES[i]);}}
    item.put("sol_availability",us);item.put("sol_checked",us.optLong("sol_checked"));return item;
  }
  private boolean matches(JSONObject item,boolean[] selected){JSONObject us=item.optJSONObject("sol_availability");if(us==null)return false;for(String mode:new String[]{"flatrate","free","ads"}){JSONArray rows=us.optJSONArray(mode);if(rows==null)continue;for(int j=0;j<rows.length();j++)for(int i=0;i<4;i++)if(selected[i]&&providerIds[i]>0&&rows.optJSONObject(j)!=null&&rows.optJSONObject(j).optInt("provider_id")==providerIds[i])return true;}return false;}
  static String title(JSONObject item){return item.optString("title",item.optString("name","Untitled"));}
  static String date(JSONObject item){return item.optString("release_date",item.optString("first_air_date",""));}
  static String encode(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
  public void close(){providerPool.shutdownNow();availability.clear();}
}
