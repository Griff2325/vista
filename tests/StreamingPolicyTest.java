package com.griffin.solhome;

public final class StreamingPolicyTest {
  private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
  public static void main(String[] args){
    boolean[] genres=new boolean[8];genres[0]=true;genres[7]=true;
    check(StreamingPolicy.genres(genres,"movie").equals("35|12"),"Movie discovery combines interests with OR");
    check(StreamingPolicy.genres(genres,"tv").equals("35|10759"),"TV adventure uses its distinct genre ID");
    check(StreamingPolicy.genres(new boolean[8],"movie").isEmpty(),"No interests must not exclude all titles");
    boolean[] expanded=new boolean[10];expanded[8]=true;expanded[9]=true;
    check(StreamingPolicy.genres(expanded,"movie").equals("28|878|14"),"Action, science fiction and fantasy combine as movie OR genres");
    check(StreamingPolicy.genres(expanded,"tv").equals("10759|10765"),"Action and science fiction use TV genre mappings");
    check(StreamingPolicy.sort("movie",2).equals("primary_release_date.desc"),"Movie dates use supported sort");
    check(StreamingPolicy.sort("tv",2).equals("first_air_date.desc"),"TV dates use supported sort");
    check(StreamingPolicy.sort("tv",1).equals("vote_average.desc"),"Rating request sort");
    check(StreamingPolicy.credentialValid("0123456789abcdef0123456789abcdef"),"v3 API key accepted");
    check(StreamingPolicy.credentialValid(new String(new char[100]).replace('\0','A')+".token.signature"),"Read token accepted");
    check(!StreamingPolicy.credentialValid("Bearer abc\r\nHeader: bad"),"Credential rejects header injection");
    check(!StreamingPolicy.credentialValid("short"),"Truncated credentials rejected");
    check(StreamingPolicy.safeWatchUrl("https://www.themoviedb.org/movie/123/watch?locale=US")!=null,"Provider attribution link allowed");
    for(String bad:new String[]{"http://www.themoviedb.org/movie/1","https://www.themoviedb.org.evil.test/","intent://app","https://evil.test/","https://x@www.themoviedb.org/","https://www.themoviedb.org:444/"})check(StreamingPolicy.safeWatchUrl(bad)==null,"Unsafe link rejected: "+bad);
    check(!StreamingPolicy.keepResult(false,false),"Explicit service filters apply to search too");
    check(StreamingPolicy.keepResult(true,false),"All services retains unlisted providers");
    check(StreamingPolicy.keepResult(false,true),"Selected offers retained");
    check(StreamingPolicy.imdbIdValid("tt1234567"),"IMDb IDs accepted");
    check(!StreamingPolicy.imdbIdValid("title"),"No ambiguous title lookup");
    check(StreamingPolicy.omdbKeyValid("abc12345"),"Free key accepted");
    check(!StreamingPolicy.omdbKeyValid("abc&apikey=bad"),"Key rejects URL injection");
    for(String rating:new String[]{"0.0","8.2","10.0"})check(StreamingPolicy.imdbRatingValid(rating),"IMDb valid: "+rating);
    for(String rating:new String[]{"N/A","NaN","-1","11.0","8.2/10","9.99"})check(!StreamingPolicy.imdbRatingValid(rating),"IMDb invalid: "+rating);
    for(String rating:new String[]{"0%","95%","100%"})check(StreamingPolicy.rottenRatingValid(rating),"RT valid: "+rating);
    for(String rating:new String[]{"N/A","101%","-2%","95","95.5%"})check(!StreamingPolicy.rottenRatingValid(rating),"RT invalid: "+rating);
    System.out.println("Streaming request policy checks passed");
  }
}
