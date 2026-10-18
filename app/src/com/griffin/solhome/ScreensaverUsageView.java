package com.griffin.solhome;

import android.graphics.*;

/** Equal-width allowance cards aligned beneath the right-hand news and events row. */
final class ScreensaverUsageView {
    private static final float TOP=914,BOTTOM=1037,LEFT=684,RIGHT=1824,GAP=36,PADDING=24;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final Typeface face=Typeface.create("sans-serif",0),medium=Typeface.create("sans-serif-medium",0);

    void draw(Canvas canvas,CodingUsage usage,long now){
        int count=windows(usage.codex,now)+windows(usage.claude,now);
        float width=(RIGHT-LEFT-GAP*(count-1))/count;
        int next=provider(canvas,"Codex",usage.codex,now,0,width,0xffbfd0ad);
        provider(canvas,"Claude Code",usage.claude,now,next,width,0xffd6c1a3);
    }

    private int windows(CodingUsage.Provider provider,long now){
        if(!provider.usable(now))return 1;
        int count=0;for(CodingUsage.Window window:provider.windows)if(window.reset>now&&count<2)count++;
        return Math.max(1,count);
    }

    private int provider(Canvas canvas,String name,CodingUsage.Provider provider,long now,int column,float width,int accent){
        if(!provider.usable(now)){card(canvas,name,provider,null,now,LEFT+column*(width+GAP),width,accent);return column+1;}
        int used=0;for(CodingUsage.Window window:provider.windows){
            if(window.reset<=now)continue;if(used>=2)break;
            card(canvas,name,provider,window,now,LEFT+column*(width+GAP),width,accent);column++;used++;
        }
        return column;
    }

    private void card(Canvas canvas,String name,CodingUsage.Provider provider,CodingUsage.Window window,long now,float left,float width,int accent){
        rect.set(left,TOP,left+width,BOTTOM);paint.setColor(0xbd0d1e23);canvas.drawRoundRect(rect,16,16,paint);
        float start=left+PADDING,barWidth=width-2*PADDING;
        fitText(canvas,name+(window==null?"":" · "+window.label()),start,TOP+27,19,0xfff2f0e7,true,barWidth);
        fitText(canvas,CodingUsage.tokens(provider,now,false),start,TOP+47,14,0xffc2ccc1,false,barWidth);
        if(window==null){
            fitText(canvas,provider.unavailable(now),start,TOP+78,17,0xffc0ccc6,false,barWidth);
            fitText(canvas,provider.at==0?"Sync from your coding Mac":CodingUsage.age(provider,now),start,TOP+109,14,0xffadbbb5,false,barWidth);return;
        }
        boolean saved=provider.stale(now);
        text(canvas,CodingUsage.percent(window),start,TOP+77,25,0xffedf0df,false);
        bar(canvas,start,TOP+85,barWidth,window.used,saved,accent);
        fitText(canvas,(saved?"Saved · ":"")+CodingUsage.reset(window,now),start,TOP+109,14,0xffbdc8bd,false,barWidth);
    }

    private void bar(Canvas canvas,float x,float y,float width,double used,boolean saved,int accent){
        rect.set(x,y,x+width,y+6);paint.setColor(0xff344744);canvas.drawRoundRect(rect,3,3,paint);
        if(used<=0)return;
        rect.right=x+width*(float)(used/100);paint.setColor(saved?0xff8d9b90:used>=90?0xffdfaa83:accent);
        canvas.drawRoundRect(rect,3,3,paint);
    }

    private void fitText(Canvas canvas,String value,float x,float y,float size,int color,boolean bold,float width){
        paint.setTypeface(bold?medium:face);paint.setTextSize(size);
        if(paint.measureText(value)>width){int count=paint.breakText(value,true,Math.max(0,width-paint.measureText("…")),null);value=value.substring(0,count)+"…";}
        text(canvas,value,x,y,size,color,bold);
    }

    private void text(Canvas canvas,String value,float x,float y,float size,int color,boolean bold){
        paint.setTypeface(bold?medium:face);paint.setTextSize(size);paint.setColor(color);canvas.drawText(value,x,y,paint);
    }

}
