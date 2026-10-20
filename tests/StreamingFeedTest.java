package com.griffin.solhome;

import org.json.JSONObject;

public final class StreamingFeedTest {
  private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
  private static JSONObject title(String type,int id)throws Exception{return new JSONObject().put("id",id).put("media_type",type);}
  public static void main(String[] args)throws Exception{
    StreamingFeed feed=new StreamingFeed();StreamingCatalog.Page page=new StreamingCatalog.Page();page.totalPages=5;
    for(int i=1;i<=40;i++)page.items.add(title("movie",i));
    check(feed.append(page,1)==40&&feed.items.size()==40&&feed.nextPage==2&&feed.more,"First full mixed batch is retained");
    page.items.clear();page.items.add(title("movie",40));page.items.add(title("movie",41));page.items.add(title("tv",40));
    check(feed.append(page,2)==2&&feed.items.size()==42,"Append preserves old titles and distinguishes movie/series IDs");
    page.items.clear();feed.append(page,3);feed.append(page,4);
    check(feed.more&&feed.emptyBatches==2&&feed.nextPage==5,"Sparse filtered batches advance without losing cursor");
    page.items.add(title("tv",99));feed.append(page,5);
    check(!feed.more&&feed.emptyBatches==0,"Final results end naturally and successful append clears sparse streak");
    try{feed.append(page,5);throw new AssertionError("Duplicate page accepted");}catch(IllegalStateException expected){}
    feed.reset();check(feed.items.isEmpty()&&feed.more&&feed.nextPage==1,"New search clears cursor and deduplication");
    page.totalPages=999;check(feed.append(page,1)==1,"Reset permits the same title in a new search");
    page.items.clear();for(int i=2;i<=500;i++)feed.append(page,i);
    check(!feed.more&&feed.nextPage==501,"Catalog's upstream 500-page boundary is respected");
    System.out.println("Continuous feed cursor, deduplication, sparse batches and end checks passed");
  }
}
