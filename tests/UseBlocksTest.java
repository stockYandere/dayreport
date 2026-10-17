package com.dongwon.sleeptimeline;
import java.util.*;
public class UseBlocksTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 public static void main(String[]args){
  long h=3600000L,m=60000L,s=1000L,hour=1000000000L*1000/1000;Models.AppMeta files=new Models.AppMeta("files","내 파일",0,null),installer=new Models.AppMeta("inst","패키지 설치 프로그램",0,null),claude=new Models.AppMeta("claude","Claude",0,null);
  // 스크린샷의 03시: 내 파일·설치 화면 클릭 몇 초, Claude 두 번, 다시 내 파일·설치 화면
  List<Models.Session> rows=Arrays.asList(new Models.Session(files,hour+27*m+10*s,hour+27*m+25*s),new Models.Session(installer,hour+27*m+25*s,hour+27*m+40*s),new Models.Session(claude,hour+31*m,hour+33*m+30*s),new Models.Session(claude,hour+42*m+10*s,hour+42*m+50*s),new Models.Session(files,hour+43*m,hour+43*m+20*s),new Models.Session(installer,hour+43*m+20*s,hour+43*m+30*s));
  List<Models.UseBlock> b=Models.blocks(rows,hour,hour+h,2*m);
  check(b.size()==3,"6 rows become 3 use blocks: "+b.size());
  check(b.get(0).apps.size()==2&&b.get(0).duration==30*s,"Quick hop between two apps is one 30s block");
  check(b.get(1).apps.size()==1&&b.get(1).duration==150*s,"Separate Claude use kept (9 min gap)");
  check(b.get(2).apps.size()==3&&b.get(2).apps.get(0).app==claude&&b.get(2).duration==70*s,"Claude then files within 2 min joined; most-used app first");
  check(Models.blocks(Arrays.asList(new Models.Session(claude,hour-10*m,hour+5*m)),hour,hour+h,2*m).get(0).duration==5*m,"Duration counts only inside the hour");
  check(Models.union(Arrays.asList(new Models.Session(claude,hour,hour+10*m),new Models.Session(files,hour+5*m,hour+12*m)),hour,hour+h)==12*m,"Overlaps counted once");
  check(MainActivity.duration(20*s).equals("20초")&&MainActivity.duration(400).equals("1초")&&MainActivity.duration(90*s).equals("2분")&&MainActivity.duration(65*m).equals("1시간 5분"),"Under a minute shows seconds");
  check(Models.blocks(new ArrayList<Models.Session>(),hour,hour+h,2*m).isEmpty(),"No use, no blocks");
  System.out.println("PASS: 8 use-block grouping and duration checks");
 }
}
