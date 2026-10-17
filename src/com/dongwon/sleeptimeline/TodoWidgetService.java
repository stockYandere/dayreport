package com.dongwon.sleeptimeline;
import android.content.*;import android.widget.*;import org.json.*;
public class TodoWidgetService extends RemoteViewsService {
 public RemoteViewsFactory onGetViewFactory(Intent intent){return new Factory(getApplicationContext());}
 static class Factory implements RemoteViewsFactory {
  final Context context;JSONArray rows=new JSONArray();
  Factory(Context c){context=c;}
  public void onCreate(){}
  public void onDataSetChanged(){try{rows=TaskStore.get(context).open();}catch(Exception e){rows=new JSONArray();}}
  public void onDestroy(){}
  public int getCount(){return rows.length();}
  public RemoteViews getViewAt(int position){
   RemoteViews v=new RemoteViews(context.getPackageName(),R.layout.widget_todo_item);JSONObject r=rows.optJSONObject(position);if(r==null)return v;
   long id=r.optLong("id");v.setTextViewText(R.id.item_title,r.optString("title"));
   v.setOnClickFillInIntent(R.id.item_done,new Intent().putExtra(TodoWidget.EXTRA_ID,id).putExtra(TodoWidget.EXTRA_STATE,"done"));
   v.setOnClickFillInIntent(R.id.item_skip,new Intent().putExtra(TodoWidget.EXTRA_ID,id).putExtra(TodoWidget.EXTRA_STATE,"skipped"));
   return v;
  }
  public RemoteViews getLoadingView(){return null;}
  public int getViewTypeCount(){return 1;}
  public long getItemId(int position){JSONObject r=rows.optJSONObject(position);return r==null?position:r.optLong("id");}
  public boolean hasStableIds(){return true;}
 }
}
