package com.griffin.solhome;

import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** Offline HTTP fixtures: no credentials or live catalog calls. */
public final class StreamingCatalogTest {
  static final List<String> requests=Collections.synchronizedList(new ArrayList<>());
  static boolean ratingError;
  static String externalId="tt1234567";
  static String rating="{\"Response\":\"True\",\"imdbID\":\"tt1234567\",\"imdbRating\":\"8.2\",\"Ratings\":[{\"Source\":\"Rotten Tomatoes\",\"Value\":\"95%\"}]}";
  static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
  static JSONObject item()throws Exception{return new JSONObject("{\"media_type\":\"movie\",\"id\":1,\"vote_average\":9.9}");}
  public static void main(String[] args)throws Exception{
    URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new URLStreamHandler(){
      protected URLConnection openConnection(URL url){return new HttpURLConnection(url){
        public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
        public int getResponseCode(){requests.add(url.toString());return url.getPath().contains("2/watch/providers")||(ratingError&&url.getHost().equals("www.omdbapi.com"))?503:200;}
        public InputStream getInputStream()throws IOException{
          String path=url.getPath(),data;
          if(url.getHost().equals("www.omdbapi.com"))data=rating;
          else if(path.contains("external_ids"))data="{\"imdb_id\":\""+externalId+"\"}";
          else if(path.contains("watch/providers/movie")||path.contains("watch/providers/tv"))data="{\"results\":[{\"provider_name\":\"Amazon Prime Video with Ads\",\"provider_id\":2100}]}";
          else if(path.contains("watch/providers"))data="{\"results\":{\"US\":{\"ads\":[{\"provider_name\":\"Unlisted Free Service\",\"provider_id\":9999}]}}}";
          else data="{\"results\":[{\"media_type\":\"movie\",\"id\":1,\"title\":\"Prime title\"},{\"media_type\":\"tv\",\"id\":2,\"name\":\"Unavailable title\"},{\"media_type\":\"person\",\"id\":3}],\"total_pages\":1}";
          return new ByteArrayInputStream(data.getBytes("UTF-8"));
        }
      };}
    }:null);
    try(StreamingCatalog c=new StreamingCatalog("fake")){
      StreamingCatalog.Query q=new StreamingCatalog.Query();q.text="Title";q.allServices=false;Arrays.fill(q.services,false);q.interests[0]=true;
      StreamingCatalog.Page page=c.load(q);
      check(page.items.size()==2,"Search retains unmatched services, genres, and failed availability; excludes people");
      check(page.unavailable==1,"Availability errors counted honestly");
      check(page.items.get(0).getJSONObject("sol_availability").getJSONArray("ads").getJSONObject(0).getString("provider_name").equals("Unlisted Free Service"),"Unknown providers preserved");
      check(page.items.get(1).getString("sol_availability_status").contains("could not"),"Availability failure exposed");
      int before=requests.size();JSONObject noKey=c.loadRatings(item());check(requests.size()==before,"Missing OMDb key makes no requests");check(!noKey.has("sol_imdb")&&noKey.getString("sol_ratings_status").contains("not configured"),"Missing key has no invented rating");
      requests.clear();q=new StreamingCatalog.Query();q.type=1;c.load(q);
      check(requests.stream().anyMatch(s->s.contains("discover/movie")&&!s.contains("with_watch_providers=")),"All-services discovery omits provider filter");
      requests.clear();q.allServices=false;Arrays.fill(q.services,false);q.services[4]=true;c.load(q);
      check(requests.stream().anyMatch(s->s.contains("with_watch_providers=2100")),"Prime ad-supported variant included in provider discovery");
    }
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
    System.out.println("Offline streaming catalog and rating fixtures passed");
  }
}
