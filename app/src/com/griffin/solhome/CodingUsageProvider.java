package com.griffin.solhome;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;

/** No network listener: only the already-authorized Android shell can publish quota observations. */
public final class CodingUsageProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(!UsageImportPolicy.allowed(Binder.getCallingUid(),android.os.Process.myUid()))throw new SecurityException("Usage import requires the authorized ADB shell");
        if(!"publish".equals(method)||arg==null||arg.length()>21848)throw new IllegalArgumentException("Invalid usage import");
        try{
            byte[] bytes=Base64.decode(arg,Base64.NO_WRAP);if(bytes.length>16384)throw new IllegalArgumentException("Usage snapshot too large");
            WidgetRepository.get(getContext()).importCodingUsage(new String(bytes,StandardCharsets.UTF_8));
            Bundle result=new Bundle();result.putBoolean("accepted",true);return result;
        }catch(Exception e){throw new IllegalArgumentException("Invalid usage snapshot");}
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
