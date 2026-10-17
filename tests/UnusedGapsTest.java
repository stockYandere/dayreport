package com.dongwon.sleeptimeline;
import java.util.*;
public class UnusedGapsTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static SleepLogic.Gap use(long a,long b){return new SleepLogic.Gap(a,b);}
 public static void main(String[]args){long t=1000000000L,h=3600000L;List<SleepLogic.Gap> sessions=Arrays.asList(use(t-6*h,t-5*h),use(t+2*h,t+3*h),use(t+8*h,t+9*h),use(t+14*h,t+15*h));
 List<SleepLogic.Gap> out=SleepLogic.unusedGaps(sessions,t,t+24*h,5*h);
 check(out.size()==3,"Overnight and multiple daytime candidates");check(out.get(0).start==t+9*h&&out.get(0).end==t+14*h,"Newest first, exact five hours retained");check(out.get(2).start==t-5*h&&out.get(2).end==t+2*h,"Overnight not clipped at midnight");
 sessions=Arrays.asList(use(t+2*h,t+3*h),use(t+3*h,t+4*h),use(t+8*h,t+9*h),use(t+14*h-1,t+15*h));out=SleepLogic.unusedGaps(sessions,t,t+24*h,5*h);check(out.isEmpty(),"Adjacent active sessions merged; 5h minus 1ms excluded");
 out=SleepLogic.unusedGaps(Arrays.asList(use(t-8*h,t-7*h),use(t-1,t+h)),t,t+24*h,5*h);check(out.isEmpty(),"Gap wholly before selected day excluded");
 check(SleepLogic.unusedGaps(Collections.<SleepLogic.Gap>emptyList(),t,t+24*h,5*h).isEmpty(),"Empty history does not create sleep");check(SleepLogic.unusedGaps(Arrays.asList(use(t,t+h)),t,t+24*h,5*h).isEmpty(),"No fabricated trailing or leading interval");
 System.out.println("PASS: 7 candidate boundaries, ordering and overnight checks");}
}
