package com.dongwon.sleeptimeline;
import android.appwidget.*;import android.content.*;import android.os.SystemClock;import android.view.View;import android.widget.RemoteViews;import java.text.SimpleDateFormat;import java.util.*;
/* 홈 화면 수면 기록 위젯. 기록 전에는 [수면 시작], 기록 중에는 [일시 중지/재개][기록 정지][다시 시작]. */
public class SleepWidget extends AppWidgetProvider {
 public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)m.updateAppWidget(id,views(c));}
 static void refreshAll(Context c){AppWidgetManager m=AppWidgetManager.getInstance(c);int[] ids=m.getAppWidgetIds(new ComponentName(c,SleepWidget.class));if(ids==null)return;for(int id:ids)m.updateAppWidget(id,views(c));}
 static RemoteViews views(Context c){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget_sleep);SleepSession s=SleepRecorder.load(c);long now=System.currentTimeMillis();SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.KOREA);
  v.setOnClickPendingIntent(R.id.sleep_title,SleepRecorder.openApp(c));
  if(!s.active()){
   v.setViewVisibility(R.id.sleep_clock,View.GONE);v.setTextViewText(R.id.sleep_status,"기록하고 있지 않아");
   v.setTextViewText(R.id.sleep_main,"수면 시작");v.setOnClickPendingIntent(R.id.sleep_main,SleepRecorder.action(c,"start",21));
   v.setViewVisibility(R.id.sleep_stop,View.GONE);v.setViewVisibility(R.id.sleep_restart,View.GONE);
   return v;
  }
  // Chronometer는 기기 부팅 후 경과 시간 기준이라, 지금까지 잔 시간만큼 뒤로 맞춘다. 일시 중지 중이면 멈춘 채로.
  v.setViewVisibility(R.id.sleep_clock,View.VISIBLE);v.setChronometer(R.id.sleep_clock,SystemClock.elapsedRealtime()-s.slept(now),null,!s.paused());
  v.setTextViewText(R.id.sleep_status,s.paused()?f.format(new Date(s.pausedAt()))+"부터 일시 중지 · 깬 시간은 빼고 저장":f.format(new Date(s.start))+" 시작 · 기록 중");
  v.setTextViewText(R.id.sleep_main,s.paused()?"재개":"일시 중지");v.setOnClickPendingIntent(R.id.sleep_main,SleepRecorder.action(c,s.paused()?"resume":"pause",22));
  v.setViewVisibility(R.id.sleep_stop,View.VISIBLE);v.setOnClickPendingIntent(R.id.sleep_stop,SleepRecorder.action(c,"stop",23));
  v.setViewVisibility(R.id.sleep_restart,View.VISIBLE);v.setOnClickPendingIntent(R.id.sleep_restart,SleepRecorder.action(c,"restart",24));
  return v;
 }
}
