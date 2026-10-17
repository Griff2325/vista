package com.griffin.solhome;

import android.content.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Local preferences shared by Home and Dream; no calendar/account permissions. */
final class SceneSettings {
    static SharedPreferences preferences(Context context){return context.getApplicationContext().getSharedPreferences("vista_scene",Context.MODE_PRIVATE);}
    static final class Theme {
        final SceneCalendar.Season season;
        final SceneCalendar.Occasion occasion;
        final String event;
        final boolean manual;
        private final String label;
        Theme(SceneCalendar.Season s,SceneCalendar.Occasion o,String e,boolean manual){season=s;occasion=o;event=e;this.manual=manual;label=(manual?"Preview · ":"")+season.label+(event.isEmpty()?"":" · "+event);}
        String label(){return label;}
    }
    static Theme theme(Context context,long epoch){
        SharedPreferences prefs=preferences(context);LocalDate today=SceneCalendar.date(epoch);
        String selected=prefs.getString("season","AUTO"),accent=prefs.getString("occasion","AUTO");
        SceneCalendar.Season season=SceneCalendar.season(today);
        try{season=SceneCalendar.Season.valueOf(selected);}catch(IllegalArgumentException ignored){}
        SceneCalendar.Occasion occasion=prefs.getBoolean("holidays",true)?SceneCalendar.occasion(today):SceneCalendar.Occasion.NONE;
        String event=occasion==SceneCalendar.Occasion.NONE?"":occasion.label;
        if(prefs.getBoolean("holidays",true))for(SceneCalendar.Entry entry:personal(context,today))if(entry.date.equals(today)){occasion=SceneCalendar.Occasion.CELEBRATION;event=entry.title;break;}
        if(!accent.equals("AUTO"))try{occasion=SceneCalendar.Occasion.valueOf(accent);event=occasion==SceneCalendar.Occasion.NONE?"":occasion.label;}catch(IllegalArgumentException ignored){}
        return new Theme(season,occasion,event,!selected.equals("AUTO")||!accent.equals("AUTO"));
    }
    static JSONArray personalData(Context context){try{return new JSONArray(preferences(context).getString("events","[]"));}catch(JSONException error){return new JSONArray();}}
    static List<SceneCalendar.Entry> personal(Context context,LocalDate today){
        JSONArray data=personalData(context);List<SceneCalendar.Entry> result=new ArrayList<>();
        for(int i=0;i<Math.min(20,data.length());i++)try{JSONObject event=data.getJSONObject(i);String title=event.getString("title");if(title.isEmpty()||title.length()>40)continue;result.add(new SceneCalendar.Entry(title,SceneCalendar.nextAnnual(today,event.getInt("month"),event.getInt("day")),SceneCalendar.Occasion.CELEBRATION));}catch(Exception ignored){}
        return result;
    }
}
