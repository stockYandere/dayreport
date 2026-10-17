package com.dongwon.sleeptimeline;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.dongwon.sleeptimeline.Models;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.ToLongFunction;

/* loaded from: classes.dex */
final class UsageRepository {
    // 빨주노초파남보 다음 분홍, 청록, 갈색, 연두. 최근 7일 많이 쓴 순서대로 앞 색부터 준다. 그 뒤는 회색.
    static final int[] APP_COLORS = {0xFFFF5252, 0xFFFF9800, 0xFFFFE03A, 0xFF4CD964, 0xFF3D9BFF, 0xFF5C6BC0, 0xFFB15CFF, 0xFFFF7EB9, 0xFF26D7C9, 0xFFC69C6D, 0xFFB4E05A};
    static final int OTHER_COLOR = 0xFF8A93A6;
    private static final long HOUR = 3600000;
    private static final long MIN_SESSION = 1000;
    private final Context context;
    private final PackageManager packageManager;
    private final UsageStatsManager usageStatsManager;
    private final Map<String, Models.AppMeta> appCache = new HashMap();
    private final Set<String> excludedPackages = new HashSet();

    UsageRepository(Context context) {
        this.context = context.getApplicationContext();
        this.packageManager = context.getPackageManager();
        this.usageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        this.excludedPackages.add(context.getPackageName());
        this.excludedPackages.add("android");
        this.excludedPackages.add("com.android.systemui");
        this.excludedPackages.add("com.sec.android.app.launcher");
        this.excludedPackages.add("com.google.android.apps.nexuslauncher");
        // 설치·권한 확인·공유 선택 화면은 다른 동작 중에 잠깐 뜨는 시스템 화면이라 사용시간에서 뺀다.
        for (String helper : new String[]{"com.google.android.packageinstaller", "com.android.packageinstaller", "com.samsung.android.packageinstaller", "com.google.android.permissioncontroller", "com.android.permissioncontroller", "com.android.intentresolver"}) {
            this.excludedPackages.add(helper);
        }
        Intent addCategory = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME");
        if (addCategory.resolveActivity(this.packageManager) != null) {
            this.excludedPackages.add(addCategory.resolveActivity(this.packageManager).getPackageName());
        }
    }

    synchronized List<Models.DayRecord> loadSevenDays() {
        ArrayList arrayList = new ArrayList();
        Calendar calendar = Calendar.getInstance();
        setStartOfDay(calendar);
        for (int i = 6; i >= 0; i--) {
            Calendar calendar2 = (Calendar) calendar.clone();
            calendar2.add(6, -i);
            arrayList.add(loadDay(calendar2.getTimeInMillis()));
        }
        assignColors(arrayList);
        return arrayList;
    }

    synchronized Models.DayRecord loadDay(long dayStart) {
        Calendar cal=Calendar.getInstance();cal.setTimeInMillis(dayStart);cal.add(Calendar.DATE,1);
        long end=Math.min(cal.getTimeInMillis(),System.currentTimeMillis());
        if(end<=dayStart||usageStatsManager==null)return Models.DayRecord.empty(dayStart);
        UsageEvents events=usageStatsManager.queryEvents(dayStart-43200000,end);
        UsageEvents.Event event=new UsageEvents.Event();
        Map<String,Long> active=new HashMap<String,Long>();List<Models.Session> sessions=new ArrayList<Models.Session>();
        while(events!=null&&events.hasNextEvent()) {
            events.getNextEvent(event);long time=event.getTimeStamp();int type=event.getEventType();String pkg=event.getPackageName();
            if(type==16||type==26){closeAll(active,sessions,time,dayStart,end);continue;}
            if(pkg==null||excludedPackages.contains(pkg))continue;
            if(type==1){if(!active.containsKey(pkg))active.put(pkg,time);}
            else if(type==2||type==23){Long start=active.remove(pkg);if(start!=null)addSession(sessions,pkg,start,time,dayStart,end);}
        }
        closeAll(active,sessions,end,dayStart,end);
        Collections.sort(sessions,new Comparator<Models.Session>(){public int compare(Models.Session a,Models.Session b){return Long.compare(a.start,b.start);}});
        List<Models.AppTotal> totals=new ArrayList<Models.AppTotal>();
        for(List<Models.Session> group:Models.groupSessions(sessions).values()){long duration=0;for(Models.Session item:group)duration+=item.duration();totals.add(new Models.AppTotal(group.get(0).app,duration,group));}
        Collections.sort(totals,new Comparator<Models.AppTotal>(){public int compare(Models.AppTotal a,Models.AppTotal b){return Long.compare(b.duration,a.duration);}});
        long first=sessions.isEmpty()?-1:sessions.get(0).start,last=-1;for(Models.Session item:sessions)last=Math.max(last,item.end);
        return new Models.DayRecord(dayStart,sessions,totals,unionDuration(sessions),first,last);
    }

    static void assignColors(List<Models.DayRecord> days) {
        final Map<Models.AppMeta, Long> totals = new HashMap<>();
        for (Models.DayRecord day : days) {
            for (Models.AppTotal total : day.totals) {
                Long sum = totals.get(total.app);
                totals.put(total.app, (sum == null ? 0L : sum) + total.duration);
            }
        }
        List<Models.AppMeta> ranked = new ArrayList<>(totals.keySet());
        Collections.sort(ranked, new Comparator<Models.AppMeta>() {
            public int compare(Models.AppMeta a, Models.AppMeta b) {
                int order = Long.compare(totals.get(b), totals.get(a));
                return order != 0 ? order : a.packageName.compareTo(b.packageName);
            }
        });
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).color = i < APP_COLORS.length ? APP_COLORS[i] : OTHER_COLOR;
        }
    }

    private void closeAll(Map<String, Long> map, List<Models.Session> list, long j, long j2, long j3) {
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            addSession(list, entry.getKey(), entry.getValue().longValue(), j, j2, j3);
        }
        map.clear();
    }

    private void addSession(List<Models.Session> list, String str, long j, long j2, long j3, long j4) {
        long max = Math.max(j, j3);
        long min = Math.min(j2, j4);
        long j5 = min - max;
        if (j5 < MIN_SESSION) {
            return;
        }
        if (j5 > 43200000) {
            max = min - 43200000;
        }
        list.add(new Models.Session(resolveApp(str), max, min));
    }

    private Models.AppMeta resolveApp(String str) {
        Bitmap bitmap;
        Models.AppMeta appMeta = this.appCache.get(str);
        if (appMeta != null) {
            return appMeta;
        }
        String simplePackageName = simplePackageName(str);
        try {
            ApplicationInfo applicationInfo = this.packageManager.getApplicationInfo(str, 0);
            CharSequence applicationLabel = this.packageManager.getApplicationLabel(applicationInfo);
            if (applicationLabel != null && applicationLabel.length() > 0) {
                simplePackageName = applicationLabel.toString();
            }
            bitmap = drawableToBitmap(this.packageManager.getApplicationIcon(applicationInfo));
        } catch (Exception e) {
            bitmap = null;
        }
        Models.AppMeta appMeta2 = new Models.AppMeta(str, simplePackageName, OTHER_COLOR, bitmap);
        this.appCache.put(str, appMeta2);
        return appMeta2;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;
        if (drawable == null) {
            return null;
        }
        if ((drawable instanceof BitmapDrawable) && (bitmap = ((BitmapDrawable) drawable).getBitmap()) != null) {
            return Bitmap.createScaledBitmap(bitmap, 72, 72, true);
        }
        Bitmap createBitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, 72, 72);
        drawable.draw(canvas);
        return createBitmap;
    }

    private static String simplePackageName(String str) {
        int lastIndexOf = str.lastIndexOf(46);
        String substring = lastIndexOf >= 0 ? str.substring(lastIndexOf + 1) : str;
        if (!substring.isEmpty()) {
            str = substring;
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1);
    }

    private static long unionDuration(List<Models.Session> list) {
        long j = 0;
        if (list.isEmpty()) {
            return 0L;
        }
        long j2 = list.get(0).start;
        long j3 = list.get(0).end;
        for (int i = 1; i < list.size(); i++) {
            Models.Session session = list.get(i);
            if (session.start <= j3) {
                j3 = Math.max(j3, session.end);
            } else {
                j += j3 - j2;
                j2 = session.start;
                j3 = session.end;
            }
        }
        return j + (j3 - j2);
    }

    private static void setStartOfDay(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }
}
