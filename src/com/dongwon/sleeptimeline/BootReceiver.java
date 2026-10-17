package com.dongwon.sleeptimeline;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){CaptureJob.schedule(c);}}
