package com.griffin.solhome;

import java.time.*;
import java.util.*;

/** NWS interval amounts, prorated where the requested window crosses an interval. */
public final class SnowForecast {
    public static final class Interval {
        public final long start,end;public final double amount;
        public Interval(String valid,double amount){String[] parts=valid.split("/");start=OffsetDateTime.parse(parts[0]).toInstant().toEpochMilli();end=start+Duration.parse(parts[1]).toMillis();this.amount=amount;}
    }
    public static double inches(List<Interval> values,String unit,long start,long end){
        double divisor=unit.endsWith(":mm")?25.4:unit.endsWith(":cm")?2.54:unit.endsWith(":in")?1:Double.NaN;
        if(!Double.isFinite(divisor)||end<=start)return Double.NaN;
        List<Interval> sorted=new ArrayList<>(values);sorted.sort(Comparator.comparingLong(v->v.start));
        long through=start;double sum=0;
        for(Interval v:sorted){
            if(v.end<=through||v.start>=end)continue;
            if(v.start>through||v.end<=v.start||!Double.isFinite(v.amount)||v.amount<0)return Double.NaN;
            long stop=Math.min(end,v.end);sum+=v.amount*(stop-through)/(double)(v.end-v.start);through=stop;if(through>=end)break;
        }
        return through<end?Double.NaN:sum/divisor;
    }
}
