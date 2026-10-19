package com.griffin.solhome;

public final class StreamingArtworkTest {
  private static void check(boolean condition){if(!condition)throw new AssertionError("Artwork URL policy");}
  public static void main(String[] args){
    check("https://image.tmdb.org/t/p/w780/abc123.jpg".equals(StreamingArtwork.url("/abc123.jpg",true)));
    check("https://image.tmdb.org/t/p/w500/abc123.png".equals(StreamingArtwork.url("/abc123.png",false)));
    for(String path:new String[]{"", "null", "https://evil.test/a.jpg", "//evil.test/a.jpg", "/../a.jpg", "/a.jpg?token=secret", "/a.svg"})check(StreamingArtwork.url(path,false)==null);
    check(StreamingArtwork.url(null,false)==null);
    System.out.println("Artwork host, path and size policy checks passed");
  }
}
