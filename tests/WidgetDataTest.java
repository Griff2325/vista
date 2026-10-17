package com.griffin.solhome;

import java.time.Instant;
import java.util.*;

public final class WidgetDataTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        long at=Instant.parse("2026-10-17T00:00:00Z").toEpochMilli();
        List<SnowForecast.Interval> snow=Arrays.asList(new SnowForecast.Interval("2026-10-17T00:00:00+00:00/PT6H",25.4),new SnowForecast.Interval("2026-10-17T06:00:00+00:00/PT6H",50.8));
        check(Math.abs(SnowForecast.inches(snow,"wmoUnit:mm",at,at+12*3600000L)-3)<.0001,"Snow amounts convert millimeters to inches");
        check(Math.abs(SnowForecast.inches(snow,"wmoUnit:mm",at+3*3600000L,at+9*3600000L)-1.5)<.0001,"Partial forecast intervals prorated");
        check(Double.isNaN(SnowForecast.inches(snow,"wmoUnit:mm",at,at+72*3600000L)),"Missing forecast coverage must not be reported as zero snow");
        check(Double.isNaN(SnowForecast.inches(snow,"wmoUnit:degC",at,at+6*3600000L)),"Unknown snow units rejected");
        check(SnowForecast.inches(Collections.singletonList(new SnowForecast.Interval("2026-10-17T00:00:00Z/P3D",0)),"wmoUnit:mm",at,at+72*3600000L)==0,"Complete zero forecast distinct from unavailable");
        String rss="<rss><channel><title>Channel</title><item><title>Research &amp; progress</title><link>https://example.com/story</link><pubDate>Sat, 17 Oct 2026 12:00:00 +0000</pubDate></item><item><title>Bad link</title><link>javascript:bad</link></item></channel></rss>";
        check(FeedParser.summary("<p>A useful publisher brief &amp; context.</p><p>Do not copy the whole article.</p>").equals("A useful publisher brief & context."),"Publisher first paragraph retained as a brief");
        check(FeedParser.rss(rss,true).isEmpty(),"General research titles are not automatically AI news");
        String ai="<rss><channel><item><title>Forecast research</title><description>Neural networks improve predictions.</description><link>https://example.com/ai</link></item></channel></rss>";check(FeedParser.rss(ai,true).size()==1,"AI subject can appear in the publisher brief");
        List<FeedParser.Item> items=FeedParser.rss(rss);check(items.size()==1&&items.get(0).title.equals("Research & progress"),"RSS entities and item scoping");check(items.get(0).date==at+12*3600000L,"Publication date parsed as publication date");
        try{FeedParser.rss("<!DOCTYPE rss [<!ENTITY test SYSTEM 'file:///etc/passwd'>]><rss/>");throw new AssertionError("DTD accepted");}catch(IllegalArgumentException expected){}
        String html="<h3 class=\"card-title featured-title\">Future &#8211; Concert</h3><p class=\"card-text show-date\">October 20, 2026</p><h3 class=\"card-title featured-title\">Past</h3><p class=\"card-text show-date\">October 1, 2026</p>";
        items=FeedParser.concerts(html,at);check(items.size()==1&&items.get(0).title.equals("Future – Concert"),"Upcoming concert dates and entity decoding");
        System.out.println("Feed parsing, future concert dates, XML rejection, and snow interval checks passed.");
    }
}
