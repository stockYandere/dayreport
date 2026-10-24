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
  List<SleepLogic.Gap> g=gaps(ev,from,now,true);
  check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==now,"Daytime sleep 10:33→now (17:02); lock-screen notification ignored; looking at the app now ends it");
  check(gaps(ev,from,now,false).isEmpty(),"In the background, a 30-second app open alone does not end the gap");
  // 홈 화면은 사용으로 보지 않는다
  List<SleepLogic.Event> home=new ArrayList<>(ev);home.addAll(5,use(day+12*h,day+12*h+20*m,"com.sec.android.app.launcher"));
  g=gaps(home,from,now,true);check(g.size()==1&&g.get(0).start==day+10*h+33*m,"Home screen (even 20 min) does not split sleep");
  // 5분 이하 사용은 위치·앞뒤 시간과 상관없이 무시
  List<SleepLogic.Event> peek=new ArrayList<>(ev);peek.add(5,e(day+13*h+30*m,18,""));peek.addAll(6,use(day+13*h+30*m,day+13*h+35*m,"kakao"));
  g=gaps(peek,from,now,true);check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==now,"Unlock + exactly 5-minute check in the middle is ignored");
  List<SleepLogic.Event> wake=new ArrayList<>(Arrays.asList(e(day+10*h,1,"claude"),e(day+10*h+33*m,2,"claude")));wake.addAll(use(day+15*h,day+15*h+3*m,"claude"));wake.addAll(use(day+15*h+30*m,day+15*h+31*m,"claude"));
  g=gaps(wake,from,day+16*h,true);check(g.size()==1&&g.get(0).start==day+10*h+33*m&&g.get(0).end==day+16*h,"Short checks near the end are ignored too; the gap ends when the app is opened");
  // 10분 안에 이어진 사용은 합쳐서 판단: 3분+3분=6분은 깨어 있던 것
  List<SleepLogic.Event> two=new ArrayList<>(ev);two.addAll(5,use(day+13*h+30*m,day+13*h+33*m,"kakao"));two.addAll(7,use(day+13*h+40*m,day+13*h+43*m,"kakao"));
  check(gaps(two,from,now,true).isEmpty(),"Two 3-min uses 7 min apart (6 min) split the gap into pieces under 4h");
  List<SleepLogic.Event> six=new ArrayList<>(ev);six.addAll(5,use(day+13*h+30*m,day+13*h+36*m,"kakao"));
  check(gaps(six,from,now,true).isEmpty(),"6-minute use splits the gap");
  check(gaps(Arrays.asList(e(day+10*h,1,"a"),e(day+10*h+10*m,2,"a"),e(day+14*h+10*m-1,1,"a"),e(day+14*h+20*m,2,"a")),from,now,false).isEmpty(),"4h minus 1ms between real uses is not a candidate");
  check(gaps(Arrays.asList(e(day+10*h,1,"a"),e(day+10*h+10*m,2,"a"),e(day+14*h+10*m,1,"a"),e(day+14*h+20*m,2,"a")),from,now,false).size()==1,"Exactly 4h between real uses is a candidate");
  System.out.println("PASS: 10 awake-gap checks (daytime sleep, lock/home screen ignored, every use of 5 min or less ignored, opening the app ends sleep, 4h boundary)");
 }
}
