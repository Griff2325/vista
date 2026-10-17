package com.griffin.solhome;
/** Explicit NWS unit conversions; unsupported/null measurements stay unavailable. */
public final class WeatherValues {
    public static double fahrenheit(double value,String unit){if(!Double.isFinite(value))return Double.NaN;if(unit.endsWith(":degC"))return value*1.8+32;if(unit.endsWith(":degF"))return value;return Double.NaN;}
    public static double metersPerSecond(double value,String unit){if(!Double.isFinite(value)||value<0)return Double.NaN;if(unit.endsWith(":km_h-1"))return value/3.6;if(unit.endsWith(":kn"))return value*.514444;if(unit.endsWith(":m_s-1"))return value;return Double.NaN;}
    public static String bearing(double degrees){if(!Double.isFinite(degrees)||degrees<0||degrees>360)return "";return new String[]{"N","NE","E","SE","S","SW","W","NW"}[(int)Math.round(degrees/45)%8];}
}
