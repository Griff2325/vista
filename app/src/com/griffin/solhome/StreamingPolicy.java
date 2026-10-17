package com.griffin.solhome;

/** Pure request policy, kept separate so service filters and mixed media sorting can be verified. */
final class StreamingPolicy {
  private static final int[] MOVIE={35,18,16,99,80,10751,9648,12},TV={35,18,16,99,80,10751,9648,10759};
  static String sort(String media,int sort){return sort==1?"vote_average.desc":sort==2?(media.equals("movie")?"primary_release_date.desc":"first_air_date.desc"):"popularity.desc";}
  static String genres(boolean[] selected,String media){StringBuilder b=new StringBuilder();int[] ids=media.equals("movie")?MOVIE:TV;for(int i=0;i<Math.min(selected.length,ids.length);i++)if(selected[i]){if(b.length()>0)b.append('|');b.append(ids[i]);}return b.toString();}
  static boolean credentialValid(String value){return value!=null&&(value.matches("[a-fA-F0-9]{32}")||value.matches("[A-Za-z0-9_\\-.]{50,2048}"));}
  static String safeWatchUrl(String raw){try{java.net.URI u=new java.net.URI(raw);return "https".equals(u.getScheme())&&"www.themoviedb.org".equals(u.getHost())&&u.getUserInfo()==null&&u.getPort()==-1?raw:null;}catch(Exception e){return null;}}
}
