package com.dongwon.sleeptimeline;
import android.content.*;import android.database.*;import android.database.sqlite.*;import org.json.*;
/* 일기: 날짜(yyyy-MM-dd, 기기 시간대 기준)마다 하나. 같은 날은 계속 고쳐 쓰고 덧붙인다. 빈 글로 저장하면 그날 일기를 지운다. */
public class DiaryStore extends SQLiteOpenHelper {
 static final int MAX_TEXT=50000;
 private static DiaryStore shared;
 public static synchronized DiaryStore get(Context c){if(shared==null)shared=new DiaryStore(c.getApplicationContext());return shared;}
 private DiaryStore(Context c){super(c,"diary.db",null,1);}
 public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE entries(day TEXT PRIMARY KEY,text TEXT NOT NULL,created INTEGER NOT NULL,updated INTEGER NOT NULL)");}
 public void onUpgrade(SQLiteDatabase d,int a,int b){}
 static boolean validDay(String day){return day!=null&&day.matches("\\d{4}-\\d{2}-\\d{2}");}
 static String preview(String text){String t=text.trim();int n=t.indexOf('\n');if(n>=0)t=t.substring(0,n);return t.length()>80?t.substring(0,80):t;}
 public synchronized JSONObject get(String day)throws Exception{if(!validDay(day))throw new Exception("날짜가 올바르지 않아.");JSONObject o=new JSONObject().put("day",day).put("text","").put("updated",0);Cursor c=getReadableDatabase().rawQuery("SELECT text,updated FROM entries WHERE day=?",new String[]{day});try{if(c.moveToFirst())o.put("text",c.getString(0)).put("updated",c.getLong(1));}finally{c.close();}return o;}
 public synchronized void save(String day,String text,long now)throws Exception{
  if(!validDay(day))throw new Exception("날짜가 올바르지 않아.");if(text==null)text="";if(text.length()>MAX_TEXT)throw new Exception("일기는 하루 "+MAX_TEXT+"자까지 쓸 수 있어.");
  SQLiteDatabase d=getWritableDatabase();if(text.trim().isEmpty()){d.delete("entries","day=?",new String[]{day});return;}
  ContentValues v=new ContentValues();v.put("text",text);v.put("updated",now);
  if(d.update("entries",v,"day=?",new String[]{day})==0){v.put("day",day);v.put("created",now);d.insertOrThrow("entries",null,v);}
 }
 /* 모아보기: 최근 날짜부터. 본문 대신 첫 줄과 글자 수만. */
 public synchronized JSONArray list()throws JSONException{JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT day,text,updated FROM entries ORDER BY day DESC",null);try{while(c.moveToNext()){String t=c.getString(1);a.put(new JSONObject().put("day",c.getString(0)).put("preview",preview(t)).put("length",t.length()).put("updated",c.getLong(2)));}}finally{c.close();}return a;}
 public synchronized JSONArray all()throws JSONException{JSONArray a=new JSONArray();Cursor c=getReadableDatabase().rawQuery("SELECT day,text,created,updated FROM entries ORDER BY day ASC",null);try{while(c.moveToNext())a.put(new JSONObject().put("day",c.getString(0)).put("text",c.getString(1)).put("created",c.getLong(2)).put("updated",c.getLong(3)));}finally{c.close();}return a;}
 /* 백업 복원: 이미 일기가 있는 날은 덮어쓰지 않고 건너뛴다. */
 public synchronized int restore(JSONArray rows)throws Exception{if(rows.length()>20000)throw new Exception("백업 일기가 너무 많아.");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!validDay(r.optString("day"))||r.optString("text").length()>MAX_TEXT)throw new Exception("백업 안에 올바르지 않은 일기가 있어.");}
  SQLiteDatabase d=getWritableDatabase();int count=0;d.beginTransaction();try{for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String text=r.optString("text");if(text.trim().isEmpty())continue;ContentValues v=new ContentValues();v.put("day",r.getString("day"));v.put("text",text);v.put("created",r.optLong("created",r.optLong("updated",System.currentTimeMillis())));v.put("updated",r.optLong("updated",System.currentTimeMillis()));if(d.insertWithOnConflict("entries",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1)count++;}d.setTransactionSuccessful();}finally{d.endTransaction();}return count;}
}
