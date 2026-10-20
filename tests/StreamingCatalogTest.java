package com.griffin.solhome;

import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** Offline HTTP fixtures: no credentials or live catalog calls. */
public final class StreamingCatalogTest {
  static final List<String> requests=Collections.synchronizedList(new ArrayList<>());
  static boolean ratingError;
  static String movieResponse,tvResponse,searchResponse;
  static String externalId="tt1234567";
  static String rating="{\"Response\":\"True\",\"imdbID\":\"tt1234567\",\"imdbRating\":\"8.2\",\"Ratings\":[{\"Source\":\"Rotten Tomatoes\",\"Value\":\"95%\"}]}";
  static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
  static JSONObject item()throws Exception{return new JSONObject("{\"media_type\":\"movie\",\"id\":1,\"vote_average\":9.9}");}
  static JSONObject title(int id,String media,int genre,String date,double vote)throws Exception{
    return new JSONObject().put("id",id).put("media_type",media).put("genre_ids",new JSONArray().put(genre)).put(media.equals("tv")?"first_air_date":"release_date",date).put("vote_average",vote).put("popularity",id);
  }
  static String response(JSONArray rows,int pages)throws Exception{return new JSONObject().put("results",rows).put("total_pages",pages).toString();}
  static void catalogFilters()throws Exception{
    JSONArray movies=new JSONArray(),tv=new JSONArray();
    for(int i=1;i<=20;i++){movies.put(title(100+i,"movie",35,"2026-01-01",8));tv.put(title(200+i,"tv",35,"2026-01-01",8));}
    movieResponse=response(movies,1);tvResponse=response(tv,700);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();StreamingCatalog.Page page=c.load(q);
      check(page.items.size()==40,"Mixed discovery keeps all 40 source titles without dropping a half page");
      Set<String> ids=new HashSet<>();for(JSONObject row:page.items)ids.add(row.getString("media_type")+row.getInt("id"));check(ids.size()==40,"Mixed titles stay distinct by media and ID");
      check(page.totalPages==500,"Mixed total pages uses greatest source total and caps at TMDB 500");
      requests.clear();q.page=2;page=c.load(q);
      check(requests.stream().noneMatch(v->v.contains("discover/movie")),"Exhausted movie source skipped on later mixed pages");
      check(page.items.size()==20&&page.totalPages==500,"Remaining TV source continues after movies exhaust");
      requests.clear();q.page=999;c.load(q);check(requests.stream().anyMatch(v->v.contains("discover/tv")&&v.contains("page=500")),"Requests never exceed upstream page 500");
    }
    // Known unmatched offers are filtered; uncertain availability remains visible and labeled.
    JSONArray rows=new JSONArray().put(title(1,"movie",35,"2026-02-01",7)).put(title(2,"tv",35,"2026-03-01",8))
      .put(title(3,"movie",18,"2026-01-01",9)).put(title(4,"movie",35,"2025-01-01",9)).put(title(5,"movie",35,"2026-01-01",5))
      .put(new JSONObject().put("id",6).put("media_type","movie").put("release_date","2026-01-01").put("vote_average",9));
    searchResponse=response(rows,4);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();q.text="Title";q.year=2026;q.minRating=7;q.interests[0]=true;q.sort=1;
      StreamingCatalog.Page page=c.load(q);
      check(page.items.size()==2&&page.items.get(0).getInt("id")==1&&page.items.get(1).getInt("id")==2,"Search applies genre, exact year and minimum rating while retaining relevance order");
      q.allServices=false;Arrays.fill(q.services,false);q.services[4]=true;page=c.load(q);
      check(page.items.size()==1&&page.items.get(0).getInt("id")==2&&page.unavailable==1,"Explicit service search drops known unmatched offers and retains uncertain availability");
      check(page.items.get(0).getString("sol_availability_status").contains("could not"),"Uncertain service match is explicitly labeled");
      requests.clear();q.type=1;q.allServices=true;c.load(q);
      check(requests.stream().anyMatch(v->v.contains("search/movie")&&v.contains("primary_release_year=2026")),"Typed movie search sends supported primary release year");
      requests.clear();q.type=2;c.load(q);check(requests.stream().anyMatch(v->v.contains("search/tv")&&v.contains("first_air_date_year=2026")),"Typed TV search sends supported first air year");
      movieResponse=searchResponse;requests.clear();q.text="";q.type=1;q.sort=0;page=c.load(q);
      check(page.items.size()==2,"Discovery also validates genre, year and rating locally");
      check(requests.stream().anyMatch(v->v.contains("discover/movie")&&v.contains("primary_release_year=2026")&&v.contains("vote_average.gte=7")&&v.contains("with_genres=35")),"Discovery sends supported year, rating and genre filters");
    }
    searchResponse=response(new JSONArray().put(title(11,"movie",878,"2026-01-01",8)).put(title(12,"movie",14,"2026-01-01",8)).put(title(13,"movie",28,"2026-01-01",8)),1);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();q.text="Genre";
      check(q.interests.length==StreamingCatalog.INTERESTS.length,"Queries cover every visible genre choice");
      q.interests[9]=true;check(c.load(q).items.size()==2,"Sci-fi and fantasy search accepts either movie genre ID");
      q.interests[9]=false;q.interests[8]=true;check(c.load(q).items.size()==1,"Action search applies its appended genre mapping");
    }
    // Typed responses omit media_type; their endpoint supplies it.
    JSONObject typed=title(9,"movie",35,"2026-01-01",8);typed.remove("media_type");searchResponse=response(new JSONArray().put(typed),1);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();q.text="Typed";q.type=1;
      check(c.load(q).items.get(0).getString("media_type").equals("movie"),"Typed movie search normalizes missing media type");
      q.type=2;check(c.load(q).items.get(0).getString("media_type").equals("tv"),"Typed TV search normalizes missing media type");
    }
    movieResponse=null;tvResponse=null;searchResponse=null;
  }
  public static void main(String[] args)throws Exception{
    URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){
      protected URLConnection openConnection(URL url){return new HttpURLConnection(url){
        public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
        public int getResponseCode(){requests.add(url.toString());return url.getPath().endsWith("/2/watch/providers")||(ratingError&&url.getHost().equals("www.omdbapi.com"))?503:200;}
        public InputStream getInputStream()throws IOException{
          String path=url.getPath(),data;
          if(url.getHost().equals("www.omdbapi.com"))data=rating;
          else if(path.contains("external_ids"))data="{\"imdb_id\":\""+externalId+"\"}";
          else if(path.contains("watch/providers/movie")||path.contains("watch/providers/tv"))data="{\"results\":[{\"provider_name\":\"Amazon Prime Video with Ads\",\"provider_id\":2100}]}";
          else if(path.contains("watch/providers"))data="{\"results\":{\"US\":{\"ads\":[{\"provider_name\":\"Unlisted Free Service\",\"provider_id\":9999}]}}}";
          else if(path.contains("discover/movie")&&movieResponse!=null)data=movieResponse;
          else if(path.contains("discover/tv")&&tvResponse!=null)data=tvResponse;
          else if(path.contains("search/")&&searchResponse!=null)data=searchResponse;
          else data="{\"results\":[{\"media_type\":\"movie\",\"id\":1,\"title\":\"Prime title\"},{\"media_type\":\"tv\",\"id\":2,\"name\":\"Unavailable title\"},{\"media_type\":\"person\",\"id\":3}],\"total_pages\":1}";
          return new ByteArrayInputStream(data.getBytes("UTF-8"));
        }
      };}
    }:null);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();q.text="Title";
      StreamingCatalog.Page page=c.load(q);
      check(page.items.size()==2,"Default search retains unknown providers and failed availability; excludes people");
      check(page.unavailable==1,"Availability errors counted honestly");
      check(page.items.get(0).getJSONObject("sol_availability").getJSONArray("ads").getJSONObject(0).getString("provider_name").equals("Unlisted Free Service"),"Unknown providers preserved");
      check(page.items.get(1).getString("sol_availability_status").contains("could not"),"Availability failure exposed");
      int before=requests.size();JSONObject noKey=c.loadRatings(item());check(requests.size()==before,"Missing OMDb key makes no requests");check(!noKey.has("sol_imdb")&&noKey.getString("sol_ratings_status").contains("not configured"),"Missing key has no invented rating");
      requests.clear();q=new StreamingCatalog.Query();q.type=1;c.load(q);
      check(requests.stream().anyMatch(s->s.contains("discover/movie")&&!s.contains("with_watch_providers=")),"All-services discovery omits provider filter");
      requests.clear();q.allServices=false;Arrays.fill(q.services,false);q.services[4]=true;c.load(q);
      check(requests.stream().anyMatch(s->s.contains("with_watch_providers=2100")),"Prime ad-supported variant included in provider discovery");
    }
    catalogFilters();
    try(StreamingCatalog c=new StreamingCatalog("fake","abc12345")){
      requests.clear();JSONObject title=c.loadRatings(item());check(title.getString("sol_imdb").equals("8.2/10")&&title.getString("sol_rt").equals("95%"),"Exact ID ratings parsed");
      check(requests.stream().anyMatch(s->s.contains("/movie/1/external_ids"))&&requests.stream().anyMatch(s->s.contains("i=tt1234567")),"Exact external ID used");
      int before=requests.size();c.loadRatings(item());check(requests.size()==before,"Ratings cache avoids duplicate quota usage");
    }
    ratingError=true;
    try(StreamingCatalog c=new StreamingCatalog("fake","abc12345")){
      JSONObject unavailable=c.loadRatings(item());check(!unavailable.has("sol_imdb")&&unavailable.getString("sol_ratings_status").contains("unavailable"),"Rating HTTP failures are nonfatal and explained");
      int before=requests.size();Thread.currentThread().interrupt();c.loadRatings(item());check(requests.size()==before,"Canceled rating work makes no requests");Thread.interrupted();
    }
    ratingError=false;externalId="";
    try(StreamingCatalog c=new StreamingCatalog("fake","abc12345")){
      requests.clear();JSONObject missing=c.loadRatings(item());check(requests.size()==1&&!missing.has("sol_imdb")&&missing.getString("sol_ratings_status").contains("No IMDb ID"),"Missing external ID skips ambiguous title lookups");
    }
    JSONObject target=item();StreamingCatalog.applyRatings(target,new JSONObject(rating),"tt7654321");check(!target.has("sol_imdb")&&!target.has("sol_rt"),"Mismatched title ID rejected");
    StreamingCatalog.applyRatings(target,new JSONObject("{\"Response\":\"True\",\"imdbID\":\"tt1234567\",\"imdbRating\":\"N/A\",\"Ratings\":[{\"Source\":\"Rotten Tomatoes\",\"Value\":\"101%\"}]}"),"tt1234567");
    check(!target.has("sol_imdb")&&!target.has("sol_rt")&&target.getString("sol_ratings_status").contains("not supplied"),"Invalid ratings omitted, never replaced with TMDB vote");
    StreamingCatalog.applyRatings(target,new JSONObject("{\"Response\":\"False\",\"Error\":\"Request limit reached!\"}"),"tt1234567");check(target.getString("sol_ratings_status").contains("limit"),"Free quota reported");
    check(StreamingCatalog.PACKAGES[4].equals("com.amazon.firebat")&&StreamingCatalog.PACKAGES[5].equals("com.hbo.hbonow")&&StreamingCatalog.PACKAGES[6].equals("com.peacock.peacockfiretv"),"Observed Fire TV provider packages");
    check(StreamingCatalog.PACKAGES[8].equals("com.tubitv.ott"),"Installed Tubi TV package");
    check(StreamingCatalog.serviceIndex(new JSONObject().put("provider_name","Max"))==5,"Max provider alias launches its installed app");
    check(StreamingCatalog.serviceIndex(new JSONObject().put("provider_name","Apple TV").put("provider_id",350))==3,"Apple stable provider ID");
    check(StreamingCatalog.serviceIndex(new JSONObject().put("provider_name","Tubi"))==8,"Current Tubi provider name supports filtering and launch");
    check(StreamingCatalog.serviceIndex(new JSONObject().put("provider_name","Peacock"))==6,"Peacock provider alias");
    System.out.println("Offline streaming catalog and rating fixtures passed");
  }
}
