package com.dongwon.sleeptimeline;
import java.util.*;
public final class SleepLogic {
 // 수면 후보 최소 길이: 휴대폰을 4시간 이상 쓰지 않은 구간. 낮·밤 구분 없음.
 public static final long MIN_CANDIDATE=4L*3600000;
 public static class Event { public long time; public int type; public String pkg; public Event(long t,int e,String p){time=t;type=e;pkg=p==null?"":p;} }
 public static class Gap {public long start,end; public Gap(long s,long e){start=s;end=e;} }
 // Both ends must be observed. An empty history never becomes a sleep record.
 public static List<Gap> candidates(List<Event> events,long min,long now){
  List<Gap> out=new ArrayList<Gap>(); long off=-1;
  for(Event e:events){
   if(e.type==16 && off<0) off=e.time;
   else if((e.type==18||e.type==1) && off>=0){long span=e.time-off;
    if(span>=min && span<=18L*3600000 && e.time<=now)out.add(new Gap(off,e.time)); off=-1;
   }else if(e.type==26){off=-1;} // Shutdown is not evidence of sleeping.
  } return out;
 }
 public static boolean overlaps(long a,long b,long c,long d){return a<d&&c<b;}
 // Only gaps between observed use sessions. No inferred gap before/after the history.
 public static List<Gap> unusedGaps(List<Gap> use,long dayStart,long dayEnd,long minimum){
  List<Gap> sorted=new ArrayList<Gap>();for(Gap p:use)if(p.end>p.start)sorted.add(new Gap(p.start,p.end));
  Collections.sort(sorted,new Comparator<Gap>(){public int compare(Gap a,Gap b){return Long.compare(a.start,b.start);}});
  List<Gap> out=new ArrayList<Gap>();long last=-1;
  for(Gap p:sorted){if(last<0){last=p.end;continue;}if(p.start>last){long span=p.start-last;if(span>=minimum&&span<=24L*3600000&&p.start>dayStart&&last<dayEnd)out.add(new Gap(last,p.start));}last=Math.max(last,p.end);}
  Collections.sort(out,new Comparator<Gap>(){public int compare(Gap a,Gap b){return Long.compare(b.end,a.end);}});return out;
 }
 // 깨어 있었다는 증거: 앱 화면 사용(이 앱 포함)과 잠금 해제(18).
 // ignore: 시스템 UI(알림으로 켜진 잠금 화면), 홈 화면처럼 사용으로 보지 않는 패키지.
 public static List<Gap> awakeSpans(List<Event> events,long from,long to,Set<String> ignore){
  List<Event> use=new ArrayList<Event>();List<Gap> out=new ArrayList<Gap>();
  for(Event e:events){if(e.type==18&&e.time>=from&&e.time<=to)out.add(new Gap(e.time,e.time+1));if((e.type==1||e.type==2)&&ignore.contains(e.pkg))continue;use.add(e);}
  for(Session s:sessions(use,from,to))if(s.end>s.start)out.add(new Gap(s.start,s.end));
  return out;
 }
 public static final long WAKE_JOIN=10L*60000,SHORT_WAKE=5L*60000;
 // 사용을 10분 간격 안에서 한 덩어리로 묶고, 실제 사용이 5분 이하인 덩어리는 위치와 상관없이 모두 없앤다.
 // 잠깐 화면을 켜거나 자다 깨서 확인한 것은 깨어 있던 것으로 보지 않는다.
 public static List<Gap> withoutBriefWakes(List<Gap> spans){
  List<Gap> sorted=new ArrayList<Gap>(spans);Collections.sort(sorted,new Comparator<Gap>(){public int compare(Gap a,Gap b){return Long.compare(a.start,b.start);}});
  List<long[]> clusters=new ArrayList<long[]>();// start, end, active
  long s=-1,e=-1,active=0,runS=-1,runE=-1;
  for(Gap g:sorted){
   if(s>=0&&g.start-e>WAKE_JOIN){active+=runE-runS;clusters.add(new long[]{s,e,active});s=-1;}
   if(s<0){s=g.start;e=g.end;active=0;runS=g.start;runE=g.end;continue;}
   if(g.start>runE){active+=runE-runS;runS=g.start;runE=g.end;}else runE=Math.max(runE,g.end);
   e=Math.max(e,g.end);
  }
  if(s>=0){active+=runE-runS;clusters.add(new long[]{s,e,active});}
  List<Gap> out=new ArrayList<Gap>();
  for(int i=0;i<clusters.size();i++){long[] c=clusters.get(i);
   if(c[2]>SHORT_WAKE)out.add(new Gap(c[0],c[1]));}
  return out;
 }
 // 4시간 이상 휴대폰을 쓰지 않은 구간. 낮·밤, 날짜 경계와 상관없이 양쪽 끝이 관측된 것만.
 // awakeNow: 지금 이 앱을 보고 있으면 지금 이 순간은 짧아도 깨어 있는 시점(기상)으로 넣어 진행 중인 미사용 구간을 닫는다.
 public static List<Gap> awakeGaps(List<Event> events,long from,long now,boolean awakeNow,Set<String> ignore){List<Gap> awake=withoutBriefWakes(awakeSpans(events,from,now,ignore));if(awakeNow)awake.add(new Gap(now,now+1));List<Gap> out=new ArrayList<Gap>();for(Gap g:unusedGaps(awake,0,Long.MAX_VALUE,MIN_CANDIDATE))if(g.end<=now)out.add(g);return out;}
 public static boolean valid(long start,long end,long now){return start>0&&end>start&&end-start<=24L*3600000&&end<=now+60000;}
 public static class Session {public long start,end; public String pkg;public Session(long s,long e,String p){start=s;end=e;pkg=p;} }
 public static List<Session> sessions(List<Event> events,long start,long end){
  List<Session> out=new ArrayList<Session>();String active="";long since=0;
  for(Event e:events){if(e.time>end)break;
   if(e.type==1){if(active.equals(e.pkg))continue;if(!active.isEmpty()&&e.time>start)out.add(new Session(Math.max(start,since),Math.min(end,e.time),active));active=e.pkg;since=e.time;}
   else if(e.type==16||e.type==26||(e.type==2&&active.equals(e.pkg))){if(!active.isEmpty()&&e.time>start)out.add(new Session(Math.max(start,since),Math.min(end,e.time),active));active="";}
  }
  if(!active.isEmpty()&&since<end)out.add(new Session(Math.max(start,since),end,active));return out;
 }
}
