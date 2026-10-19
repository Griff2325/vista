package com.griffin.solhome;
import java.util.*;
public final class CodingUsageTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        long now=1792300000000L;CodingUsage.Window week=new CodingUsage.Window(8,10080,now+86400000),shorter=new CodingUsage.Window(0,300,now+3600000);
        check(UsageImportPolicy.allowed(2000,10001)&&UsageImportPolicy.allowed(10001,10001),"shell and own UID allowed");
        for(int uid:new int[]{0,1000,10002,99001})check(!UsageImportPolicy.allowed(uid,10001),"other UIDs rejected");
        CodingUsage.Provider p=new CodingUsage.Provider(now,false,Arrays.asList(shorter,week));
        check(p.usable(now)&&!p.stale(now),"fresh reading");check(CodingUsage.percent(shorter).equals("0% used"),"reported zero is valid");
        check(p.stale(now+CodingUsage.FRESH_MS+1),"saved age");check(!p.usable(now+CodingUsage.MAX_AGE_MS+1),"old data hidden");
        CodingUsage.Window laterWeek=new CodingUsage.Window(22,10080,now+4*86400000L);
        check(new CodingUsage.Provider(now-CodingUsage.MAX_AGE_MS-1,false,Arrays.asList(laterWeek)).unavailable(now).equals("Quota reading expired"),"expired quota does not imply an app update");
        check(new CodingUsage.Provider(now+600000,false,Arrays.asList(laterWeek)).unavailable(now).equals("Waiting for fresh limits"),"invalid observation time is not described as an app update");
        check(p.current(now+7200000)==week,"expired short window does not erase weekly window");
        check(p.current(now+2*86400000)==null,"expired window not inferred as zero");
        check(!CodingUsage.empty().usable(now)&&CodingUsage.empty().unavailable(now).equals("Waiting for sync"),"unknown not zero");
        check(!CodingUsage.valid(Double.NaN,300,now+1,now)&&!CodingUsage.valid(101,300,now+1,now)&&!CodingUsage.valid(-1,300,now+1,now),"percentage bounds");
        check(!CodingUsage.valid(50,0,now+1,now)&&!CodingUsage.valid(50,300,Long.MAX_VALUE,now),"window bounds");
        check(new CodingUsage.Window(1,60,now+1).label().equals("1h")&&week.label().equals("Weekly"),"duration drives labels");
        check(new CodingUsage.Provider(now+600000,false,Arrays.asList(week)).usable(now)==false,"future source time rejected");
        check(new CodingUsage.Provider(now,true,Arrays.asList(week)).stale(now),"failed refresh retains saved marker");
        check(CodingUsage.reset(shorter,now).startsWith("Resets "),"Cincinnati reset display");
        CodingUsage.Tokens tokens=new CodingUsage.Tokens("2026-10-18",1234567,now);
        check(tokens.usable(now)&&!tokens.usable(now+86400000),"token day expires at Cincinnati midnight");
        check(!new CodingUsage.Tokens("2026-10-18",-1,now).usable(now),"negative token count rejected");
        check(CodingUsage.tokens(new CodingUsage.Provider(0,true,Collections.emptyList(),tokens),now,false).contains("1.2M tokens"),"tokens remain separate from missing quota");
        System.out.println("Coding usage bounds, missing/reset/saved state and window selection passed");
    }
}
