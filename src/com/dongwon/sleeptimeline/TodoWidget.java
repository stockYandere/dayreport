package com.dongwon.sleeptimeline;
import android.app.*;import android.appwidget.*;import android.content.*;import android.net.Uri;import android.widget.RemoteViews;
/* 바탕화면 할 일 위젯. O=완료, X=안 함, +=추가, 기록 보기·제목=앱의 하루 리포트(한 일). */
public class TodoWidget extends AppWidgetProvider {
 static final String ACTION_ITEM="com.dongwon.sleeptimeline.TODO_ITEM",ACTION_MIDNIGHT="com.dongwon.sleeptimeline.TODO_MIDNIGHT";
 static final String EXTRA_ID="id",EXTRA_STATE="state";
 static final int FLAG_MUTABLE=0x02000000; // PendingIntent.FLAG_MUTABLE (API 31)

 public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)m.updateAppWidget(id,views(c,id));m.notifyAppWidgetViewDataChanged(ids,R.id.widget_list);scheduleMidnight(c);}
 public void onEnabled(Context c){scheduleMidnight(c);}

 public void onReceive(final Context c,Intent intent){
  super.onReceive(c,intent);String action=intent.getAction();
  if(ACTION_ITEM.equals(action)){
   final long id=intent.getLongExtra(EXTRA_ID,-1);final String state=intent.getStringExtra(EXTRA_STATE);if(id<=0||state==null)return;
   final PendingResult result=goAsync();
   new Thread(new Runnable(){public void run(){try{TaskStore.get(c).setState(id,state,System.currentTimeMillis());}catch(Exception ignored){}finally{refreshAll(c);result.finish();}}}).start();
  }else if(ACTION_MIDNIGHT.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||Intent.ACTION_TIMEZONE_CHANGED.equals(action)){refreshAll(c);scheduleMidnight(c);}
 }

 static void refreshAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,TodoWidget.class));if(ids==null||ids.length==0)return;for(int id:ids)m.updateAppWidget(id,views(c,id));m.notifyAppWidgetViewDataChanged(ids,R.id.widget_list);}

 static RemoteViews views(Context c,int widgetId){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_todo);
  Intent service=new Intent(c,TodoWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);service.setData(Uri.parse(service.toUri(Intent.URI_INTENT_SCHEME)));
  v.setRemoteAdapter(R.id.widget_list,service);v.setEmptyView(R.id.widget_list,R.id.widget_empty);
  Intent item=new Intent(c,TodoWidget.class).setAction(ACTION_ITEM);
  v.setPendingIntentTemplate(R.id.widget_list,PendingIntent.getBroadcast(c,1,item,PendingIntent.FLAG_UPDATE_CURRENT|FLAG_MUTABLE));
  Intent add=new Intent(c,TaskAddActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
  v.setOnClickPendingIntent(R.id.widget_add,PendingIntent.getActivity(c,2,add,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent history=new Intent(c,MainActivity.class).putExtra(MainActivity.EXTRA_ROUTE,"report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  v.setOnClickPendingIntent(R.id.widget_history,PendingIntent.getActivity(c,3,history,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent open=new Intent(c,MainActivity.class).putExtra(MainActivity.EXTRA_ROUTE,"report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
  v.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(c,4,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  long day=TaskStore.dayStart(System.currentTimeMillis());
  v.setTextViewText(R.id.widget_count,"오늘 완료 "+TaskStore.get(c).doneCount(day,TaskStore.nextDay(day))+"개");
  return v;
 }

 // 자정이 지나면 '오늘 완료' 숫자를 0으로 다시 그린다. 정확한 알람 권한 없이 대략 자정에 실행된다.
 static void scheduleMidnight(Context c){
  AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(a==null)return;
  PendingIntent p=PendingIntent.getBroadcast(c,5,new Intent(c,TodoWidget.class).setAction(ACTION_MIDNIGHT),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  a.set(AlarmManager.RTC,TaskStore.nextDay(TaskStore.dayStart(System.currentTimeMillis()))+1000,p);
 }
}
