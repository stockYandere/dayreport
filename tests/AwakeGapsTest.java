package com.dongwon.sleeptimeline;
import java.util.*;
public class AwakeGapsTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static SleepLogic.Event e(long t,int type,String pkg){return new SleepLogic.Event(t,type,pkg);}
 static final Set<String> IGNORE=new HashSet<>(Arrays.asList("com.android.systemui","android","com.sec.android.app.launcher"));
 static List<SleepLogic.Gap> gaps(List<SleepLogic.Event> ev,long from,long now,boolean awakeNow){return SleepLogic.awakeGaps(ev,from,now,awakeNow,IGNORE);}
 static List<SleepLogic.Event> use(long start,long end,String pkg){return Arrays.asList(e(start,1,pkg),e(end,2,pkg));}
 public static void main(String[]args){
  long h=3600000L,m=60000L,day=1000000000000L,from=day-24*h;
  // 야간 근무 후: 10:00–10:33 Claude, 10:34 화면 꺼짐, 13:00 알림으로 잠금 화면만 잠깐, 17:02 dayreport 열기
  List<SleepLogic.Event> ev=new ArrayList<>(Arrays.asList(e(day+10*h,1,"claude"),e(day+10*h+33*m,2,"claude"),e(day+10*h+34*m,16,""),e(day+13*h,1,"com.android.systemui"),e(day+13*h+5000,2,"com.android.systemui"),e(day+17*h+2*m,1,"com.dongwon.sleeptimeline")));
  long now=day+17*h+2*m+30000;
  List<SleepLogic.Gap> g=gaps(ev,from,now,false);
  check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==day+17*h+2*m,"Daytime sleep 10:33→17:02; lock-screen notification ignored; opening this app ends it");
  List<SleepLogic.Event> noOwn=new ArrayList<>(ev.subList(0,5));
  check(gaps(noOwn,from,now,false).isEmpty(),"Without any later use the gap stays open");
  g=gaps(noOwn,from,now,true);check(g.size()==1&&g.get(0).end==now,"Looking at the app now closes the open gap");
  // 홈 화면은 사용으로 보지 않는다
  List<SleepLogic.Event> home=new ArrayList<>(ev);home.addAll(5,use(day+12*h,day+12*h+20*m,"com.sec.android.app.launcher"));
  g=gaps(home,from,now,false);check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==day+17*h+2*m,"Home screen (even 20 min) does not split sleep");
  // 중간에 5분 이하로 잠깐 본 것(잠금 해제 + 앱 3분)은 배제
  List<SleepLogic.Event> peek=new ArrayList<>(ev);peek.add(5,e(day+13*h+30*m,18,""));peek.addAll(6,use(day+13*h+30*m,day+13*h+33*m,"kakao"));
  g=gaps(peek,from,now,false);check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==day+17*h+2*m,"3-minute check in the middle is ignored");
  // 여러 번 잠깐 본 것도 10분 안이면 한 덩어리: 합계 5분 이하면 배제, 넘으면 유지
  List<SleepLogic.Event> two=new ArrayList<>(ev);two.addAll(5,use(day+13*h+30*m,day+13*h+33*m,"kakao"));two.addAll(7,use(day+13*h+40*m,day+13*h+43*m,"kakao"));
  g=gaps(two,from,now,false);check(g.isEmpty(),"Two 3-min uses 7 min apart = 6 min awake: kept, splits into two pieces under 4h");
  // 6분 사용은 짧은 사용이 아니므로 유지
  List<SleepLogic.Event> six=new ArrayList<>(ev);six.addAll(5,use(day+13*h+30*m,day+13*h+36*m,"kakao"));
  g=gaps(six,from,now,false);check(g.isEmpty(),"6-minute use splits the gap (both pieces under 4h)");
  // 앞뒤 조용한 시간이 1시간 미만이면 잠깐 본 것이 아니라 깨어 있던 것
  List<SleepLogic.Event> awake=new ArrayList<>(Arrays.asList(e(day+10*h,1,"claude"),e(day+10*h+33*m,2,"claude")));awake.addAll(use(day+15*h,day+15*h+3*m,"claude"));awake.addAll(use(day+15*h+30*m,day+15*h+31*m,"claude"));
  g=gaps(awake,from,day+16*h,false);check(g.size()==1&&g.get(0).end==day+15*h,"Wake at 15:00, check again 15:30: 15:00 not bridged (only 27 min quiet after)");
  // 마지막 짧은 사용은 구간의 끝이라 남긴다
  check(gaps(Arrays.asList(e(day+10*h,1,"a"),e(day+10*h+m,2,"a"),e(day+15*h,1,"a"),e(day+15*h+m,2,"a")),from,day+16*h,false).size()==1,"Short last use still ends the gap");
  check(gaps(Arrays.asList(e(day+10*h,1,"a"),e(day+10*h+m,2,"a"),e(day+14*h+m-1,1,"a")),from,now,false).isEmpty(),"4h minus 1ms is not a candidate");
  System.out.println("PASS: 10 awake-gap checks (daytime sleep, lock/home screen ignored, brief middle checks ignored, real wake kept, 4h boundary)");
 }
}
