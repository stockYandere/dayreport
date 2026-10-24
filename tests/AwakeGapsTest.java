package com.dongwon.sleeptimeline;
import java.util.*;
public class AwakeGapsTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static SleepLogic.Event e(long t,int type,String pkg){return new SleepLogic.Event(t,type,pkg);}
 public static void main(String[]args){
  long h=3600000L,m=60000L,day=1000000000000L,from=day-24*h;
  // 야간 근무 후: 10:00–10:33 Claude, 10:34 화면 꺼짐, 13:00 알림으로 잠금 화면(System UI)만 잠깐, 17:02 dayreport 열기
  List<SleepLogic.Event> ev=new ArrayList<>(Arrays.asList(e(day+10*h,1,"claude"),e(day+10*h+33*m,2,"claude"),e(day+10*h+34*m,16,""),e(day+13*h,1,"com.android.systemui"),e(day+13*h+5000,2,"com.android.systemui"),e(day+17*h+2*m,1,"com.dongwon.sleeptimeline")));
  long now=day+17*h+2*m+30000;
  List<SleepLogic.Gap> g=SleepLogic.awakeGaps(ev,from,now,false);
  check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==day+17*h+2*m,"Daytime sleep 10:33→17:02 found; lock-screen notification does not end it; opening this app does");
  List<SleepLogic.Event> noOwn=new ArrayList<>(ev.subList(0,5));
  check(SleepLogic.awakeGaps(noOwn,from,now,false).isEmpty(),"Without any later use the gap stays open (not a candidate)");
  g=SleepLogic.awakeGaps(noOwn,from,now,true);check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==now,"Looking at the app now closes the open gap");
  List<SleepLogic.Event> unlock=new ArrayList<>(ev);unlock.add(5,e(day+15*h,18,""));
  g=SleepLogic.awakeGaps(unlock,from,now,false);check(g.size()==1&&g.get(0).end==day+15*h,"Unlock at 15:00 ends the gap (10:33→15:00 is 4h27m)");
  List<SleepLogic.Event> home=new ArrayList<>(ev);home.add(5,e(day+12*h,1,"com.sec.android.app.launcher"));home.add(6,e(day+12*h+m,2,"com.sec.android.app.launcher"));
  g=SleepLogic.awakeGaps(home,from,now,false);check(g.size()==1&&g.get(0).start==day+12*h+m,"Home screen use counts as awake (10:33→12:00 too short, 12:01→17:02 kept)");
  check(SleepLogic.awakeGaps(Arrays.asList(e(day+10*h,1,"a"),e(day+10*h+m,2,"a"),e(day+14*h+m-1,1,"a")),from,now,false).isEmpty(),"4h minus 1ms is not a candidate");
  System.out.println("PASS: 6 awake-gap checks (daytime sleep, lock screen, open app, unlock, home screen, 4h boundary)");
 }
}
