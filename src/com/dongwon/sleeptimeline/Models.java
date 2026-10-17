package com.dongwon.sleeptimeline;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
final class Models {
    private Models() {
    }

    static final class AppMeta {
        final int color;
        final Bitmap icon;
        final String label;
        final String packageName;

        AppMeta(String str, String str2, int i, Bitmap bitmap) {
            this.packageName = str;
            this.label = str2;
            this.color = i;
            this.icon = bitmap;
        }
    }

    static final class Session {
        final AppMeta app;
        final long end;
        final long start;

        Session(AppMeta appMeta, long j, long j2) {
            this.app = appMeta;
            this.start = j;
            this.end = j2;
        }

        long duration() {
            return Math.max(0L, this.end - this.start);
        }
    }

    static final class AppTotal {
        final AppMeta app;
        final long duration;
        final List<Session> sessions;

        AppTotal(AppMeta appMeta, long j, List<Session> list) {
            this.app = appMeta;
            this.duration = j;
            this.sessions = list;
        }
    }

    static final class DayRecord {
        final long dayStart;
        final long firstUse;
        final long lastUse;
        final List<Session> sessions;
        final List<AppTotal> totals;
        final long uniqueDuration;

        DayRecord(long j, List<Session> list, List<AppTotal> list2, long j2, long j3, long j4) {
            this.dayStart = j;
            this.sessions = list;
            this.totals = list2;
            this.uniqueDuration = j2;
            this.firstUse = j3;
            this.lastUse = j4;
        }

        static DayRecord empty(long j) {
            return new DayRecord(j, new ArrayList(), new ArrayList(), 0L, -1L, -1L);
        }
    }

    static Map<String, List<Session>> groupSessions(List<Session> list) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        for (Session session : list) {
            List list2 = (List) linkedHashMap.get(session.app.packageName);
            if (list2 == null) {
                list2 = new ArrayList();
                linkedHashMap.put(session.app.packageName, list2);
            }
            list2.add(session);
        }
        return linkedHashMap;
    }
}
