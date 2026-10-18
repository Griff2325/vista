package com.griffin.solhome;

import android.graphics.*;

/** Tiny native wildlife, drawn on dry bank/sky coordinates. No textures or frame objects. */
final class WorldOutside {
    private final Paint paint=new Paint();
    void sky(Canvas c,double seconds,float daylight,boolean calm,boolean winter){
        double age=WorldMotion.phase(seconds,113,23);
        if(daylight<.2f||!calm||age>23)return;
        paint.setColor(0xff334854);paint.setAlpha((int)(200*WorldMotion.fade(age,23)));paint.setStrokeWidth(.7f);
        for(int i=0;i<3;i++){
            float x=485-(float)age*23+i*7,y=91+i%2*4+(float)Math.sin(age*.45+i)*2;
            float wing=(float)Math.sin(age*5.5+i*.7)*1.8f;
            c.drawLine(x-2,y+wing,x,y,paint);c.drawLine(x,y,x+2,y+wing,paint);
        }
    }
    void foreground(Canvas c,double seconds,SceneCalendar.Season season,float daylight,boolean calm){
        double age=WorldMotion.phase(seconds,229,13);
        if(daylight>.17f&&calm&&age<34){
            float x=WorldMotion.foxX(age),y=314+(x-113)*.17f;
            int alpha=(int)(255*WorldMotion.fade(age,34));
            boolean walking=age<12||age>21;float step=walking?(float)Math.sin(age*5)*1.1f:0;
            c.save();c.translate(x,y);
            color(0xff283738,alpha/3);c.drawRect(-9,0,9,1,paint);
            color(0xffa55f39,alpha);c.drawRect(-7,-7,5,-2,paint);c.drawRect(-11,-6,-6,-3,paint);c.drawRect(-14,-8,-10,-5,paint);
            color(0xffe7d7b4,alpha);c.drawRect(-15,-8,-12,-6,paint);c.drawRect(4,-6,9,-3,paint);
            color(0xffc78048,alpha);c.drawRect(3,-11,8,-5,paint);c.drawRect(3,-13,5,-10,paint);c.drawRect(7,-13,9,-10,paint);
            color(0xff302f32,alpha);c.drawRect(8,-8,9,-7,paint);c.drawRect(9,-5,10,-4,paint);
            c.drawRect(-5,-3,-3,step,paint);c.drawRect(2,-3,4,-step,paint);
            if(!walking){color(0xffe7d7b4,alpha);c.drawRect(5,-6,7,-3,paint);}
            c.restore();
        }
    }
    void distant(Canvas c,double seconds,boolean winter,float daylight,boolean calm){
        // Brief traversal on the snowy ridge above the far shore, never across the lake.
        double age=WorldMotion.phase(seconds,167,41);
        if(!winter||daylight<.2f||!calm||age>16)return;
        float x=263+(float)age*3.8f,y=232+(float)age*.08f;
        int alpha=(int)(235*WorldMotion.fade(age,16));
        c.save();c.translate(x,y);
        color(0xffdc9857,alpha);c.drawRect(-1,-7,1,-5,paint);
        color(0xff397e8f,alpha);c.drawRect(-2,-5,1,-2,paint);c.drawRect(-3,-4,-1,-3,paint);
        color(0xff253c4a,alpha);c.drawRect(-2,-2,-1,0,paint);c.drawRect(1,-2,2,0,paint);
        color(0xffe0ba80,alpha);c.drawRect(-4,0,4,1,paint);c.restore();
    }
    private void color(int color,int alpha){paint.setColor(color);paint.setAlpha(alpha);}
}
