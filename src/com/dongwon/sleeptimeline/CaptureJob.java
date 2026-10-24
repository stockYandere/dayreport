package com.dongwon.sleeptimeline;
import android.app.job.*;import android.content.*;
public class CaptureJob extends JobService {
 private volatile boolean stopped;
 public static void schedule(Context c){JobScheduler s=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);s.schedule(new JobInfo.Builder(200,new ComponentName(c,CaptureJob.class)).setPeriodic(6L*3600000).setPersisted(true).build());}
 public boolean onStartJob(final JobParameters p){stopped=false;new Thread(new Runnable(){public void run(){try{UsageReader.capture(CaptureJob.this,false);}catch(Exception ignored){}finally{if(!stopped)jobFinished(p,false);}}}).start();return true;}
 public boolean onStopJob(JobParameters p){stopped=true;return true;}
}
