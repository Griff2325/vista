package com.griffin.solhome;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Credential-free quota observations. Missing, saved and reset-passed readings stay distinct. */
final class CodingUsage {
    static final long FRESH_MS=15*60000L,MAX_AGE_MS=24*3600000L;
    static final class Window {
        final double used;final int minutes;final long reset;
        Window(double used,int minutes,long reset){this.used=used;this.minutes=minutes;this.reset=reset;}
        String label(){return minutes==300?"5h":minutes==10080?"Weekly":minutes>=40320&&minutes<=44640?"Monthly":minutes%60==0?minutes/60+"h":minutes+"m";}
    }
    static final class Provider {
        final long at;final boolean saved;final List<Window> windows;final Tokens tokens;
        Provider(long at,boolean saved,List<Window> windows){this(at,saved,windows,null);}
        Provider(long at,boolean saved,List<Window> windows,Tokens tokens){this.at=at;this.saved=saved;this.windows=Collections.unmodifiableList(new ArrayList<>(windows));this.tokens=tokens;}
        Window current(long now){for(Window w:windows)if(w.reset>now)return w;return null;}
        boolean stale(long now){return saved||now-at>FRESH_MS;}
        boolean usable(long now){return at>0&&at<=now+300000&&now-at<=MAX_AGE_MS&&current(now)!=null;}
        String unavailable(long now){return at==0?"Waiting for sync":windows.isEmpty()?"No limits reported":current(now)==null?"Reset passed · refresh needed":"Update needed";}
    }
    static final class Tokens {
        final String day;final long count,at;
        Tokens(String day,long count,long at){this.day=day;this.count=count;this.at=at;}
        boolean usable(long now){return count>=0&&count<=1000000000000L&&at>0&&at<=now+300000&&now-at<=MAX_AGE_MS&&day.equals(Instant.ofEpochMilli(now).atZone(ZoneId.of(CincinnatiSky.ZONE_ID)).toLocalDate().toString());}
    }
    final Provider codex,claude;
    CodingUsage(Provider codex,Provider claude){this.codex=codex;this.claude=claude;}
    static Provider empty(){return new Provider(0,true,Collections.emptyList());}
    static boolean valid(double used,int minutes,long reset,long now){return Double.isFinite(used)&&used>=0&&used<=100&&minutes>0&&minutes<=527040&&reset>0&&reset<=now+400L*86400000;}
    static String percent(Window w){return String.format(Locale.US,"%.0f%% used",w.used);}
    static String tokenCount(long n){return n>=1000000000?String.format(Locale.US,"%.1fB",n/1e9):n>=1000000?String.format(Locale.US,"%.1fM",n/1e6):n>=1000?String.format(Locale.US,"%.1fK",n/1e3):Long.toString(n);}
    static String tokens(Provider p,long now,boolean shortLabel){return p.tokens!=null&&p.tokens.usable(now)?"Mac today · "+tokenCount(p.tokens.count)+(shortLabel?" tok":" tokens")+(now-p.tokens.at>FRESH_MS?" · saved":""):"Tokens unavailable";}
    static String reset(Window w,long now){String pattern=w.reset-now<86400000?"h:mm a":"EEE h:mm a";return "Resets "+Instant.ofEpochMilli(w.reset).atZone(ZoneId.of(CincinnatiSky.ZONE_ID)).format(DateTimeFormatter.ofPattern(pattern,Locale.US));}
    static String age(Provider p,long now){if(p.at==0)return "No reading yet";long minutes=Math.max(0,(now-p.at)/60000);return minutes<1?"Updated just now":minutes<60?"Updated "+minutes+"m ago":"Saved "+minutes/60+"h ago";}
}
