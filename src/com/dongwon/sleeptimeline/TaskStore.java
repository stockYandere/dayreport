package com.dongwon.sleeptimeline;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;import java.util.*;
/* 할 일 / 한 일. open → done(완료) 또는 skipped(안 함). finished는 완료·안 함 처리 시각. */
public class TaskStore extends SQLiteOpenHelper {
 static final int MAX_TITLE=200;
 private static TaskStore shared;
 public static synchronized TaskStore get(Context c){if(shared==null)shared=new TaskStore(c.getApplicationContext());return shared;}
 private TaskStore(Context c){super(c,"tasks.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE tasks(id INTEGER PRIMARY KEY,title TEXT NOT NULL,created INTEGER NOT NULL,state TEXT NOT NULL,finished INTEGER NOT NULL DEFAULT 0)");d.execSQL("CREATE INDEX tasks_finished ON tasks(state,finished)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 static String clean(String title)throws Exception{String t=title==null?"":title.replaceAll("\\s+"," ").trim();if(t.isEmpty())throw new Exception("할 일을 입력해 줘.");if(t.length()>MAX_TITLE)throw new Exception("할 일은 "+MAX_TITLE+"자까지 입력할 수 있어.");return t;}
 static boolean validState(String s){return "open".equals(s)||"done".equals(s)||"skipped".equals(s);}
 private static JSONObject row(Cursor c)throws JSONException{return new JSONObject().put("id",c.getLong(0)).put("title",c.getString(1)).put("created",c.getLong(2)).put("state",c.getString(3)).put("finished",c.getLong(4));}
 public synchronized long add(String title,long now)throws Exception{ContentValues v=new ContentValues();v.put("title",clean(title));v.put("created",now);v.put("state","open");v.put("finished",0);return getWritableDatabase().insertOrThrow("tasks",null,v);}
 /* 할 일 목록을 거치지 않고 바로 '한 일'로 남긴다. */
 public synchronized long addDone(String title,long time,long now)throws Exception{if(time<=0||time>now+60000)throw new Exception("미래 시간은 기록할 수 없어.");ContentValues v=new ContentValues();v.put("title",clean(title));v.put("created",time);v.put("state","done");v.put("finished",time);return getWritableDatabase().insertOrThrow("tasks",null,v);}
 public synchronized void setState(long id,String state,long now)throws Exception{if(!validState(state))throw new Exception("상태가 올바르지 않아.");ContentValues v=new ContentValues();v.put("state",state);v.put("finished","open".equals(state)?0:now);if(getWritableDatabase().update("tasks",v,"id=?",new String[]{""+id})!=1)throw new Exception("이 할 일을 찾을 수 없어.");}
 public synchronized void edit(long id,String title,long finished,long now)throws Exception{Cursor c=getReadableDatabase().rawQuery("SELECT state FROM tasks WHERE id=?",new String[]{""+id});String state;try{if(!c.moveToFirst())throw new Exception("이 할 일을 찾을 수 없어.");state=c.getString(0);}finally{c.close();}ContentValues v=new ContentValues();v.put("title",clean(title));if(!"open".equals(state)&&finished>0){if(finished>now+60000)throw new Exception("미래 시간은 기록할 수 없어.");v.put("finished",finished);}getWritableDatabase().update("tasks",v,"id=?",new String[]{""+id});}
 public synchronized void remove(long id){getWritableDatabase().delete("tasks","id=?",new String[]{""+id});}
 public synchronized JSONArray open()throws JSONException{return query("SELECT id,title,created,state,finished FROM tasks WHERE state='open' ORDER BY created ASC,id ASC",new String[0]);}
 /* start 이상 end 미만에 완료·안 함 처리한 기록. */
 public synchronized JSONArray finished(long start,long end)throws JSONException{return query("SELECT id,title,created,state,finished FROM tasks WHERE state!='open' AND finished>=? AND finished<? ORDER BY finished DESC,id DESC",new String[]{""+start,""+end});}
 public synchronized int doneCount(long start,long end){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tasks WHERE state='done' AND finished>=? AND finished<?",new String[]{""+start,""+end});try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
 public synchronized JSONArray all()throws JSONException{return query("SELECT id,title,created,state,finished FROM tasks ORDER BY created ASC,id ASC",new String[0]);}
 private JSONArray query(String sql,String[] args)throws JSONException{JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery(sql,args);try{while(c.moveToNext())a.put(row(c));}finally{c.close();}return a;}
 /* 백업 복원: 같은 제목·생성 시각 기록은 건너뛰고 기존 기록은 덮어쓰지 않는다. */
 public synchronized int restore(JSONArray rows,long now)throws Exception{if(rows.length()>20000)throw new Exception("백업 할 일이 너무 많아.");List<ContentValues> clean=new ArrayList<ContentValues>();for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String state=r.optString("state");long created=r.optLong("created"),finished=r.optLong("finished");if(!validState(state)||created<=0||created>now+60000||("open".equals(state)?finished!=0:finished<=0||finished>now+60000))throw new Exception("백업 안에 올바르지 않은 할 일이 있어.");ContentValues v=new ContentValues();v.put("title",clean(r.optString("title")));v.put("created",created);v.put("state",state);v.put("finished",finished);clean.add(v);}
  SQLiteDatabase d=getWritableDatabase();int count=0;d.beginTransaction();try{for(ContentValues v:clean){Cursor c=d.rawQuery("SELECT id FROM tasks WHERE title=? AND created=?",new String[]{v.getAsString("title"),""+v.getAsLong("created")});boolean exists;try{exists=c.moveToFirst();}finally{c.close();}if(exists)continue;d.insertOrThrow("tasks",null,v);count++;}d.setTransactionSuccessful();}finally{d.endTransaction();}return count;}
 static long dayStart(long time){Calendar c=Calendar.getInstance();c.setTimeInMillis(time);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
 static long nextDay(long dayStart){Calendar c=Calendar.getInstance();c.setTimeInMillis(dayStart);c.add(Calendar.DATE,1);return c.getTimeInMillis();}
}
