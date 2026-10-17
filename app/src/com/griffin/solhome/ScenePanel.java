package com.griffin.solhome;

import android.app.*;
import android.content.SharedPreferences;
import android.widget.*;
import android.view.KeyEvent;
import android.text.InputFilter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.json.*;

/** Remote-friendly season/holiday previews and a small private recurring calendar. */
final class ScenePanel {
    static void show(Activity activity,Runnable changed){
        SharedPreferences prefs=SceneSettings.preferences(activity);
        SceneSettings.Theme theme=SceneSettings.theme(activity,System.currentTimeMillis());
        String[] items={"Season · "+(prefs.getString("season","AUTO").equals("AUTO")?"Automatic · ":"Preview · ")+theme.season.label,"Holiday accents · "+(prefs.getBoolean("holidays",true)?"On":"Off"),"Preview a holiday or celebration","Calendar & annual events","Restore automatic calendar","About the scene calendar"};
        choices(activity,changed,items);
    }
    private static void choices(Activity a,Runnable changed,String[] items){
        new AlertDialog.Builder(a).setTitle("Seasons & occasions").setItems(items,(d,n)->{
            if(n==0)seasons(a,changed);
            else if(n==1){SharedPreferences p=SceneSettings.preferences(a);p.edit().putBoolean("holidays",!p.getBoolean("holidays",true)).putString("occasion","AUTO").apply();changed.run();show(a,changed);}
            else if(n==2)occasions(a,changed);
            else if(n==3)calendar(a,changed);
            else if(n==4){SceneSettings.preferences(a).edit().putString("season","AUTO").putString("occasion","AUTO").apply();changed.run();}
            else new AlertDialog.Builder(a).setTitle("Cincinnati scene calendar").setMessage("Scene seasons begin March 20, June 21, September 22 and December 21. These are fixed visual dates. Holiday accents can be disabled. Previews remain selected until you restore Automatic; the clock and Cincinnati weather always stay live. Snow falls only when the weather source reports it. Personal annual events stay on this TV and trigger a small celebration on their date.").setPositiveButton("OK",null).show();
        }).setNegativeButton("Close",null).show();
    }
    private static void seasons(Activity a,Runnable changed){
        String[] labels={"Automatic · Cincinnati calendar","Spring · blossoms & butterflies","Summer · green forest & fireflies","Autumn · golden leaves","Winter · snow & snowboard"};
        String current=SceneSettings.preferences(a).getString("season","AUTO");int checked=0;
        for(int i=0;i<SceneCalendar.Season.values().length;i++)if(SceneCalendar.Season.values()[i].name().equals(current))checked=i+1;
        new AlertDialog.Builder(a).setTitle("Scene season").setSingleChoiceItems(labels,checked,(d,n)->{
            SceneSettings.preferences(a).edit().putString("season",n==0?"AUTO":SceneCalendar.Season.values()[n-1].name()).putString("occasion",n==0?"AUTO":"NONE").apply();changed.run();d.dismiss();
        }).setNegativeButton("Cancel",null).show();
    }
    private static void occasions(Activity a,Runnable changed){
        SceneCalendar.Occasion[] values=SceneCalendar.Occasion.values();String[] labels=new String[values.length+1];labels[0]="Automatic · restore live calendar";
        for(int i=0;i<values.length;i++)labels[i+1]=values[i]==SceneCalendar.Occasion.NONE?"No holiday accents":values[i].label;
        new AlertDialog.Builder(a).setTitle("Preview occasion").setItems(labels,(d,n)->{
            SharedPreferences.Editor edit=SceneSettings.preferences(a).edit();
            if(n==0)edit.putString("season","AUTO").putString("occasion","AUTO");
            else{
                SceneCalendar.Occasion o=values[n-1];edit.putString("occasion",o.name());
                SceneCalendar.Season s=o==SceneCalendar.Occasion.CHRISTMAS||o==SceneCalendar.Occasion.NEW_YEAR||o==SceneCalendar.Occasion.VALENTINE?SceneCalendar.Season.WINTER:o==SceneCalendar.Occasion.ST_PATRICK||o==SceneCalendar.Occasion.EARTH_DAY?SceneCalendar.Season.SPRING:o==SceneCalendar.Occasion.INDEPENDENCE?SceneCalendar.Season.SUMMER:o==SceneCalendar.Occasion.HALLOWEEN||o==SceneCalendar.Occasion.HARVEST?SceneCalendar.Season.AUTUMN:null;
                if(s!=null)edit.putString("season",s.name());
            }
            edit.apply();changed.run();
        }).setNegativeButton("Cancel",null).show();
    }
    private static void calendar(Activity a,Runnable changed){
        LocalDate today=SceneCalendar.date(System.currentTimeMillis());List<SceneCalendar.Entry> entries=SceneCalendar.upcoming(today,SceneSettings.personal(a,today));
        StringBuilder text=new StringBuilder();DateTimeFormatter date=DateTimeFormatter.ofPattern("EEE, MMM d",Locale.US);
        for(int i=0;i<Math.min(9,entries.size());i++){SceneCalendar.Entry entry=entries.get(i);long days=ChronoUnit.DAYS.between(today,entry.date);text.append(entry.date.format(date)).append(days==0?" · Today":" · in "+days+"d").append("\n").append(entry.title).append("\n\n");}
        new AlertDialog.Builder(a).setTitle("Upcoming · Cincinnati").setMessage(text.toString().trim()).setPositiveButton("Add annual event",(d,w)->add(a,changed)).setNeutralButton("Manage my events",(d,w)->manage(a,changed)).setNegativeButton("Close",null).show();
    }
    private static void add(Activity a,Runnable changed){
        JSONArray data=SceneSettings.personalData(a);if(data.length()>=20){Toast.makeText(a,"Up to 20 annual events. Remove one to add another.",Toast.LENGTH_LONG).show();return;}
        LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(32,12,32,12);
        EditText title=new EditText(a),date=new EditText(a);title.setHint("Birthday, anniversary, or event");date.setHint("Month-day · MM-DD");title.setSingleLine();date.setSingleLine();form.addView(title);form.addView(date);
        title.setSelectAllOnFocus(true);date.setSelectAllOnFocus(true);
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});date.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Annual event · saved on this TV").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        // TV EditText can consume arrows as cursor movement. Make vertical navigation explicit.
        title.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN){date.requestFocus();return true;}return false;});
        date.setOnKeyListener((v,key,event)->{if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_UP){title.requestFocus();return true;}if(key==KeyEvent.KEYCODE_DPAD_DOWN){dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();return true;}return false;});
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
            String name=title.getText().toString().trim().replaceAll("[\\p{Cntrl}]","");if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException();
            MonthDay day=MonthDay.parse("--"+date.getText().toString().trim());
            data.put(new JSONObject().put("title",name).put("month",day.getMonthValue()).put("day",day.getDayOfMonth()));
            SceneSettings.preferences(a).edit().putString("events",data.toString()).apply();changed.run();dialog.dismiss();calendar(a,changed);
        }catch(Exception error){date.setError("Enter a name (up to 40 characters) and a valid MM-DD date");}}));dialog.show();
    }
    private static void manage(Activity a,Runnable changed){
        JSONArray data=SceneSettings.personalData(a);String[] labels=new String[data.length()];
        for(int i=0;i<labels.length;i++){JSONObject event=data.optJSONObject(i);labels[i]=event==null?"Invalid event":String.format(Locale.US,"%02d-%02d · %s",event.optInt("month"),event.optInt("day"),event.optString("title"));}
        if(labels.length==0){new AlertDialog.Builder(a).setTitle("My annual events").setMessage("No personal events yet. Birthdays and anniversaries stay on this TV. February 29 events occur in leap years.").setPositiveButton("Add",(d,w)->add(a,changed)).setNegativeButton("Close",null).show();return;}
        new AlertDialog.Builder(a).setTitle("Select an event to remove").setItems(labels,(d,n)->new AlertDialog.Builder(a).setTitle("Remove "+labels[n]+"?").setPositiveButton("Remove",(confirm,w)->{data.remove(n);SceneSettings.preferences(a).edit().putString("events",data.toString()).apply();changed.run();manage(a,changed);}).setNegativeButton("Cancel",null).show()).setNegativeButton("Close",null).show();
    }
}
