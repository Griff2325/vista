package com.griffin.solhome;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import java.io.IOException;
import java.util.UUID;

/** DUMP-protected, bounded rendezvous with the authorized local shell helper. No network listener. */
public final class PictureBridgeProvider extends ContentProvider {
    private static final Object lock=new Object();
    private static Bundle waiting,answer;
    private static boolean dispatched;
    private static long lastPoll;
    static Bundle request(String key,int value,String mode,IBinder source)throws Exception{
        synchronized(lock){
            if(lastPoll==0||SystemClock.elapsedRealtime()-lastPoll>45000)throw new IOException("Picture bridge is offline");
            if(waiting!=null)throw new IOException("Picture adjustment is busy");
            Bundle command=new Bundle();command.putString("id",UUID.randomUUID().toString());command.putString("key",key);
            command.putInt("value",value);command.putString("mode",mode);command.putBinder("source",source);long deadline=SystemClock.elapsedRealtime()+3000;command.putLong("deadline",deadline);
            waiting=command;answer=null;dispatched=false;lock.notifyAll();
            try{
                while(answer==null){long remaining=deadline-SystemClock.elapsedRealtime();if(remaining<=0)throw new IOException("Picture bridge timed out; reopen to read current values");lock.wait(remaining);}
                if(answer.containsKey("error"))throw new IOException(answer.getString("error"));return new Bundle(answer);
            }finally{waiting=null;answer=null;dispatched=false;lock.notifyAll();}
        }
    }
    @Override public boolean onCreate(){return true;}
    @Override public Bundle call(String method,String arg,Bundle extras){
        if(!PicturePolicy.shell(Binder.getCallingUid()))throw new SecurityException("Authorized shell required");
        synchronized(lock){
            if("next".equals(method)){
                long until=SystemClock.elapsedRealtime()+25000;lastPoll=SystemClock.elapsedRealtime();
                while(waiting==null||dispatched){long remaining=until-SystemClock.elapsedRealtime();if(remaining<=0)return new Bundle();try{lock.wait(remaining);}catch(InterruptedException stopped){Thread.currentThread().interrupt();return new Bundle();}}
                dispatched=true;lastPoll=SystemClock.elapsedRealtime();return new Bundle(waiting);
            }
            if("reply".equals(method)&&extras!=null){
                if(waiting!=null&&dispatched&&waiting.getString("id").equals(extras.getString("id"))){answer=new Bundle(extras);lock.notifyAll();}
                return new Bundle();
            }
            throw new IllegalArgumentException("Invalid picture bridge operation");
        }
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException();}
    @Override public String getType(Uri u){return null;}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
