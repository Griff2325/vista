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
    void compact(Canvas c,CodingUsage data,long now,boolean focused){
        if(focused){paint.setColor(0xffcfb684);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);rect.set(950,961,1420,1070);c.drawRoundRect(rect,10,10,paint);paint.setStyle(Paint.Style.FILL);}
        text(c,"CODING LIMITS",970,981,15,0xffb6c4ba,true);
        small(c,"Codex",data.codex,now,1007);small(c,"Claude",data.claude,now,1047);
    }
    private void small(Canvas c,String name,CodingUsage.Provider p,long now,float y){
        text(c,name,970,y,18,0xfff2f0e7,false);CodingUsage.Window w=p.current(now);
        text(c,CodingUsage.tokens(p,now,true),1220,y+16,12,0xffadbbb5,false);
        if(!p.usable(now)){text(c,p.unavailable(now),1050,y,17,0xffb0bfba,false);return;}
        boolean saved=p.stale(now);text(c,w.label()+" · "+CodingUsage.percent(w)+(saved?" · saved":""),1050,y,17,0xffe2e6d7,false);
        bar(c,1310,y-11,85,5,w.used,saved);
        text(c,CodingUsage.reset(w,now),1050,y+16,13,0xffadbbb5,false);
    }
    void ambient(Canvas c,CodingUsage data,long now){
        rect.set(96,914,1824,1037);paint.setColor(0xbd0d1e23);c.drawRoundRect(rect,16,16,paint);
        text(c,"CODING LIMITS",120,944,17,0xffc7d0ba,true);
        large(c,"Codex",data.codex,now,330);large(c,"Claude Code",data.claude,now,1050);
    }
    private void large(Canvas c,String name,CodingUsage.Provider p,long now,float start){
        text(c,name,start,944,21,0xfff2f0e7,true);
        text(c,CodingUsage.tokens(p,now,false),start+170,944,17,0xffbdc8bd,false);
        if(!p.usable(now)){text(c,p.unavailable(now),start,978,20,0xffc0ccc6,false);text(c,p.at==0?"Sync from your coding Mac":CodingUsage.age(p,now),start,1013,16,0xffadbbb5,false);return;}
        int column=0;for(CodingUsage.Window w:p.windows){if(w.reset<=now||column>=2)continue;float x=start+column*320;boolean saved=p.stale(now);
            text(c,w.label()+" · "+CodingUsage.percent(w),x,976,19,0xffedf0df,false);bar(c,x,986,275,5,w.used,saved);
            text(c,(saved?"Saved · ":"")+CodingUsage.reset(w,now),x,1013,15,0xffbdc8bd,false);column++;}
    }
    static void show(Activity a){CodingUsage data=WidgetRepository.get(a).codingUsage();long now=System.currentTimeMillis();StringBuilder message=new StringBuilder();
        detail(message,"Codex",data.codex,now);detail(message,"Claude Code",data.claude,now);
        message.append("Tokens are today's recorded activity on this Mac, including cached input. They are separate from account allowance percentages. Refreshes every 5 minutes while the Mac is awake and TV reachable. Saved readings may be out of date.");
        new AlertDialog.Builder(a).setTitle("Coding usage").setMessage(message.toString()).setPositiveButton("Close",null).show();
    }
    private static void detail(StringBuilder text,String name,CodingUsage.Provider p,long now){text.append(name).append("\n");if(!p.usable(now))text.append(p.unavailable(now)).append("\n");else for(CodingUsage.Window w:p.windows)if(w.reset>now)text.append(w.label()).append(" · ").append(CodingUsage.percent(w)).append(" · ").append(CodingUsage.reset(w,now)).append("\n");text.append(CodingUsage.tokens(p,now,false)).append("\n").append(CodingUsage.age(p,now)).append("\n\n");}
}
