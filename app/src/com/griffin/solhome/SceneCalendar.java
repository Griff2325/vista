package com.griffin.solhome;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Offline Cincinnati calendar. Scene boundaries are fixed dates, not an ephemeris. */
public final class SceneCalendar {
    public enum Season { SPRING("Spring"), SUMMER("Summer"), AUTUMN("Autumn"), WINTER("Winter");
        public final String label; Season(String label){this.label=label;}
    }
    public enum Occasion {
        NONE("None"), HALLOWEEN("Halloween"), HARVEST("Thanksgiving"), CHRISTMAS("Christmas"),
        NEW_YEAR("New Year"), VALENTINE("Valentine’s Day"), ST_PATRICK("St. Patrick’s Day"),
        EARTH_DAY("Earth Day"), INDEPENDENCE("July Fourth"), CELEBRATION("Celebration");
        public final String label; Occasion(String label){this.label=label;}
    }
    public static final class Entry {
        public final String title;
        public final LocalDate date;
        public final Occasion occasion;
        public Entry(String title,LocalDate date,Occasion occasion){this.title=title;this.date=date;this.occasion=occasion;}
    }
    public static LocalDate date(long epoch){return Instant.ofEpochMilli(epoch).atZone(ZoneId.of(CincinnatiSky.ZONE_ID)).toLocalDate();}
    public static Season season(LocalDate date){
        int key=date.getMonthValue()*100+date.getDayOfMonth();
        return key>=1221||key<320?Season.WINTER:key<621?Season.SPRING:key<922?Season.SUMMER:Season.AUTUMN;
    }
    public static LocalDate thanksgiving(int year){return LocalDate.of(year,11,1).with(TemporalAdjusters.dayOfWeekInMonth(4,DayOfWeek.THURSDAY));}
    public static Occasion occasion(LocalDate date){
        int month=date.getMonthValue(),day=date.getDayOfMonth();
        if(month==12&&day==31||month==1&&day<=2)return Occasion.NEW_YEAR;
        if(month==12)return Occasion.CHRISTMAS;
        if(month==10)return Occasion.HALLOWEEN;
        if(month==2&&day>=12&&day<=14)return Occasion.VALENTINE;
        if(month==3&&day>=16&&day<=17)return Occasion.ST_PATRICK;
        if(month==4&&day>=20&&day<=22)return Occasion.EARTH_DAY;
        if(month==7&&day>=2&&day<=4)return Occasion.INDEPENDENCE;
        LocalDate thanksgiving=thanksgiving(date.getYear());
        if(!date.isBefore(thanksgiving.minusDays(4))&&!date.isAfter(thanksgiving.plusDays(3)))return Occasion.HARVEST;
        return Occasion.NONE;
    }
    public static List<Entry> year(int year){
        List<Entry> entries=new ArrayList<>();
        entries.add(new Entry("New Year’s Day",LocalDate.of(year,1,1),Occasion.NEW_YEAR));
        entries.add(new Entry("Valentine’s Day",LocalDate.of(year,2,14),Occasion.VALENTINE));
        entries.add(new Entry("St. Patrick’s Day",LocalDate.of(year,3,17),Occasion.ST_PATRICK));
        entries.add(new Entry("Spring scene begins",LocalDate.of(year,3,20),Occasion.NONE));
        entries.add(new Entry("Earth Day",LocalDate.of(year,4,22),Occasion.EARTH_DAY));
        entries.add(new Entry("Summer scene begins",LocalDate.of(year,6,21),Occasion.NONE));
        entries.add(new Entry("July Fourth",LocalDate.of(year,7,4),Occasion.INDEPENDENCE));
        entries.add(new Entry("Autumn scene begins",LocalDate.of(year,9,22),Occasion.NONE));
        entries.add(new Entry("Halloween",LocalDate.of(year,10,31),Occasion.HALLOWEEN));
        entries.add(new Entry("Thanksgiving",thanksgiving(year),Occasion.HARVEST));
        entries.add(new Entry("Winter scene begins",LocalDate.of(year,12,21),Occasion.NONE));
        entries.add(new Entry("Christmas",LocalDate.of(year,12,25),Occasion.CHRISTMAS));
        return entries;
    }
    /** Leap-day events retain their actual date rather than silently moving to February 28. */
    public static LocalDate nextAnnual(LocalDate today,int month,int day){
        MonthDay recurring=MonthDay.of(month,day);
        for(int year=today.getYear();year<=today.getYear()+8;year++){
            if(!recurring.isValidYear(year))continue;
            LocalDate date=recurring.atYear(year);if(!date.isBefore(today))return date;
        }
        throw new IllegalArgumentException("No upcoming annual date");
    }
    public static List<Entry> upcoming(LocalDate today,List<Entry> personal){
        List<Entry> entries=new ArrayList<>();
        for(int year=today.getYear();year<=today.getYear()+1;year++)for(Entry entry:year(year))if(!entry.date.isBefore(today))entries.add(entry);
        entries.addAll(personal);
        Collections.sort(entries,(a,b)->a.date.compareTo(b.date));
        return entries;
    }
}
