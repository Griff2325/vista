package com.griffin.solhome;
public final class PicturePolicyTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    private static void rejects(String key,int value){try{PicturePolicy.validate(key,value);throw new AssertionError("Accepted invalid adjustment");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        for(String key:PicturePolicy.KEYS){PicturePolicy.validate(key,0);PicturePolicy.validate(key,100);rejects(key,-1);rejects(key,101);}
        rejects("Picture_Mode",50);rejects("reset",0);rejects(null,10);
        check(PicturePolicy.shell(2000));check(!PicturePolicy.shell(0));check(!PicturePolicy.shell(10200));
        check(PicturePolicy.writable("MM","SDR"));check(!PicturePolicy.writable("HDMI1","SDR"));check(!PicturePolicy.writable("MM","HDR"));check(!PicturePolicy.writable(null,"SDR"));
        check(PicturePolicy.current(100,101));check(!PicturePolicy.current(100,100));check(!PicturePolicy.current(100,99));check(!PicturePolicy.current(100,4101));
        System.out.println("Picture command authorization, range and expiry checks passed");
    }
}
