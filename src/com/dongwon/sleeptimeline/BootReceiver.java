package com.dongwon.sleeptimeline;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){CaptureJob.schedule(c);TodoWidget.refreshAll(c);TodoWidget.scheduleMidnight(c);}}
