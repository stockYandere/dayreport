package com.dongwon.sleeptimeline;
import java.util.*;
/* 수면 기록 버튼의 상태. 시작 → (일시 중지 ↔ 재개)* → 기록 정지.
   잠든 구간(조각)만 모으고 일시 중지한 동안(깨어 있던 시간)은 빼고 저장한다. 안드로이드에 기대지 않는 순수 로직. */
final class SleepSession {
 static final long MIN_PIECE=60000L; // 1분 미만 조각은 버린다(버튼을 잘못 눌렀다 바로 되돌린 경우).
 long start=-1;      // 기록을 시작한 시각, 없으면 -1
 long pieceStart=-1; // 지금 자는 중인 조각의 시작, 일시 중지 중이면 -1
 final List<long[]> pieces=new ArrayList<long[]>();

 boolean active(){return start>0;}
 boolean paused(){return active()&&pieceStart<0;}
 String state(){return !active()?"idle":paused()?"paused":"recording";}

 void begin(long now){start=now;pieceStart=now;pieces.clear();}
 void pause(long now){if(active()&&pieceStart>0){if(now>pieceStart)pieces.add(new long[]{pieceStart,now});pieceStart=-1;}}
 void resume(long now){if(paused())pieceStart=now;}
 // 일시 중지를 시작한 시각(일시 중지 중일 때만).
 long pausedAt(){return paused()&&!pieces.isEmpty()?pieces.get(pieces.size()-1)[1]:paused()?start:-1;}
 long slept(long now){long sum=0;for(long[] p:pieces)sum+=p[1]-p[0];if(pieceStart>0&&now>pieceStart)sum+=now-pieceStart;return sum;}

 /* 기록 정지: 저장할 조각(1분 이상)을 돌려주고 비운다. */
 List<long[]> finish(long now){pause(now);List<long[]> out=new ArrayList<long[]>();for(long[] p:pieces)if(p[1]-p[0]>=MIN_PIECE)out.add(new long[]{p[0],p[1]});clear();return out;}
 void clear(){start=-1;pieceStart=-1;pieces.clear();}

 // 저장 형식: start;pieceStart;s-e,s-e
 String encode(){return start+";"+pieceStart+";"+encodePieces(pieces);}
 static SleepSession decode(String text){SleepSession s=new SleepSession();if(text==null||text.isEmpty())return s;try{String[] part=text.split(";",-1);s.start=Long.parseLong(part[0]);s.pieceStart=Long.parseLong(part[1]);s.pieces.addAll(decodePieces(part.length>2?part[2]:""));}catch(Exception e){s.clear();}return s;}
 static String encodePieces(List<long[]> rows){StringBuilder b=new StringBuilder();for(long[] p:rows){if(b.length()>0)b.append(',');b.append(p[0]).append('-').append(p[1]);}return b.toString();}
 static List<long[]> decodePieces(String text){List<long[]> out=new ArrayList<long[]>();if(text==null||text.isEmpty())return out;try{for(String p:text.split(",")){String[] se=p.split("-");out.add(new long[]{Long.parseLong(se[0]),Long.parseLong(se[1])});}}catch(Exception e){out.clear();}return out;}
}
