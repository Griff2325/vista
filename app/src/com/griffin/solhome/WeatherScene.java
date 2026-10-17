package com.griffin.solhome;

import java.util.Locale;

/** Bounded visual interpretation of the shared Cincinnati observation/forecast cache. */
public final class WeatherScene {
    public final float clouds, haze, rain, snow, wind, drift;
    public final boolean stale;
    public final String source;
    private WeatherScene(float clouds,float haze,float rain,float snow,float wind,float drift,boolean stale,String source) {
        this.clouds=clouds;this.haze=haze;this.rain=rain;this.snow=snow;this.wind=wind;this.drift=drift;this.stale=stale;this.source=source;
    }
    public static WeatherScene neutral() { return new WeatherScene(.35f,0,0,0,.14f,1,true,"unavailable"); }
    public static WeatherScene from(String summary,double windMps,double direction,long observedAt,boolean stale,String source,long now) {
        if(summary==null||summary.trim().isEmpty())return neutral();
        String text=summary.toLowerCase(Locale.US);
        boolean old=stale||(observedAt>0&&(now-observedAt>3*3600000L||observedAt-now>300000L));
        float clouds=.5f,haze=0,rain=0,snow=0;
        if(has(text,"cloudy"))clouds=.98f;
        if(has(text,"clear","sunny","fair"))clouds=.12f;
        if(has(text,"mostly sunny","mostly clear"))clouds=.25f;
        if(has(text,"partly cloudy","partly sunny","few clouds"))clouds=.43f;
        if(has(text,"mostly cloudy","broken clouds"))clouds=.78f;
        if(has(text,"overcast")||text.equals("cloudy"))clouds=.98f;
        if(has(text,"fog","mist","haze","smoke")){haze=has(text,"fog")?.62f:.3f;clouds=Math.max(clouds,.72f);}
        boolean wet=has(text,"rain","drizzle","shower","thunderstorm");
        boolean frozen=has(text,"snow","flurr","sleet","ice pellet");
        boolean uncertain=has(text,"chance","possible","slight","isolated","scattered","vicinity","likely");
        if(wet||frozen)clouds=Math.max(clouds,has(text,"thunderstorm","heavy")?.98f:.86f);
        if(!uncertain&&!old){
            float intensity=has(text,"heavy","thunderstorm")?.85f:has(text,"light","drizzle","flurr")?.3f:.55f;
            if(wet)rain=intensity;
            if(frozen){snow=intensity;rain=wet?intensity*.35f:0;}
        }
        // A forecast cache can be useful visually, but stale precipitation is never replayed as live.
        if(old)haze=Math.min(haze,.2f);
        float wind=Double.isFinite(windMps)?(float)Math.max(.06,Math.min(1,windMps/12)):.14f;
        if(old)wind=Math.min(wind,.2f);
        // Meteorological bearing is where wind comes FROM. Project east/west onto the scene.
        float drift=Double.isFinite(direction)?(float)-Math.sin(Math.toRadians(direction)):1;
        if(Math.abs(drift)<.18f)drift=drift<0?-.18f:.18f;
        return new WeatherScene(clouds,haze,rain,snow,wind,drift,old,source==null?"cached weather":source);
    }
    private static boolean has(String value,String... terms){for(String term:terms)if(value.contains(term))return true;return false;}
}
