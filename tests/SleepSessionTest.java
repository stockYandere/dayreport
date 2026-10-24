package com.dongwon.sleeptimeline;
import java.util.*;
public class SleepSessionTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[]args){
  long h=3600000L,m=60000L,t=1000000000000L;SleepSession s=new SleepSession();
  check(s.state().equals("idle")&&s.slept(t)==0,"Idle at first");
  s.begin(t);check(s.state().equals("recording")&&s.slept(t+2*h)==2*h,"Recording counts sleep");
  s.pause(t+2*h);check(s.state().equals("paused")&&s.pausedAt()==t+2*h&&s.slept(t+3*h)==2*h,"Paused time is not counted");
  s.resume(t+2*h+20*m);check(s.state().equals("recording")&&s.slept(t+7*h)==2*h+4*h+40*m,"Resume continues counting");
  SleepSession copy=SleepSession.decode(s.encode());check(copy.start==t&&copy.pieceStart==t+2*h+20*m&&copy.pieces.size()==1&&copy.slept(t+7*h)==s.slept(t+7*h),"State survives app restart (encode/decode)");
  List<long[]> pieces=s.finish(t+7*h);check(pieces.size()==2&&pieces.get(0)[0]==t&&pieces.get(0)[1]==t+2*h&&pieces.get(1)[0]==t+2*h+20*m&&pieces.get(1)[1]==t+7*h,"Stop saves 01:00-03:00 and 03:20-08:00 separately");
  check(s.state().equals("idle"),"Stop returns to idle");
  s.begin(t);s.pause(t+30000);s.resume(t+5*m);pieces=s.finish(t+3*h);check(pieces.size()==1&&pieces.get(0)[0]==t+5*m,"Pieces under 1 minute are dropped");
  s.begin(t);check(s.finish(t+20000).isEmpty(),"Start then immediate stop saves nothing");
  s.begin(t);s.pause(t+h);s.begin(t+2*h);check(s.pieces.isEmpty()&&s.start==t+2*h&&s.state().equals("recording"),"Restart discards and starts over now");
  s.begin(t);s.pause(t+h);check(s.finish(t+5*h).size()==1,"Stop while paused saves only sleep before the pause");
  s.pause(t);s.resume(t);check(s.state().equals("idle"),"Pause/resume without a session do nothing");
  check(SleepSession.decode("garbage").state().equals("idle")&&SleepSession.decode("").state().equals("idle"),"Broken saved state resets safely");
  check(SleepSession.decodePieces(SleepSession.encodePieces(Arrays.asList(new long[]{1,2},new long[]{3,4}))).size()==2,"Unsaved pieces round trip");
  System.out.println("PASS: 14 sleep recorder session checks");
 }
}
