package com.griffin.solhome;

import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Reading mode uses publisher-provided briefs, never an invented headline summary. */
public final class NewsPanel {
    public static void show(Activity activity){new AlertDialog.Builder(activity).setTitle("News briefs").setItems(new String[]{"AI research · MIT News","World news · BBC News"},(d,n)->list(activity,n==0?"ai":"world")).setNegativeButton("Close",null).show();}
    private static void list(Activity activity,String key){
        WidgetRepository repo=WidgetRepository.get(activity);repo.refresh();JSONArray items=repo.data(key).optJSONArray("items");
        if(items==null||items.length()==0){new AlertDialog.Builder(activity).setTitle("News briefs").setMessage("This feed has not loaded yet. Please try again after reconnecting.").setPositiveButton("OK",null).show();return;}
        String[] titles=new String[items.length()];for(int i=0;i<titles.length;i++)titles[i]=items.optJSONObject(i).optString("title");
        new AlertDialog.Builder(activity).setTitle(key.equals("ai")?"AI research · MIT News":"World news · BBC News").setItems(titles,(d,n)->read(activity,key,items.optJSONObject(n))).setNegativeButton("Back",(d,n)->show(activity)).show();
    }
    private static void read(Activity activity,String key,JSONObject item){
        SimpleDateFormat date=new SimpleDateFormat("EEE, MMM d · h:mm a",Locale.US);date.setTimeZone(TimeZone.getTimeZone(CincinnatiSky.ZONE_ID));
        String source=key.equals("ai")?"MIT News":"BBC News";String summary=item.optString("detail");if(summary.isEmpty())summary="The publisher did not provide a brief. Open the source for context.";
        String message=summary+"\n\nPublisher brief · "+source+(item.optLong("date")>0?"\nPublished "+date.format(new Date(item.optLong("date"))):"");
        new AlertDialog.Builder(activity).setTitle(item.optString("title")).setMessage(message).setPositiveButton("Open source",(d,n)->{String link=item.optString("link");if(link.startsWith("https://"))try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)));}catch(Exception e){android.widget.Toast.makeText(activity,"No browser available on this TV",0).show();}}).setNegativeButton("Back",(d,n)->list(activity,key)).show();
    }
}
