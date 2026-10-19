package com.griffin.solhome;

/** Native Panasonic units; the similarly named generic cache controls are different. */
public final class PicturePolicy {
    public static final String[] KEYS={"Luminance","Cont","Col"};
    public static boolean key(String key){return "Luminance".equals(key)||"Cont".equals(key)||"Col".equals(key);}
    public static void validate(String key,int value){if(!key(key)||value<0||value>100)throw new IllegalArgumentException("Invalid picture adjustment");}
    public static boolean writable(String source,String hdr){return "MM".equals(source)&&"SDR".equals(hdr);}
    public static boolean shell(int uid){return uid==2000;}
    public static boolean current(long now,long deadline){return deadline>now&&deadline-now<=4000;}
}
