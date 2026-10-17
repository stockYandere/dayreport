package com.dongwon.sleeptimeline;
import java.util.*;
public class HourSegmentsTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[]args)throws Exception{
  long day=1000000000L,h=3600000L,m=60000L,hour=day+14*h;Models.AppMeta a=new Models.AppMeta("a","유튜브",1,null),b=new Models.AppMeta("b","카톡",2,null);
  check(DashboardView.minuteRange(hour+3*m+20000,hour+21*m+5000,hour).equals("03–22"),"Start floors, end rounds up");
  check(DashboardView.minuteRange(hour,hour+h,hour).equals("00–60"),"Full hour ends at 60");
  check(DashboardView.minuteRange(hour+10*m,hour+10*m+5000,hour).equals("10–11"),"Short use still spans one minute");
  List<Models.Session> sessions=Arrays.asList(new Models.Session(a,hour-10*m,hour+5*m),new Models.Session(a,hour+5*m+30000,hour+12*m),new Models.Session(b,hour+20*m,hour+25*m),new Models.Session(a,hour+40*m,hour+70*m),new Models.Session(b,hour+30*m,hour+31*m));
  Models.DayRecord r=new Models.DayRecord(day,sessions,new ArrayList<Models.AppTotal>(),0,0,0);
  List<Models.Session> seg=DashboardView.hourSegments(r,14,new HashSet<String>(Arrays.asList("a","b")));
  check(seg.size()==4,"Same app within 1 minute joined, others separate: "+seg.size());
  check(seg.get(0).start==hour&&seg.get(0).end==hour+12*m,"Session from previous hour clipped to :00 and joined");
  check(seg.get(1).app==b&&seg.get(2).app==b&&seg.get(3).start==hour+40*m&&seg.get(3).end==hour+h,"Chronological, clipped at next hour");
  seg=DashboardView.hourSegments(r,14,new HashSet<String>(Collections.singletonList("a")));check(seg.size()==2,"Hidden apps are not drawn");
  List<Models.Session> merged=DashboardView.mergeSessions(Arrays.asList(new Models.Session(a,hour,hour+m),new Models.Session(b,hour+m,hour+2*m),new Models.Session(a,hour+2*m,hour+3*m)),60000L);check(merged.size()==3,"Different app in between keeps separate rows");
  check(DashboardView.hourSegments(new Models.DayRecord(day,new ArrayList<Models.Session>(),new ArrayList<Models.AppTotal>(),0,0,0),3,new HashSet<String>()).isEmpty(),"Empty hour draws nothing");
  try{TaskStore.clean("   ");check(false,"Blank title rejected");}catch(Exception expected){}
  check(TaskStore.clean("  창문에   검은 네모\n붙이기 ").equals("창문에 검은 네모 붙이기"),"Whitespace normalised");
  StringBuilder big=new StringBuilder();for(int i=0;i<201;i++)big.append('가');try{TaskStore.clean(big.toString());check(false,"201 chars rejected");}catch(Exception expected){}
  check(TaskStore.validState("done")&&TaskStore.validState("skipped")&&TaskStore.validState("open")&&!TaskStore.validState("dismissed"),"Task states");
  Models.AppMeta c=new Models.AppMeta("c","C",0,null);List<Models.DayRecord> days=Arrays.asList(new Models.DayRecord(day,new ArrayList<Models.Session>(),Arrays.asList(new Models.AppTotal(a,10*m,new ArrayList<Models.Session>()),new Models.AppTotal(b,30*m,new ArrayList<Models.Session>())),0,0,0),new Models.DayRecord(day+24*h,new ArrayList<Models.Session>(),Arrays.asList(new Models.AppTotal(a,25*m,new ArrayList<Models.Session>()),new Models.AppTotal(c,m,new ArrayList<Models.Session>())),0,0,0));
  UsageRepository.assignColors(days);check(a.color==0xFFFF5252&&b.color==0xFFFF9800&&c.color==0xFFFFE03A,"Rainbow colors by 7-day usage rank: red, orange, yellow");
  check(DashboardView.labelColor(0xFFFFE03A)!=-1&&DashboardView.labelColor(0xFF5C6BC0)==-1,"Dark text on yellow, white text on indigo");
  List<SleepLogic.Gap> use=Arrays.asList(new SleepLogic.Gap(day-h,day+h),new SleepLogic.Gap(day+5*h,day+6*h),new SleepLogic.Gap(day+10*h-1,day+11*h));
  List<SleepLogic.Gap> gaps=SleepLogic.unusedGaps(use,0,Long.MAX_VALUE,SleepLogic.MIN_CANDIDATE);check(gaps.size()==1&&gaps.get(0).start==day+h&&gaps.get(0).end==day+5*h,"Exactly 4h daytime gap is a candidate, 4h minus 1ms is not");
  System.out.println("PASS: 16 minute-range, hourly segment, rainbow color, 4h candidate and task validation checks");
 }
}
