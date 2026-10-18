package com.griffin.solhome;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Pairing is always requested on the TV. QR contains only the LAN URL, never a credential. */
final class CompanionPanel {
    static void show(Activity a){
        CompanionService active=CompanionService.active();
        if(active==null){new AlertDialog.Builder(a).setTitle("Phone companion")
            .setMessage("Use your phone to navigate Vista, type an Explore search, open apps, change the scene and control Spotify.\n\nEnable a local connection on this TV, then scan its QR in the Vista iPhone app and enter the TV code. A browser fallback also works. Use trusted home Wi-Fi: this local HTTP connection is not encrypted. No cloud account needed.\n\nTurns off after 30 minutes without use, at TV restart, or when you turn it off here.")
            .setPositiveButton("Enable companion",(d,w)->{
                a.startForegroundService(new Intent(a,CompanionService.class));waitForStart(a,0);
            }).setNegativeButton("Cancel",null).show();return;}
        if(active.address().isEmpty()){waitForStart(a,0);return;}active.attach(a);
        new AlertDialog.Builder(a).setTitle("Phone companion is on").setItems(new String[]{"Pair a phone · replaces existing pairing","Turn companion off"},(d,n)->{if(n==0)pair(a,active);else a.stopService(new Intent(a,CompanionService.class));}).setNegativeButton("Close",null).show();
    }
    private static void waitForStart(Activity a,int tries){new Handler(Looper.getMainLooper()).postDelayed(()->{
        if(a.isFinishing()||a.isDestroyed())return;CompanionService active=CompanionService.active();
        if(active!=null&&!active.address().isEmpty()){active.attach(a);pair(a,active);}
        else if(tries<16)waitForStart(a,tries+1);else Toast.makeText(a,"Companion could not start. Check the TV network.",Toast.LENGTH_LONG).show();
    },250);}
    private static void pair(Activity a,CompanionService active){
        String code=active.pair();
        LinearLayout body=new LinearLayout(a);body.setOrientation(LinearLayout.HORIZONTAL);body.setPadding(32,12,32,12);
        LinearLayout words=new LinearLayout(a);words.setOrientation(1);body.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        TextView help=new TextView(a);help.setText("1  Connect your phone to the same home network.\n2  Open the Vista iPhone app and scan this QR, or enter the TV address:\n"+active.address()+"\n3  Enter the code below on your phone.");help.setTextSize(17);words.addView(help);
        TextView pin=new TextView(a);pin.setText(code);pin.setTextSize(42);pin.setTypeface(Typeface.MONOSPACE);words.addView(pin);
        TextView status=new TextView(a);status.setTextSize(15);words.addView(status);
        QrCode qr=QrCode.encodeText(active.address(),QrCode.Ecc.MEDIUM);View image=new View(a){private final Paint paint=new Paint();@Override protected void onDraw(Canvas c){float s=Math.min(getWidth(),getHeight())/(float)(qr.size+8);paint.setColor(Color.WHITE);c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setColor(Color.BLACK);for(int y=0;y<qr.size;y++)for(int x=0;x<qr.size;x++)if(qr.getModule(x,y))c.drawRect((x+4)*s,(y+4)*s,(x+5)*s,(y+5)*s,paint);}};
        int side=(int)(190*a.getResources().getDisplayMetrics().density);body.addView(image,new LinearLayout.LayoutParams(side,side));
        Handler handler=new Handler(Looper.getMainLooper());AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Pair with Vista").setView(body).setPositiveButton("Done",null).setNeutralButton("New code",(d,w)->pair(a,active)).setNegativeButton("Turn off",(d,w)->a.stopService(new Intent(a,CompanionService.class))).create();
        Runnable tick=new Runnable(){public void run(){String state=CompanionService.active()==active?active.pairingStatus():"Companion is off";status.setText(state);if(state.equals("Phone connected")){Toast.makeText(a,"Phone connected to Vista",Toast.LENGTH_SHORT).show();dialog.dismiss();return;}handler.postDelayed(this,5000);}};
        dialog.setOnDismissListener(d->handler.removeCallbacksAndMessages(null));dialog.show();handler.post(tick);
    }
}
