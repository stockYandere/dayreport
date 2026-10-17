package com.dongwon.sleeptimeline;
import java.util.*;import java.lang.reflect.*;
public class RestoredUsageTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static Object bucket(List<Models.Session> sessions)throws Exception {Method m=DashboardView.class.getDeclaredMethod("buildHourlyBucket",Models.DayRecord.class,int.class);m.setAccessible(true);return m.invoke(null,new Models.DayRecord(1000000000L,sessions,new ArrayList<Models.AppTotal>(),0,0,0),0);}
 static Object field(Object obj,String name)throws Exception {Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
 public static void main(String[]args)throws Exception {
  Models.AppMeta a=new Models.AppMeta("a","A",1,null),b=new Models.AppMeta("b","B",2,null),c=new Models.AppMeta("c","C",3,null),d=new Models.AppMeta("d","D",4,null);long start=1000000000L;
  Object result=bucket(Arrays.asList(new Models.Session(a,start,start+1800000),new Models.Session(a,start+600000,start+1800000),new Models.Session(b,start+1800000,start+2700000)));
  check(((List)field(result,"apps")).size()==2,"Both qualifying apps retain their colored segments");check((Long)field(result,"totalDuration")==2700000L,"Overlapping sessions from same app counted once");check(!(Boolean)field(result,"fallbackTopThree"),"Normal threshold case");
  result=bucket(Arrays.asList(new Models.Session(a,start,start+300000),new Models.Session(b,start+300000,start+420000),new Models.Session(c,start+420000,start+480000),new Models.Session(d,start+480000,start+510000)));
  check(((List)field(result,"apps")).size()==3,"Top 3 retained when all apps under 10 minutes");check((Boolean)field(result,"fallbackTopThree"),"Fallback marker preserved");check((Long)field(result,"totalDuration")==480000L,"Fallback bars retain actual duration");
  result=bucket(Collections.<Models.Session>emptyList());check(((List)field(result,"apps")).isEmpty(),"Empty hour does not fabricate usage");
  Method m=DashboardView.class.getDeclaredMethod("filteredApps",Models.DayRecord.class);m.setAccessible(true);List<Models.AppTotal> totals=Arrays.asList(new Models.AppTotal(a,600000,new ArrayList<Models.Session>()),new Models.AppTotal(b,599999,new ArrayList<Models.Session>()));List selected=(List)m.invoke(null,new Models.DayRecord(start,new ArrayList<Models.Session>(),totals,0,0,0));check(selected.size()==1,"10 minute list boundary");System.out.println("PASS: 8 restored original usage aggregation checks");
 }
}
