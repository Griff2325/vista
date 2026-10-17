package com.griffin.solhome;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import java.util.List;

/** In-memory Spotify-only controller. Open while a panel is visible, close on dismissal. */
public final class SpotifyPlayback implements AutoCloseable {
  public interface Listener { void changed(State state); }
  public static final class State {
    public final boolean access,connected,playing,canToggle,canPrevious,canNext;
    public final String title,artist;
    State(boolean access,boolean connected,boolean playing,boolean toggle,boolean previous,boolean next,String title,String artist){
      this.access=access;this.connected=connected;this.playing=playing;this.canToggle=toggle;this.canPrevious=previous;this.canNext=next;this.title=title;this.artist=artist;
    }
  }
  private final Context context;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final MediaSessionManager manager;
  private final ComponentName component;
  private final Listener listener;
  private MediaController controller;
  private boolean started,listening,access;
  private final MediaSessionManager.OnActiveSessionsChangedListener sessions=this::choose;
  private final MediaController.Callback callback=new MediaController.Callback(){
    @Override public void onMetadataChanged(MediaMetadata metadata){publish();}
    @Override public void onPlaybackStateChanged(PlaybackState state){publish();}
    @Override public void onSessionDestroyed(){refresh();}
  };
  public SpotifyPlayback(Context context,Listener listener){
    this.context=context.getApplicationContext();this.listener=listener;
    manager=(MediaSessionManager)this.context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    component=new ComponentName(this.context,SpotifyMediaListener.class);
  }
  public static boolean accessEnabled(Context context){
    String setting=Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
    if(setting==null)return false;
    ComponentName expected=new ComponentName(context,SpotifyMediaListener.class);
    for(String item:setting.split(":"))if(expected.equals(ComponentName.unflattenFromString(item)))return true;
    return false;
  }
  public void start(){
    if(started)return;started=true;access=accessEnabled(context);
    if(access&&manager!=null){try{manager.addOnActiveSessionsChangedListener(sessions,component,handler);listening=true;}catch(SecurityException ignored){access=false;}}
    refresh();
  }
  private void refresh(){
    if(!started)return;
    if(access&&manager!=null){try{choose(manager.getActiveSessions(component));return;}catch(SecurityException ignored){access=false;}}
    choose(null);
  }
  private static boolean spotify(MediaController candidate){String pkg=candidate.getPackageName();return "com.spotify.tv.android".equals(pkg)||"com.spotify.music".equals(pkg);}
  private void choose(List<MediaController> active){
    if(!started)return;
    MediaController selected=null;
    if(active!=null)for(MediaController item:active){if(!spotify(item))continue;if(selected==null)selected=item;PlaybackState state=item.getPlaybackState();if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){selected=item;break;}}
    if(controller!=null&&(selected==null||!controller.getSessionToken().equals(selected.getSessionToken()))){controller.unregisterCallback(callback);controller=null;}
    if(controller==null&&selected!=null){controller=selected;controller.registerCallback(callback,handler);}
    publish();
  }
  private void publish(){
    if(!started)return;
    MediaMetadata metadata=controller==null?null:controller.getMetadata();
    PlaybackState playback=controller==null?null:controller.getPlaybackState();
    boolean playing=playback!=null&&playback.getState()==PlaybackState.STATE_PLAYING;
    long actions=playback==null?0:playback.getActions();
    boolean toggle=(actions&PlaybackState.ACTION_PLAY_PAUSE)!=0||(actions&(playing?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY))!=0;
    listener.changed(new State(access,controller!=null,playing,toggle,(actions&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0,(actions&PlaybackState.ACTION_SKIP_TO_NEXT)!=0,
      text(metadata,MediaMetadata.METADATA_KEY_TITLE),text(metadata,MediaMetadata.METADATA_KEY_ARTIST)));
  }
  private static String text(MediaMetadata metadata,String key){CharSequence value=metadata==null?null:metadata.getText(key);return value==null?"":value.toString();}
  public boolean toggle(){
    if(controller==null)return false;
    try{
      PlaybackState state=controller.getPlaybackState();if(state==null)return false;
      boolean playing=state.getState()==PlaybackState.STATE_PLAYING;long action=playing?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY;
      if((state.getActions()&action)!=0){if(playing)controller.getTransportControls().pause();else controller.getTransportControls().play();return true;}
      if((state.getActions()&PlaybackState.ACTION_PLAY_PAUSE)!=0){controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));return true;}
    }catch(RuntimeException ignored){refresh();}
    return false;
  }
  public boolean previous(){return skip(false);}
  public boolean next(){return skip(true);}
  private boolean skip(boolean next){
    if(controller==null)return false;
    try{PlaybackState state=controller.getPlaybackState();long action=next?PlaybackState.ACTION_SKIP_TO_NEXT:PlaybackState.ACTION_SKIP_TO_PREVIOUS;if(state==null||(state.getActions()&action)==0)return false;if(next)controller.getTransportControls().skipToNext();else controller.getTransportControls().skipToPrevious();return true;}
    catch(RuntimeException ignored){refresh();return false;}
  }
  @Override public void close(){
    started=false;if(controller!=null){controller.unregisterCallback(callback);controller=null;}
    if(listening&&manager!=null){manager.removeOnActiveSessionsChangedListener(sessions);listening=false;}
    handler.removeCallbacksAndMessages(null);
  }
}
