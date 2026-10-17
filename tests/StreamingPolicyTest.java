package com.griffin.solhome;

public final class StreamingPolicyTest {
  private static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
  public static void main(String[] args){
    boolean[] genres=new boolean[8];genres[0]=true;genres[7]=true;
    check(StreamingPolicy.genres(genres,"movie").equals("35|12"),"Movie discovery combines interests with OR");
    check(StreamingPolicy.genres(genres,"tv").equals("35|10759"),"TV adventure uses its distinct genre ID");
    check(StreamingPolicy.genres(new boolean[8],"movie").isEmpty(),"No interests must not exclude all titles");
    check(StreamingPolicy.sort("movie",2).equals("primary_release_date.desc"),"Movie dates use supported sort");
    check(StreamingPolicy.sort("tv",2).equals("first_air_date.desc"),"TV dates use supported sort");
    check(StreamingPolicy.sort("tv",1).equals("vote_average.desc"),"Rating request sort");
    check(StreamingPolicy.credentialValid("0123456789abcdef0123456789abcdef"),"v3 API key accepted");
    check(StreamingPolicy.credentialValid(new String(new char[100]).replace('\0','A')+".token.signature"),"Read token accepted");
    check(!StreamingPolicy.credentialValid("Bearer abc\r\nHeader: bad"),"Credential rejects header injection");
    check(!StreamingPolicy.credentialValid("short"),"Truncated credentials rejected");
    check(StreamingPolicy.safeWatchUrl("https://www.themoviedb.org/movie/123/watch?locale=US")!=null,"Provider attribution link allowed");
    for(String bad:new String[]{"http://www.themoviedb.org/movie/1","https://www.themoviedb.org.evil.test/","intent://app","https://evil.test/","https://x@www.themoviedb.org/","https://www.themoviedb.org:444/"})check(StreamingPolicy.safeWatchUrl(bad)==null,"Unsafe link rejected: "+bad);
    System.out.println("Streaming request policy checks passed");
  }
}
