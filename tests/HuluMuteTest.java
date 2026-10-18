package com.griffin.solhome;

import java.util.Arrays;

public final class HuluMuteTest {
  static void equal(int expected,int actual){if(expected!=actual)throw new AssertionError(expected+" != "+actual);}
  public static void main(String[] args){
    HuluMutePolicy policy=new HuluMutePolicy();
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.sample(true,true,26));
    equal(HuluMutePolicy.NONE,policy.sample(false,true,26));
    equal(HuluMutePolicy.UNMUTE,policy.sample(false,true,26));
    equal(HuluMutePolicy.NONE,policy.sample(false,false,26));
    // A pre-existing mute belongs to the viewer.
    equal(HuluMutePolicy.NONE,policy.sample(true,true,26));
    equal(HuluMutePolicy.NONE,policy.stop(true,26));
    // A manual unmute wins for the rest of this break.
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.stop(false,26));
    // A volume adjustment must never be overwritten.
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.sample(true,true,21));
    equal(HuluMutePolicy.NONE,policy.stop(true,21));
    // This TV reports getStreamVolume() == 0 while muted.
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,26));
    equal(HuluMutePolicy.NONE,policy.sample(true,true,0));
    equal(HuluMutePolicy.NONE,policy.sample(false,true,0));
    equal(HuluMutePolicy.UNMUTE,policy.sample(false,true,0));
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,26));
    policy.volumeChanged(21);
    equal(HuluMutePolicy.NONE,policy.sample(true,true,0));
    equal(HuluMutePolicy.NONE,policy.stop(true,0));
    // Leaving Hulu or a capture failure releases our own mute immediately.
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,21));
    equal(HuluMutePolicy.UNMUTE,policy.stop(true,21));
    equal(HuluMutePolicy.NONE,policy.stop(true,21));
    // A missed sample must not make sound flicker within a break.
    equal(HuluMutePolicy.MUTE,policy.sample(true,false,21));
    equal(HuluMutePolicy.NONE,policy.sample(false,true,21));
    equal(HuluMutePolicy.NONE,policy.sample(true,true,21));
    equal(HuluMutePolicy.UNMUTE,policy.stop(true,21));

    int[] pixels=new int[46*36];
    if(HuluAdBadge.matches(pixels,46,36))throw new AssertionError("Blank frame");
    Arrays.fill(pixels,0xfff2bc3c);
    if(HuluAdBadge.matches(pixels,46,36))throw new AssertionError("Yellow rectangle without text");
    String[] glyph={".......##........#.....",".......###.......#.....","......##.#....####.....","......#..#...#..##.....","......#####.##...#.....",".....######.##...#.....",".....#....#..##.##.....","..............#........"};
    for(int y=0;y<glyph.length;y++)for(int x=0;x<23;x++)if(glyph[y].charAt(x)=='#')for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++)pixels[((y+5)*2+dy)*46+x*2+dx]=0xff2d2d2d;
    if(!HuluAdBadge.matches(pixels,46,36))throw new AssertionError("Ad glyph on yellow badge");
    int[] large=new int[92*72];for(int y=0;y<72;y++)for(int x=0;x<92;x++)large[y*92+x]=pixels[(y/2)*46+x/2];
    if(!HuluAdBadge.matches(large,92,72))throw new AssertionError("Scaled badge");
    for(int n=0;n<pixels.length;n++)if(pixels[n]==0xfff2bc3c)pixels[n]=0xff00cc55;
    if(HuluAdBadge.matches(pixels,46,36))throw new AssertionError("Wrong badge color");
    if(HuluAdBadge.matches(pixels,45,36)||HuluAdBadge.matches(null,46,36))throw new AssertionError("Invalid buffer");
    equal(1778,HuluAdBadge.left(1920));equal(60,HuluAdBadge.top(1080));
    equal(92,HuluAdBadge.width(3840));equal(72,HuluAdBadge.height(2160));
    System.out.println("Hulu mute ownership, manual overrides, badge glyph, scaling and negative cases passed");
  }
}
