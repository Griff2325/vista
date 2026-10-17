package com.griffin.solhome;

/** Converts warm foliage/reflections once during asset loading; never runs per frame. */
final class SeasonPalette {
    static int terrain(int pixel,int row,SceneCalendar.Season season){
        int mapped=map(pixel,season);
        if(season==SceneCalendar.Season.WINTER||season==SceneCalendar.Season.AUTUMN||row>=185)return mapped;
        // Sunlit mountain rock should not become green. Feather into the wooded hills.
        float amount=Math.max(0,(row-145)/40f);
        int r=Math.round(((pixel>>16)&255)*(1-amount)+((mapped>>16)&255)*amount);
        int g=Math.round(((pixel>>8)&255)*(1-amount)+((mapped>>8)&255)*amount);
        int b=Math.round((pixel&255)*(1-amount)+(mapped&255)*amount);
        return (pixel&0xff000000)|(r<<16)|(g<<8)|b;
    }
    static int map(int pixel,SceneCalendar.Season season){
        if(season==SceneCalendar.Season.AUTUMN||(pixel>>>24)==0)return pixel;
        int r=(pixel>>16)&255,g=(pixel>>8)&255,b=pixel&255;
        int high=Math.max(r,Math.max(g,b)),low=Math.min(r,Math.min(g,b));
        // Preserve blue water, evergreen shade, gray rocks and dark bark.
        if(r<70||r<g*.94f||r<b*1.22f||g<b*1.03f||high-low<24)return pixel;
        float light=r*.30f+g*.59f+b*.11f;
        if(season==SceneCalendar.Season.SPRING){r=(int)(35+light*.60f);g=(int)(56+light*.87f);b=(int)(29+light*.48f);}
        else if(season==SceneCalendar.Season.SUMMER){r=(int)(20+light*.43f);g=(int)(38+light*.76f);b=(int)(24+light*.45f);}
        else{r=(int)(105+light*.59f);g=(int)(122+light*.55f);b=(int)(139+light*.50f);}
        return (pixel&0xff000000)|(Math.min(255,r)<<16)|(Math.min(255,g)<<8)|Math.min(255,b);
    }
}
