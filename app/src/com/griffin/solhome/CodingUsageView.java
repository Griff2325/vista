package com.griffin.solhome;

import android.app.*;
import android.graphics.*;
import java.util.*;

/** Shared native Home / ambient card, drawn only with the surrounding widget view. */
final class CodingUsageView {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final Typeface face=Typeface.create("sans-serif",0),medium=Typeface.create("sans-serif-medium",0);
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean bold){paint.setTypeface(bold?medium:face);paint.setTextSize(size);paint.setColor(color);c.drawText(value,x,y,paint);}
    private void bar(Canvas c,float x,float y,float width,float height,double used,boolean saved){
        rect.set(x,y,x+width,y+height);paint.setColor(0xff344744);c.drawRoundRect(rect,height/2,height/2,paint);
        if(used<=0)return;rect.right=x+width*(float)(used/100);paint.setColor(saved?0xff8d9b90:used>=90?0xffdfaa83:0xffbfd0ad);c.drawRoundRect(rect,height/2,height/2,paint);
    }
    void home(Canvas c,CodingUsage data,long now,int selected){
        homeProvider(c,"Codex",data.codex,now,96,selected==15);
        homeProvider(c,"Claude Code",data.claude,now,744,selected==16);
    }
    private void homeProvider(Canvas c,String name,CodingUsage.Provider p,long now,float x,boolean focused){
        rect.set(x,958,x+604,1073);paint.setColor(0xff16272a);c.drawRoundRect(rect,12,12,paint);
        if(focused){paint.setColor(0xffcfb684);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);c.drawRoundRect(rect,12,12,paint);paint.setStyle(Paint.Style.FILL);}
        text(c,name,x+24,985,22,0xfff2f0e7,true);
        text(c,CodingUsage.tokens(p,now,false),x+195,984,15,0xffbdc8bd,false);
        if(!p.usable(now)){text(c,p.unavailable(now),x+24,1022,20,0xffb0bfba,false);return;}
        int count=0;for(CodingUsage.Window w:p.windows)if(w.reset>now&&count<2)count++;
        float width=(556-(count-1)*28)/count;
        int column=0;for(CodingUsage.Window w:p.windows){if(w.reset<=now||column>=2)continue;float start=x+24+column*(width+28);boolean saved=p.stale(now);
            text(c,w.label(),start,1016,19,0xffc7d0c3,false);
            String percent=CodingUsage.percent(w);paint.setTextSize(20);paint.setTypeface(face);text(c,percent,start+width-paint.measureText(percent),1016,20,0xffedf0df,false);
            bar(c,start,1027,width,5,w.used,saved);
            text(c,(saved?"Saved · ":"")+CodingUsage.reset(w,now),start,1055,15,0xffbdc8bd,false);column++;}
    }
    static void show(Activity a){CodingUsage data=WidgetRepository.get(a).codingUsage();long now=System.currentTimeMillis();StringBuilder message=new StringBuilder();
        detail(message,"Codex",data.codex,now);detail(message,"Claude Code",data.claude,now);
        message.append("Tokens are today's recorded activity on this Mac, including cached input. They are separate from account allowance percentages. Refreshes every 5 minutes while the Mac is awake and TV reachable. Saved readings may be out of date.");
        new AlertDialog.Builder(a).setTitle("Coding usage").setMessage(message.toString()).setPositiveButton("Close",null).show();
    }
    private static void detail(StringBuilder text,String name,CodingUsage.Provider p,long now){text.append(name).append("\n");if(!p.usable(now))text.append(p.unavailable(now)).append("\n");else for(CodingUsage.Window w:p.windows)if(w.reset>now)text.append(w.label()).append(" · ").append(CodingUsage.percent(w)).append(" · ").append(CodingUsage.reset(w,now)).append("\n");text.append(CodingUsage.tokens(p,now,false)).append("\n").append(CodingUsage.age(p,now)).append("\n\n");}
}
