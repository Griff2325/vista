package com.griffin.solhome;

import android.graphics.*;
import org.json.JSONObject;
import java.util.Locale;

/** Small native forecast panel; glyphs are vector primitives, with no image downloads. */
final class HomeWeatherView {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final Path path=new Path();
    private void text(Canvas c,String s,float x,float y,float size,int color){p.setColor(color);p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(size);c.drawText(s,x,y,p);}
    void draw(Canvas c,JSONObject w,long now,boolean focus){
        rect.set(1420,958,1832,1073);p.setColor(0xff16272a);c.drawRoundRect(rect,12,12,p);
        if(focus){p.setStyle(Paint.Style.STROKE);p.setColor(0xffcfb684);p.setStrokeWidth(1.5f);c.drawRoundRect(rect,12,12,p);p.setStyle(Paint.Style.FILL);}
        boolean observed=w!=null&&w.has("currentTemperature")&&now-w.optLong("observedAt")<3*3600000L;
        String summary=w==null?"Forecast unavailable":w.optString(observed?"condition":"summary");
        text(c,"CINCINNATI",1442,981,15,0xffb6c4ba);
        text(c,w==null?"—":w.optInt(observed?"currentTemperature":"temperature")+"°",1440,1032,44,0xfff2f0e7);
        glyph(c,summary,1555,1012,CincinnatiSky.at(now).elevation<0);
        p.setTextSize(18);int chars=p.breakText(summary,true,193,null);if(chars<summary.length())summary=summary.substring(0,Math.max(0,chars-1))+"…";
        text(c,summary,1603,1009,18,0xffe2e6d7);
        text(c,observed?"Current conditions":"NWS forecast",1603,1032,13,0xffadbbb5);
        String high=w!=null&&w.has("nextHigh")?w.optInt("nextHigh")+"°":"—",low=w!=null&&w.has("nextLow")?w.optInt("nextLow")+"°":"—";
        text(c,"Next high "+high+"  /  low "+low+(w!=null&&w.optBoolean("stale")?" · saved":""),1442,1059,16,0xffc7d0ba);
    }
    private void glyph(Canvas c,String summary,float x,float y,boolean night){
        String s=summary.toLowerCase(Locale.US);boolean snow=s.contains("snow")||s.contains("flurr")||s.contains("sleet"),rain=s.contains("rain")||s.contains("shower")||s.contains("drizzle"),storm=s.contains("thunder"),fog=s.contains("fog")||s.contains("mist")||s.contains("haze");
        boolean cloud=s.contains("cloud")||s.contains("overcast")||snow||rain||storm||fog;
        c.save();c.translate(x,y);p.setStyle(Paint.Style.FILL);
        if(summary.isEmpty()||s.contains("unavailable")){text(c,"—",-12,6,26,0xffadbbb5);c.restore();return;}
        if(!cloud||s.contains("partly")){
            p.setColor(night?0xffc3d9dd:0xffddc08b);c.drawCircle(cloud?-13:0,cloud?-10:0,13,p);
            if(night){p.setColor(0xff16272a);c.drawCircle(cloud?-7:6,cloud?-15:-5,12,p);}
            else{p.setStrokeWidth(2);for(int i=0;i<8;i++){double a=i*Math.PI/4;float ox=cloud?-13:0,oy=cloud?-10:0;c.drawLine(ox+(float)Math.cos(a)*18,oy+(float)Math.sin(a)*18,ox+(float)Math.cos(a)*23,oy+(float)Math.sin(a)*23,p);}}
        }
        if(cloud){p.setColor(0xffc1cfc8);c.drawCircle(-14,0,10,p);c.drawCircle(0,-8,14,p);c.drawCircle(16,0,10,p);c.drawRect(-14,-2,16,10,p);}
        if(rain||snow){p.setColor(0xff9cc6d1);p.setStrokeWidth(2.2f);for(int i=-1;i<=1;i++){float dx=i*13;if(snow){c.drawLine(dx-3,18,dx+3,24,p);c.drawLine(dx+3,18,dx-3,24,p);}else c.drawLine(dx+2,17,dx-2,25,p);}}
        if(storm){p.setColor(0xffddc08b);path.reset();path.moveTo(2,10);path.lineTo(-7,23);path.lineTo(1,23);path.lineTo(-3,33);path.lineTo(12,17);path.lineTo(4,17);path.close();c.drawPath(path,p);}
        if(fog){p.setColor(0xffadbbb5);p.setStrokeWidth(2);c.drawLine(-23,18,23,18,p);c.drawLine(-16,25,16,25,p);}
        p.setStyle(Paint.Style.FILL);c.restore();
    }
}
