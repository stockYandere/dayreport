package com.dongwon.sleeptimeline;
import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.webkit.*;import android.widget.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
public class MainActivity extends Activity implements DashboardView.Actions {
 static final String EXTRA_ROUTE="route";
 WebView web;Store store;TaskStore tasks;String exportText;DashboardView dashboard;UsageRepository repository;String pendingRoute;
 final ExecutorService executor=Executors.newSingleThreadExecutor();int generation;boolean webReady;List<Models.DayRecord> nativeDays=new ArrayList<Models.DayRecord>();
 public void onCreate(Bundle state){
  super.onCreate(state);store=new Store(this);tasks=TaskStore.get(this);repository=new UsageRepository(this);pendingRoute=route(getIntent());
  getWindow().setStatusBarColor(Color.rgb(9,10,16));getWindow().setNavigationBarColor(Color.rgb(9,10,16));
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  final FrameLayout host=new FrameLayout(this);host.setBackgroundColor(Color.rgb(9,10,16));
  // 첫 화면은 시간대 그래프. 위젯에서 열면(route 있음) 하루 리포트.
  dashboard=new DashboardView(this,this);host.addView(dashboard);
  web=new WebView(this);web.setBackgroundColor(Color.TRANSPARENT);host.addView(web);setContentView(host);
  dashboard.setVisibility(pendingRoute==null?View.VISIBLE:View.GONE);web.setVisibility(pendingRoute==null?View.GONE:View.VISIBLE);
  host.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener(){public WindowInsets onApplyWindowInsets(View v,WindowInsets in){
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()),ime=in.getInsets(WindowInsets.Type.ime());host.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));}
   else host.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());
   return in;
  }});host.requestApplyInsets();
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(true);settings.setAllowContentAccess(false);settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);settings.setTextZoom(100);web.setOverScrollMode(View.OVER_SCROLL_NEVER);
  web.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return !r.getUrl().toString().startsWith("file:///android_asset/");}public void onPageFinished(WebView w,String url){webReady=true;w.evaluateJavascript("window.startApp&&startApp("+JSONObject.quote(pendingRoute==null?"report":pendingRoute)+")",null);pendingRoute=null;}});
  web.addJavascriptInterface(new Bridge(),"Native");web.loadUrl("file:///android_asset/index.html");CaptureJob.schedule(this);TodoWidget.scheduleMidnight(this);
 }
 static String route(Intent i){String r=i==null?null:i.getStringExtra(EXTRA_ROUTE);return r;}
 protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String r=route(intent);if(r==null)return;if(!webReady){pendingRoute=r;dashboard.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);return;}dashboard.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);web.evaluateJavascript("openRoute("+JSONObject.quote(r)+")",null);}
 protected void onResume(){super.onResume();if(dashboard!=null)refreshData();if(web!=null&&webReady)web.evaluateJavascript("window.refresh&&refresh()",null);}
 public void onBackPressed(){if(web.getVisibility()==View.VISIBLE)web.evaluateJavascript("window.back&&back()",null);else if(!dashboard.backToTimeline())finish();}
 protected void onDestroy(){generation++;executor.shutdownNow();if(web!=null){web.removeJavascriptInterface("Native");web.destroy();}if(store!=null)store.close();super.onDestroy();}
 public void openUsageAccessSettings(){try{startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}}
 public void refreshData(){
  dashboard.setPermissionGranted(UsageReader.allowed(this));
  if(!UsageReader.allowed(this))return;
  final int token=++generation;dashboard.setLoading(true);
  executor.execute(new Runnable(){public void run(){try{
   final List<Models.DayRecord> days=repository.loadSevenDays();final JSONArray records=store.all();
   runOnUiThread(new Runnable(){public void run(){if(token!=generation||isDestroyed())return;nativeDays=days;dashboard.setData(days);dashboard.setSleepRecords(records);}});
  }catch(final Exception e){runOnUiThread(new Runnable(){public void run(){if(token!=generation||isDestroyed())return;dashboard.setError("기록을 읽지 못했어. 사용정보 접근을 확인한 뒤 새로고침해 줘.");}});}}});
 }
 public void openWebPage(String tab){if(!webReady)return;dashboard.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);web.evaluateJavascript("openRoute("+JSONObject.quote(tab)+")",null);}
 public void showSleepCandidates(long day,long start,long end){
  if(!webReady)return;
  List<SleepLogic.Gap> use=new ArrayList<SleepLogic.Gap>();for(Models.DayRecord row:nativeDays)for(Models.Session session:row.sessions)use.add(new SleepLogic.Gap(session.start,session.end));
  Calendar c=Calendar.getInstance();c.setTimeInMillis(day);c.add(Calendar.DATE,1);
  List<SleepLogic.Gap> gaps=SleepLogic.unusedGaps(use,day,c.getTimeInMillis(),SleepLogic.MIN_CANDIDATE);
  JSONArray items=new JSONArray();try{for(SleepLogic.Gap gap:gaps)if(!store.isDismissed(gap.start,gap.end))items.put(new JSONObject().put("start",gap.start).put("end",gap.end));}catch(Exception ignored){}
  web.setVisibility(View.VISIBLE);web.evaluateJavascript("openNativeCandidates("+day+","+items.toString()+")",null);
 }
 void showDashboard(final int page){showDashboard(page,-1);}
 void showDashboard(final int page,final long day){runOnUiThread(new Runnable(){public void run(){web.setVisibility(View.GONE);dashboard.setVisibility(View.VISIBLE);if(page>=0)dashboard.setPage(page);if(day>0)dashboard.selectDayStart(day);refreshData();}});}
 static final long BLOCK_GAP=2*60000L;
 // 막대를 누르면 그 시간의 사용을 묶음 단위로 보여 준다. 2분 안에 이어서 쓴 앱들은 한 줄.
 public void showAppsAt(long hour,List<Models.Session> sessions){
  java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("HH:mm",Locale.KOREA);
  List<Models.UseBlock> blocks=Models.blocks(sessions,hour,hour+3600000,BLOCK_GAP);
  String note=blocks.isEmpty()?null:"총 "+duration(Models.union(sessions,hour,hour+3600000))+" · "+blocks.size()+"번 사용 · 2분 안에 이어 쓴 앱은 한 줄로 묶어";
  blockDialog(f.format(new Date(hour))+"–"+f.format(new Date(hour+3600000))+" 사용",blocks,note,"이 시간에는 사용 기록이 없어.");
 }
 public void showAppSessions(Models.AppTotal total,long day){
  String date=new java.text.SimpleDateFormat("M월 d일",Locale.KOREA).format(new Date(day));
  List<Models.UseBlock> blocks=Models.blocks(total.sessions,Long.MIN_VALUE,Long.MAX_VALUE,BLOCK_GAP);
  blockDialog(total.app.label,blocks,date+" · "+blocks.size()+"번 사용 · 총 "+duration(total.duration),null);
 }
 // 1분 미만은 초로 적는다. 짧게 스친 사용이 1분으로 부풀려 보이지 않게.
 static String duration(long ms){if(ms<60000)return Math.max(1,Math.round(ms/1000.0))+"초";long m=Math.round(ms/60000.0);return m>=60?(m/60)+"시간"+(m%60>0?" "+(m%60)+"분":""):m+"분";}
 static String range(java.text.SimpleDateFormat f,long start,long end){String a=f.format(new Date(start)),b=f.format(new Date(end));return a.equals(b)?a:a+"–"+b;}
 /* 한 줄: ● 14:03–14:21  18분. 여러 앱이면 아래 줄에 앱별 시간(많이 쓴 순, 3개까지). */
 void blockDialog(String title,List<Models.UseBlock> blocks,String note,String empty){
  java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("HH:mm",Locale.KOREA);float d=getResources().getDisplayMetrics().density;
  android.text.SpannableStringBuilder text=new android.text.SpannableStringBuilder();
  if(note!=null){int s0=text.length();text.append(note).append("\n\n");text.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(159,170,193)),s0,text.length(),0);text.setSpan(new android.text.style.RelativeSizeSpan(.85f),s0,text.length(),0);}
  if(blocks.isEmpty()&&empty!=null)text.append(empty);
  for(Models.UseBlock b:blocks){
   Models.AppMeta main=b.apps.get(0).app;
   int s0=text.length();text.append("● ");text.setSpan(new android.text.style.ForegroundColorSpan(main.color|0xff000000),s0,s0+1,0);
   int t0=text.length();text.append(range(f,b.start,b.end));text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),t0,text.length(),0);text.setSpan(new android.text.style.TypefaceSpan("monospace"),t0,text.length(),0);
   if(b.apps.size()==1){text.append("  ").append(main.label);}
   int m0=text.length();text.append("  ").append(duration(b.duration)).append("\n");text.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(120,167,255)),m0,text.length(),0);
   if(b.apps.size()>1){
    StringBuilder line=new StringBuilder("    ");for(int n=0;n<Math.min(3,b.apps.size());n++){if(n>0)line.append(" · ");line.append(b.apps.get(n).app.label).append(' ').append(duration(b.apps.get(n).duration));}
    if(b.apps.size()>3)line.append(" 외 ").append(b.apps.size()-3).append("개");
    int l0=text.length();text.append(line).append("\n");text.setSpan(new android.text.style.ForegroundColorSpan(Color.rgb(159,170,193)),l0,text.length(),0);text.setSpan(new android.text.style.RelativeSizeSpan(.85f),l0,text.length(),0);
   }
  }
  TextView view=new TextView(this);view.setText(text);view.setTextColor(Color.rgb(237,240,250));view.setTextSize(15);view.setLineSpacing(0,1.25f);int padding=(int)(d*22);view.setPadding(padding,(int)(d*6),padding,0);
  ScrollView scroll=new ScrollView(this);scroll.addView(view);
  new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("확인",null).show();
 }
 void notifyUI(final String text){runOnUiThread(new Runnable(){public void run(){web.evaluateJavascript("window.nativeNotice&&nativeNotice("+JSONObject.quote(text)+")",null);}});}
 String error(Exception e){return new JSONObjectResult(false,e.getMessage()).text();}
 static class JSONObjectResult{JSONObject j=new JSONObject();JSONObjectResult(boolean ok,String msg){try{j.put("ok",ok).put("message",msg);}catch(Exception ignored){}}String text(){return j.toString();}}
 /* 하루 리포트: 앱 사용(최근 7일만 조회 가능) + 그날 완료·안 함 처리한 일. 수면은 웹 쪽 기록으로 계산한다. */
 JSONObject report(long day)throws Exception{
  long start=TaskStore.dayStart(day),end=TaskStore.nextDay(start),now=System.currentTimeMillis();
  Calendar oldest=Calendar.getInstance();oldest.setTimeInMillis(TaskStore.dayStart(now));oldest.add(Calendar.DATE,-6);
  boolean permission=UsageReader.allowed(this);JSONObject o=new JSONObject().put("ok",true).put("day",start).put("permission",permission).put("now",now);
  JSONArray apps=new JSONArray(),spans=new JSONArray();
  if(permission&&start>=oldest.getTimeInMillis()&&start<=now){
   Models.DayRecord r=repository.loadDay(start);
   for(int n=0;n<Math.min(5,r.totals.size());n++){Models.AppTotal a=r.totals.get(n);apps.put(new JSONObject().put("name",a.app.label).put("ms",a.duration).put("count",a.sessions.size()).put("color",String.format(Locale.US,"#%06X",a.app.color&0xffffff)));}
   long s=-1,e=-1;for(Models.Session x:r.sessions){if(s<0){s=x.start;e=x.end;}else if(x.start<=e)e=Math.max(e,x.end);else{spans.put(new JSONArray().put(s).put(e));s=x.start;e=x.end;}}if(s>=0)spans.put(new JSONArray().put(s).put(e));
   o.put("available",true).put("total",r.uniqueDuration).put("first",r.firstUse).put("last",r.lastUse).put("apps",apps).put("spans",spans).put("appCount",r.totals.size());
  }else o.put("available",false).put("apps",apps).put("spans",spans);
  return o.put("tasks",tasks.finished(start,end));
 }
 public class Bridge {
  @JavascriptInterface public void dashboard(int page){showDashboard(page);}
  @JavascriptInterface public void dashboardAt(int page,long day){showDashboard(page,day);}
  @JavascriptInterface public String dayReport(long day){try{return report(day).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String tasks(long from){try{long now=System.currentTimeMillis(),today=TaskStore.dayStart(now);return new JSONObject().put("ok",true).put("open",tasks.open()).put("finished",tasks.finished(TaskStore.dayStart(from),TaskStore.nextDay(today))).put("today",tasks.doneCount(today,TaskStore.nextDay(today))).put("now",now).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String addDone(String title,long time){try{tasks.addDone(title,time,System.currentTimeMillis());TodoWidget.refreshAll(MainActivity.this);return new JSONObjectResult(true,"한 일을 기록했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String taskState(long id,String state){try{tasks.setState(id,state,System.currentTimeMillis());TodoWidget.refreshAll(MainActivity.this);return new JSONObjectResult(true,"처리했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String editTask(long id,String title,long finished){try{tasks.edit(id,title,finished,System.currentTimeMillis());TodoWidget.refreshAll(MainActivity.this);return new JSONObjectResult(true,"저장했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String deleteTask(long id){try{tasks.remove(id);TodoWidget.refreshAll(MainActivity.this);return new JSONObjectResult(true,"삭제했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public void closeNativeEditor(){showDashboard(-1);}
  @JavascriptInterface public String refresh(){try{int events=UsageReader.capture(MainActivity.this);return new JSONObject().put("ok",true).put("permission",UsageReader.allowed(MainActivity.this)).put("records",store.all()).put("minimum",SleepLogic.MIN_CANDIDATE/60000).put("timer",getSharedPreferences("settings",0).getLong("timer",0)).put("events",events).put("now",System.currentTimeMillis()).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String usage(long date){try{return UsageReader.usage(MainActivity.this,date).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String save(String data){try{store.save(new JSONObject(data));return new JSONObjectResult(true,"저장했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String remove(long id){try{store.remove(id);return new JSONObjectResult(true,"처리했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public void minimum(int min){getSharedPreferences("settings",0).edit().putInt("minimum",(int)(SleepLogic.MIN_CANDIDATE/60000)).apply();}
  @JavascriptInterface public void timer(long time){if(time>=0&&time<=System.currentTimeMillis())getSharedPreferences("settings",0).edit().putLong("timer",time).commit();}
  @JavascriptInterface public void permission(){runOnUiThread(new Runnable(){public void run(){openUsageAccessSettings();}});}
  @JavascriptInterface public void export(){runOnUiThread(new Runnable(){public void run(){try{exportText=store.backup().put("tasks",tasks.all()).toString(2);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"dayreport-"+new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date())+".json");startActivityForResult(i,10);}catch(Exception e){notifyUI("백업 파일을 만들지 못했어.");}}});}
  @JavascriptInterface public void restore(){runOnUiThread(new Runnable(){public void run(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,11);}});}
  @JavascriptInterface public void exit(){runOnUiThread(new Runnable(){public void run(){finish();}});}
 }
 protected void onActivityResult(int request,int result,Intent data) {
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null||data.getData()==null)return;
  final Uri uri=data.getData(); final int action=request;
  new Thread(new Runnable(){public void run(){
   try {
    if(action==10) {
     OutputStream stream=getContentResolver().openOutputStream(uri,"wt");
     if(stream==null)throw new IOException();
     try { stream.write(exportText.getBytes("UTF-8")); } finally { stream.close(); }
     notifyUI("수면·할 일 백업을 저장했어.");
    } else if(action==11) {
     InputStream stream=getContentResolver().openInputStream(uri);
     if(stream==null)throw new IOException();
     ByteArrayOutputStream bytes=new ByteArrayOutputStream();
     try { byte[] buf=new byte[8192]; int n;
      while((n=stream.read(buf))!=-1) {
       if(bytes.size()+n>2*1024*1024)throw new IOException("백업 파일은 2MB까지 읽을 수 있어.");
       bytes.write(buf,0,n);
      }
     } finally {stream.close();}
     final String input=bytes.toString("UTF-8");
     JSONObject parsed=new JSONObject(input);final int count=parsed.getJSONArray("records").length();final JSONArray taskRows=parsed.optJSONArray("tasks");
     runOnUiThread(new Runnable(){public void run(){
      new AlertDialog.Builder(MainActivity.this).setTitle("백업 복원")
       .setMessage("수면 "+count+"개"+(taskRows==null?"":", 할 일 "+taskRows.length()+"개")+"를 현재 기록에 추가할게. 같은 기록은 건너뛰고, 수면 시간이 겹치면 복원을 중단해.")
       .setNegativeButton("취소",null)
       .setPositiveButton("복원",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
        new Thread(new Runnable(){public void run(){
         try {int sleep=store.restore(input);int done=taskRows==null?0:tasks.restore(taskRows,System.currentTimeMillis());TodoWidget.refreshAll(MainActivity.this);notifyUI("수면 "+sleep+"개"+(taskRows==null?"":", 할 일 "+done+"개")+"를 복원했어.");}
         catch(Exception e){notifyUI(e.getMessage());}
        }}).start();
       }}).show();
     }});
    }
   } catch(Exception e) {notifyUI(e.getMessage()==null?"파일을 읽거나 저장하지 못했어.":e.getMessage());}
  }}).start();
 }
}
