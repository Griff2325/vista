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
  private static final int BG=0xff09090b,PANEL=0xff202024,INK=0xfff5f5f7,MUTED=0xffa1a1a8,ACCENT=0xffffffff;
  private android.content.SharedPreferences prefs;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private StreamingCatalog catalog;
  private StreamingCatalog.Query query=new StreamingCatalog.Query();
  private LinearLayout content,results,filters;
  private final StreamingArtwork artwork=new StreamingArtwork();
  private final ExecutorService ratingWorker=Executors.newFixedThreadPool(2);
  private final Map<String,ArrayList<TextView>> ratingViews=new HashMap<>();
  private final ArrayList<Future<?>> ratingRequests=new ArrayList<>();
  private ScrollView page;
  private EditText search;
  private TextView status;
  private Button prev,next;
  private volatile boolean destroyed;
  private boolean savedMode;
  private Future<?> loading;
  private volatile int generation;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(5894);
    prefs=getSharedPreferences("sol.streaming",MODE_PRIVATE);
    for(int i=0;i<StreamingCatalog.SERVICES.length;i++)query.services[i]=prefs.getBoolean("service."+i,installed(StreamingCatalog.PACKAGES[i]));
    for(int i=0;i<8;i++)query.interests[i]=prefs.getBoolean("interest."+i,false);
    if(!prefs.getString("tmdb","").isEmpty())catalog=new StreamingCatalog(prefs.getString("tmdb",""),prefs.getString("omdb",""));
    query.allServices=prefs.getBoolean("allServices",true);
    LinearLayout shell=row();shell.setGravity(Gravity.TOP);shell.setBackgroundColor(BG);setContentView(shell);
    LinearLayout nav=new LinearLayout(this);nav.setOrientation(1);nav.setPadding(dp(16),dp(26),dp(12),dp(20));nav.setBackgroundColor(0xff131316);shell.addView(nav,new LinearLayout.LayoutParams(dp(144),-1));
    TextView brand=text("◉  Explore",22,INK);brand.setTypeface(null,Typeface.BOLD);nav.addView(brand);
    TextView byline=text("V I S T A",11,MUTED);byline.setPadding(dp(6),0,0,dp(28));nav.addView(byline);
    nav.addView(button("Home",v->{search.setText("");query.text="";query.type=0;query.page=1;savedMode=false;renderFilters();load();}));
    nav.addView(button("Search",v->{search.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT);}));
    nav.addView(button("Movies",v->{search.setText("");query.text="";query.type=1;changed();}));
    nav.addView(button("TV shows",v->{search.setText("");query.text="";query.type=2;changed();}));
    nav.addView(button("My list",v->showSaved()));
    Space spacer=new Space(this);nav.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
    nav.addView(button("Settings",v->settings()));nav.addView(button("Back to Vista",v->finish()));
    page=new ScrollView(this);page.setFillViewport(true);page.setClipToPadding(false);shell.addView(page,new LinearLayout.LayoutParams(0,-1,1));
    content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(26),dp(20),dp(26),dp(24));page.addView(content);
    LinearLayout box=row();search=new EditText(this);search.setSingleLine(true);search.setTextColor(INK);search.setHintTextColor(MUTED);search.setHint("Movies, shows, all your services");search.setTextSize(17);search.setPadding(dp(16),0,dp(12),0);search.setBackground(shape(PANEL,0x00434343));search.setInputType(InputType.TYPE_CLASS_TEXT);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);box.addView(search,new LinearLayout.LayoutParams(0,dp(46),1));box.addView(button("Search",v->runSearch()));content.addView(box);
    search.setOnEditorActionListener((v,action,e)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){runSearch();return true;}return false;});
    HorizontalScrollView filterScroll=new HorizontalScrollView(this);filterScroll.setHorizontalScrollBarEnabled(false);filters=row();filterScroll.addView(filters);content.addView(filterScroll);renderFilters();
    status=text("",12,MUTED);status.setPadding(0,dp(8),0,dp(12));content.addView(status);
    results=new LinearLayout(this);results.setOrientation(1);content.addView(results);
    LinearLayout paging=row();paging.setPadding(0,dp(12),0,dp(12));prev=button("Previous",v->{if(query.page>1){query.page--;load();}});next=button("Next page",v->{query.page++;load();});paging.addView(prev);paging.addView(next);content.addView(paging);
    TextView apps=text("Your apps",20,INK);apps.setTypeface(null,Typeface.BOLD);content.addView(apps);
    HorizontalScrollView appScroll=new HorizontalScrollView(this);appScroll.setHorizontalScrollBarEnabled(false);LinearLayout shortcuts=row();appScroll.addView(shortcuts);
    for(int i=0;i<StreamingCatalog.SERVICES.length;i++){final int service=i;if(installed(StreamingCatalog.PACKAGES[i]))shortcuts.addView(appTile(StreamingCatalog.SERVICES[i],StreamingCatalog.PACKAGES[i],v->openService(service,search.getText().toString())));}
    if(installed("com.amazon.firetv.youtube"))shortcuts.addView(appTile("YouTube","com.amazon.firetv.youtube",v->openPackage("com.amazon.firetv.youtube",search.getText().toString())));
    content.addView(appScroll);
    content.addView(text("TMDB / JustWatch · US availability · Provider apps confirm your access",11,MUTED));
    content.addView(button("Sources & privacy",v->about()));
    String incoming=getIntent().getStringExtra("vista.search");
    if(incoming!=null&&incoming.length()<=120)companionSearch(incoming);else load();
  }
  void companionSearch(String text){search.setText(text);runSearch();}
  private void runSearch(){query.text=search.getText().toString().trim();if(query.text.length()>120){search.setError("Use up to 120 characters");return;}query.page=1;savedMode=false;((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);load();}
  private void renderFilters(){
    filters.removeAllViews();filters.addView(button(query.allServices?"All services":"My services ("+selected(query.services)+")",v->services()));
    filters.addView(button(new String[]{"Movies + TV","Movies","TV shows"}[query.type],v->new AlertDialog.Builder(this).setTitle("Content type").setSingleChoiceItems(new String[]{"Movies + TV","Movies","TV shows"},query.type,(d,n)->{query.type=n;d.dismiss();changed();}).setNegativeButton("Cancel",null).show()));
    filters.addView(button("Interests ("+selected(query.interests)+")",v->interests()));
    filters.addView(button(new String[]{"Popular","Top rated","Newest releases"}[query.sort],v->new AlertDialog.Builder(this).setTitle("Sort discovery").setSingleChoiceItems(new String[]{"Popular","Top TMDB rated (100+ votes)","Newest releases"},query.sort,(d,n)->{query.sort=n;d.dismiss();changed();}).setNegativeButton("Cancel",null).show()));
  }
  private void changed(){query.page=1;savedMode=false;renderFilters();load();}
  private void services(){
    boolean[] choices=new boolean[StreamingCatalog.SERVICES.length+1];choices[0]=query.allServices;System.arraycopy(query.services,0,choices,1,query.services.length);
    String[] names=new String[choices.length];names[0]="All services · search everywhere";
    for(int i=0;i<StreamingCatalog.SERVICES.length;i++)names[i+1]=StreamingCatalog.SERVICES[i]+(installed(StreamingCatalog.PACKAGES[i])?" · installed":"");
    new AlertDialog.Builder(this).setTitle("Where to watch · US").setMultiChoiceItems(names,choices,(d,n,on)->choices[n]=on).setPositiveButton("Apply",(d,w)->{
      query.allServices=choices[0];System.arraycopy(choices,1,query.services,0,query.services.length);android.content.SharedPreferences.Editor p=prefs.edit().putBoolean("allServices",query.allServices);for(int i=0;i<query.services.length;i++)p.putBoolean("service."+i,query.services[i]);p.apply();changed();
    }).setNegativeButton("Cancel",null).show();
  }
  private void interests(){boolean[] selected=query.interests.clone();new AlertDialog.Builder(this).setTitle("Genres you enjoy · matches any selected").setMultiChoiceItems(StreamingCatalog.INTERESTS,selected,(d,n,on)->selected[n]=on).setPositiveButton("Apply",(d,w)->{query.interests=selected;android.content.SharedPreferences.Editor p=prefs.edit();for(int i=0;i<8;i++)p.putBoolean("interest."+i,selected[i]);p.apply();changed();}).setNegativeButton("Cancel",null).show();}
  private StreamingCatalog.Query snapshot(){StreamingCatalog.Query q=new StreamingCatalog.Query();q.text=query.text;q.type=query.type;q.sort=query.sort;q.page=query.page;q.allServices=query.allServices;q.services=query.services.clone();q.interests=query.interests.clone();return q;}
  private void load(){
    if(loading!=null)loading.cancel(true);int request=++generation;cancelRatings();page.smoothScrollTo(0,0);results.removeAllViews();ratingViews.clear();prev.setEnabled(false);next.setEnabled(false);
    if(catalog==null){prev.setVisibility(View.GONE);next.setVisibility(View.GONE);status.setText("Your apps are ready. Connect the free catalog to browse and search titles.");welcome();return;}
    if(!query.allServices&&selected(query.services)==0&&query.text.isEmpty()){status.setText("Choose All services or select a service to browse.");results.addView(button("Choose services",v->services()));return;}
    prev.setVisibility(View.VISIBLE);next.setVisibility(View.VISIBLE);
    status.setText("Finding titles and checking US availability…");final StreamingCatalog active=catalog;final StreamingCatalog.Query q=snapshot();
    loading=worker.submit(()->{try{StreamingCatalog.Page page=active.load(q);runOnUiThread(()->{if(destroyed||request!=generation)return;render(page,q);});}catch(Exception error){String message=error.getMessage();runOnUiThread(()->{if(destroyed||request!=generation)return;status.setText(message!=null&&(message.startsWith("TMDB")||message.startsWith("Catalog"))?message:"Could not reach the catalog. Check the connection and try again.");results.addView(button("Try again",v->load()));});}});
  }
  private void render(StreamingCatalog.Page data,StreamingCatalog.Query q){
    results.removeAllViews();ratingViews.clear();
    String description=q.text.isEmpty()?(q.type==1?"Movies":q.type==2?"TV shows":"Home"):"Results for “"+q.text+"” · all services";
    status.setText(description+" · "+data.items.size()+" titles · page "+q.page+(data.unavailable>0?" · some availability is temporarily unavailable":""));
    if(data.items.isEmpty()){results.addView(text("Nothing here yet",28,INK));results.addView(text("Try another title, fewer interests, or the next page.",17,MUTED));}
    else{
      results.addView(hero(data.items.get(0)));
      ArrayList<JSONObject> movies=new ArrayList<>(),shows=new ArrayList<>();for(JSONObject item:data.items)("tv".equals(item.optString("media_type"))?shows:movies).add(item);
      shelf(q.text.isEmpty()?"Movies to explore":"Movies",movies);shelf(q.text.isEmpty()?"Series to settle into":"TV shows",shows);
      final int request=generation;final StreamingCatalog active=catalog;
      for(JSONObject item:data.items){final String snapshot=item.toString();ratingRequests.add(ratingWorker.submit(()->{
        try{if(request!=generation||destroyed)return;JSONObject rated=active.loadRatings(new JSONObject(snapshot));runOnUiThread(()->{if(destroyed||request!=generation)return;try{item.put("sol_imdb",rated.optString("sol_imdb"));item.put("sol_rt",rated.optString("sol_rt"));item.put("sol_ratings_status",rated.optString("sol_ratings_status"));}catch(JSONException ignored){}ArrayList<TextView> views=ratingViews.get(itemKey(item));if(views!=null)for(TextView view:views){view.setText(ratings(item));View parent=(View)view.getParent();if(parent.isClickable())parent.setContentDescription(StreamingCatalog.title(item)+". "+ratings(item)+". "+availableNames(item));}});}catch(Exception ignored){}
      }));}
    }
    prev.setEnabled(q.page>1);next.setEnabled(q.page<Math.min(500,data.totalPages));
  }
  private void cancelRatings(){for(Future<?> request:ratingRequests)request.cancel(true);ratingRequests.clear();}
  private void welcome(){
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);panel.setPadding(dp(28),dp(24),dp(28),dp(26));panel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff30394d,0xff18181d,0xff101012}));
    panel.addView(text("ALL YOUR STORIES. ONE PLACE.",12,0xffc3cce2));TextView title=text("Your next great watch.",36,INK);title.setTypeface(null,Typeface.BOLD);panel.addView(title);
    panel.addView(text("Find a movie. Discover a series.\nSee where it streams, then open your app.",19,0xffd1d1d8));
    panel.addView(button("Connect free catalog",v->settings()));panel.addView(text("TMDB offers a free personal API key. Optional IMDb / Rotten Tomatoes ratings use an OMDb free key. No purchase or subscription is required by Vista.",13,MUTED));results.addView(panel);
  }
  private View hero(JSONObject item){
    FrameLayout frame=new FrameLayout(this);frame.setBackground(shape(PANEL,0));frame.setClipToOutline(true);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(220));fp.bottomMargin=dp(22);frame.setLayoutParams(fp);
    ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));artwork.show(image,item.optString("backdrop_path"),true);
    View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xf009090b,0xc009090b,0x2209090b}));frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout copy=new LinearLayout(this);copy.setOrientation(1);copy.setPadding(dp(24),dp(18),dp(24),dp(14));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(dp(450),-1,Gravity.LEFT|Gravity.BOTTOM);frame.addView(copy,cp);
    copy.addView(text(item.optString("media_type").equals("tv")?"FEATURED SERIES"+year(item):"FEATURED MOVIE"+year(item),11,0xffd2d2da));TextView title=text(StreamingCatalog.title(item),30,INK);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(title);
    copy.addView(ratingView(item,13));TextView overview=text(item.optString("overview","Explore this title and see where to watch."),14,0xffd1d1d8);overview.setMaxLines(2);overview.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(overview);
    LinearLayout actions=row();actions.addView(button("View details",v->details(item)));actions.addView(button(saved(item)?"✓ In my list":"+ My list",v->{if(!toggleSaved(item))return;((Button)v).setText(saved(item)?"✓ In my list":"+ My list");}));copy.addView(actions);return frame;
  }
  private void shelf(String heading,List<JSONObject> items){
    if(items.isEmpty())return;TextView label=text(heading,22,INK);label.setTypeface(null,Typeface.BOLD);results.addView(label);
    HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);scroll.setPadding(dp(4),dp(8),dp(4),dp(18));LinearLayout track=row();track.setGravity(Gravity.TOP);scroll.addView(track);for(JSONObject item:items)track.addView(card(item));results.addView(scroll);
  }
  private View card(JSONObject item){
    LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setFocusable(true);card.setClickable(true);card.setPadding(dp(4),dp(4),dp(4),dp(8));card.setBackground(focusBackground());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(214),dp(210));cp.setMargins(0,dp(3),dp(14),dp(3));card.setLayoutParams(cp);
    FrameLayout art=new FrameLayout(this);art.setBackground(shape(0xff292930,0));ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);art.addView(image,new FrameLayout.LayoutParams(-1,-1));card.addView(art,new LinearLayout.LayoutParams(-1,dp(116)));
    String path=item.optString("backdrop_path");if(path.isEmpty()||path.equals("null"))path=item.optString("poster_path");artwork.show(image,path,false);
    TextView title=text(StreamingCatalog.title(item)+year(item),16,INK);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(title);card.addView(ratingView(item,11));
    TextView services=text(availableNames(item),11,MUTED);services.setMaxLines(1);services.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(services);
    card.setContentDescription(StreamingCatalog.title(item)+". "+ratings(item)+". "+availableNames(item));card.setOnClickListener(v->details(item));card.setOnFocusChangeListener((v,on)->{v.setElevation(dp(on?10:0));v.animate().scaleX(on?1.035f:1).scaleY(on?1.035f:1).setDuration(120).start();if(on)v.post(()->v.requestRectangleOnScreen(new android.graphics.Rect(0,-dp(8),v.getWidth(),v.getHeight()+dp(10)),false));});return card;
  }
  private String year(JSONObject item){String date=StreamingCatalog.date(item);return date.length()>=4?" · "+date.substring(0,4):"";}
  private String itemKey(JSONObject item){return item.optString("media_type")+"/"+item.optInt("id");}
  private String ratings(JSONObject item){String imdb=item.optString("sol_imdb"),rt=item.optString("sol_rt");return "IMDb "+(imdb.isEmpty()?"—":imdb)+"   ·   RT "+(rt.isEmpty()?"—":rt);}
  private TextView ratingView(JSONObject item,int size){TextView label=text(ratings(item),size,0xffdedee4);ratingViews.computeIfAbsent(itemKey(item),k->new ArrayList<>()).add(label);return label;}
  private View appTile(String name,String pkg,View.OnClickListener click){
    Button tile=button(name,click);tile.setMinWidth(dp(142));tile.setMinHeight(dp(76));
    try{Drawable logo=getPackageManager().getApplicationBanner(pkg);if(logo!=null){logo.setBounds(0,0,dp(104),dp(44));tile.setCompoundDrawables(null,logo,null,null);tile.setTextSize(11);}}catch(Exception ignored){}return tile;
  }
  private String availableNames(JSONObject item){LinkedHashSet<String> names=new LinkedHashSet<>();JSONObject us=item.optJSONObject("sol_availability");if(us!=null)for(String mode:new String[]{"flatrate","free","ads"}){JSONArray a=us.optJSONArray(mode);if(a!=null)for(int j=0;j<a.length();j++){JSONObject p=a.optJSONObject(j);if(p!=null)names.add(displayService(p.optString("provider_name")));}}return names.isEmpty()?(item.optString("sol_availability_status").contains("could not")?"Availability unavailable":"No US streaming offer listed"):android.text.TextUtils.join(" · ",names);}
  private String displayService(String raw){for(int i=0;i<StreamingCatalog.SERVICES.length;i++)if(StreamingCatalog.PROVIDER_NAMES[i].equals(raw))return StreamingCatalog.SERVICES[i];return raw;}
  private void details(JSONObject item){
    LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(22),dp(8),dp(22),dp(8));
    body.addView(text((item.optString("media_type").equals("tv")?"TV series":"Movie")+year(item),14,MUTED));body.addView(ratingView(item,20));body.addView(text(item.optString("sol_ratings_status","Ratings are optional; connect a free OMDb key in Settings."),12,MUTED));
    String overview=item.optString("overview");body.addView(text(overview.isEmpty()?"No synopsis available.":overview,17,INK));
    body.addView(text("US · "+availableNames(item)+(item.optLong("sol_checked")>0?" · checked "+new java.text.SimpleDateFormat("MMM d, h:mm a",Locale.US).format(new Date(item.optLong("sol_checked"))):"")+"\nAvailability supplied by JustWatch via TMDB. Provider apps confirm access and subscription tier. App buttons open/search the selected app; they do not start a verified title deep link.",13,MUTED));
    for(int i=0;i<StreamingCatalog.SERVICES.length;i++){final int service=i;if(providerOffered(item,i)&&installed(StreamingCatalog.PACKAGES[i]))body.addView(button("Open "+StreamingCatalog.SERVICES[i],v->openService(service,StreamingCatalog.title(item))));}
    body.addView(button(saved(item)?"Remove from my list":"Save to my list",v->{if(!toggleSaved(item))return;((Button)v).setText(saved(item)?"Remove from my list":"Save to my list");toast(saved(item)?"Saved on this TV":"Removed from this TV");if(savedMode)showSaved();}));
    String watch=StreamingPolicy.safeWatchUrl(item.optJSONObject("sol_availability")==null?"":item.optJSONObject("sol_availability").optString("link"));if(watch!=null)body.addView(button("Where to watch · TMDB / JustWatch",v->openWeb(watch)));
    ScrollView scroll=new ScrollView(this);scroll.addView(body);FrameLayout viewport=new FrameLayout(this);viewport.addView(scroll,new FrameLayout.LayoutParams(-1,dp(250)));AlertDialog dialog=new AlertDialog.Builder(this).setTitle(StreamingCatalog.title(item)).setView(viewport).setNegativeButton("Close",null).create();dialog.show();styleDialog(dialog);
  }
  private boolean providerOffered(JSONObject item,int service){JSONObject us=item.optJSONObject("sol_availability");if(us==null)return false;for(String mode:new String[]{"flatrate","free","ads"}){JSONArray rows=us.optJSONArray(mode);if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject offer=rows.optJSONObject(i);if(offer!=null&&StreamingCatalog.serviceIndex(offer)==service)return true;}}return false;}
  private JSONArray savedItems(){try{return new JSONArray(prefs.getString("saved","[]"));}catch(Exception e){return new JSONArray();}}
  private boolean saved(JSONObject item){JSONArray a=savedItems();for(int i=0;i<a.length();i++){JSONObject s=a.optJSONObject(i);if(s!=null&&s.optInt("id")==item.optInt("id")&&s.optString("media_type").equals(item.optString("media_type")))return true;}return false;}
  private boolean toggleSaved(JSONObject item){JSONArray old=savedItems(),out=new JSONArray();boolean remove=saved(item);for(int i=0;i<old.length();i++){JSONObject s=old.optJSONObject(i);if(s!=null&&!(s.optInt("id")==item.optInt("id")&&s.optString("media_type").equals(item.optString("media_type"))))out.put(s);}if(!remove){if(out.length()>=100){toast("My list holds up to 100 titles");return false;}out.put(item);}prefs.edit().putString("saved",out.toString()).apply();return true;}
  private void showSaved(){++generation;cancelRatings();if(loading!=null)loading.cancel(true);savedMode=true;results.removeAllViews();ratingViews.clear();prev.setEnabled(false);next.setEnabled(false);JSONArray a=savedItems();status.setText("My list · saved on this TV · availability from the last lookup");ArrayList<JSONObject> movies=new ArrayList<>(),shows=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject item=a.optJSONObject(i);if(item!=null)("tv".equals(item.optString("media_type"))?shows:movies).add(item);}if(a.length()==0){results.addView(text("Make it your own.",30,INK));results.addView(text("Save a movie or series from its details to find it here.",18,MUTED));}shelf("Movies",movies);shelf("TV shows",shows);page.smoothScrollTo(0,0);}
  private void settings(){
    LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(dp(24),dp(8),dp(24),dp(8));
    form.addView(text("Add your TMDB API Read Access Token (recommended) or v3 key. This credential stays in Vista’s private app storage. No Netflix, Hulu, Disney+ or Apple passwords are needed. Non-commercial API access requires a TMDB account.",15,INK));
    EditText key=new EditText(this);key.setSingleLine(true);key.setTextColor(INK);key.setHintTextColor(MUTED);key.setHint("TMDB token or API key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setText(prefs.getString("tmdb",""));form.addView(key);
    form.addView(button("Get a free TMDB key",v->openWeb("https://www.themoviedb.org/settings/api")));
    form.addView(text("Optional ratings · OMDb free key. Vista does not purchase an API plan. Free limits and source coverage apply; some titles have no Rotten Tomatoes score.",14,MUTED));
    EditText ratingKey=new EditText(this);ratingKey.setSingleLine(true);ratingKey.setTextColor(INK);ratingKey.setHintTextColor(MUTED);ratingKey.setHint("OMDb free API key (optional)");ratingKey.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);ratingKey.setText(prefs.getString("omdb",""));form.addView(ratingKey);
    form.addView(button("Get an OMDb free key",v->openWeb("https://www.omdbapi.com/apikey.aspx")));
    ScrollView setupScroll=new ScrollView(this);setupScroll.addView(form);FrameLayout viewport=new FrameLayout(this);viewport.addView(setupScroll,new FrameLayout.LayoutParams(-1,dp(260)));
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Catalog connection").setView(viewport).setPositiveButton("Save",null).setNeutralButton("Disconnect",(d,w)->{prefs.edit().remove("tmdb").remove("omdb").apply();if(catalog!=null)catalog.close();catalog=null;load();}).setNegativeButton("Cancel",null).create();
    dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=key.getText().toString().trim();if(!StreamingPolicy.credentialValid(value)){key.setError("Enter the API Read Access Token or 32-character v3 key");return;}String omdb=ratingKey.getText().toString().trim();if(!omdb.isEmpty()&&!StreamingPolicy.omdbKeyValid(omdb)){ratingKey.setError("Use the free API key supplied by OMDb");return;}prefs.edit().putString("tmdb",value).putString("omdb",omdb).apply();if(catalog!=null)catalog.close();catalog=new StreamingCatalog(value,omdb);dialog.dismiss();changed();}));dialog.show();styleDialog(dialog);
  }
  private void about(){
    LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(24),dp(12),dp(24),dp(8));
    try(java.io.InputStream in=getAssets().open("streaming-tmdb.png")){ImageView logo=new ImageView(this);logo.setImageDrawable(Drawable.createFromStream(in,"TMDB"));logo.setContentDescription("The Movie Database");logo.setScaleType(ImageView.ScaleType.FIT_START);body.addView(logo,new LinearLayout.LayoutParams(dp(120),dp(87)));}catch(Exception ignored){}
    body.addView(text("This product uses the TMDB API but is not endorsed or certified by TMDB. US streaming availability is supplied by JustWatch through TMDB.\n\nDiscovery uses popularity, rating or release date and your selected genres/services. It does not use your private viewing history. Rental/purchase offers are excluded. Catalog completeness and provider tiers vary.\n\nYour keys and saved list stay on this TV. Searches and availability requests go to TMDB over HTTPS; artwork loads from TMDB. Optional IMDb and Rotten Tomatoes ratings come from OMDb using exact IMDb IDs. OMDb requests go over HTTPS with your key. Missing scores are shown as a dash; TMDB scores are never labeled IMDb or Rotten Tomatoes. Free API keys require the provider’s own account setup and are subject to its limits. OMDb data: CC BY-NC 4.0. Vista does not enroll you in a paid service.",15,INK));
    ScrollView scroll=new ScrollView(this);scroll.addView(body);new AlertDialog.Builder(this).setTitle("Catalog sources").setView(scroll).setPositiveButton("TMDB",(d,w)->openWeb("https://www.themoviedb.org")).setNeutralButton("JustWatch",(d,w)->openWeb("https://www.justwatch.com/us")).setNegativeButton("Close",null).show();
  }
  private void styleDialog(AlertDialog dialog){Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(shape(0xff18181c,0xff333338));window.setDimAmount(.8f);window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.76f),WindowManager.LayoutParams.WRAP_CONTENT);}}
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
  private Button button(String s,View.OnClickListener click){Button b=new Button(this);b.setText(s);b.setTextColor(INK);b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(44));b.setMinWidth(dp(64));b.setPadding(dp(14),dp(8),dp(14),dp(8));StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_focused},shape(0xff44444d,ACCENT));states.addState(new int[]{android.R.attr.state_pressed},shape(0xff44444d,ACCENT));states.addState(new int[]{},shape(PANEL,0x00304343));b.setBackground(states);b.setOnClickListener(click);b.setOnFocusChangeListener((v,f)->b.setTextColor(INK));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(3),dp(3),dp(3),dp(3));b.setLayoutParams(p);return b;}
  private StateListDrawable focusBackground(){StateListDrawable states=new StateListDrawable();states.addState(new int[]{android.R.attr.state_focused},shape(0xff303038,ACCENT));states.addState(new int[]{android.R.attr.state_pressed},shape(0xff303038,ACCENT));states.addState(new int[]{},shape(0xff151518,0));return states;}
  private GradientDrawable shape(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(7));d.setStroke(dp(2),stroke);return d;}
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override protected void onDestroy(){destroyed=true;++generation;worker.shutdownNow();ratingWorker.shutdownNow();artwork.close();if(catalog!=null)catalog.close();super.onDestroy();}
}
