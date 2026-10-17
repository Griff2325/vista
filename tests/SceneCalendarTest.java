package com.griffin.solhome;

import java.time.*;
import java.util.*;

public final class SceneCalendarTest {
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static LocalDate date(String value){return LocalDate.parse(value);}
    private static void season(String when,SceneCalendar.Season expected){require(SceneCalendar.season(date(when))==expected,"Season at "+when);}
    private static void occasion(String when,SceneCalendar.Occasion expected){require(SceneCalendar.occasion(date(when))==expected,"Occasion at "+when);}
    public static void main(String[] args){
        season("2026-03-19",SceneCalendar.Season.WINTER);season("2026-03-20",SceneCalendar.Season.SPRING);
        season("2026-06-20",SceneCalendar.Season.SPRING);season("2026-06-21",SceneCalendar.Season.SUMMER);
        season("2026-09-21",SceneCalendar.Season.SUMMER);season("2026-09-22",SceneCalendar.Season.AUTUMN);
        season("2026-12-20",SceneCalendar.Season.AUTUMN);season("2026-12-21",SceneCalendar.Season.WINTER);
        occasion("2026-10-01",SceneCalendar.Occasion.HALLOWEEN);occasion("2026-10-31",SceneCalendar.Occasion.HALLOWEEN);occasion("2026-11-01",SceneCalendar.Occasion.NONE);
        occasion("2026-12-25",SceneCalendar.Occasion.CHRISTMAS);occasion("2026-12-31",SceneCalendar.Occasion.NEW_YEAR);occasion("2027-01-02",SceneCalendar.Occasion.NEW_YEAR);occasion("2027-01-03",SceneCalendar.Occasion.NONE);
        occasion("2026-02-14",SceneCalendar.Occasion.VALENTINE);occasion("2026-03-17",SceneCalendar.Occasion.ST_PATRICK);occasion("2026-04-22",SceneCalendar.Occasion.EARTH_DAY);occasion("2026-07-04",SceneCalendar.Occasion.INDEPENDENCE);
        for(int year=2020;year<2040;year++){
            LocalDate thanksgiving=SceneCalendar.thanksgiving(year);
            require(thanksgiving.getDayOfWeek()==DayOfWeek.THURSDAY&&thanksgiving.getDayOfMonth()>=22&&thanksgiving.getDayOfMonth()<=28,"Fourth Thursday");
            require(SceneCalendar.occasion(thanksgiving)==SceneCalendar.Occasion.HARVEST,"Harvest on Thanksgiving");
            require(SceneCalendar.occasion(thanksgiving.minusDays(5))==SceneCalendar.Occasion.NONE,"Harvest begins four days before");
        }
        require(SceneCalendar.nextAnnual(date("2026-10-17"),10,17).equals(date("2026-10-17")),"Today's event retained");
        require(SceneCalendar.nextAnnual(date("2026-10-18"),10,17).equals(date("2027-10-17")),"Annual event rolls forward");
        require(SceneCalendar.nextAnnual(date("2099-03-01"),2,29).equals(date("2104-02-29")),"Leap-day event skips non-leap century");
        boolean invalid=false;try{SceneCalendar.nextAnnual(date("2026-01-01"),2,30);}catch(DateTimeException error){invalid=true;}require(invalid,"Invalid custom date rejected");
        List<SceneCalendar.Entry> upcoming=SceneCalendar.upcoming(date("2026-12-30"),Collections.singletonList(new SceneCalendar.Entry("Annual event",date("2027-01-03"),SceneCalendar.Occasion.CELEBRATION)));
        require(upcoming.get(0).date.equals(date("2027-01-01")),"Calendar crosses New Year");
        for(int i=1;i<upcoming.size();i++)require(!upcoming.get(i).date.isBefore(upcoming.get(i-1).date),"Upcoming dates sorted");
        TimeZone saved=TimeZone.getDefault();try{
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            require(SceneCalendar.date(Instant.parse("2026-11-01T03:59:00Z").toEpochMilli()).equals(date("2026-10-31")),"Cincinnati before Halloween midnight");
            require(SceneCalendar.date(Instant.parse("2026-11-01T04:00:00Z").toEpochMilli()).equals(date("2026-11-01")),"Cincinnati after Halloween midnight");
            require(SceneCalendar.date(Instant.parse("2026-11-01T05:59:00Z").toEpochMilli()).equals(SceneCalendar.date(Instant.parse("2026-11-01T06:01:00Z").toEpochMilli())),"DST does not change calendar date");
        }finally{TimeZone.setDefault(saved);}
        int autumn=0xffdf8b32,blue=0xff315b88,bark=0xff362b24,transparent=0x00335566;
        Set<Integer> palettes=new HashSet<>();
        for(SceneCalendar.Season s:SceneCalendar.Season.values()){
            palettes.add(SeasonPalette.map(autumn,s));require(SeasonPalette.map(blue,s)==blue,"Blue lake preserved");require(SeasonPalette.map(bark,s)==bark,"Dark bark preserved");require(SeasonPalette.map(transparent,s)==transparent,"Transparency preserved");
        }
        require(palettes.size()==4,"Four distinct seasonal foliage palettes");
        require(SeasonPalette.map(autumn,SceneCalendar.Season.AUTUMN)==autumn,"Approved autumn art preserved");
        require(SeasonPalette.terrain(autumn,100,SceneCalendar.Season.SPRING)==autumn,"Sunlit mountain rock stays warm");
        require(SeasonPalette.terrain(autumn,220,SceneCalendar.Season.SPRING)==SeasonPalette.map(autumn,SceneCalendar.Season.SPRING),"Wooded foreground gets the full palette");
        System.out.println("Season boundaries, occasion windows, Thanksgiving, annual events, leap years, Cincinnati/DST and palette preservation passed.");
    }
}
