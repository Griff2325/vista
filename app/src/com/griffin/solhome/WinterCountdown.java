package com.griffin.solhome;
import java.time.*;
import java.time.temporal.ChronoUnit;
/** Calendar countdown to Dec 21, in Cincinnati; not a yearly solstice ephemeris. */
public final class WinterCountdown {
    public static long days(long now){LocalDate today=Instant.ofEpochMilli(now).atZone(ZoneId.of(CincinnatiSky.ZONE_ID)).toLocalDate();int month=today.getMonthValue();if(month<3||month==3&&today.getDayOfMonth()<20||month==12&&today.getDayOfMonth()>=21)return 0;return ChronoUnit.DAYS.between(today,LocalDate.of(today.getYear(),12,21));}
    public static String label(long now){long days=days(now);return days==0?"Winter is here":"Winter in "+days+" day"+(days==1?"":"s")+" · Dec 21";}
}
