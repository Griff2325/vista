package com.griffin.solhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Lightweight Home-owned popout. No overlay permission, artwork downloads, or API credentials. */
public final class SpotifyPanel {
  private static final String[] VIBES={"Focus","Chill","Energy","Cozy","Acoustic","Sleep"};
  private static final String[] PLAYLISTS={"37i9dQZF1DWZeKCadgRdKQ","37i9dQZF1DX4WYpdgoIcn6","37i9dQZF1DX76Wlfdnj7AP","37i9dQZF1DX0SM0LYsmbMT","37i9dQZF1DX4E3UdUs7fUx","37i9dQZF1DWZd79rJ6a7lp"};
  private static final String[] DEFAULT_NAMES={"Deep Focus","Chill Hits","Beast Mode","Jazz Vibes","Afternoon Acoustic","Sleep"};
  private static SpotifyPanel current;
  private final Activity activity;
  private final Dialog dialog;
  private final SharedPreferences preferences;
  private final SpotifyPlayback playback;
  private final ArrayList<Dialog> children=new ArrayList<>();
  private final TextView title,artist,status;
  private final Button previous,toggle,next,accessButton;
  private boolean closed;
  private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){
    public void onActivityCreated(Activity a,Bundle b){}
    public void onActivityStarted(Activity a){}
    public void onActivityResumed(Activity a){}
    public void onActivityPaused(Activity a){if(a==activity)dialog.dismiss();}
    public void onActivityStopped(Activity a){}
    public void onActivitySaveInstanceState(Activity a,Bundle b){}
    public void onActivityDestroyed(Activity a){if(a==activity)dialog.dismiss();}
  };
  public static Dialog show(Activity activity){dismiss();SpotifyPanel panel=new SpotifyPanel(activity);current=panel;panel.open();return panel.dialog;}
  /** May also be called from Home's onNewIntent to return focus straight to its app grid. */
  public static void dismiss(){if(current!=null)current.dialog.dismiss();}
  private SpotifyPanel(Activity activity){
    this.activity=activity;preferences=activity.getSharedPreferences("sol_spotify_vibes",Activity.MODE_PRIVATE);playback=new SpotifyPlayback(activity,this::render);
    dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(14),dp(16),dp(12));body.setBackground(background(0xf51c2b30,18));
    TextView heading=text("Spotify",18,0xffedf1df);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));body.addView(heading);
    title=text("No Spotify session",16,0xfffaf9ee);title.setPadding(0,dp(10),0,0);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);body.addView(title);
    artist=text("Open Spotify and choose some music",12,0xffb8c6bf);artist.setSingleLine(true);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);body.addView(artist);
    LinearLayout transport=row();previous=button("Previous",v->control(playback.previous()));toggle=button("Play",v->control(playback.toggle()));next=button("Next",v->control(playback.next()));
    transport.addView(previous,weighted());transport.addView(toggle,weighted());transport.addView(next,weighted());body.addView(transport);
    TextView label=text("Choose a vibe",13,0xffedf1df);label.setPadding(0,dp(8),0,dp(2));body.addView(label);
    for(int r=0;r<2;r++){LinearLayout row=row();for(int col=0;col<3;col++){final int index=r*3+col;row.addView(button(VIBES[index],v->openVibe(index)),weighted());}body.addView(row);}
    TextView hint=text("Vibes open Spotify playlists. Edit vibes to change them.\nPlayback stays in Spotify; Home returns here.",11,0xffa9bab2);hint.setPadding(0,dp(6),0,dp(6));body.addView(hint);
    status=text("",11,0xffb8c6bf);body.addView(status);
    accessButton=button("Enable playback controls…",v->requestAccess());body.addView(accessButton);
    LinearLayout footer=row();footer.addView(button("Edit vibes",v->editVibes()),weighted());footer.addView(button("Open Spotify",v->openSpotify(null)),weighted());footer.addView(button("Close",v->dialog.dismiss()),weighted());body.addView(footer);
    dialog.setContentView(body);dialog.setOnDismissListener(d->close());
  }
  private void open(){
    activity.getApplication().registerActivityLifecycleCallbacks(lifecycle);dialog.show();
    Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setDimAmount(.16f);window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.setGravity(Gravity.BOTTOM|Gravity.RIGHT);WindowManager.LayoutParams layout=window.getAttributes();layout.width=Math.min(dp(360),activity.getResources().getDisplayMetrics().widthPixels-dp(32));layout.height=WindowManager.LayoutParams.WRAP_CONTENT;layout.x=dp(24);layout.y=dp(24);window.setAttributes(layout);window.getDecorView().setSystemUiVisibility(5894);}
    playback.start();toggle.requestFocus();
  }
  private void render(SpotifyPlayback.State state){
    title.setText(!state.access?"Playback controls need access":!state.connected?"No Spotify session":state.title.isEmpty()?"Spotify has not shared a title":state.title);
    artist.setText(state.connected?(state.artist.isEmpty()?"Spotify":state.artist):"Open Spotify and choose some music");
    toggle.setText(state.playing?"Pause":"Play");toggle.setEnabled(state.canToggle);previous.setEnabled(state.canPrevious);next.setEnabled(state.canNext);
    accessButton.setVisibility(state.access?View.GONE:View.VISIBLE);
    status.setText(!state.access?"Optional: allow local media-session access for this popout.":state.connected?"Live controls · Spotify on this TV":"Controls appear when Spotify shares an active session on this TV.");
  }
  private void control(boolean sent){if(!sent)Toast.makeText(activity,"Spotify has not made this control available",Toast.LENGTH_SHORT).show();}
  private void requestAccess(){
    AlertDialog explanation=new AlertDialog.Builder(activity).setTitle("Enable Spotify controls")
      .setMessage("Android requires notification access to read another app’s active media session. This permission is broader than music controls. Vista uses it only for Spotify track title, artist, playback state, and buttons while this panel is open. It does not read or store notification content or listening history.\n\nOn the next screen, enable Vista’s Spotify controls. You can revoke access there at any time.")
      .setPositiveButton("Open access settings",(d,w)->{
        try{activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}
        catch(Exception unsupported){new AlertDialog.Builder(activity).setTitle("Not available on this TV").setMessage("This Fire OS version does not expose Android’s notification-access screen. Vibe shortcuts still work. Playback control access needs a compatible system setting; Vista will not silently grant it.").setPositiveButton("OK",null).show();}
      }).setNegativeButton("Cancel",null).create();children.add(explanation);explanation.show();
  }
  private void editVibes(){
    String[] labels=new String[VIBES.length];for(int i=0;i<labels.length;i++)labels[i]=VIBES[i]+(preferences.contains("vibe_"+i)?" · custom link":" · "+DEFAULT_NAMES[i]);
    AlertDialog picker=new AlertDialog.Builder(activity).setTitle("Vibe shortcuts").setItems(labels,(d,index)->editVibe(index)).setNegativeButton("Close",null).create();children.add(picker);picker.show();
  }
  private void editVibe(int index){
    LinearLayout form=new LinearLayout(activity);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(24),dp(8),dp(24),0);
    form.addView(text("Paste a Spotify playlist, album, track, or artist link. It opens in Spotify; autoplay depends on the TV app.",15,Color.WHITE));
    EditText uri=new EditText(activity);uri.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);uri.setSingleLine(true);uri.setHint("https://open.spotify.com/playlist/…");uri.setText(preferences.getString("vibe_"+index,""));form.addView(uri);
    AlertDialog editor=new AlertDialog.Builder(activity).setTitle(VIBES[index]+" shortcut").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",null).setNeutralButton("Reset playlist",(d,w)->preferences.edit().remove("vibe_"+index).apply()).create();children.add(editor);
    editor.setOnShowListener(d->editor.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{String normalized=SpotifyLinks.normalize(uri.getText().toString());preferences.edit().putString("vibe_"+index,normalized).apply();editor.dismiss();}catch(IllegalArgumentException invalid){uri.setError(invalid.getMessage());}}));editor.show();
  }
  private void openVibe(int index){
    String uri=preferences.getString("vibe_"+index,null);
    if(uri!=null){try{uri=SpotifyLinks.normalize(uri);}catch(IllegalArgumentException invalid){uri=null;}}
    openSpotify(uri==null?"spotify:playlist:"+PLAYLISTS[index]:uri);
  }
  private void openSpotify(String content){
    String[] packages={"com.spotify.tv.android","com.spotify.music"};
    for(String pkg:packages){
      Intent launch=activity.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(launch==null)launch=activity.getPackageManager().getLaunchIntentForPackage(pkg);if(launch==null)continue;
      if(content!=null){Intent deep=new Intent(Intent.ACTION_VIEW,Uri.parse(content)).setPackage(pkg);deep.putExtra(Intent.EXTRA_REFERRER,Uri.parse("android-app://"+activity.getPackageName()));try{activity.startActivity(deep);dialog.dismiss();return;}catch(Exception unsupported){/* TV builds may accept app launch only. */}}
      try{activity.startActivity(launch);dialog.dismiss();if(content!=null)Toast.makeText(activity,"This Spotify TV app could not open the shortcut. Choose music in Spotify.",Toast.LENGTH_LONG).show();return;}catch(Exception unsupported){/* Try the other installed Spotify package. */}
    }
    Toast.makeText(activity,"Spotify is not available on this TV",Toast.LENGTH_LONG).show();
  }
  private void close(){if(closed)return;closed=true;playback.close();activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycle);for(Dialog child:children)if(child.isShowing())child.dismiss();children.clear();if(current==this)current=null;}
  private int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
  private TextView text(String value,int size,int color){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(color);return view;}
  private LinearLayout row(){LinearLayout row=new LinearLayout(activity);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(0,dp(4),0,0);return row;}
  private LinearLayout.LayoutParams weighted(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(36),1);p.setMargins(dp(3),0,dp(3),0);return p;}
  private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
  private Button button(String label,View.OnClickListener click){Button b=new Button(activity);b.setText(label);b.setTextSize(12);b.setTextColor(0xffecf0e2);b.setAllCaps(false);b.setMinHeight(dp(36));b.setMinimumHeight(dp(36));b.setPadding(dp(8),0,dp(8),0);StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},background(0xff263333,8));states.addState(new int[]{android.R.attr.state_focused},background(0xff687b68,8));states.addState(new int[]{android.R.attr.state_pressed},background(0xff687b68,8));states.addState(new int[]{},background(0xff32443f,8));b.setBackground(states);b.setOnClickListener(click);return b;}
}
