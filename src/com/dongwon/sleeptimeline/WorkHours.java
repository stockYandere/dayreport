package com.dongwon.sleeptimeline;
import android.content.Context;import org.json.*;
// 근무 시간: 요일마다 구간 하나. 근무 중은 수면 후보로 만들지 않는다.
final class WorkHours {
 static final String KEY="workHours";
 // 월=0 … 일=6, {켜짐, 시작 분, 끝 분}. 기본값: 수–금 00:00–08:00, 토–일 00:00–10:00.
 static final int[][] DEFAULT={{0,0,480},{0,0,480},{1,0,480},{1,0,480},{1,0,480},{1,0,600},{1,0,600}};
 static int[][] load(Context c){try{String s=c.getSharedPreferences("settings",0).getString(KEY,null);if(s!=null)return parse(new JSONArray(s));}catch(Exception ignored){}int[][] out=new int[7][];for(int i=0;i<7;i++)out[i]=DEFAULT[i].clone();return out;}
 static int[][] parse(JSONArray a)throws Exception{if(a.length()!=7)throw new Exception("요일 7개를 모두 입력해 줘.");int[][] out=new int[7][];for(int i=0;i<7;i++){JSONObject o=a.getJSONObject(i);boolean on=o.getBoolean("on");int s=o.getInt("start"),e=o.getInt("end");if(s<0||s>=1440||e<0||e>=1440)throw new Exception("시각이 올바르지 않아.");if(on&&s==e)throw new Exception("근무하는 요일은 시작과 끝을 다르게 입력해 줘.");out[i]=new int[]{on?1:0,s,e};}return out;}
 static JSONArray json(int[][] w)throws JSONException{JSONArray a=new JSONArray();for(int[] d:w)a.put(new JSONObject().put("on",d[0]==1).put("start",d[1]).put("end",d[2]));return a;}
 static void save(Context c,String days)throws Exception{c.getSharedPreferences("settings",0).edit().putString(KEY,json(parse(new JSONArray(days))).toString()).commit();}
}
