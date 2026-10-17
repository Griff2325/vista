package com.griffin.solhome;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** Bounded public feed parsing; no HTML execution or external XML entities. */
public final class FeedParser {
    public static final class Item {
        public final String title,link,detail;
        public final long date;
        Item(String title,String link,String detail,long date){this.title=title;this.link=link;this.detail=detail;this.date=date;}
    }
    public static List<Item> rss(String xml) throws Exception {return rss(xml,false);}
    public static List<Item> rss(String xml,boolean aiOnly) throws Exception {
        if(xml.length()>1500000||xml.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new IllegalArgumentException("Unsupported XML");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        NodeList nodes=factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml))).getElementsByTagName("item");
        List<Item> items=new ArrayList<>();
        for(int i=0;i<nodes.getLength()&&items.size()<8;i++){
            Element e=(Element)nodes.item(i);String title=clean(tag(e,"title")),link=tag(e,"link").trim();
            String detail=summary(tag(e,"description"));
            if(aiOnly&&!Pattern.compile("\\b(ai|artificial intelligence|machine learning|neural|language model|generative|robotics|computer vision)\\b",Pattern.CASE_INSENSITIVE).matcher(title+" "+detail).find())continue;
            if(!title.isEmpty()&&link.startsWith("https://"))items.add(new Item(title,link,detail,rssDate(tag(e,"pubDate"))));
        }
        return items;
    }
    public static String summary(String html){
        Matcher paragraph=Pattern.compile("<p(?:\\s[^>]*)?>(.*?)</p>",Pattern.DOTALL|Pattern.CASE_INSENSITIVE).matcher(html);
        String value=paragraph.find()?paragraph.group(1):html;
        return clean(value,650);
    }
    private static String tag(Element e,String name){NodeList n=e.getElementsByTagName(name);return n.getLength()==0?"":n.item(0).getTextContent();}
    private static long rssDate(String value){
        for(String pattern:new String[]{"EEE, dd MMM yyyy HH:mm:ss Z","EEE, dd MMM yyyy HH:mm:ss z"})try{return new SimpleDateFormat(pattern,Locale.US).parse(value).getTime();}catch(Exception ignored){}
        return 0;
    }
    public static List<Item> concerts(String html,long now) {
        List<Item> items=new ArrayList<>();Set<String> seen=new HashSet<>();
        Pattern p=Pattern.compile("<h3[^>]*class=\"[^\"]*card-title[^\"]*\"[^>]*>(.*?)</h3>\\s*<p[^>]*class=\"[^\"]*show-date[^\"]*\"[^>]*>(.*?)</p>",Pattern.DOTALL);
        Matcher m=p.matcher(html);Calendar today=Calendar.getInstance(TimeZone.getTimeZone(CincinnatiSky.ZONE_ID));today.setTimeInMillis(now);today.set(Calendar.HOUR_OF_DAY,0);today.set(Calendar.MINUTE,0);today.set(Calendar.SECOND,0);today.set(Calendar.MILLISECOND,0);
        while(m.find()&&items.size()<20){String title=clean(m.group(1)),date=clean(m.group(2));
            try{SimpleDateFormat f=new SimpleDateFormat("MMMM d, yyyy",Locale.US);f.setTimeZone(today.getTimeZone());f.setLenient(false);long at=f.parse(date).getTime();
                if(at>=today.getTimeInMillis()&&at<now+183L*86400000&&seen.add(title+date)&&!title.toLowerCase(Locale.US).contains("cancelled")){
                    int next=html.indexOf("<h3",m.end());String tail=html.substring(m.end(),next<0?Math.min(html.length(),m.end()+2500):next);
                    Matcher link=Pattern.compile("href=\"(https://www\\.memorialhallotr\\.com/[^\"]+)\"[^>]*>More Details").matcher(tail);
                    Matcher desc=Pattern.compile("<p[^>]*>(.*?)</p>",Pattern.DOTALL).matcher(tail);
                    items.add(new Item(title,link.find()?link.group(1):"https://www.memorialhallotr.com/cincinnati-shows/",desc.find()?clean(desc.group(1),650):"Show at Memorial Hall.",at));
                }
            }catch(Exception ignored){}
        }
        items.sort(Comparator.comparingLong(i->i.date));return items;
    }
    public static String clean(String text){return clean(text,240);}
    public static String clean(String text,int limit){
        String s=text.replaceAll("<[^>]+>"," ").replace("&amp;","&").replace("&quot;","\"").replace("&apos;","'").replace("&nbsp;"," ").replace("&hellip;","…");
        Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(s);StringBuffer out=new StringBuffer();
        while(m.find()){String n=m.group(1),replacement="";try{int code=n.startsWith("x")?Integer.parseInt(n.substring(1),16):Integer.parseInt(n);if(Character.isValidCodePoint(code))replacement=new String(Character.toChars(code));}catch(Exception ignored){}m.appendReplacement(out,Matcher.quoteReplacement(replacement));}m.appendTail(out);
        s=out.toString().replaceAll("\\s+"," ").trim();return s.length()>limit?s.substring(0,limit)+"…":s;
    }
}
