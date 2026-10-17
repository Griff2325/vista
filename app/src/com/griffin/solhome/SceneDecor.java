package com.griffin.solhome;

import android.graphics.*;

/** Sparse native accents; fixed loop bounds, reusable geometry, no image/video frames. */
final class SceneDecor {
    private final Paint paint=new Paint();
    private final RectF bounds=new RectF();
    private final Path lightCord=new Path();
    private static final int[] BULBS={0xffe8b967,0xff77c9bc,0xffdb887b,0xffc1cf88};
    private void color(int value){int alpha=paint.getAlpha();paint.setColor(value);paint.setAlpha(alpha);}
    SceneDecor(){lightCord.moveTo(8,130);lightCord.quadTo(65,168,132,128);}

    void drawSky(Canvas canvas,float time,SceneSettings.Theme theme,float daylight,float immersion,boolean insectWeather){
        if(insectWeather&&(theme.season==SceneCalendar.Season.SPRING||theme.season==SceneCalendar.Season.SUMMER||theme.occasion==SceneCalendar.Occasion.EARTH_DAY))
            if(daylight>.3f)butterflies(canvas,time,daylight);
        if(theme.occasion==SceneCalendar.Occasion.VALENTINE){
            for(int i=0;i<3;i++){
                float life=(time/28+i*.33f)%1;
                paint.setAlpha((int)(Math.sin(life*Math.PI)*125));
                canvas.save();canvas.translate(225+i*48+(float)Math.sin(time*.13+i)*9,241-life*55);canvas.scale(.8f,.8f);
                heart(canvas,0xffdc9aab);canvas.restore();
            }
            paint.setAlpha(255);
        }
        if(theme.occasion==SceneCalendar.Occasion.NEW_YEAR||theme.occasion==SceneCalendar.Occasion.INDEPENDENCE||theme.occasion==SceneCalendar.Occasion.CELEBRATION)
            if(daylight<.65f||theme.manual)fireworks(canvas,time,daylight,immersion);
    }

    void drawForeground(Canvas canvas,float time,SceneSettings.Theme theme,float daylight,float immersion){
        if(theme.season==SceneCalendar.Season.WINTER)snowboard(canvas,daylight);
        if(theme.season==SceneCalendar.Season.SPRING)blossoms(canvas,daylight);
        if(theme.occasion==SceneCalendar.Occasion.CHRISTMAS)christmas(canvas,time,daylight);
        if(theme.occasion==SceneCalendar.Occasion.HARVEST)harvest(canvas,daylight);
        if(theme.occasion==SceneCalendar.Occasion.ST_PATRICK){
            color(0xff6e9c73);paint.setAlpha((int)(95+daylight*145));
            for(int i=0;i<3;i++){canvas.save();canvas.translate(118+i*10,314+i%2*3);clover(canvas);canvas.restore();}paint.setAlpha(255);
        }
    }

    private void snowboard(Canvas canvas,float daylight){
        // Rest on the near bank, well outside the lake mask. Bindings face the room.
        paint.setAlpha((int)(110+145*daylight));color(0xffdbe7e9);
        bounds.set(143,326,177,330);canvas.drawOval(bounds,paint);
        canvas.save();canvas.translate(158,307);canvas.rotate(-17);
        color(0xff152c3a);canvas.drawRect(-5,-19,5,19,paint);canvas.drawRect(-3,-22,3,22,paint);canvas.drawRect(-4,-21,4,21,paint);
        color(0xff5fb7c3);canvas.drawRect(-4,-18,4,18,paint);canvas.drawRect(-2,-21,2,21,paint);canvas.drawRect(-3,-20,3,20,paint);
        color(0xffe4a163);canvas.drawRect(-4,-15,4,-9,paint);canvas.drawRect(-3,12,3,18,paint);
        color(0xffe6e4cf);canvas.drawRect(-2,-4,0,7,paint);canvas.drawRect(0,0,2,10,paint);
        color(0xff152c3a);canvas.drawRect(-4,-7,4,-3,paint);canvas.drawRect(-4,5,4,9,paint);
        color(0xffd1d8cf);canvas.drawRect(-2,-8,2,-6,paint);canvas.drawRect(-2,4,2,6,paint);
        canvas.restore();paint.setAlpha(255);
    }

    private void blossoms(Canvas canvas,float daylight){
        paint.setAlpha((int)(100+155*daylight));
        for(int i=0;i<12;i++){
            float x=38+(i*29)%135,y=293+(i*13)%37;
            color(0xff6d9264);canvas.drawRect(x,y,x+1,y+4,paint);
            color(i%3==0?0xfff5e2aa:0xffe8b6cb);canvas.drawRect(x-1,y-1,x+2,y+1,paint);canvas.drawRect(x,y-2,x+1,y+2,paint);
        }
        paint.setAlpha(255);
    }

    private void butterflies(Canvas canvas,float time,float daylight){
        paint.setAlpha((int)(daylight*195));
        for(int i=0;i<2;i++){
            float x=180+i*64+(float)Math.sin(time*.12+i*2)*22,y=238+(float)Math.sin(time*.21+i)*9;
            float wing=.6f+(float)Math.abs(Math.sin(time*3.6+i))*1.8f;
            color(i==0?0xfff1cc91:0xffeabacb);canvas.drawRect(x-wing-1,y-1,x-.4f,y+1.3f,paint);canvas.drawRect(x+.4f,y-1,x+wing+1,y+1.3f,paint);
            color(0xff354c46);canvas.drawRect(x-.35f,y-1,x+.35f,y+1.5f,paint);
        }
        paint.setAlpha(255);
    }

    private void christmas(Canvas canvas,float time,float daylight){
        color(0xff536a59);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.45f);canvas.drawPath(lightCord,paint);paint.setStyle(Paint.Style.FILL);
        for(int i=0;i<12;i++){
            float t=(i+.5f)/12,x=8+124*t,y=130*(1-t)*(1-t)+2*168*t*(1-t)+128*t*t;
            float glow=.78f+.22f*(float)Math.sin(time*.6+i*.8);
            color(BULBS[i%4]);paint.setAlpha((int)(glow*(125+85*(1-daylight))));
            canvas.drawRect(x-.85f,y,x+.85f,y+2.4f,paint);
            if(daylight<.5f){paint.setAlpha((int)(glow*(1-daylight)*27));bounds.set(x-3,y-2,x+3,y+5);canvas.drawOval(bounds,paint);}
        }
        paint.setAlpha((int)(110+145*daylight));
        for(int i=0;i<3;i++){
            float x=109+i*11,y=319-i%2*4;color(BULBS[(i+1)%4]);canvas.drawRect(x,y,x+9,y+8,paint);
            color(0xffe9d5a2);canvas.drawRect(x+4,y,x+5,y+8,paint);canvas.drawRect(x,y+3,x+9,y+4,paint);
        }
        paint.setAlpha(255);
    }

    private void harvest(Canvas canvas,float daylight){
        paint.setAlpha((int)(100+155*daylight));color(0xff79583e);canvas.drawRect(109,319,137,327,paint);
        color(0xffaf8251);canvas.drawRect(108,318,138,320,paint);canvas.drawRect(112,322,134,323,paint);canvas.drawRect(112,325,134,326,paint);
        for(int i=0;i<3;i++){
            float x=112+i*8,y=312+i%2*2;color(i==1?0xffcfab60:0xffce8146);canvas.drawRect(x,y,x+7,y+6,paint);canvas.drawRect(x+1,y-1,x+6,y+7,paint);
            color(0xff68784d);canvas.drawRect(x+3,y-3,x+4,y,paint);
        }
        paint.setAlpha(255);
    }

    private void heart(Canvas canvas,int color){
        color(color);canvas.drawRect(-3,-2,-1,0,paint);canvas.drawRect(1,-2,3,0,paint);canvas.drawRect(-4,-1,4,1,paint);canvas.drawRect(-3,1,3,2,paint);canvas.drawRect(-2,2,2,3,paint);canvas.drawRect(-1,3,1,4,paint);
    }
    private void clover(Canvas canvas){
        canvas.drawRect(-3,-3,-1,-1,paint);canvas.drawRect(0,-4,2,-2,paint);canvas.drawRect(2,-1,4,1,paint);canvas.drawRect(-1,-2,2,1,paint);canvas.drawRect(0,0,1,4,paint);
    }

    private void fireworks(Canvas canvas,float time,float daylight,float immersion){
        // Two staggered bursts per 26-second cycle; quiet between them, never full-screen flashes.
        for(int burst=0;burst<2;burst++){
            float age=(time+burst*9)%26;if(age>4)continue;
            float grow=1-(float)Math.exp(-age*.9),radius=5+grow*23;
            float fade=(float)(Math.sin(Math.min(1,age/.5)*Math.PI/2)*Math.pow(1-age/4,1.5));
            float cx=265+burst*95,cy=76+immersion*35+burst*23;
            color(BULBS[burst]);paint.setAlpha((int)(fade*(195-daylight*45)));paint.setStrokeWidth(.7f);
            for(int ray=0;ray<12;ray++){
                double angle=ray*Math.PI/6;float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                canvas.drawLine(cx+dx*radius*.72f,cy+dy*radius*.72f+age*2,cx+dx*radius,cy+dy*radius+age*2,paint);
            }
        }
        paint.setAlpha(255);
    }
}
