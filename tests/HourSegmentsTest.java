package com.dongwon.sleeptimeline;
import java.util.*;
public class HourSegmentsTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[]args)throws Exception{
  long day=1000000000L,h=3600000L,m=60000L,hour=day+14*h;Models.AppMeta a=new Models.AppMeta("a","유튜브",1,null),b=new Models.AppMeta("b","카톡",2,null);
  // 14시: 00–10분 유튜브 6분 + 카톡 3분, 10–20분 카톡 30초만, 20–30분 유튜브 2분·카톡 4분, 40–60분 이어서 유튜브(전 시간에서 넘어옴 아님)
  List<Models.Session> sessions=Arrays.asList(new Models.Session(a,hour-5*m,hour+6*m),new Models.Session(b,hour+6*m,hour+9*m),new Models.Session(b,hour+12*m,hour+12*m+30000),new Models.Session(a,hour+20*m,hour+22*m),new Models.Session(b,hour+23*m,hour+27*m),new Models.Session(a,hour+45*m,hour+h+5*m));
  Models.AppTotal[] slots=DashboardView.tenMinuteSlots(new Models.DayRecord(day,sessions,new ArrayList<Models.AppTotal>(),0,0,0),14);
  check(slots.length==6,"Six 10-minute cells per hour");
  check(slots[0].app==a&&slots[0].duration==6*m,"Cell 0: most-used app only, time inside the cell (clipped from previous hour)");
  check(slots[1]==null,"Cell 1: 30 seconds of use leaves the cell empty");
  check(slots[2].app==b&&slots[2].duration==4*m,"Cell 2: app with more minutes wins");
  check(slots[3]==null,"Cell 3: no use");
  check(slots[4].app==a&&slots[4].duration==5*m&&slots[5].app==a&&slots[5].duration==10*m,"Cells 4-5: continuing use clipped per cell and at the next hour");
  List<long[]> spans=DashboardView.slotSpans(slots[2],hour,2);check(spans.size()==1&&spans.get(0)[0]==hour+23*m&&spans.get(0)[1]==hour+27*m,"Cell 2 colors only 23-27 where the top app was used");
  check(DashboardView.minuteRange(spans.get(0)[0],spans.get(0)[1],hour).equals("23–27"),"Range label");
  spans=DashboardView.slotSpans(slots[4],hour,4);check(spans.size()==1&&spans.get(0)[0]==hour+45*m&&spans.get(0)[1]==hour+50*m,"Continuing use clipped to its cell (45-50)");
  check(DashboardView.minuteRange(hour+50*m,hour+h,hour).equals("50–60"),"Through the hour ends at 60");
  check(DashboardView.slotSpans(new Models.AppTotal(a,2*m,Arrays.asList(new Models.Session(a,hour+m,hour+2*m),new Models.Session(a,hour+2*m+30000,hour+3*m),new Models.Session(a,hour+6*m,hour+7*m))),hour,0).size()==2,"Re-open within a minute joins, longer gap stays separate");
  check(Models.durationText(10*m).equals("10분")&&Models.durationText(45000).equals("45초"),"Cell labels");
  Models.AppTotal[] empty=DashboardView.tenMinuteSlots(new Models.DayRecord(day,new ArrayList<Models.Session>(),new ArrayList<Models.AppTotal>(),0,0,0),3);for(Models.AppTotal t:empty)check(t==null,"Empty hour draws nothing");
  try{TaskStore.clean("   ");check(false,"Blank title rejected");}catch(Exception expected){}
  check(TaskStore.clean("  창문에   검은 네모\n붙이기 ").equals("창문에 검은 네모 붙이기"),"Whitespace normalised");
  StringBuilder big=new StringBuilder();for(int i=0;i<201;i++)big.append('가');try{TaskStore.clean(big.toString());check(false,"201 chars rejected");}catch(Exception expected){}
  check(TaskStore.validState("done")&&TaskStore.validState("skipped")&&TaskStore.validState("open")&&!TaskStore.validState("dismissed"),"Task states");
  Models.AppMeta c=new Models.AppMeta("c","C",0,null);List<Models.DayRecord> days=Arrays.asList(new Models.DayRecord(day,new ArrayList<Models.Session>(),Arrays.asList(new Models.AppTotal(a,10*m,new ArrayList<Models.Session>()),new Models.AppTotal(b,30*m,new ArrayList<Models.Session>())),0,0,0),new Models.DayRecord(day+24*h,new ArrayList<Models.Session>(),Arrays.asList(new Models.AppTotal(a,25*m,new ArrayList<Models.Session>()),new Models.AppTotal(c,m,new ArrayList<Models.Session>())),0,0,0));
  UsageRepository.assignColors(days);check(a.color==0xFFFF5252&&b.color==0xFFFF9800&&c.color==0xFFFFE03A,"Rainbow colors by 7-day usage rank: red, orange, yellow");
  check(DashboardView.labelColor(0xFFFFE03A)!=-1&&DashboardView.labelColor(0xFF5C6BC0)==-1,"Dark text on yellow, white text on indigo");
  List<SleepLogic.Gap> use=Arrays.asList(new SleepLogic.Gap(day-h,day+h),new SleepLogic.Gap(day+5*h,day+6*h),new SleepLogic.Gap(day+10*h-1,day+11*h));
  List<SleepLogic.Gap> gaps=SleepLogic.unusedGaps(use,0,Long.MAX_VALUE,SleepLogic.MIN_CANDIDATE);check(gaps.size()==1&&gaps.get(0).start==day+h&&gaps.get(0).end==day+5*h,"Exactly 4h daytime gap is a candidate, 4h minus 1ms is not");
  System.out.println("PASS: 22 ten-minute cell, rainbow color, 4h candidate and task validation checks");
 }
}
