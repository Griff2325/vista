package com.griffin.solhome;

import android.graphics.*;

/** Two balanced provider cards beneath the ambient news, events and snow widgets. */
final class ScreensaverUsageView {
    private static final float TOP=914,BOTTOM=1037,CARD_WIDTH=846,PADDING=24;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final Typeface face=Typeface.create("sans-serif",0),medium=Typeface.create("sans-serif-medium",0);

    void draw(Canvas canvas,CodingUsage usage,long now){
        provider(canvas,"Codex",usage.codex,now,96,0xffbfd0ad);
        provider(canvas,"Claude Code",usage.claude,now,978,0xffd6c1a3);
    }

    private void provider(Canvas canvas,String name,CodingUsage.Provider provider,long now,float left,int accent){
        rect.set(left,TOP,left+CARD_WIDTH,BOTTOM);paint.setColor(0xbd0d1e23);
        canvas.drawRoundRect(rect,16,16,paint);
        float start=left+PADDING,end=left+CARD_WIDTH-PADDING;
        text(canvas,name,start,TOP+30,22,0xfff2f0e7,true);
        rightText(canvas,CodingUsage.tokens(provider,now,false),end,TOP+29,17,0xffc2ccc1);

        if(!provider.usable(now)){
            text(canvas,provider.unavailable(now),start,TOP+66,20,0xffc0ccc6,false);
            text(canvas,provider.at==0?"Sync from your coding Mac":CodingUsage.age(provider,now),start,TOP+100,16,0xffadbbb5,false);
            return;
        }

        int count=0;
        for(CodingUsage.Window window:provider.windows)if(window.reset>now&&count<2)count++;
        float gap=28,columnWidth=(end-start-gap*(count-1))/count;
        boolean saved=provider.stale(now);int column=0;
        for(CodingUsage.Window window:provider.windows){
            if(window.reset<=now)continue;
            if(column>=count)break;
            float x=start+column*(columnWidth+gap);
            text(canvas,window.label(),x,TOP+61,18,0xffc7d0c3,false);
            rightText(canvas,CodingUsage.percent(window),x+columnWidth,TOP+61,20,0xffedf0df);
            bar(canvas,x,TOP+72,columnWidth,window.used,saved,accent);
            text(canvas,(saved?"Saved · ":"")+CodingUsage.reset(window,now),x,TOP+101,16,0xffbdc8bd,false);
            column++;
        }
    }

    private void bar(Canvas canvas,float x,float y,float width,double used,boolean saved,int accent){
        rect.set(x,y,x+width,y+6);paint.setColor(0xff344744);canvas.drawRoundRect(rect,3,3,paint);
        if(used<=0)return;
        rect.right=x+width*(float)(used/100);paint.setColor(saved?0xff8d9b90:used>=90?0xffdfaa83:accent);
        canvas.drawRoundRect(rect,3,3,paint);
    }

    private void text(Canvas canvas,String value,float x,float y,float size,int color,boolean bold){
        paint.setTypeface(bold?medium:face);paint.setTextSize(size);paint.setColor(color);canvas.drawText(value,x,y,paint);
    }

    private void rightText(Canvas canvas,String value,float right,float y,float size,int color){
        paint.setTypeface(face);paint.setTextSize(size);paint.setColor(color);canvas.drawText(value,right-paint.measureText(value),y,paint);
    }
}
