package com.griffin.solhome;
import java.time.Instant;
public final class EventPolicyTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long at(String s){return Instant.parse(s).toEpochMilli();}
    public static void main(String[] args){
        long now=at("2026-10-17T18:00:00Z");
        check(EventPolicy.category("The City Flea · shop local makers").equals("markets"),"Market interests");
        check(EventPolicy.category("FC Cincinnati PreMatch at the Park").equals("sports"),"Sports events");
        check(EventPolicy.category("Jazz at the Park").equals("music"),"Concert interests");
        check(EventPolicy.routine("Fall Porch Hours")&&!EventPolicy.routine("The Porch Set: live music"),"Exclude ordinary opening hours, preserve events");
        check(EventPolicy.upcoming(at("2026-10-17T15:00:00Z"),at("2026-10-17T20:00:00Z"),now),"Ongoing event retained");
        check(!EventPolicy.upcoming(at("2026-10-17T15:00:00Z"),at("2026-10-17T16:00:00Z"),now),"Finished event removed");
        check(!EventPolicy.upcoming(at("2026-10-24T18:00:00Z"),0,now),"Beyond next seven calendar days removed");
        check(EventPolicy.when(at("2026-10-18T17:30:00Z"),0,false,now).equals("Tomorrow · 1:30 PM ET"),"Explicit Cincinnati time and relative day");
        check(EventPolicy.when(at("2026-10-20T04:00:00Z"),0,true,now).contains("time not listed"),"Unknown start time is explicit");
        check(EventPolicy.weekEnd(at("2026-10-31T16:00:00Z"))==at("2026-11-07T05:00:00Z"),"Seven local calendar days cross DST correctly");
        System.out.println("Weekly event dates, matching, ongoing/expired entries and DST checks passed.");
    }
}
