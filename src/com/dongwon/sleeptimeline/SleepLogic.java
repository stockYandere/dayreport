package com.dongwon.sleeptimeline;
import java.util.*;
public final class SleepLogic {
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
