package com.griffin.solhome;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Cincinnati calendar boundaries and transparent, local interest matching. */
public final class EventPolicy {
    public static final String[] IDS={"music","markets","food","sports","arts","outdoors"};
    public static final String[] LABELS={"Live music & shows","Markets & unusual events","Food & new restaurants","Sports","Arts & comedy","Outdoors & community"};
    private static final ZoneId ZONE=ZoneId.of(CincinnatiSky.ZONE_ID);
    public static long weekEnd(long now){return Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate().plusDays(7).atStartOfDay(ZONE).toInstant().toEpochMilli();}
    public static boolean upcoming(long start,long end,long now){return start>0&&start<weekEnd(now)&&(end>0?end>now:start>=Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant().toEpochMilli());}
    public static String category(String text){String s=text.toLowerCase(Locale.US);
        if(s.matches("(?s).*(bengals|reds|fc cincinnati|soccer|football|baseball|prematch|watch party).*$"))return "sports";
        if(s.matches("(?s).*(flea|market|vintage|maker|festival|fest |fright|halloween|trivia|bingo).*$"))return "markets";
        if(s.matches("(?s).*(music|concert|jazz|blues|band|singer|orchestra|porch set|tour|acoustic).*$"))return "music";
        if(s.matches("(?s).*(food|restaurant|taco|dining|chef|brunch|tasting).*$"))return "food";
        if(s.matches("(?s).*(art|comedy|theatre|theater|film|writing).*$"))return "arts";
        return "outdoors";
    }
    public static boolean routine(String title){String s=title.toLowerCase(Locale.US);return s.contains("porch hours")||s.contains("bar is open")||s.contains("happy hour");}
    public static String when(long start,long end,boolean dateOnly,long now){
        ZonedDateTime at=Instant.ofEpochMilli(start).atZone(ZONE);LocalDate today=Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate();
        String day=at.toLocalDate().equals(today)?"Today":at.toLocalDate().equals(today.plusDays(1))?"Tomorrow":at.format(DateTimeFormatter.ofPattern("EEE, MMM d",Locale.US));
        if(dateOnly)return day+" · time not listed";
        String result=day+" · "+at.format(DateTimeFormatter.ofPattern("h:mm a",Locale.US));
        if(end>start&&Instant.ofEpochMilli(end).atZone(ZONE).toLocalDate().equals(at.toLocalDate()))result+="–"+Instant.ofEpochMilli(end).atZone(ZONE).format(DateTimeFormatter.ofPattern("h:mm a",Locale.US));
        return result+" ET";
    }
}
