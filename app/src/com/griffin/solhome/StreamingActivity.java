package com.griffin.solhome;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.InputType;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Remote-friendly streaming discovery. Platform apps keep ownership of sign-in and playback. */
public final class StreamingActivity extends Activity {
  private static final int BG=0xff17242a,PANEL=0xff24383b,INK=0xfff2f0e7,MUTED=0xffaebdb8,ACCENT=0xffd2b98b;
  private android.content.SharedPreferences prefs;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private StreamingCatalog catalog;
  private StreamingCatalog.Query query=new StreamingCatalog.Query();
  private LinearLayout content,results,filters;
  private EditText search;
  private TextView status;
  private Button prev,next;
  private boolean destroyed,savedMode;
  private Future<?> loading;
  private int generation;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
    prefs=getSharedPreferences("sol.streaming",MODE_PRIVATE);
    for(int i=0;i<4;i++)query.services[i]=prefs.getBoolean("service."+i,installed(StreamingCatalog.PACKAGES[i]));
    for(int i=0;i<8;i++)query.interests[i]=prefs.getBoolean("interest."+i,false);
    if(!prefs.getString("tmdb","").isEmpty())catalog=new StreamingCatalog(prefs.getString("tmdb",""));
    ScrollView page=new ScrollView(this);page.setFillViewport(true);page.setBackgroundColor(BG);
    content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(38),dp(24),dp(38),dp(26));page.addView(content);setContentView(page);
    LinearLayout title=row();TextView heading=text("Explore",30,INK);heading.setTypeface(null,Typeface.BOLD);title.addView(heading,new LinearLayout.LayoutParams(0,-2,1));title.addView(button("My list",v->showSaved()));title.addView(button("Catalog settings",v->settings()));title.addView(button("Back",v->finish()));content.addView(title);
    content.addView(text("One place to find something to watch",15,MUTED));
    LinearLayout box=row();box.setPadding(0,dp(16),0,dp(10));search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("Search movies and TV shows");search.setTextSize(18);search.setInputType(InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);box.addView(search,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(button("Search",v->runSearch()));box.addView(button("Discover",v->{search.setText("");query.text="";query.page=1;savedMode=false;load();}));content.addView(box);
    search.setOnEditorActionListener((v,action,e)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){runSearch();return true;}return false;});
    filters=row();content.addView(filters);renderFilters();
    LinearLayout shortcuts=row();shortcuts.setPadding(0,dp(10),0,dp(8));shortcuts.addView(text("Open apps",14,MUTED));
    for(int i=0;i<4;i++){final int service=i;if(installed(StreamingCatalog.PACKAGES[i]))shortcuts.addView(button(StreamingCatalog.SERVICES[i],v->openService(service,search.getText().toString())));}
    if(installed("com.amazon.firetv.youtube"))shortcuts.addView(button("YouTube",v->openPackage("com.amazon.firetv.youtube",search.getText().toString())));
    content.addView(shortcuts);
    status=text("",14,MUTED);status.setPadding(0,dp(8),0,dp(12));content.addView(status);
    results=new LinearLayout(this);results.setOrientation(1);content.addView(results);
    LinearLayout paging=row();paging.setPadding(0,dp(12),0,dp(12));prev=button("Previous page",v->{if(query.page>1){query.page--;load();}});next=button("Next page",v->{query.page++;load();});paging.addView(prev);paging.addView(next);content.addView(paging);
    content.addView(text("Catalog: TMDB · US streaming availability: JustWatch · Subscription, free and ad-supported offers only",12,MUTED));
    content.addView(text("Selected services are your preferences, not detected sign-ins. Availability can change; confirm in the provider app. YouTube is an app shortcut, outside the movie/TV catalog.",12,MUTED));
    content.addView(button("Sources and API attribution",v->about()));
    String incoming=getIntent().getStringExtra("vista.search");
    if(incoming!=null&&incoming.length()<=120)companionSearch(incoming);else load();
  }
  void companionSearch(String text){search.setText(text);runSearch();}
  private void runSearch(){query.text=search.getText().toString().trim();if(query.text.length()>120){search.setError("Use up to 120 characters");return;}query.page=1;savedMode=false;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);load();}
  private void renderFilters(){
    filters.removeAllViews();filters.addView(button("Services ("+selected(query.services)+")",v->services()));
    filters.addView(button(new String[]{"Movies + TV","Movies","TV shows"}[query.type],v->new AlertDialog.Builder(this).setTitle("Content type").setSingleChoiceItems(new String[]{"Movies + TV","Movies","TV shows"},query.type,(d,n)->{query.type=n;d.dismiss();changed();}).setNegativeButton("Cancel",null).show()));
    filters.addView(button("Interests ("+selected(query.interests)+")",v->interests()));
    filters.addView(button(new String[]{"Popular","Top rated","Newest releases"}[query.sort],v->new AlertDialog.Builder(this).setTitle("Sort discovery").setSingleChoiceItems(new String[]{"Popular","Top rated (100+ ratings)","Newest releases"},query.sort,(d,n)->{query.sort=n;d.dismiss();changed();}).setNegativeButton("Cancel",null).show()));
  }
  private void changed(){query.page=1;savedMode=false;renderFilters();load();}
  private void services(){boolean[] selected=query.services.clone();String[] names=new String[4];for(int i=0;i<4;i++)names[i]=StreamingCatalog.SERVICES[i]+(installed(StreamingCatalog.PACKAGES[i])?" · installed":" · app not installed");new AlertDialog.Builder(this).setTitle("My video services · US").setMultiChoiceItems(names,selected,(d,n,on)->selected[n]=on).setPositiveButton("Apply",(d,w)->{query.services=selected;android.content.SharedPreferences.Editor p=prefs.edit();for(int i=0;i<4;i++)p.putBoolean("service."+i,selected[i]);p.apply();changed();}).setNegativeButton("Cancel",null).show();}
  private void interests(){boolean[] selected=query.interests.clone();new AlertDialog.Builder(this).setTitle("Genres you enjoy · matches any selected").setMultiChoiceItems(StreamingCatalog.INTERESTS,selected,(d,n,on)->selected[n]=on).setPositiveButton("Apply",(d,w)->{query.interests=selected;android.content.SharedPreferences.Editor p=prefs.edit();for(int i=0;i<8;i++)p.putBoolean("interest."+i,selected[i]);p.apply();changed();}).setNegativeButton("Cancel",null).show();}
  private StreamingCatalog.Query snapshot(){StreamingCatalog.Query q=new StreamingCatalog.Query();q.text=query.text;q.type=query.type;q.sort=query.sort;q.page=query.page;q.services=query.services.clone();q.interests=query.interests.clone();return q;}
  private void load(){
    if(loading!=null)loading.cancel(true);int request=++generation;results.removeAllViews();prev.setEnabled(false);next.setEnabled(false);
    if(catalog==null){status.setText("Connect a catalog to search across services. App shortcuts above work now.");results.addView(text("Your streaming apps keep their own accounts and playback. Vista uses an optional TMDB API credential to look up titles and US availability, without reading streaming passwords or watch history.",18,INK));results.addView(button("Set up TMDB catalog",v->settings()));return;}
    if(selected(query.services)==0){status.setText("Choose at least one video service to find titles.");return;}
    status.setText("Finding titles and checking US availability…");final StreamingCatalog active=catalog;final StreamingCatalog.Query q=snapshot();
    loading=worker.submit(()->{try{StreamingCatalog.Page page=active.load(q);runOnUiThread(()->{if(destroyed||request!=generation)return;render(page,q);});}catch(Exception error){String message=error.getMessage();runOnUiThread(()->{if(destroyed||request!=generation)return;status.setText(message!=null&&(message.startsWith("TMDB")||message.startsWith("Catalog"))?message:"Could not reach the catalog. Check the connection and try again.");results.addView(button("Try again",v->load()));});}});
  }
  private void render(StreamingCatalog.Page page,StreamingCatalog.Query q){
    results.removeAllViews();String description=q.text.isEmpty()?(selected(q.interests)>0?"Discovery for your selected interests":new String[]{"Popular picks from your selected services","Top rated picks from your selected services","Newest releases from your selected services"}[q.sort]):"Search: “"+q.text+"”";
    status.setText(description+" · "+page.items.size()+" matches · catalog page "+q.page+(q.text.isEmpty()?"":" (first 20 title matches per page; filters may leave fewer)")+(page.unavailable>0?" · "+page.unavailable+" availability lookups failed":""));
    if(page.items.isEmpty())results.addView(text("No verified streaming matches on this catalog page. Try the next page, fewer interests, or more services.",18,INK));
    LinearLayout row=null;for(int i=0;i<page.items.size();i++){if(i%3==0){row=row();row.setGravity(Gravity.TOP);results.addView(row);}row.addView(card(page.items.get(i)),new LinearLayout.LayoutParams(0,dp(150),1));}
    prev.setEnabled(q.page>1);next.setEnabled(q.page<Math.min(500,page.totalPages));
  }
  private View card(JSONObject item){
    Button b=button("",v->details(item));String date=StreamingCatalog.date(item);String year=date.length()>=4?date.substring(0,4):"";
    b.setText(StreamingCatalog.title(item)+"\n"+(item.optString("media_type").equals("tv")?"TV":"Movie")+" · "+year+" · ★ "+String.format(Locale.US,"%.1f",item.optDouble("vote_average"))+"\n"+availableNames(item));b.setTextSize(17);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(dp(16),dp(10),dp(16),dp(10));b.setMaxLines(5);b.setAllCaps(false);return b;
  }
  private String availableNames(JSONObject item){LinkedHashSet<String> names=new LinkedHashSet<>();JSONObject us=item.optJSONObject("sol_availability");if(us!=null)for(String mode:new String[]{"flatrate","free","ads"}){JSONArray a=us.optJSONArray(mode);if(a!=null)for(int j=0;j<a.length();j++){JSONObject p=a.optJSONObject(j);if(p!=null)for(String s:StreamingCatalog.PROVIDER_NAMES)if(s.equals(p.optString("provider_name")))names.add(displayService(s));}}return names.isEmpty()?"Availability not checked":android.text.TextUtils.join(" · ",names);}
  private String displayService(String raw){for(int i=0;i<4;i++)if(StreamingCatalog.PROVIDER_NAMES[i].equals(raw))return StreamingCatalog.SERVICES[i];return raw;}
  private void details(JSONObject item){
    LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(22),dp(8),dp(22),dp(8));
    String overview=item.optString("overview");body.addView(text(overview.isEmpty()?"No synopsis available.":overview,17,INK));
    body.addView(text("US · "+availableNames(item)+" · checked "+new java.text.SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date(item.optLong("sol_checked")))+"\nAvailability supplied by JustWatch via TMDB. Provider apps confirm access and subscription tier. App buttons open/search the selected app; they do not start a verified title deep link.",13,MUTED));
    for(int i=0;i<4;i++){final int service=i;if(providerOffered(item,StreamingCatalog.PROVIDER_NAMES[i]))body.addView(button("Open "+StreamingCatalog.SERVICES[i],v->openService(service,StreamingCatalog.title(item))));}
    body.addView(button(saved(item)?"Remove from my list":"Save to my list",v->{toggleSaved(item);toast(saved(item)?"Saved on this TV":"Removed from this TV");if(savedMode)showSaved();}));
    String watch=StreamingPolicy.safeWatchUrl(item.optJSONObject("sol_availability")==null?"":item.optJSONObject("sol_availability").optString("link"));if(watch!=null)body.addView(button("Where to watch · TMDB / JustWatch",v->openWeb(watch)));
    ScrollView scroll=new ScrollView(this);scroll.addView(body);new AlertDialog.Builder(this).setTitle(StreamingCatalog.title(item)).setView(scroll).setNegativeButton("Close",null).show();
  }
  private boolean providerOffered(JSONObject item,String name){JSONObject us=item.optJSONObject("sol_availability");if(us==null)return false;for(String mode:new String[]{"flatrate","free","ads"}){JSONArray rows=us.optJSONArray(mode);if(rows!=null)for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i)!=null&&name.equals(rows.optJSONObject(i).optString("provider_name")))return true;}return false;}
  private JSONArray savedItems(){try{return new JSONArray(prefs.getString("saved","[]"));}catch(Exception e){return new JSONArray();}}
  private boolean saved(JSONObject item){JSONArray a=savedItems();for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s!=null&&s.optInt("id")==item.optInt("id")&&s.optString("media_type").equals(item.optString("media_type")))return true;}return false;}
  private void toggleSaved(JSONObject item){JSONArray old=savedItems(),out=new JSONArray();boolean remove=saved(item);for(int i=0;i<old.length();i++){JSONObject s=old.optJSONObject(i);if(s!=null&&!(s.optInt("id")==item.optInt("id")&&s.optString("media_type").equals(item.optString("media_type"))))out.put(s);}if(!remove){if(out.length()>=100){toast("My list holds up to 100 titles");return;}out.put(item);}prefs.edit().putString("saved",out.toString()).apply();}
  private void showSaved(){++generation;savedMode=true;results.removeAllViews();prev.setEnabled(false);next.setEnabled(false);JSONArray a=savedItems();status.setText("My list · saved on this TV · availability reflects the last lookup (open details for timestamp)");if(a.length()==0)results.addView(text("Save titles from their details to build your list.",18,INK));for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null)results.addView(card(a.optJSONObject(i)),new LinearLayout.LayoutParams(-1,dp(120)));}
  private void settings(){
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(24),dp(8),dp(24),dp(8));
    form.addView(text("Add your TMDB API Read Access Token (recommended) or v3 key. This credential stays in Vista’s private app storage. No Netflix, Hulu, Disney+ or Apple passwords are needed. Non-commercial API access requires a TMDB account.",15,INK));
    EditText key=new EditText(this);key.setSingleLine(true);key.setTextColor(INK);key.setHintTextColor(MUTED);key.setHint("TMDB token or API key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setText(prefs.getString("tmdb",""));form.addView(key);
    form.addView(button("TMDB API account settings",v->openWeb("https://www.themoviedb.org/settings/api")));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Catalog connection").setView(form).setPositiveButton("Save",null).setNeutralButton("Disconnect",(d,w)->{prefs.edit().remove("tmdb").apply();if(catalog!=null)catalog.close();catalog=null;load();}).setNegativeButton("Cancel",null).create();
    dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=key.getText().toString().trim();if(!StreamingPolicy.credentialValid(value)){key.setError("Enter the API Read Access Token or 32-character v3 key");return;}prefs.edit().putString("tmdb",value).apply();if(catalog!=null)catalog.close();catalog=new StreamingCatalog(value);dialog.dismiss();changed();}));dialog.show();
  }
  private void about(){
    LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(24),dp(12),dp(24),dp(8));
    try(java.io.InputStream in=getAssets().open("streaming-tmdb.png")){ImageView logo=new ImageView(this);logo.setImageDrawable(Drawable.createFromStream(in,"TMDB"));logo.setContentDescription("The Movie Database");logo.setScaleType(ImageView.ScaleType.FIT_START);body.addView(logo,new LinearLayout.LayoutParams(dp(120),dp(87)));}catch(Exception ignored){}
    body.addView(text("This product uses the TMDB API but is not endorsed or certified by TMDB. US streaming availability is supplied by JustWatch through TMDB.\n\nDiscovery uses popularity, rating or release date and your selected genres/services. It does not use your private viewing history. Rental/purchase offers are excluded. Catalog completeness and provider tiers vary.\n\nYour credential and saved list stay on this TV; searches and API requests go directly to TMDB over HTTPS.",15,INK));
    ScrollView scroll=new ScrollView(this);scroll.addView(body);new AlertDialog.Builder(this).setTitle("Catalog sources").setView(scroll).setPositiveButton("TMDB",(d,w)->openWeb("https://www.themoviedb.org")).setNeutralButton("JustWatch",(d,w)->openWeb("https://www.justwatch.com/us")).setNegativeButton("Close",null).show();
  }
  private boolean installed(String pkg){try{getPackageManager().getApplicationInfo(pkg,0);return true;}catch(Exception e){return false;}}
  private void openService(int i,String text){openPackage(StreamingCatalog.PACKAGES[i],text);}
  private void openPackage(String pkg,String text){try{
    if(!text.trim().isEmpty()){Intent searchIntent=new Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(android.app.SearchManager.QUERY,text);if(searchIntent.resolveActivity(getPackageManager())!=null){startActivity(searchIntent);return;}}
    Intent intent=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(intent==null)intent=getPackageManager().getLaunchIntentForPackage(pkg);if(intent==null){toast("Install this app to open it");return;}startActivity(intent);if(!text.trim().isEmpty())toast("Search “"+text+"” in the app");
  }catch(Exception e){toast("Could not open this app");}}
  private void openWeb(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE));}catch(Exception e){toast("Open this address on another device: "+url);}}
  private int selected(boolean[] values){int n=0;for(boolean v:values)if(v)n++;return n;}
  private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
  private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,dp(4),0,dp(4));return t;}
  private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  private Button button(String s,View.OnClickListener click){Button b=new Button(this);b.setText(s);b.setTextColor(INK);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(44));b.setMinWidth(dp(64));b.setPadding(dp(14),dp(8),dp(14),dp(8));StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_focused},shape(0xff48615b,ACCENT));states.addState(new int[]{android.R.attr.state_pressed},shape(0xff48615b,ACCENT));states.addState(new int[]{},shape(PANEL,0x00304343));b.setBackground(states);b.setOnClickListener(click);b.setOnFocusChangeListener((v,f)->b.setTextColor(f?Color.WHITE:INK));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(3),dp(3),dp(3),dp(3));b.setLayoutParams(p);return b;}
  private GradientDrawable shape(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(7));d.setStroke(dp(2),stroke);return d;}
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override protected void onDestroy(){destroyed=true;++generation;worker.shutdownNow();if(catalog!=null)catalog.close();super.onDestroy();}
}
