package com.griffin.solhome;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Small memory-only artwork cache; no posters or credentials are stored on disk. */
final class StreamingArtwork implements AutoCloseable {
  private final ExecutorService workers=Executors.newFixedThreadPool(2);
  private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024){
    protected int sizeOf(String key,Bitmap value){return value.getByteCount();}
  };
  private volatile boolean closed;
  static String url(String path,boolean hero){
    return path!=null&&path.matches("/[A-Za-z0-9]+\\.(jpg|png|webp)")?"https://image.tmdb.org/t/p/"+(hero?"w780":"w500")+path:null;
  }
  void show(ImageView view,String path,boolean hero){
    String url=url(path,hero);view.setTag(url);view.setImageDrawable(null);if(url==null||closed)return;
    Bitmap hit=cache.get(url);if(hit!=null){view.setImageBitmap(hit);return;}
    workers.submit(()->{
      try{
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(6000);connection.setInstanceFollowRedirects(false);
        Bitmap bitmap;
        try{
          if(connection.getResponseCode()!=200)return;
          ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count,total=0;
          try(InputStream in=connection.getInputStream()){while((count=in.read(buffer))!=-1){if(closed||Thread.currentThread().isInterrupted())return;total+=count;if(total>3000000)return;out.write(buffer,0,count);}}
          byte[] bytes=out.toByteArray();BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
          if(options.outWidth<=0||options.outHeight<=0||(long)options.outWidth*options.outHeight>8000000)return;
          options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
          bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        }finally{connection.disconnect();}
        if(bitmap==null||closed)return;cache.put(url,bitmap);
        view.post(()->{if(!closed&&url.equals(view.getTag()))view.setImageBitmap(bitmap);});
      }catch(IOException ignored){/* Artwork failure leaves the title and its controls available. */}
    });
  }
  public void close(){closed=true;workers.shutdownNow();cache.evictAll();}
}
