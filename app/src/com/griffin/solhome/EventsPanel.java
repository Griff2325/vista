package com.griffin.solhome;

import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.*;
import java.util.*;

/** One reading list for this week's shows, markets, games and food discoveries. */
public final class EventsPanel {
    public static void show(Activity activity){
        WidgetRepository repo=WidgetRepository.get(activity);repo.refresh();List<JSONObject> items=repo.weeklyEvents();
        if(items.isEmpty()){new AlertDialog.Builder(activity).setTitle("Cincinnati · next 7 days").setMessage("No current picks have loaded. Sources refresh automatically; try again after reconnecting.").setPositiveButton("Interests",(d,n)->interests(activity)).setNegativeButton("Close",null).show();return;}
        String[] labels=new String[items.size()];for(int i=0;i<labels.length;i++){JSONObject item=items.get(i);labels[i]=item.optString("title")+"\n"+repo.eventWhen(item)+" · "+item.optString("venue");}
        new AlertDialog.Builder(activity).setTitle("Cincinnati · next 7 days").setItems(labels,(d,n)->read(activity,items.get(n))).setPositiveButton("Interests",(d,n)->interests(activity)).setNegativeButton("Close",null).show();
    }
    public static void interests(Activity activity){WidgetRepository repo=WidgetRepository.get(activity);Set<String> selected=new HashSet<>(repo.eventInterests());boolean[] checks=new boolean[EventPolicy.IDS.length];for(int i=0;i<checks.length;i++)checks[i]=selected.contains(EventPolicy.IDS[i]);
        new AlertDialog.Builder(activity).setTitle("This week · interests").setMultiChoiceItems(EventPolicy.LABELS,checks,(d,n,on)->{if(on)selected.add(EventPolicy.IDS[n]);else selected.remove(EventPolicy.IDS[n]);}).setPositiveButton("Save",(d,n)->{repo.saveEventInterests(selected);show(activity);}).setNegativeButton("Cancel",null).show();}
    private static void read(Activity activity,JSONObject item){WidgetRepository repo=WidgetRepository.get(activity);String message=repo.eventWhen(item)+"\n"+item.optString("venue")+"\n\n"+item.optString("detail")+"\n\n"+item.optString("source")+" · Check source for changes, tickets and availability.";
        new AlertDialog.Builder(activity).setTitle(item.optString("title")).setMessage(message).setPositiveButton("Open source",(d,n)->{String link=item.optString("link");if(link.startsWith("https://"))try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(link)));}catch(Exception e){android.widget.Toast.makeText(activity,"No browser available",0).show();}}).setNegativeButton("Back",(d,n)->show(activity)).show();
    }
}
