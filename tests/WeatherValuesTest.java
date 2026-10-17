package com.griffin.solhome;
import java.time.Instant;
public final class WeatherValuesTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long at(String s){return Instant.parse(s).toEpochMilli();}
    public static void main(String[] args){
        check(WeatherValues.fahrenheit(0,"wmoUnit:degC")==32,"Celsius observation converts to Fahrenheit");check(WeatherValues.fahrenheit(47,"wmoUnit:degF")==47,"Fahrenheit is not double converted");check(Double.isNaN(WeatherValues.fahrenheit(5,"unknown")),"Unknown units stay unavailable");check(Math.abs(WeatherValues.metersPerSecond(36,"wmoUnit:km_h-1")-10)<.001,"NWS km/h wind conversion");check(Double.isNaN(WeatherValues.metersPerSecond(-1,"wmoUnit:km_h-1")),"Invalid wind not displayed");check(WeatherValues.bearing(360).equals("N")&&WeatherValues.bearing(225).equals("SW"),"Meteorological bearing labels");
        check(WinterCountdown.days(at("2026-10-17T16:00:00Z"))==65,"Cincinnati calendar countdown");check(WinterCountdown.days(at("2026-12-21T02:00:00Z"))==1,"UTC midnight does not prematurely change local countdown");check(WinterCountdown.days(at("2026-12-21T06:00:00Z"))==0,"Winter date reached locally");check(WinterCountdown.days(at("2027-01-10T18:00:00Z"))==0,"January is already winter");
        System.out.println("Observation unit conversions and Cincinnati winter countdown checks passed.");
    }
}
