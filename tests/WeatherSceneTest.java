package com.griffin.solhome;

public final class WeatherSceneTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static WeatherScene state(String text,double wind,double direction,boolean stale){return WeatherScene.from(text,wind,direction,1000000,stale,"NWS observation",1000000);}
    public static void main(String[] args){
        check(state("Clear",2,270,false).clouds<state("Mostly Cloudy",2,270,false).clouds,"Cloud cover follows report");
        check(state("Overcast",2,270,false).clouds>.9f,"Overcast sky");
        check(state("Heavy Rain",20,270,false).rain>.8f,"Reported heavy rain");
        check(state("Chance Rain Showers",2,270,false).rain==0,"A chance is not current rain");
        check(state("Slight Chance Snow",2,270,false).snow==0,"A chance is not current snow");
        check(state("Light Snow",2,270,false).snow>0,"Reported snow");
        check(state("Scattered Rain Showers",2,270,false).rain==0,"Scattered forecast is not current rain");
        check(state("Rain in Vicinity",2,270,false).rain==0,"Vicinity precipitation is not at the scene");
        check(state("Cloudy and Breezy",2,270,false).clouds>.9f,"Qualified cloudy report");
        check(state("Heavy Rain",20,270,true).rain==0,"Stale precipitation suppressed");
        check(state("Heavy Rain",20,270,true).wind<=.2f,"Stale gusts subdued");
        check(state("Clear",20,270,false).wind<=1,"Bounded gust intensity");
        check(state("Clear",2,270,false).drift>0&&state("Clear",2,90,false).drift<0,"Meteorological wind direction");
        check(state("Fog",2,0,false).haze>.5f,"Fog affects distance visibility");
        check(WeatherScene.from(null,Double.NaN,Double.NaN,0,false,null,1000).stale,"Unavailable stays neutral");
        check(WeatherScene.from("Rain",2,270,1,false,"observation",20000000).rain==0,"Old timestamps independently suppress rain");
        check(WeatherScene.from("Rain",2,270,2000000,false,"observation",1000000).rain==0,"Future timestamps rejected");
        System.out.println("Weather scene condition checks passed");
    }
}
