package com.griffin.solhome;

public final class SpotifyLinksTest {
  private static final String ID="0123456789AbCdEfGhIjKl";
  public static void main(String[] args) {
    equal("spotify:playlist:"+ID,SpotifyLinks.normalize(" https://open.spotify.com/playlist/"+ID+"?si=shared "));
    equal("spotify:album:"+ID,SpotifyLinks.normalize("https://open.spotify.com/intl-en/album/"+ID));
    equal("spotify:track:"+ID,SpotifyLinks.normalize("spotify:track:"+ID));
    equal("spotify:search:quiet%20piano",SpotifyLinks.search("quiet piano"));
    reject("https://open.spotify.com.evil.example/playlist/"+ID);
    reject("https://open.spotify.com@evil.example/playlist/"+ID);
    reject("http://open.spotify.com/playlist/"+ID);
    reject("https://open.spotify.com:443/playlist/"+ID);
    reject("intent://anything");
    reject("spotify:playlist:bad");
    reject("spotify:playlist:"+ID+":play");
    reject("https://open.spotify.com/playlist/"+ID+"/extra");
    reject(null);
    System.out.println("Spotify link tests passed");
  }
  private static void equal(String expected,String actual){if(!expected.equals(actual))throw new AssertionError(actual);}
  private static void reject(String value){try{SpotifyLinks.normalize(value);}catch(IllegalArgumentException expected){return;}throw new AssertionError("Accepted invalid link: "+value);}
}
