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
        int color;
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

    // 앞 사용이 끝나고 10분 안에 다시 쓰면 같은 사용 묶음으로 본다.
    static final long BLOCK_GAP = 10 * 60000L;

    // 앱을 오가며 잠깐씩 쓴 것을 한 번의 사용으로 묶는다. 다음 사용이 gap 안에 시작하면 같은 묶음.
    // 길이는 [from, to) 안에서 겹치지 않게 센 시간, 표시 시각은 실제 시작·끝.
    static final class UseBlock {
        final long start;
        final long end;
        final long duration;
        final List<AppTotal> apps;

        UseBlock(long start, long end, long duration, List<AppTotal> apps) {
            this.start = start;
            this.end = end;
            this.duration = duration;
            this.apps = apps;
        }
    }

    static List<UseBlock> blocks(List<Session> sessions, long from, long to, long gap) {
        List<Session> sorted = new ArrayList<>();
        for (Session s : sessions) {
            if (s.end > s.start && s.start < to && s.end > from) sorted.add(s);
        }
        java.util.Collections.sort(sorted, new java.util.Comparator<Session>() {
            public int compare(Session a, Session b) {
                return Long.compare(a.start, b.start);
            }
        });
        List<UseBlock> out = new ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            long start = sorted.get(i).start, end = sorted.get(i).end;
            int j = i + 1;
            while (j < sorted.size() && sorted.get(j).start <= end + gap) {
                end = Math.max(end, sorted.get(j).end);
                j++;
            }
            List<Session> group = sorted.subList(i, j);
            List<AppTotal> apps = new ArrayList<>();
            for (List<Session> list : groupSessions(group).values()) {
                apps.add(new AppTotal(list.get(0).app, union(list, from, to), list));
            }
            java.util.Collections.sort(apps, new java.util.Comparator<AppTotal>() {
                public int compare(AppTotal a, AppTotal b) {
                    return Long.compare(b.duration, a.duration);
                }
            });
            out.add(new UseBlock(start, end, union(group, from, to), apps));
            i = j;
        }
        return out;
    }

    // 1분 미만은 초로 적는다. 짧게 스친 사용이 1분으로 부풀려 보이지 않게.
    static String durationText(long ms) {
        if (ms < 60000) return Math.max(1, Math.round(ms / 1000.0)) + "초";
        long m = Math.round(ms / 60000.0);
        return m >= 60 ? (m / 60) + "시간" + (m % 60 > 0 ? " " + (m % 60) + "분" : "") : m + "분";
    }

    // 겹치는 시간은 한 번만 센다.
    static long union(List<Session> sessions, long from, long to) {
        List<long[]> spans = new ArrayList<>();
        for (Session s : sessions) {
            long a = Math.max(from, s.start), b = Math.min(to, s.end);
            if (b > a) spans.add(new long[]{a, b});
        }
        java.util.Collections.sort(spans, new java.util.Comparator<long[]>() {
            public int compare(long[] a, long[] b) {
                return Long.compare(a[0], b[0]);
            }
        });
        long total = 0, s = -1, e = -1;
        for (long[] span : spans) {
            if (span[0] > e) {
                total += e - s;
                s = span[0];
                e = span[1];
            } else {
                e = Math.max(e, span[1]);
            }
        }
        return total + (e - s);
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
