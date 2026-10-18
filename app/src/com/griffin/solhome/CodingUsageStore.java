package com.griffin.solhome;

import android.content.*;
import org.json.*;
import java.util.*;

/** Bounded, validated private cache. Imports run on the shell-only provider's Binder thread. */
final class CodingUsageStore {
    private final SharedPreferences cache;
    private volatile CodingUsage value=new CodingUsage(CodingUsage.empty(),CodingUsage.empty());
    CodingUsageStore(Context context){cache=context.getSharedPreferences("coding_usage",0);try{value=parse(cache.getString("snapshot","{}"));}catch(Exception ignored){}}
    CodingUsage get(){return value;}
    synchronized void importSnapshot(String json)throws JSONException{
        CodingUsage parsed=parse(json);
        // Serialize only the validated model, even if an authorized sender included extra fields.
        JSONObject providers=new JSONObject().put("codex",encode(parsed.codex)).put("claude",encode(parsed.claude));
        String safe=new JSONObject().put("schema",1).put("providers",providers).toString();
        if(!cache.edit().putString("snapshot",safe).commit())throw new JSONException("Could not save usage");value=parsed;
    }
    private static JSONObject encode(CodingUsage.Provider p)throws JSONException{
        JSONArray windows=new JSONArray();for(CodingUsage.Window w:p.windows)windows.put(new JSONObject().put("usedPercent",w.used).put("durationMins",w.minutes).put("resetAt",w.reset));
        JSONObject data=new JSONObject().put("at",p.at).put("status",p.saved?"saved":"ok").put("windows",windows);
        if(p.tokens!=null)data.put("tokens",new JSONObject().put("day",p.tokens.day).put("count",p.tokens.count).put("at",p.tokens.at));return data;
    }
    private static CodingUsage parse(String text)throws JSONException{
        JSONObject root=new JSONObject(text);if(root.optInt("schema")!=1)throw new JSONException("Unsupported usage snapshot");
        JSONObject providers=root.getJSONObject("providers");return new CodingUsage(provider(providers.optJSONObject("codex")),provider(providers.optJSONObject("claude")));
    }
    private static CodingUsage.Provider provider(JSONObject data){
        if(data==null)return CodingUsage.empty();long now=System.currentTimeMillis(),at=data.optLong("at");if(at<=0||at>now+300000)at=0;
        JSONArray list=data.optJSONArray("windows");List<CodingUsage.Window> windows=new ArrayList<>();
        if(list!=null)for(int i=0;i<Math.min(4,list.length());i++){JSONObject w=list.optJSONObject(i);if(w==null)continue;Object used=w.opt("usedPercent"),duration=w.opt("durationMins"),reset=w.opt("resetAt");if(!(used instanceof Number)||!(duration instanceof Number)||!(reset instanceof Number))continue;
            double percent=((Number)used).doubleValue();int minutes=((Number)duration).intValue();long ends=((Number)reset).longValue();if(CodingUsage.valid(percent,minutes,ends,now))windows.add(new CodingUsage.Window(percent,minutes,ends));}
        CodingUsage.Tokens tokens=null;JSONObject raw=data.optJSONObject("tokens");if(raw!=null&&raw.opt("count") instanceof Number&&raw.opt("at") instanceof Number){
            double count=raw.optDouble("count",-1);if(Double.isFinite(count)&&count>=0&&count<=1000000000000L&&count==Math.floor(count)){
                CodingUsage.Tokens candidate=new CodingUsage.Tokens(raw.optString("day"),raw.optLong("count",-1),raw.optLong("at"));if(candidate.usable(now))tokens=candidate;}}
        windows.sort((a,b)->Integer.compare(a.minutes,b.minutes));return new CodingUsage.Provider(at,!data.optString("status").equals("ok"),windows,tokens);
    }
}
