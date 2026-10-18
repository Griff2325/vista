package com.griffin.solhome;

/** Own only mutes we initiated; a manual unmute/volume change wins for this break. */
final class HuluMutePolicy {
  static final int NONE=0,MUTE=1,UNMUTE=2;
  private boolean owned,suppressed;
  private int originalVolume,misses;
  int sample(boolean ad,boolean muted,int volume){
    if(owned&&(!muted||!sameVolume(volume))){owned=false;suppressed=true;}
    if(!ad){
      if(++misses<2)return NONE;
      suppressed=false;
      return release(muted,volume);
    }
    misses=0;
    if(!owned&&!suppressed&&!muted){owned=true;originalVolume=volume;return MUTE;}
    return NONE;
  }
  int stop(boolean muted,int volume){int action=release(muted,volume);suppressed=false;misses=0;return action;}
  void volumeChanged(int volume){if(owned&&volume>0&&volume!=originalVolume){owned=false;suppressed=true;}}
  // Android reports zero for a muted stream; broadcasts still report underlying volume changes.
  private boolean sameVolume(int volume){return volume==0||volume==originalVolume;}
  private int release(boolean muted,int volume){boolean restore=owned&&muted&&sameVolume(volume);owned=false;return restore?UNMUTE:NONE;}
}
