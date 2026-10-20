package com.griffin.solhome;

import org.json.JSONObject;
import java.util.*;

/** Catalog cursor and title identity; views are recycled independently by the activity. */
final class StreamingFeed {
  final ArrayList<JSONObject> items=new ArrayList<>();
  private final HashSet<String> seen=new HashSet<>();
  int nextPage=1,emptyBatches;
  boolean more=true;
  void reset(){items.clear();seen.clear();nextPage=1;emptyBatches=0;more=true;}
  int append(StreamingCatalog.Page page,int requestedPage){
    if(requestedPage!=nextPage)throw new IllegalStateException("Unexpected catalog cursor");
    int added=0;
    for(JSONObject item:page.items){
      String type=item.optString("media_type");int id=item.optInt("id");
      if(id>0&&(type.equals("movie")||type.equals("tv"))&&seen.add(type+"/"+id)){items.add(item);added++;}
    }
    nextPage++;more=nextPage<=Math.min(500,page.totalPages);emptyBatches=added==0?emptyBatches+1:0;return added;
  }
}
