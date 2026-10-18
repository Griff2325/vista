package com.griffin.solhome;

/** Small, conservative template for Hulu's yellow Ad badge, not video analysis. */
final class HuluAdBadge {
  static final int WIDTH=46, HEIGHT=36;
  // shortcut: calibrated to Hulu's current 16:9 TV layout; recheck after a Hulu UI update.
  static int left(int width){return Math.round(width*1778f/1920);}
  static int top(int height){return Math.round(height*60f/1080);}
  static int width(int width){return Math.max(1,Math.round(width*WIDTH/1920f));}
  static int height(int height){return Math.max(1,Math.round(height*HEIGHT/1080f));}
  private static final int[][] INK={{7,5},{8,6},{6,7},{6,9},{5,10},{9,10},{17,5},{17,6},{15,7},{13,9},{17,10},{15,11}};
  static boolean matches(int[] pixels,int width,int height){
    if(width<23||height<18||pixels==null||pixels.length!=width*height)return false;
    int yellow=0,dark=0;
    for(int p:pixels)if(yellow(p))yellow++;
    if(yellow<pixels.length*.72)return false;
    for(int[] point:INK){int x=Math.min(width-1,(2*point[0]+1)*width/46),y=Math.min(height-1,(2*point[1]+1)*height/36);int p=pixels[y*width+x];if(((p>>16)&255)<150&&((p>>8)&255)<135&&(p&255)<110)dark++;}
    return dark>=10;
  }
  private static boolean yellow(int p){int r=(p>>16)&255,g=(p>>8)&255,b=p&255;return r>=210&&g>=150&&g<=215&&b>=25&&b<=100&&r-g>=30;}
}
