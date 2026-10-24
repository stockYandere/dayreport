package com.dongwon.sleeptimeline;
import android.app.*;import android.content.*;import android.graphics.drawable.Icon;import android.os.Build;import org.json.*;import java.text.SimpleDateFormat;import java.util.*;
/* 수면 기록 버튼: 앱·잠금 화면 알림·홈 화면 위젯이 모두 여기로 모인다.
   상태는 SharedPreferences에 두고(앱이 꺼져도 유지), 기록 중에는 잠금 화면에 버튼이 달린 알림을 띄운다. */
public class SleepRecorder extends BroadcastReceiver {
 static final String ACTION="com.dongwon.sleeptimeline.RECORDER",EXTRA="do",CHANNEL="sleep_recorder";
 static final int NOTE_ID=301,FAIL_ID=302;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("recorder",0);}

 public void onReceive(final Context c,Intent intent){
  final String action=intent.getStringExtra(EXTRA);if(action==null)return;final PendingResult result=goAsync();
  new Thread(new Runnable(){public void run(){try{perform(c,action);}finally{result.finish();}}}).start();
 }

 static synchronized SleepSession load(Context c){
  SleepSession s=SleepSession.decode(prefs(c).getString("session",""));
  // 예전 '잠 시작' 버튼으로 시작해 둔 시각은 새 기록으로 옮긴다.
  SharedPreferences old=c.getSharedPreferences("settings",0);long timer=old.getLong("timer",0);
  if(timer>0){if(!s.active()){s.begin(timer);store(c,s);}old.edit().putLong("timer",0).commit();}
  return s;
 }
 static void store(Context c,SleepSession s){prefs(c).edit().putString("session",s.encode()).commit();}

 /* start, pause, resume, restart, stop. 사용자에게 보여 줄 문장을 돌려준다. */
 static synchronized String perform(Context c,String action){
  long now=System.currentTimeMillis();SleepSession s=load(c);String message;
  if("start".equals(action)){if(s.active())message="이미 수면을 기록하고 있어.";else{s.begin(now);message="수면 기록을 시작했어. 깨면 일시 중지나 기록 정지를 눌러 줘.";}}
  else if("pause".equals(action)){if(!s.active()||s.paused())message="기록 중인 수면이 없어.";else{s.pause(now);message="일시 중지했어. 깨어 있는 시간은 빼고 저장해.";}}
  else if("resume".equals(action)){if(!s.paused())message="일시 중지한 기록이 없어.";else{s.resume(now);message="다시 수면을 기록해.";}}
  else if("restart".equals(action)){s.begin(now);message="지금부터 다시 기록해. 이전 기록은 버렸어.";}
  else if("stop".equals(action)){if(!s.active())message="기록 중인 수면이 없어.";else message=save(c,s.finish(now));}
  else message="알 수 없는 동작이야.";
  store(c,s);update(c);return message;
 }

 // 조각마다 확정 수면 기록으로 저장. 기존 기록과 겹치는 등 저장 못 한 조각은 보관해 두고 앱에서 고치게 한다.
 static String save(Context c,List<long[]> pieces){
  if(pieces.isEmpty())return "1분 넘게 잔 기록이 없어서 저장하지 않았어.";
  Store store=new Store(c);List<long[]> failed=new ArrayList<long[]>();String error=null;long total=0;int saved=0;
  try{for(long[] p:pieces){try{store.save(new JSONObject().put("start",p[0]).put("end",p[1]).put("kind","sleep").put("note","수면 기록 버튼").put("zone",TimeZone.getDefault().getID()));total+=p[1]-p[0];saved++;}catch(Exception e){failed.add(p);error=e.getMessage();}}}finally{store.close();}
  if(!failed.isEmpty()){List<long[]> all=unsaved(c);all.addAll(failed);storeUnsaved(c,all);notifyFailure(c,error);}
  String done=saved>0?"수면 "+Models.durationText(total)+" 저장했어"+(saved>1?" ("+saved+"개 구간)":"")+".":"";
  return failed.isEmpty()?done:(done.isEmpty()?"":done+" ")+"저장하지 못한 구간이 "+failed.size()+"개 있어: "+error;
 }
 static List<long[]> unsaved(Context c){return SleepSession.decodePieces(prefs(c).getString("unsaved",""));}
 static void storeUnsaved(Context c,List<long[]> rows){prefs(c).edit().putString("unsaved",SleepSession.encodePieces(rows)).commit();}
 static synchronized void dropUnsaved(Context c,long start,long end){List<long[]> rows=unsaved(c);for(Iterator<long[]> i=rows.iterator();i.hasNext();){long[] p=i.next();if(p[0]==start&&p[1]==end)i.remove();}storeUnsaved(c,rows);}

 static JSONObject stateJson(Context c)throws JSONException{
  long now=System.currentTimeMillis();SleepSession s=load(c);JSONArray pending=new JSONArray();for(long[] p:unsaved(c))pending.put(new JSONObject().put("start",p[0]).put("end",p[1]));
  return new JSONObject().put("state",s.state()).put("start",s.start).put("slept",s.slept(now)).put("pausedAt",s.pausedAt()).put("now",now).put("unsaved",pending);
 }

 // 알림·위젯을 지금 상태로 다시 그린다.
 static void update(Context c){notification(c);SleepWidget.refreshAll(c);}

 static PendingIntent action(Context c,String what,int code){return PendingIntent.getBroadcast(c,code,new Intent(c,SleepRecorder.class).setAction(ACTION+"."+what).putExtra(EXTRA,what),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static PendingIntent openApp(Context c){return PendingIntent.getActivity(c,40,new Intent(c,MainActivity.class).putExtra(MainActivity.EXTRA_ROUTE,"sleep").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static NotificationManager channel(Context c){NotificationManager m=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel(CHANNEL,"수면 기록",NotificationManager.IMPORTANCE_LOW);ch.setDescription("수면 기록 중 잠금 화면에 버튼을 보여 줘.");ch.setShowBadge(false);ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);m.createNotificationChannel(ch);return m;}
 static Notification.Action button(Context c,String label,String what,int code){return new Notification.Action.Builder(Icon.createWithResource(c,R.drawable.ic_sleep),label,action(c,what,code)).build();}

 static void notification(Context c){
  NotificationManager m=channel(c);SleepSession s=load(c);if(!s.active()){m.cancel(NOTE_ID);return;}
  long now=System.currentTimeMillis();SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.KOREA);
  Notification.Builder b=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_sleep).setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_STATUS).setContentIntent(openApp(c)).setColor(0xffaaa1ff);
  if(s.paused()){b.setContentTitle("수면 일시 중지").setContentText(f.format(new Date(s.pausedAt()))+"부터 깨어 있음 · 지금까지 "+Models.durationText(s.slept(now))).setShowWhen(false);
   b.addAction(button(c,"재개","resume",31));}
  else{b.setContentTitle("수면 기록 중").setContentText(f.format(new Date(s.start))+" 시작 · 깬 시간은 일시 중지로 빼").setUsesChronometer(true).setWhen(now-s.slept(now)).setShowWhen(true);
   b.addAction(button(c,"일시 중지","pause",32));}
  b.addAction(button(c,"기록 정지","stop",33));b.addAction(button(c,"다시 시작","restart",34));
  try{m.notify(NOTE_ID,b.build());}catch(SecurityException ignored){} // 알림 권한이 없으면 앱·위젯으로만 조작.
 }
 static void notifyFailure(Context c,String error){
  Notification n=new Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_sleep).setContentTitle("수면 기록을 저장하지 못했어").setContentText((error==null?"":error+" ")+"앱의 수면 탭에서 확인해 줘.").setContentIntent(openApp(c)).setAutoCancel(true).build();
  try{channel(c).notify(FAIL_ID,n);}catch(SecurityException ignored){}
 }
}
