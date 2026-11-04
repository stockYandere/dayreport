package com.dongwon.sleeptimeline;
import java.util.*;
public class AutoConfirmTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[]args){
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
  long h=3600000L,m=60000L;Calendar c=Calendar.getInstance();c.set(2026,Calendar.NOVEMBER,4,0,0,0);c.set(Calendar.MILLISECOND,0);long day=c.getTimeInMillis(),D=86400000L;
  // 야간 근무 후 평소: 10:30 전후 잠들어 7시간 안팎
  List<SleepLogic.Gap> past=new ArrayList<>(Arrays.asList(new SleepLogic.Gap(day-D+10*h+30*m,day-D+17*h+30*m),new SleepLogic.Gap(day-2*D+10*h,day-2*D+17*h+15*m),new SleepLogic.Gap(day-5*D+11*h,day-5*D+18*h)));
  check(SleepLogic.matchesUsual(day+10*h+40*m,day+17*h+20*m,past),"10:40–17:20 (6h40m) matches usual daytime sleep");
  check(!SleepLogic.matchesUsual(day+13*h,day+20*h,past),"Start 13:00 is over 1h from every usual start (10:00–11:00) → not auto");
  check(!SleepLogic.matchesUsual(day+10*h+30*m,day+13*h,past),"Same start but only 2h30m → length too different");
  check(!SleepLogic.matchesUsual(day+10*h+30*m,day+17*h,Arrays.asList(past.get(0))),"One similar record is not enough");
  List<SleepLogic.Gap> old=Arrays.asList(new SleepLogic.Gap(day-40*D+10*h,day-40*D+17*h),new SleepLogic.Gap(day-41*D+10*h,day-41*D+17*h));
  check(!SleepLogic.matchesUsual(day+10*h,day+17*h,old),"Records older than 30 days are not used");
  // 자정을 넘는 시각 비교
  List<SleepLogic.Gap> night=Arrays.asList(new SleepLogic.Gap(day-D-30*m,day-D+7*h),new SleepLogic.Gap(day-2*D+20*m,day-2*D+7*h+20*m));
  check(SleepLogic.matchesUsual(day+10*m,day+7*h+30*m,night),"23:30 / 00:20 history matches 00:10 (wraps midnight)");
  check(SleepLogic.clockDistance(day-30*m,day+20*m)==50*m,"23:30 vs 00:20 is 50 minutes apart");
  check(!SleepLogic.matchesUsual(past.get(0).start,past.get(0).end,Arrays.asList(past.get(0),past.get(0))),"A record does not match itself");
  // 막대에 칠할 수면 구간(시간 안으로 자름)
  List<long[]> spans=Arrays.asList(new long[]{day+10*h+30*m,day+17*h+30*m,0},new long[]{day+19*h,day+19*h+40*m,1});
  List<long[]> in=DashboardView.sleepIn(spans,day+10*h,day+11*h);check(in.size()==1&&in.get(0)[0]==day+10*h+30*m&&in.get(0)[1]==day+11*h,"10시 막대: 10:30–11:00 sleep");
  in=DashboardView.sleepIn(spans,day+12*h,day+13*h);check(in.size()==1&&in.get(0)[1]-in.get(0)[0]==h,"Full hour asleep fills the bar");
  in=DashboardView.sleepIn(spans,day+19*h,day+20*h);check(in.size()==1&&in.get(0)[2]==1,"Nap keeps its kind");
  check(DashboardView.sleepIn(spans,day+18*h,day+19*h).isEmpty(),"No sleep, nothing drawn");
  System.out.println("PASS: 12 auto-confirm and sleep-bar checks");
 }
}
