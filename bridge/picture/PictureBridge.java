package com.griffin.solhome;

import android.content.*;
import android.net.Uri;
import android.os.*;
import java.io.IOException;
import java.lang.reflect.*;

import org.json.JSONObject;

/** Shell-only adapter for the installed TV API. No vendor code or credentials are bundled. */
public final class PictureBridge {
    private static final Uri QUEUE=Uri.parse("content://com.griffin.solhome.picture-bridge");
    private static final String DESCRIPTOR="com.mediatek.extservice.pqutils.ICustomerPqService";
    private static Class<?> api;
    private static Object engine;
    private static IBinder cache;
    private static Object manager,provider;
    private static final IBinder token=new Binder();
    private static Bundle call(String method,Bundle extras)throws Exception{
        if(provider==null){
            Object holder=manager.getClass().getMethod("getContentProviderExternal",String.class,int.class,IBinder.class,String.class).invoke(manager,QUEUE.getAuthority(),0,token,"vista-picture");
            if(holder==null)throw new IOException("Vista picture provider unavailable");provider=holder.getClass().getField("provider").get(holder);
        }
        return (Bundle)Class.forName("android.content.IContentProvider").getMethod("call",String.class,String.class,String.class,String.class,String.class,Bundle.class)
            .invoke(provider,"com.android.shell",null,QUEUE.getAuthority(),method,null,extras);
    }
    private static void release(){
        if(provider!=null)try{manager.getClass().getMethod("removeContentProviderExternalAsUser",String.class,IBinder.class,int.class).invoke(manager,QUEUE.getAuthority(),token,0);}catch(Exception ignored){}
        provider=null;
    }
    private static String cached(String key)throws Exception{
        IBinder remote=cache;if(remote==null||!remote.isBinderAlive())throw new IOException("Picture source service unavailable");
        if(!DESCRIPTOR.equals(remote.getInterfaceDescriptor()))throw new IOException("Unexpected picture source service");
        Parcel q=Parcel.obtain(),r=Parcel.obtain();try{q.writeInterfaceToken(DESCRIPTOR);q.writeString(key);if(!remote.transact(1,q,r,0))throw new IOException("Source read failed");r.readException();return r.readString();}finally{q.recycle();r.recycle();}
    }
    private static JSONObject hardware()throws Exception{
        Class<?> cb=Class.forName(api.getName()+"$getGlobalNonAwarePqSettingCallback");final String[] json={null};
        Object callback=Proxy.newProxyInstance(cb.getClassLoader(),new Class<?>[]{cb},(p,m,v)->{
            if(m.getName().equals("onValues")){if(((Number)v[0]).intValue()!=0)throw new IOException("Picture read failed");json[0]=(String)v[1];}return null;
        });
        api.getMethod("getGlobalNonAwarePqSetting",cb).invoke(engine,callback);
        if(json[0]==null||json[0].length()>65536)throw new IOException("Invalid picture response");return new JSONObject(json[0]);
    }
    private static void apply(String key,String value)throws Exception{
        int status=((Number)api.getMethod("setPqParamsByGlobal",String.class).invoke(engine,new JSONObject().put(key,value).toString())).intValue();
        if(status!=0)throw new IOException("Picture engine rejected adjustment");
    }
    private static void context(String mode,long deadline)throws Exception{
        if(!PicturePolicy.current(SystemClock.elapsedRealtime(),deadline))throw new IOException("Expired picture adjustment");
        if(!PicturePolicy.writable(cached("Source"),cached("HdrType")))throw new IOException("Use Panasonic settings for HDMI or HDR");
        if(mode==null||!mode.equals(hardware().getString("Picture_Mode")))throw new IOException("Picture mode changed; reopen the panel");
    }
    private static Bundle execute(Bundle command)throws Exception{
        cache=command.getBinder("source");String key=command.getString("key");
        if(!"read".equals(key)){
            int value=command.getInt("value",-1);PicturePolicy.validate(key,value);
            String mode=command.getString("mode");long deadline=command.getLong("deadline");context(mode,deadline);
            String original=hardware().getString(key);PicturePolicy.validate(key,Integer.parseInt(original));
            try{
                context(mode,deadline);apply(key,Integer.toString(value));
                if(!Integer.toString(value).equals(hardware().getString(key)))throw new IOException("Picture readback mismatch");
            }catch(Exception failed){
                // Do not restore another input's settings if playback changed during the request.
                if(PicturePolicy.writable(cached("Source"),cached("HdrType"))&&mode.equals(hardware().getString("Picture_Mode")))apply(key,original);
                throw failed;
            }
        }
        JSONObject values=hardware();Bundle result=new Bundle();
        for(String k:PicturePolicy.KEYS){int value=Integer.parseInt(values.getString(k));PicturePolicy.validate(k,value);result.putInt(k,value);}
        result.putString("mode",values.getString("Picture_Mode"));result.putBoolean("writable",PicturePolicy.writable(cached("Source"),cached("HdrType")));return result;
    }
    public static void main(String[] args)throws Exception{
        if(!PicturePolicy.shell(android.os.Process.myUid()))throw new SecurityException("ADB shell required");
        if(!"lassen".equals(Build.DEVICE)||!Build.FINGERPRINT.contains("RS8180.3742N"))throw new IOException("Unsupported TV firmware");
        manager=Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        api=Class.forName("vendor.mediatek.hardware.pq.V1_0.IPq");engine=api.getMethod("getService").invoke(null);if(engine==null)throw new IOException("Picture engine unavailable");
        System.out.println("Vista picture bridge ready");
        while(true){
            try{
                Bundle command=call("next",null);if(command==null||!command.containsKey("id"))continue;
                Bundle result;try{result=execute(command);}catch(Exception failed){result=new Bundle();result.putString("error","Picture operation failed; reopen or use Panasonic settings");}
                result.putString("id",command.getString("id"));call("reply",result);
            }catch(Exception unavailable){release();Thread.sleep(1000);}
        }
    }
}
