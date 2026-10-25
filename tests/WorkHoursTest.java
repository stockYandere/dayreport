package com.dongwon.sleeptimeline;
import java.util.*;
public class WorkHoursTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static final Set<String> IGNORE=new HashSet<>(Arrays.asList("com.android.systemui"));
 static long at(int month,int date,int hour,int min){Calendar c=Calendar.getInstance();c.clear();c.set(2026,month-1,date,hour,min);return c.getTimeInMillis();}
 static List<SleepLogic.Event> use(long... t){List<SleepLogic.Event> out=new ArrayList<>();for(long x:t){out.add(new SleepLogic.Event(x,1,"app"));out.add(new SleepLogic.Event(x+20*60000L,2,"app"));}return out;}
 static int[][] week(){int[][] w=new int[7][];for(int i=0;i<7;i++)w[i]=WorkHours.DEFAULT[i].clone();return w;}
 // 첫 사용–마지막 사용 사이에 걸친 후보만 본다(테스트에는 그 밖의 사용이 없어서 근무 구간끼리 사이도 후보가 된다).
 static List<SleepLogic.Gap> gaps(List<SleepLogic.Event> ev,long from,long now,int[][] w){long a=ev.get(0).time,b=ev.get(ev.size()-1).time;List<SleepLogic.Gap> out=new ArrayList<>();for(SleepLogic.Gap g:SleepLogic.awakeGaps(ev,from,now,false,IGNORE,SleepLogic.workSpans(w,from,now)))if(g.start<b&&g.end>a)out.add(g);return out;}
 public static void main(String[] args){
  TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
  long h=3600000L;Calendar c=Calendar.getInstance();c.setTimeInMillis(at(10,20,0,0));check(c.get(Calendar.DAY_OF_WEEK)==Calendar.TUESDAY,"2026-10-20 is Tuesday");
  // 기본값: 수–금 00–08, 토–일 00–10
  List<SleepLogic.Gap> w=SleepLogic.workSpans(week(),at(10,20,0,0),at(10,27,0,0));
  check(w.size()==5,"5 shifts Tue→next Tue, got "+w.size());
  check(w.get(0).start==at(10,21,0,0)&&w.get(0).end==at(10,21,8,0),"Wed 00:00–08:00");
  check(w.get(2).end==at(10,23,8,0)&&w.get(3).start==at(10,24,0,0)&&w.get(3).end==at(10,24,10,0)&&w.get(4).end==at(10,25,10,0),"Fri ends 08:00, Sat/Sun 00:00–10:00");
  // 다음 날로 넘어가는 근무, 범위 안에서 잘림
  int[][] night=week();night[0]=new int[]{1,22*60,6*60};
  w=SleepLogic.workSpans(night,at(10,20,2,0),at(10,20,12,0));
  check(w.size()==1&&w.get(0).start==at(10,20,2,0)&&w.get(0).end==at(10,20,6,0),"Mon 22:00→Tue 06:00 clipped to from");
  long from=at(10,19,0,0),now=at(10,26,12,0);
  // 화 23:00 마지막 사용 → 수 15:00 사용: 근무(00–08)를 빼고 08:00–15:00만 후보
  List<SleepLogic.Gap> g=gaps(use(at(10,20,22,40),at(10,21,15,0)),from,now,week());
  check(g.size()==1&&g.get(0).start==at(10,21,8,0)&&g.get(0).end==at(10,21,15,0),"After-shift sleep is cut at 08:00");
  check(gaps(use(at(10,20,22,40),at(10,21,15,0)),from,now,new int[][]{{0,0,1},{0,0,1},{0,0,1},{0,0,1},{0,0,1},{0,0,1},{0,0,1}}).get(0).start==at(10,20,23,0),"All days off: unchanged");
  // 근무 시간 안에서만 안 쓴 구간은 후보가 아니다
  check(gaps(use(at(10,22,0,40),at(10,22,7,0)),from,now,week()).isEmpty(),"Unused only during the shift is not a candidate");
  // 근무를 빼고 4시간 미만이면 후보 없음
  check(gaps(use(at(10,20,22,40),at(10,21,10,0)),from,now,week()).isEmpty(),"08:00–10:00 left (2h) is not a candidate");
  // 출근 전 잠: 18:00–00:00(6h)만 후보, 퇴근 후 08–09는 4시간 미만
  g=gaps(use(at(10,20,17,40),at(10,21,9,0)),from,now,week());
  check(g.size()==1&&g.get(0).start==at(10,20,18,0)&&g.get(0).end==at(10,21,0,0),"Pre-shift sleep ends at shift start");
  // 토요일 근무는 10시까지
  g=gaps(use(at(10,23,22,40),at(10,24,16,0)),from,now,week());
  check(g.size()==1&&g.get(0).start==at(10,24,10,0)&&g.get(0).end==at(10,24,16,0),"Sat shift ends 10:00");
  // 근무 없는 날(월→화 밤)은 그대로
  g=gaps(use(at(10,19,22,40),at(10,20,8,0)),from,now,week());
  check(g.size()==1&&g.get(0).start==at(10,19,23,0)&&g.get(0).end==at(10,20,8,0),"Mon→Tue night is unaffected");
  // 앞으로 올 근무 시간은 아직 끝나지 않은 구간을 닫지 않는다
  long tue=at(10,20,23,30);check(SleepLogic.awakeGaps(use(at(10,20,12,0),at(10,20,18,0)),from,tue,false,IGNORE,SleepLogic.workSpans(week(),from,tue)).size()==SleepLogic.awakeGaps(use(at(10,20,12,0),at(10,20,18,0)),from,tue,false,IGNORE).size(),"Tue 23:30: the shift starting at 00:00 has not happened yet, so it closes nothing");
  System.out.println("PASS: 11 work-hour checks (weekly schedule, overnight shift, cut at shift edges, under 4h dropped, days off unaffected)");
 }
}
