package com.dongwon.sleeptimeline;
import android.app.*;import android.content.*;import android.graphics.Color;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.webkit.*;import android.widget.*;import java.io.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
public class MainActivity extends Activity implements DashboardView.Actions {
 WebView web;Store store;String exportText;DashboardView dashboard;UsageRepository repository;
 final ExecutorService executor=Executors.newSingleThreadExecutor();int generation;boolean webReady;List<Models.DayRecord> nativeDays=new ArrayList<Models.DayRecord>();
 public void onCreate(Bundle state){
  super.onCreate(state);store=new Store(this);repository=new UsageRepository(this);
  getWindow().setStatusBarColor(Color.rgb(9,10,16));getWindow().setNavigationBarColor(Color.rgb(9,10,16));
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
  final FrameLayout host=new FrameLayout(this);host.setBackgroundColor(Color.rgb(9,10,16));
  dashboard=new DashboardView(this,this);host.addView(dashboard);
  web=new WebView(this);web.setBackgroundColor(Color.TRANSPARENT);host.addView(web);web.setVisibility(View.GONE);setContentView(host);
  host.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener(){public WindowInsets onApplyWindowInsets(View v,WindowInsets in){
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()),ime=in.getInsets(WindowInsets.Type.ime());host.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));}
   else host.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());
   return in;
  }});host.requestApplyInsets();
  WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(true);settings.setAllowContentAccess(false);settings.setAllowFileAccessFromFileURLs(false);settings.setAllowUniversalAccessFromFileURLs(false);settings.setTextZoom(100);web.setOverScrollMode(View.OVER_SCROLL_NEVER);
  web.setWebViewClient(new WebViewClient(){public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return !r.getUrl().toString().startsWith("file:///android_asset/");}public void onPageFinished(WebView w,String url){webReady=true;w.evaluateJavascript("window.startApp&&startApp()",null);}});
  web.addJavascriptInterface(new Bridge(),"Native");web.loadUrl("file:///android_asset/index.html");CaptureJob.schedule(this);
 }
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
 public void openSleepPage(){if(!webReady)return;dashboard.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);web.evaluateJavascript("openSleepPage()",null);}
 public void showSleepCandidates(long day,long start,long end){
  if(!webReady)return;
  List<SleepLogic.Gap> use=new ArrayList<SleepLogic.Gap>();for(Models.DayRecord row:nativeDays)for(Models.Session session:row.sessions)use.add(new SleepLogic.Gap(session.start,session.end));
  Calendar c=Calendar.getInstance();c.setTimeInMillis(day);c.add(Calendar.DATE,1);
  List<SleepLogic.Gap> gaps=SleepLogic.unusedGaps(use,day,c.getTimeInMillis(),5L*3600000);
  JSONArray items=new JSONArray();try{for(SleepLogic.Gap gap:gaps)if(!store.isDismissed(gap.start,gap.end))items.put(new JSONObject().put("start",gap.start).put("end",gap.end));}catch(Exception ignored){}
  web.setVisibility(View.VISIBLE);web.evaluateJavascript("openNativeCandidates("+day+","+items.toString()+")",null);
 }
 void showDashboard(final int page){runOnUiThread(new Runnable(){public void run(){web.setVisibility(View.GONE);dashboard.setVisibility(View.VISIBLE);if(page>=0)dashboard.setPage(page);refreshData();}});}
 public void showAppsAt(long hour,List<Models.Session> sessions,boolean fallback){
  Map<String,Long> durations=new LinkedHashMap<String,Long>();Map<String,String> names=new HashMap<String,String>();
  for(Models.Session session:sessions){long n=Math.max(0,Math.min(session.end,hour+3600000)-Math.max(session.start,hour));durations.put(session.app.packageName,durations.containsKey(session.app.packageName)?durations.get(session.app.packageName)+n:n);names.put(session.app.packageName,session.app.label);}
  StringBuilder text=new StringBuilder();if(fallback)text.append("10분 이상 앱이 없어 상위 3개를 표시해.\n\n");
  for(Map.Entry<String,Long> row:durations.entrySet())text.append("• ").append(names.get(row.getKey())).append(" · ").append(Math.max(1,Math.round(row.getValue()/60000.0))).append("분\n");
  if(durations.isEmpty())text.append("이 시간에는 사용 기록이 없어.");
  TextView view=new TextView(this);view.setText(text.toString().trim());view.setTextColor(Color.rgb(237,240,250));view.setTextSize(15);int padding=(int)(getResources().getDisplayMetrics().density*20);view.setPadding(padding,0,padding,0);
  String label=new java.text.SimpleDateFormat("HH:mm",Locale.KOREA).format(new Date(hour));
  new AlertDialog.Builder(this).setTitle(label+" 사용 앱").setView(view).setPositiveButton("확인",null).show();
 }
 void notifyUI(final String text){runOnUiThread(new Runnable(){public void run(){web.evaluateJavascript("window.nativeNotice&&nativeNotice("+JSONObject.quote(text)+")",null);}});}
 String error(Exception e){return new JSONObjectResult(false,e.getMessage()).text();}
 static class JSONObjectResult{JSONObject j=new JSONObject();JSONObjectResult(boolean ok,String msg){try{j.put("ok",ok).put("message",msg);}catch(Exception ignored){}}String text(){return j.toString();}}
 public class Bridge {
  @JavascriptInterface public void dashboard(int page){showDashboard(page);}
  @JavascriptInterface public void closeNativeEditor(){showDashboard(-1);}
  @JavascriptInterface public String refresh(){try{int events=UsageReader.capture(MainActivity.this);return new JSONObject().put("ok",true).put("permission",UsageReader.allowed(MainActivity.this)).put("records",store.all()).put("minimum",300).put("timer",getSharedPreferences("settings",0).getLong("timer",0)).put("events",events).put("now",System.currentTimeMillis()).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String usage(long date){try{return UsageReader.usage(MainActivity.this,date).toString();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String save(String data){try{store.save(new JSONObject(data));return new JSONObjectResult(true,"저장했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public String remove(long id){try{store.remove(id);return new JSONObjectResult(true,"처리했어").text();}catch(Exception e){return error(e);}}
  @JavascriptInterface public void minimum(int min){getSharedPreferences("settings",0).edit().putInt("minimum",300).apply();}
  @JavascriptInterface public void timer(long time){if(time>=0&&time<=System.currentTimeMillis())getSharedPreferences("settings",0).edit().putLong("timer",time).commit();}
  @JavascriptInterface public void permission(){runOnUiThread(new Runnable(){public void run(){openUsageAccessSettings();}});}
  @JavascriptInterface public void export(){runOnUiThread(new Runnable(){public void run(){try{exportText=store.backup().toString(2);Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"SleepTimeline-"+new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(new java.util.Date())+".json");startActivityForResult(i,10);}catch(Exception e){notifyUI("백업 파일을 만들지 못했어.");}}});}
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
     notifyUI("수면 기록 백업을 저장했어.");
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
     final int count=new JSONObject(input).getJSONArray("records").length();
     runOnUiThread(new Runnable(){public void run(){
      new AlertDialog.Builder(MainActivity.this).setTitle("수면 기록 복원")
       .setMessage(count+"개 기록을 현재 기록에 추가할게. 같은 기록은 건너뛰고, 시간이 겹치면 복원을 중단해.")
       .setNegativeButton("취소",null)
       .setPositiveButton("복원",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
        new Thread(new Runnable(){public void run(){
         try {notifyUI(store.restore(input)+"개 기록을 복원했어.");}
         catch(Exception e){notifyUI(e.getMessage());}
        }}).start();
       }}).show();
     }});
    }
   } catch(Exception e) {notifyUI(e.getMessage()==null?"파일을 읽거나 저장하지 못했어.":e.getMessage());}
  }}).start();
 }
}
