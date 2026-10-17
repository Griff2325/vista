package com.griffin.solhome;

import java.net.URI;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/** Only Spotify content/search links are accepted; never launch an arbitrary saved URI. */
public final class SpotifyLinks {
  private static final Pattern CONTENT=Pattern.compile("(playlist|album|track|artist):[A-Za-z0-9]{22}");
  private SpotifyLinks() {}
  public static String normalize(String input) {
    if(input==null)throw new IllegalArgumentException("Enter a Spotify playlist or album link");
    String value=input.trim();
    if(value.length()>512)throw new IllegalArgumentException("Spotify link is too long");
    if(value.startsWith("spotify:")) {
      String path=value.substring(8);
      if(CONTENT.matcher(path).matches())return "spotify:"+path;
      throw new IllegalArgumentException("Use a Spotify playlist, album, track, or artist URI");
    }
    try {
      URI uri=new URI(value);
      if(!"https".equalsIgnoreCase(uri.getScheme())||!"open.spotify.com".equalsIgnoreCase(uri.getHost())||uri.getPort()!=-1||uri.getRawUserInfo()!=null)throw new IllegalArgumentException();
      String path=uri.getPath().replaceFirst("^/intl-[A-Za-z-]+/","/");
      if(path.startsWith("/"))path=path.substring(1);
      String content=path.replace('/',':');
      if(!CONTENT.matcher(content).matches())throw new IllegalArgumentException();
      return "spotify:"+content;
    }catch(Exception error){throw new IllegalArgumentException("Use an open.spotify.com playlist or album link, or a Spotify content URI");}
  }
  public static String search(String query) {
    try{return "spotify:search:"+URLEncoder.encode(query,"UTF-8").replace("+","%20");}
    catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
  }
}
