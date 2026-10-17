package com.griffin.solhome;

import android.content.ComponentName;
import android.os.IBinder;
import java.lang.reflect.*;

/** Explicit ADB-shell setup entry point; never called with an ordinary app's UID. */
public final class DreamConfig {
    public static void main(String[] args)throws Exception {
        if(android.os.Process.myUid()!=2000)throw new SecurityException("Run this setup helper through authorized ADB shell");
        Class<?> services=Class.forName("android.os.ServiceManager");
        IBinder binder=(IBinder)services.getMethod("getService",String.class).invoke(null,"dreams");
        Class<?> stub=Class.forName("android.service.dreams.IDreamManager$Stub");
        Object manager=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
        Class<?> api=Class.forName("android.service.dreams.IDreamManager");
        api.getMethod("setDreamComponents",ComponentName[].class).invoke(manager,(Object)new ComponentName[]{new ComponentName("com.griffin.solhome","com.griffin.solhome.SolDreamService")});
        Field transaction=stub.getDeclaredField("TRANSACTION_setDreamComponents");transaction.setAccessible(true);
        System.out.println("SET_DREAM_TRANSACTION="+transaction.getInt(null));
    }
}
