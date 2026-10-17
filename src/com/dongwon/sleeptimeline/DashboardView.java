package com.dongwon.sleeptimeline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.widget.OverScroller;
import com.dongwon.sleeptimeline.DashboardView;
import com.dongwon.sleeptimeline.Models;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/* loaded from: classes.dex */
final class DashboardView extends View {
    private static final int BG = -16184560;
    private static final int BLUE = -8869889;
    private static final int BLUE_DARK = -14863270;
    private static final int CARD = -15394784;
    private static final int CARD_2 = -14999767;
    private static final long HOUR_MS = 3600000;
    private static final int LINE = -13617341;
    private static final long MIN_HOURLY_APP = 600000;
    private static final long MIN_VISIBLE_APP = 600000;
    private static final int MUTED = -5919300;
    private static final int SUBTLE = -9274485;
    private static final int TEXT = -854534;
    private final Actions actions;
    private int appListPage;
    private int appsPerPage;
    private final Runnable clockRefresh;
    private final RectF[] dateRects;
    private final SimpleDateFormat dateTitleFormat;
    private List<Models.DayRecord> days;
    private float downX;
    private float downY;
    private String errorMessage;
    private final RectF firstNavRect;
    private final Map<Long, List<HourlyBucket>> hourlyBucketsByDay;
    private float lastTouchX;
    private boolean loading;
    private final int maximumFlingVelocity;
    private final int minimumFlingVelocity;
    private boolean moved;
    private boolean needsTimelinePosition;
    private final RectF nextPageRect;
    private int page;
    private final Paint paint;
    private boolean permissionGranted;
    private final RectF permissionRect;
    private final RectF previousPageRect;
    private final RectF refreshRect;
    private int safeInsetBottom;
    private int safeInsetTop;
    private final RectF secondNavRect;
    private final RectF thirdNavRect=new RectF();
    private final RectF fifthNavRect=new RectF();
    private final List<RectF> appRowRects=new ArrayList<>();
    private final List<Models.AppTotal> appRowItems=new ArrayList<>();
    private final RectF sleepGapRect=new RectF();
    private long gapStart=-1,gapEnd=-1;
    private org.json.JSONArray sleepRecords=new org.json.JSONArray();
    private int selectedDay;
    private final SimpleDateFormat timeFormat;
    private float timelineContentWidth;
    private boolean timelineGesture;
    private final RectF timelineRect;
    private float timelineScrollX;
    private final OverScroller timelineScroller;
    private final int touchSlop;
    private VelocityTracker velocityTracker;
    private final RectF[] weekBarRects;

    interface Actions {
        void openUsageAccessSettings();

        void refreshData();
        void openWebPage(String tab);
        void showSleepCandidates(long day,long start,long end);

        void showAppsAt(long hourStart, List<Models.Session> sessions);

        void showAppSessions(Models.AppTotal total, long dayStart);
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class HourlyAppUsage {
        final Models.AppMeta app;
        final long duration;

        HourlyAppUsage(Models.AppMeta appMeta, long j) {
            this.app = appMeta;
            this.duration = j;
        }
    }

    private static final class HourlyBucket {
        final List<HourlyAppUsage> apps;
        final boolean fallbackTopThree;
        final Set<String> packages;
        final long totalDuration;

        HourlyBucket(List<HourlyAppUsage> list, Set<String> set, long j, boolean z) {
            this.apps = list;
            this.packages = set;
            this.totalDuration = j;
            this.fallbackTopThree = z;
        }
    }

    DashboardView(Context context, Actions actions) {
        super(context);
        this.paint = new Paint(1);
        this.refreshRect = new RectF();
        this.permissionRect = new RectF();
        this.timelineRect = new RectF();
        this.firstNavRect = new RectF();
        this.secondNavRect = new RectF();
        this.previousPageRect = new RectF();
        this.nextPageRect = new RectF();
        this.dateRects = new RectF[7];
        this.weekBarRects = new RectF[7];
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.KOREA);
        this.dateTitleFormat = new SimpleDateFormat("M월 d일 EEEE", Locale.KOREA);
        this.hourlyBucketsByDay = new LinkedHashMap();
        this.days = new ArrayList();
        this.selectedDay = 6;
        this.appsPerPage = 4;
        this.needsTimelinePosition = true;
        this.clockRefresh = new Runnable() { // from class: com.dongwon.sleeptimeline.DashboardView.1
            @Override // java.lang.Runnable
            public void run() {
                DashboardView.this.invalidate();
                DashboardView.this.postDelayed(this, 60000L);
            }
        };
        this.actions = actions;
        setBackgroundColor(BG);
        setFocusable(true);
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        this.minimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
        this.maximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        this.touchSlop = viewConfiguration.getScaledTouchSlop();
        this.timelineScroller = new OverScroller(context);
        this.timelineScroller.setFriction(ViewConfiguration.getScrollFriction() * 0.55f);
        for (int i = 0; i < 7; i++) {
            this.dateRects[i] = new RectF();
            this.weekBarRects[i] = new RectF();
        }
    }

    @Override // android.view.View
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        // Parent host applies system-bar, cutout and IME insets for both views.
        this.safeInsetTop=0;this.safeInsetBottom=0;invalidate();return windowInsets;
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(this.clockRefresh);
        post(this.clockRefresh);
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        removeCallbacks(this.clockRefresh);
        super.onDetachedFromWindow();
    }

    void setPermissionGranted(boolean z) {
        this.permissionGranted = z;
        this.loading = false;
        this.errorMessage = null;
        invalidate();
    }

    void setLoading(boolean z) {
        this.loading = z;
        this.errorMessage = null;
        invalidate();
    }

    void setData(List<Models.DayRecord> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        long chosen=this.pendingDayStart>0?this.pendingDayStart:this.selectedDay<this.days.size()?this.days.get(this.selectedDay).dayStart:-1;
        this.pendingDayStart=-1;
        this.days = list;
        rebuildHourlyCache();
        this.selectedDay = Math.max(0, this.days.size() - 1);
        for(int n=0;n<this.days.size();n++)if(this.days.get(n).dayStart==chosen)this.selectedDay=n;
        this.appListPage = 0;
        this.needsTimelinePosition = true;
        this.loading = false;
        this.errorMessage = null;
        invalidate();
    }

    void setError(String str) {
        this.loading = false;
        this.errorMessage = str;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Canvas canvas2;
        super.onDraw(canvas);
        canvas.drawColor(BG);
        float dp = dp(14.0f);
        float width = getWidth() - dp(14.0f);
        float dp2 = dp(56.0f);
        float height = (getHeight() - this.safeInsetBottom) - dp2;
        float dp3 = this.safeInsetTop + dp(10.0f);
        drawText(canvas, "사용시간", dp, dp3 + dp(27.0f), 25.0f, TEXT, true, Paint.Align.LEFT);
        this.refreshRect.set(width - dp(70.0f), dp3, width, dp(38.0f) + dp3);
        drawRoundRect(canvas, this.refreshRect, dp(18.0f), CARD_2);
        drawText(canvas, this.loading ? "읽는 중" : "새로고침", this.refreshRect.centerX(), this.refreshRect.centerY() + dp(5.0f), 11.0f, this.loading ? SUBTLE : BLUE, true, Paint.Align.CENTER);
        float drawDateSelector = drawDateSelector(canvas, dp, width, dp3 + dp(46.0f));
        float dp4 = height - dp(10.0f);
        if (!this.permissionGranted) {
            drawPermissionCard(canvas, dp, width, drawDateSelector + dp(8.0f), dp4);
            canvas2 = canvas;
        } else if (this.errorMessage != null) {
            canvas2 = canvas;
            drawMessageCard(canvas2, dp, width, dp(8.0f) + drawDateSelector, dp4, "기록을 불러오지 못했습니다", this.errorMessage, true);
        } else if (this.days.isEmpty()) {
            canvas2 = canvas;
            drawMessageCard(canvas2, dp, width, dp(8.0f) + drawDateSelector, dp4, this.loading ? "사용 기록을 읽는 중" : "표시할 기록이 없습니다", this.loading ? "잠시만 기다려 주세요." : "새로고침을 눌러 다시 확인해 주세요.", false);
        } else {
            Models.DayRecord dayRecord = this.days.get(Math.min(this.selectedDay, this.days.size() - 1));
            if (this.page == 0) {
                canvas2 = canvas;
                drawTimelinePage(canvas2, dayRecord, dp, width, drawDateSelector + dp(6.0f), dp4);
            } else {
                canvas2 = canvas;
                drawAppsPage(canvas2, dayRecord, dp, width, drawDateSelector + dp(6.0f), dp4);
            }
        }
        drawBottomNavigation(canvas2, height, dp2);
    }

    private float drawDateSelector(Canvas canvas, float f, float f2, float f3) {
        long timeInMillis;
        float dp = dp(3.0f);
        float f4 = ((f2 - f) - (6.0f * dp)) / 7.0f;
        Calendar calendar = Calendar.getInstance();
        setStartOfDay(calendar);
        int i = 0;
        while (i < 7) {
            if (i < this.days.size()) {
                timeInMillis = this.days.get(i).dayStart;
            } else {
                Calendar calendar2 = (Calendar) calendar.clone();
                calendar2.add(6, i - 6);
                timeInMillis = calendar2.getTimeInMillis();
            }
            Calendar calendar3 = Calendar.getInstance();
            calendar3.setTimeInMillis(timeInMillis);
            float f5 = f + (i * (f4 + dp));
            RectF rectF = this.dateRects[i];
            rectF.set(f5, f3, f5 + f4, dp(48.0f) + f3);
            boolean z = i == this.selectedDay;
            if (z) {
                drawRoundRect(canvas, rectF, dp(15.0f), BLUE_DARK);
            }
            drawText(canvas, new SimpleDateFormat("E", Locale.KOREA).format(new Date(timeInMillis)), rectF.centerX(), dp(16.0f) + f3, 9.0f, z ? BLUE : SUBTLE, true, Paint.Align.CENTER);
            drawText(canvas, String.valueOf(calendar3.get(5)), rectF.centerX(), f3 + dp(38.0f), 15.0f, z ? TEXT : MUTED, true, Paint.Align.CENTER);
            i++;
        }
        return dp(54.0f) + f3;
    }

    private void drawTimelinePage(Canvas canvas, Models.DayRecord dayRecord, float f, float f2, float f3, float f4) {
        RectF rectF = new RectF(f, f3, f2, dp(62.0f) + f3);
        drawRoundRect(canvas, rectF, dp(18.0f), CARD);
        float f5 = (f2 - f) / 3.0f;
        drawCompactMetric(canvas, "첫 사용", valueTime(dayRecord.firstUse), f + (0.5f * f5), f3 + dp(17.0f));
        drawCompactMetric(canvas, "마지막", valueTime(dayRecord.lastUse), f + (1.5f * f5), f3 + dp(17.0f));
        drawCompactMetric(canvas, "실사용", formatDuration(dayRecord.uniqueDuration), f + (2.5f * f5), f3 + dp(17.0f));
        this.paint.setColor(LINE);
        this.paint.setStrokeWidth(dp(1.0f));
        float f6 = f + f5;
        canvas.drawLine(f6, f3 + dp(12.0f), f6, f3 + dp(50.0f), this.paint);
        float f7 = f + (f5 * 2.0f);
        canvas.drawLine(f7, f3 + dp(12.0f), f7, f3 + dp(50.0f), this.paint);
        float dp = rectF.bottom + dp(6.0f);
        sleepGapRect.set(f,dp,f2,dp(30.0f)+dp);
        long[] gap=unusedGap(dayRecord);gapStart=gap[0];gapEnd=gap[1];
        drawRoundRect(canvas,sleepGapRect,dp(11.0f),-15657182);
        String action="후보 보기";
        float buttonLeft=f2-dp(65.0f);
        drawText(canvas,ellipsizeToWidth(buildUnusedNote(dayRecord),buttonLeft-f-dp(18.0f),10.0f),f+dp(10.0f),dp+dp(20.0f),10.0f,BLUE,true,Paint.Align.LEFT);
        if(true){
            drawRoundRect(canvas,new RectF(buttonLeft,dp+dp(3.0f),f2-dp(4.0f),dp+dp(27.0f)),dp(8.0f),BLUE_DARK);
            drawText(canvas,action,(buttonLeft+f2-dp(4.0f))/2,dp+dp(19.0f),10.0f,TEXT,true,Paint.Align.CENTER);
        }
        float dp2 = dp + dp(36.0f);
        drawRoundRect(canvas, new RectF(f, dp2, f2, f4), dp(20.0f), CARD);
        drawText(canvas, this.dateTitleFormat.format(new Date(dayRecord.dayStart)) + " · 24시간", dp(16.0f) + f, dp(27.0f) + dp2, 15.0f, TEXT, true, Paint.Align.LEFT);
        drawText(canvas, "0–60분 · 터치=상세", f2 - dp(16.0f), dp2 + dp(27.0f), 9.0f, MUTED, false, Paint.Align.RIGHT);
        this.timelineRect.set(dp(14.0f) + f, dp2 + dp(42.0f), f2 - dp(14.0f), f4 - dp(34.0f));
        drawHourlyTimeline(canvas, dayRecord, this.timelineRect);
        drawTimelineLegend(canvas, dayRecord, f + dp(16.0f), f4 - dp(10.0f), f2 - dp(16.0f));
    }

    private void drawHourlyTimeline(Canvas canvas, Models.DayRecord dayRecord, RectF rectF) {
        drawRoundRect(canvas, rectF, dp(10.0f), -15921130);
        float dp = dp(64.0f);
        this.timelineContentWidth = 24.0f * dp;
        float max = Math.max(0.0f, this.timelineContentWidth - rectF.width());
        int i = 0;
        if (this.needsTimelinePosition) {
            if (isToday(dayRecord.dayStart)) {
                this.timelineScrollX = clamp(((Calendar.getInstance().get(11) * dp) + (dp / 2.0f)) - (rectF.width() / 2.0f), 0.0f, max);
            } else {
                this.timelineScrollX = clamp(((dayRecord.firstUse < 0 ? 0 : (int) ((dayRecord.firstUse - dayRecord.dayStart) / HOUR_MS)) - 0.5f) * dp, 0.0f, max);
            }
            this.needsTimelinePosition = false;
        } else {
            this.timelineScrollX = clamp(this.timelineScrollX, 0.0f, max);
        }
        canvas.save();
        canvas.clipRect(rectF);
        this.paint.setStrokeWidth(dp(1.0f));
        while (i <= 3) {
            float dp2 = rectF.top + dp(22.0f) + (((rectF.height() - dp(62.0f)) * i) / 3.0f);
            this.paint.setColor(i == 3 ? LINE : -14538701);
            canvas.drawLine(rectF.left, dp2, rectF.right, dp2, this.paint);
            i++;
        }
        drawHourlyBars(canvas, dayRecord, rectF, dp);
        canvas.restore();
        for (int minute = 0; minute <= 60; minute += 20) {
            float y = rectF.top + dp(22.0f) + ((rectF.height() - dp(62.0f)) * minute) / 60.0f;
            drawText(canvas, String.valueOf(minute), rectF.left + dp(2.0f), y + (minute == 0 ? dp(9.0f) : -dp(2.0f)), 7.5f, SUBTLE, false, Paint.Align.LEFT);
        }
        drawTimelineScrollThumb(canvas, rectF);
    }

    // 막대 하나 = 한 시간. 위쪽 끝이 정각, 아래쪽 끝이 다음 정각이다.
    // 앱별 색 구간을 실제로 사용한 분 위치에 그리고, 칸이 충분하면 "14–32"처럼 사용한 분을 적는다.
    private void drawHourlyBars(Canvas canvas, Models.DayRecord dayRecord, RectF area, float slot) {
        float top = area.top + dp(22.0f);
        float bottom = area.bottom - dp(40.0f);
        float barWidth = dp(38.0f);
        float radius = dp(12.0f);
        int currentHour = isToday(dayRecord.dayStart) ? Calendar.getInstance().get(Calendar.HOUR_OF_DAY) : -1;
        for (int hour = 0; hour < 24; hour++) {
            float center = area.left + hour * slot + slot / 2.0f - this.timelineScrollX;
            if (center + barWidth < area.left || center - barWidth > area.right) {
                continue;
            }
            boolean current = hour == currentHour;
            if (current) {
                drawRoundRect(canvas, new RectF(center - slot / 2.0f + dp(5.0f), top - dp(8.0f), center + slot / 2.0f - dp(5.0f), area.bottom - dp(6.0f)), dp(15.0f), 622686184);
            }
            RectF bar = new RectF(center - barWidth / 2.0f, top, center + barWidth / 2.0f, bottom);
            drawRoundRect(canvas, bar, radius, -15262683);
            HourlyBucket bucket = hourlyBucket(dayRecord, hour);
            if (!bucket.apps.isEmpty()) {
                drawHourSegments(canvas, hourSegments(dayRecord, hour, bucket.packages), dayRecord.dayStart + hour * HOUR_MS, bar, radius);
            }
            this.paint.setStyle(Paint.Style.STROKE);
            this.paint.setStrokeWidth(dp(current ? 2.5f : 1.0f));
            this.paint.setColor(current ? BLUE : bucket.apps.isEmpty() ? -13617082 : -10523779);
            canvas.drawRoundRect(bar, radius, radius, this.paint);
            this.paint.setStyle(Paint.Style.FILL);
            if (current) {
                drawRoundRect(canvas, new RectF(center - dp(18.0f), area.bottom - dp(32.0f), center + dp(18.0f), area.bottom - dp(7.0f)), dp(10.0f), BLUE_DARK);
            }
            drawText(canvas, String.format(Locale.KOREA, "%02d", hour), center, area.bottom - dp(16.0f), 10.0f, current ? BLUE : bucket.apps.isEmpty() ? SUBTLE : TEXT, current || !bucket.apps.isEmpty(), Paint.Align.CENTER);
        }
    }

    private void drawHourSegments(Canvas canvas, List<Models.Session> segments, long hourStart, RectF bar, float radius) {
        float scale = bar.height() / HOUR_MS;
        Path path = new Path();
        path.addRoundRect(bar, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(path);
        for (Models.Session segment : segments) {
            float y1 = bar.top + (segment.start - hourStart) * scale;
            float y2 = Math.max(y1 + dp(2.0f), bar.top + (segment.end - hourStart) * scale);
            this.paint.setColor(segment.app.color);
            canvas.drawRect(bar.left, y1, bar.right, y2, this.paint);
            if (y1 > bar.top + dp(1.0f)) {
                this.paint.setColor(-1726999267);
                canvas.drawRect(bar.left, y1, bar.right, y1 + dp(0.8f), this.paint);
            }
        }
        canvas.restore();
        for (Models.Session segment : segments) {
            int ink = labelColor(segment.app.color);
            float y1 = bar.top + (segment.start - hourStart) * scale;
            float y2 = bar.top + (segment.end - hourStart) * scale;
            String range = minuteRange(segment.start, segment.end, hourStart);
            if (y2 - y1 >= dp(27.0f)) {
                float middle = (y1 + y2) / 2.0f;
                drawText(canvas, ellipsize(segment.app.label, 4), bar.centerX(), middle - dp(1.5f), 8.0f, ink, false, Paint.Align.CENTER);
                drawText(canvas, range, bar.centerX(), middle + dp(9.0f), 8.5f, ink, true, Paint.Align.CENTER);
            } else if (y2 - y1 >= dp(12.0f)) {
                drawText(canvas, range, bar.centerX(), (y1 + y2) / 2.0f + dp(3.0f), 8.5f, ink, true, Paint.Align.CENTER);
            }
        }
    }

    // 밝은 색(주황·노랑·초록 등) 위에는 어두운 글자, 어두운 색(빨강·파랑·남색·보라) 위에는 흰 글자.
    static int labelColor(int color) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        return 0.299 * r + 0.587 * g + 0.114 * b > 150 ? BG : -1;
    }

    // 시작 분은 내림, 끝 분은 올림. 다음 정각까지 이어지면 60으로 적는다.
    static String minuteRange(long start, long end, long hourStart) {
        long from = Math.max(0, Math.min(59, (start - hourStart) / 60000));
        long to = Math.max(from + 1, Math.min(60, (end - hourStart + 59999) / 60000));
        return String.format(Locale.KOREA, "%02d–%02d", from, to);
    }

    // 표시 대상 앱의 사용을 이 시간 안으로 자르고, 같은 앱이 1분 안에 다시 이어지면 한 구간으로 합친다.
    static List<Models.Session> hourSegments(Models.DayRecord dayRecord, int hour, Set<String> packages) {
        long from = dayRecord.dayStart + hour * HOUR_MS;
        long to = from + HOUR_MS;
        List<Models.Session> clipped = new ArrayList<>();
        for (Models.Session session : dayRecord.sessions) {
            if (packages.contains(session.app.packageName) && session.start < to && session.end > from) {
                clipped.add(new Models.Session(session.app, Math.max(from, session.start), Math.min(to, session.end)));
            }
        }
        return mergeSessions(clipped, 60000L);
    }

    static List<Models.Session> mergeSessions(List<Models.Session> sessions, long joinGap) {
        List<Models.Session> sorted = new ArrayList<>(sessions);
        Collections.sort(sorted, new Comparator<Models.Session>() {
            public int compare(Models.Session a, Models.Session b) {
                return Long.compare(a.start, b.start);
            }
        });
        List<Models.Session> out = new ArrayList<>();
        Map<String, Integer> lastIndex = new LinkedHashMap<>();
        for (Models.Session session : sorted) {
            Integer index = lastIndex.get(session.app.packageName);
            if (index != null && index == out.size() - 1 && session.start - out.get(index).end <= joinGap) {
                Models.Session previous = out.get(index);
                out.set(index, new Models.Session(previous.app, previous.start, Math.max(previous.end, session.end)));
                continue;
            }
            lastIndex.put(session.app.packageName, out.size());
            out.add(session);
        }
        return out;
    }

    private static boolean isToday(long j) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        Calendar calendar2 = Calendar.getInstance();
        if (calendar.get(0) != calendar2.get(0) || calendar.get(1) != calendar2.get(1) || calendar.get(6) != calendar2.get(6)) {
            return false;
        }
        return true;
    }

    private void drawTimelineScrollThumb(Canvas canvas, RectF rectF) {
        if (this.timelineContentWidth <= rectF.width()) {
            return;
        }
        float dp = rectF.left + dp(10.0f);
        float dp2 = (rectF.right - dp(10.0f)) - dp;
        float max = Math.max(dp(42.0f), (rectF.width() * dp2) / this.timelineContentWidth);
        float width = dp + (((dp2 - max) * this.timelineScrollX) / (this.timelineContentWidth - rectF.width()));
        this.paint.setColor(-1720145921);
        canvas.drawRoundRect(new RectF(width, rectF.bottom - dp(5.0f), max + width, rectF.bottom - dp(2.0f)), dp(2.0f), dp(2.0f), this.paint);
    }

    private void rebuildHourlyCache() {
        this.hourlyBucketsByDay.clear();
        for (Models.DayRecord dayRecord : this.days) {
            ArrayList arrayList = new ArrayList(24);
            for (int i = 0; i < 24; i++) {
                arrayList.add(buildHourlyBucket(dayRecord, i));
            }
            this.hourlyBucketsByDay.put(Long.valueOf(dayRecord.dayStart), arrayList);
        }
    }

    private HourlyBucket hourlyBucket(Models.DayRecord dayRecord, int i) {
        List<HourlyBucket> list = this.hourlyBucketsByDay.get(Long.valueOf(dayRecord.dayStart));
        return (list == null || i < 0 || i >= list.size()) ? buildHourlyBucket(dayRecord, i) : list.get(i);
    }

    private static HourlyBucket buildHourlyBucket(Models.DayRecord dayRecord, int i) {
        long j = dayRecord.dayStart + (i * HOUR_MS);
        long j2 = HOUR_MS + j;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        LinkedHashMap linkedHashMap2 = new LinkedHashMap();
        for (Models.Session session : dayRecord.sessions) {
            if (session.start < j2 && session.end > j) {
                linkedHashMap2.put(session.app.packageName, session.app);
                List list = (List) linkedHashMap.get(session.app.packageName);
                if (list == null) {
                    list = new ArrayList();
                    linkedHashMap.put(session.app.packageName, list);
                }
                list.add(new long[]{Math.max(j, session.start), Math.min(j2, session.end)});
            }
        }
        ArrayList arrayList = new ArrayList();
        for (Object rawEntry : linkedHashMap.entrySet()) {
            Map.Entry entry=(Map.Entry)rawEntry;
            long mergedDuration = mergedDuration((List) entry.getValue());
            if (mergedDuration > 0) {
                arrayList.add(new HourlyAppUsage((Models.AppMeta) linkedHashMap2.get(entry.getKey()), mergedDuration));
            }
        }
        Collections.sort(arrayList, new Comparator() { // from class: com.dongwon.sleeptimeline.DashboardView$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return DashboardView.lambda$buildHourlyBucket$0((DashboardView.HourlyAppUsage) obj, (DashboardView.HourlyAppUsage) obj2);
            }
        });
        ArrayList arrayList2 = new ArrayList();
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            HourlyAppUsage hourlyAppUsage = (HourlyAppUsage) it.next();
            if (hourlyAppUsage.duration >= MIN_HOURLY_APP) {
                arrayList2.add(hourlyAppUsage);
            }
        }
        boolean z = arrayList2.isEmpty() && !arrayList.isEmpty();
        if (z) {
            arrayList2.addAll(arrayList.subList(0, Math.min(3, arrayList.size())));
        }
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        Iterator it2 = arrayList2.iterator();
        long j3 = 0;
        while (it2.hasNext()) {
            HourlyAppUsage hourlyAppUsage2 = (HourlyAppUsage) it2.next();
            linkedHashSet.add(hourlyAppUsage2.app.packageName);
            j3 += hourlyAppUsage2.duration;
        }
        return new HourlyBucket(arrayList2, linkedHashSet, j3, z);
    }

    static /* synthetic */ int lambda$buildHourlyBucket$0(HourlyAppUsage hourlyAppUsage, HourlyAppUsage hourlyAppUsage2) {
        int compare = Long.compare(hourlyAppUsage2.duration, hourlyAppUsage.duration);
        return compare != 0 ? compare : hourlyAppUsage.app.label.compareToIgnoreCase(hourlyAppUsage2.app.label);
    }

    private static long mergedDuration(List<long[]> list) {
        if (list.isEmpty()) {
            return 0L;
        }
        Collections.sort(list, new Comparator() { // from class: com.dongwon.sleeptimeline.DashboardView$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                int compare;
                compare = Long.compare(((long[]) obj)[0], ((long[]) obj2)[0]);
                return compare;
            }
        });
        long j = list.get(0)[0];
        long j2 = list.get(0)[1];
        long j3 = 0;
        for (int i = 1; i < list.size(); i++) {
            long[] jArr = list.get(i);
            if (jArr[0] <= j2) {
                j2 = Math.max(j2, jArr[1]);
            } else {
                j3 += Math.max(0L, j2 - j);
                j = jArr[0];
                j2 = jArr[1];
            }
        }
        return j3 + Math.max(0L, j2 - j);
    }

    private void drawTimelineLegend(Canvas canvas, Models.DayRecord dayRecord, float f, float f2, float f3) {
        int min = Math.min(4, dayRecord.totals.size());
        if (min == 0) {
            drawText(canvas, "기록 없음", f, f2, 10.0f, SUBTLE, false, Paint.Align.LEFT);
            return;
        }
        float f4 = f;
        for (int i = 0; i < min; i++) {
            Models.AppTotal appTotal = dayRecord.totals.get(i);
            this.paint.setColor(appTotal.app.color);
            canvas.drawCircle(dp(3.0f) + f4, f2 - dp(3.0f), dp(3.0f), this.paint);
            String ellipsize = ellipsize(appTotal.app.label, 7);
            drawText(canvas, ellipsize, f4 + dp(10.0f), f2, 9.0f, MUTED, false, Paint.Align.LEFT);
            f4 += Math.min(dp(88.0f), textWidth(ellipsize, 9.0f) + dp(22.0f));
            if (f4 > f3 - dp(48.0f)) {
                return;
            }
        }
    }

    private void drawAppsPage(Canvas canvas, Models.DayRecord dayRecord, float f, float f2, float f3, float f4) {
        DashboardView dashboardView = this;
        float f5 = f;
        List<Models.AppTotal> filteredApps = filteredApps(dayRecord);
        RectF rectF = new RectF(f5, f3, f2, dashboardView.dp(204.0f) + f3);
        dashboardView.drawRoundRect(canvas, rectF, dashboardView.dp(24.0f), -15263459);
        dashboardView.drawText(canvas, formatDuration(sumAppDuration(dayRecord)), dashboardView.dp(20.0f) + f5, dashboardView.dp(35.0f) + f3, 24.0f, TEXT, true, Paint.Align.LEFT);
        dashboardView.drawText(canvas, "총 실제 실행 시간", f5 + dashboardView.dp(20.0f), f3 + dashboardView.dp(57.0f), 11.0f, MUTED, false, Paint.Align.LEFT);
        dashboardView.drawText(canvas, "최근 7일", f2 - dashboardView.dp(20.0f), f3 + dashboardView.dp(35.0f), 11.0f, BLUE, true, Paint.Align.RIGHT);
        dashboardView.drawWeekBars(canvas, f5 + dashboardView.dp(20.0f), f2 - dashboardView.dp(18.0f), f3 + dashboardView.dp(74.0f), f3 + dashboardView.dp(168.0f));
        float dp = rectF.bottom + dashboardView.dp(12.0f);
        dashboardView.drawText(canvas, "앱별 사용시간", f5, dp + dashboardView.dp(19.0f), 16.0f, TEXT, true, Paint.Align.LEFT);
        Canvas canvas2 = canvas;
        float f6 = f2;
        dashboardView.drawText(canvas2, appFilterCaption(dayRecord,filteredApps.size()), f6, dp + dashboardView.dp(19.0f), 10.0f, MUTED, false, Paint.Align.RIGHT);
        float dp2 = dp + dashboardView.dp(30.0f);
        float dp3 = dashboardView.dp(38.0f);
        float max = Math.max(dashboardView.dp(68.0f), (f4 - dp2) - dp3);
        float dp4 = dashboardView.dp(58.0f);
        dashboardView.appsPerPage = Math.max(1, (int) Math.floor(max / dp4));
        int max2 = Math.max(1, ((filteredApps.size() + dashboardView.appsPerPage) - 1) / dashboardView.appsPerPage);
        dashboardView.appListPage = Math.max(0, Math.min(dashboardView.appListPage, max2 - 1));
        if (filteredApps.isEmpty()) {
            RectF rectF2 = new RectF(f5, dp2, f6, f4 - dashboardView.dp(6.0f));
            dashboardView.drawRoundRect(canvas2, rectF2, dashboardView.dp(18.0f), CARD);
            dashboardView.drawText(canvas2, "10분 이상 사용한 앱이 없습니다", rectF2.centerX(), rectF2.centerY() - dashboardView.dp(3.0f), 15.0f, MUTED, true, Paint.Align.CENTER);
            dashboardView.drawText(canvas, "짧게 사용한 앱은 목록에서 숨깁니다", rectF2.centerX(), rectF2.centerY() + dashboardView.dp(22.0f), 10.0f, SUBTLE, false, Paint.Align.CENTER);
            dashboardView.previousPageRect.setEmpty();
            dashboardView.nextPageRect.setEmpty();
            appRowRects.clear();
            appRowItems.clear();
            return;
        }
        appRowRects.clear();
        appRowItems.clear();
        int i = dashboardView.appListPage * dashboardView.appsPerPage;
        int min = Math.min(filteredApps.size(), dashboardView.appsPerPage + i);
        dashboardView.drawRoundRect(canvas2, new RectF(f5, dp2, f6, ((min - i) * dp4) + dp2), dashboardView.dp(21.0f), CARD);
        float f7 = dp2;
        int i2 = i;
        while (i2 < min) {
            dashboardView.drawCompactAppRow(canvas2, filteredApps.get(i2), f5, f6, f7, dp4);
            appRowRects.add(new RectF(f5, f7, f6, f7 + dp4));
            appRowItems.add(filteredApps.get(i2));
            float f8 = dp4;
            DashboardView dashboardView2 = dashboardView;
            float f9 = f7 + f8;
            if (i2 < min - 1) {
                dashboardView2.paint.setColor(-14144202);
                dashboardView2.paint.setStrokeWidth(dashboardView2.dp(0.7f));
                canvas.drawLine(f + dashboardView2.dp(60.0f), f9, f2 - dashboardView2.dp(14.0f), f9, dashboardView2.paint);
            }
            i2++;
            canvas2 = canvas;
            f6 = f2;
            f7 = f9;
            dashboardView = dashboardView2;
            dp4 = f8;
            f5 = f;
        }
        DashboardView dashboardView3 = dashboardView;
        dashboardView3.drawPager(canvas, f, f2, Math.min(f4 - dp3, f7 + dashboardView3.dp(5.0f)), dashboardView3.appListPage, max2);
    }

    private void drawWeekBars(Canvas canvas, float f, float f2, float f3, float f4) {
        char c;
        Iterator<Models.DayRecord> it = this.days.iterator();
        long j = 1;
        while (it.hasNext()) {
            j = Math.max(j, sumAppDuration(it.next()));
        }
        long max = Math.max(2L, ((j + HOUR_MS) - 1) / HOUR_MS);
        if ((max & 1) != 0) {
            max++;
        }
        long j2 = max;
        long j3 = j2 * HOUR_MS;
        float dp = f2 - dp(43.0f);
        float f5 = (dp - f) / 7.0f;
        float min = Math.min(dp(17.0f), f5 - dp(10.0f));
        this.paint.setStrokeWidth(dp(1.0f));
        this.paint.setColor(-11841964);
        canvas.drawLine(f, f3, dp, f3, this.paint);
        float f6 = f4 - f3;
        float f7 = f3 + (f6 / 2.0f);
        canvas.drawLine(f, f7, dp, f7, this.paint);
        canvas.drawLine(f, f4, dp, f4, this.paint);
        long j4 = 0;
        drawText(canvas, j2 + "시간", f2, f3 + dp(4.0f), 9.0f, MUTED, false, Paint.Align.RIGHT);
        Canvas canvas2 = canvas;
        drawText(canvas2, (j2 / 2) + "시간", f2, f7 + dp(4.0f), 9.0f, MUTED, false, Paint.Align.RIGHT);
        DashboardView dashboardView = this;
        int i = 0;
        while (i < 7) {
            float f8 = f + ((i + 0.5f) * f5);
            Models.DayRecord dayRecord = i < dashboardView.days.size() ? dashboardView.days.get(i) : null;
            float f9 = j3;
            float min2 = f4 - ((Math.min(dayRecord==null?0:sumAppDuration(dayRecord), j3) * f6) / f9);
            float f10 = f5 / 2.0f;
            long sumAppDuration = dayRecord == null ? j4 : sumAppDuration(dayRecord);
            dashboardView.weekBarRects[i].set(f8 - f10, f3 - dashboardView.dp(5.0f), f10 + f8, dashboardView.dp(29.0f) + f4);
            if (dayRecord != null && sumAppDuration > j4) {
                float f11 = min / 2.0f;
                RectF rectF = new RectF(f8 - f11, min2, f11 + f8, f4);
                Path path = new Path();
                path.addRoundRect(rectF, dashboardView.dp(7.0f), dashboardView.dp(7.0f), Path.Direction.CW);
                canvas2.save();
                canvas2.clipPath(path);
                int min3 = Math.min(3, dayRecord.totals.size());
                float f12 = f4;
                long j5 = j4;
                int i2 = 0;
                while (i2 < min3) {
                    Models.AppTotal appTotal = dayRecord.totals.get(i2);
                    int i3 = min3;
                    j5 += appTotal.duration;
                    dashboardView.paint.setColor(appTotal.app.color);
                    float max2 = Math.max(min2, f12 - ((appTotal.duration * f6) / f9));
                    canvas.drawRect(rectF.left, max2, rectF.right, f12, dashboardView.paint);
                    i2++;
                    f12 = max2;
                    min2 = min2;
                    min3 = i3;
                }
                float f13 = min2;
                long j6 = j4;
                if (Math.max(j6, sumAppDuration - j5) <= j6 || f12 <= f13) {
                    j4 = j6;
                    canvas2 = canvas;
                } else {
                    dashboardView.paint.setColor(-6445646);
                    j4 = j6;
                    canvas.drawRect(rectF.left, f13, rectF.right, f12, dashboardView.paint);
                    canvas2 = canvas;
                }
                canvas2.restore();
            }
            String format = dayRecord == null ? "-" : new SimpleDateFormat("E", Locale.KOREA).format(new Date(dayRecord.dayStart));
            if (i != dashboardView.selectedDay) {
                c = 0;
            } else {
                c = 0;
                dashboardView.drawRoundRect(canvas2, new RectF(f8 - dashboardView.dp(14.0f), dashboardView.dp(4.0f) + f4, dashboardView.dp(14.0f) + f8, dashboardView.dp(30.0f) + f4), dashboardView.dp(13.0f), -1841428);
            }
            dashboardView.drawText(canvas2, format, f8, f4 + dashboardView.dp(22.0f), 9.0f, i == dashboardView.selectedDay ? -15395044 : MUTED, true, Paint.Align.CENTER);
            i++;
            dashboardView = this;
            canvas2 = canvas;
        }
    }

    private void drawCompactAppRow(Canvas canvas, Models.AppTotal appTotal, float f, float f2, float f3, float f4) {
        float dp = dp(36.0f);
        float dp2 = f + dp(14.0f);
        drawAppIcon(canvas, appTotal.app, dp2, f3 + ((f4 - dp) / 2.0f), dp);
        float dp3 = dp2 + dp(48.0f);
        drawText(canvas, ellipsize(appTotal.app.label, 21), dp3, f3 + dp(23.0f), 13.0f, TEXT, true, Paint.Align.LEFT);
        drawText(canvas, formatDuration(appTotal.duration), f2 - dp(15.0f), f3 + dp(23.0f), 13.0f, BLUE, true, Paint.Align.RIGHT);
        drawText(canvas, ellipsizeToWidth(formatBlocks(Models.blocks(appTotal.sessions, Long.MIN_VALUE, Long.MAX_VALUE, 120000L)), (f2 - dp3) - dp(17.0f), 9.0f), dp3, f3 + dp(43.0f), 9.0f, MUTED, false, Paint.Align.LEFT);
    }

    private void drawPager(Canvas canvas, float f, float f2, float f3, int i, int i2) {
        if (i2 > 1) {
            this.previousPageRect.set(f, f3, dp(82.0f) + f, dp(34.0f) + f3);
            this.nextPageRect.set(f2 - dp(82.0f), f3, f2, dp(34.0f) + f3);
            RectF rectF = this.previousPageRect;
            float dp = dp(12.0f);
            int i3 = CARD_2;
            drawRoundRect(canvas, rectF, dp, i > 0 ? CARD_2 : -15723750);
            RectF rectF2 = this.nextPageRect;
            float dp2 = dp(12.0f);
            int i4 = i2 - 1;
            if (i >= i4) {
                i3 = -15723750;
            }
            drawRoundRect(canvas, rectF2, dp2, i3);
            drawText(canvas, "이전", this.previousPageRect.centerX(), dp(22.0f) + f3, 11.0f, i > 0 ? TEXT : SUBTLE, true, Paint.Align.CENTER);
            drawText(canvas, "다음", this.nextPageRect.centerX(), f3 + dp(22.0f), 11.0f, i < i4 ? TEXT : SUBTLE, true, Paint.Align.CENTER);
            drawText(canvas, (i + 1) + " / " + i2, (f + f2) / 2.0f, f3 + dp(22.0f), 10.0f, MUTED, false, Paint.Align.CENTER);
            return;
        }
        this.previousPageRect.setEmpty();
        this.nextPageRect.setEmpty();
    }

    private void drawBottomNavigation(Canvas canvas,float top,float height){
        paint.setColor(0xff10131b);canvas.drawRect(0,top,getWidth(),top+height,paint);
        paint.setColor(0xff2a3244);canvas.drawRect(dp(12.0f),top,getWidth()-dp(12.0f),top+dp(1.0f),paint);
        RectF[] rects={firstNavRect,fifthNavRect,secondNavRect,thirdNavRect};
        String[] labels={"시간대","하루","앱별","수면"};
        float left=dp(12.0f),gap=dp(5.0f),width=(getWidth()-dp(24.0f)-gap*3)/4;
        for(int n=0;n<rects.length;n++){rects[n].set(left,top,left+width,top+height);left+=width+gap;drawBottomItem(canvas,rects[n],labels[n],(n==0&&page==0)||(n==2&&page==1),true);}
    }

    private void drawBottomItem(Canvas canvas,RectF rect,String label,boolean active,boolean unused){
        float left=rect.left,right=rect.right;
        RectF box=new RectF(left,rect.top+dp(6.0f),right,rect.top+dp(46.0f));
        if(active)drawRoundRect(canvas,box,dp(9.0f),0xffaaa1ff);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(active?0xffaaa1ff:0xff2a3244);paint.setStrokeWidth(dp(1.0f));canvas.drawRoundRect(box,dp(9.0f),dp(9.0f),paint);paint.setStyle(Paint.Style.FILL);
        drawText(canvas,label,rect.centerX(),rect.top+dp(31.0f),14.0f,active?0xff141626:0xffeef0fa,active,Paint.Align.CENTER);
    }

    private void drawPermissionCard(Canvas canvas, float f, float f2, float f3, float f4) {
        drawRoundRect(canvas, new RectF(f, f3, f2, f4), dp(22.0f), CARD);
        drawText(canvas, "사용 정보 접근이 필요합니다", f + dp(20.0f), f3 + dp(42.0f), 19.0f, TEXT, true, Paint.Align.LEFT);
        drawMultiline(canvas, "Android 설정에서 ‘사용 흐름’을 허용하면 앱별 사용 시각을 읽을 수 있습니다. 사용 기록은 기기 안에서만 처리합니다.", f + dp(20.0f), f3 + dp(78.0f), (f2 - f) - dp(40.0f), 13.0f, MUTED, dp(21.0f));
        this.permissionRect.set(dp(20.0f) + f, f4 - dp(66.0f), f2 - dp(20.0f), f4 - dp(18.0f));
        drawRoundRect(canvas, this.permissionRect, dp(15.0f), BLUE);
        drawText(canvas, "사용 정보 접근 열기", this.permissionRect.centerX(), dp(5.0f) + this.permissionRect.centerY(), 14.0f, -16248802, true, Paint.Align.CENTER);
    }

    private void drawMessageCard(Canvas canvas, float f, float f2, float f3, float f4, String str, String str2, boolean z) {
        drawRoundRect(canvas, new RectF(f, f3, f2, f4), dp(22.0f), CARD);
        drawText(canvas, str, f + dp(20.0f), dp(42.0f) + f3, 18.0f, z ? -25431 : TEXT, true, Paint.Align.LEFT);
        drawMultiline(canvas, str2, f + dp(20.0f), f3 + dp(76.0f), (f2 - f) - dp(40.0f), 13.0f, MUTED, dp(21.0f));
    }

    private void drawCompactMetric(Canvas canvas, String str, String str2, float f, float f2) {
        drawText(canvas, str, f, f2, 9.0f, SUBTLE, true, Paint.Align.CENTER);
        drawText(canvas, str2, f, f2 + dp(26.0f), 15.0f, TEXT, true, Paint.Align.CENTER);
    }

    private void drawAppIcon(Canvas canvas, Models.AppMeta appMeta, float f, float f2, float f3) {
        RectF rectF = new RectF(f, f2, f + f3, f3 + f2);
        if (appMeta.icon != null && !appMeta.icon.isRecycled()) {
            canvas.drawBitmap(appMeta.icon, (Rect) null, rectF, this.paint);
        } else {
            drawRoundRect(canvas, rectF, dp(11.0f), appMeta.color);
            drawText(canvas, appMeta.label.isEmpty() ? "?" : appMeta.label.substring(0, 1), rectF.centerX(), rectF.centerY() + dp(6.0f), 16.0f, -1, true, Paint.Align.CENTER);
        }
    }

    private void drawRoundRect(Canvas canvas, RectF rectF, float f, int i) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(i);
        canvas.drawRoundRect(rectF, f, f, this.paint);
    }

    private void drawText(Canvas canvas, String str, float f, float f2, float f3, int i, boolean z, Paint.Align align) {
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(i);
        this.paint.setTextSize(sp(f3));
        this.paint.setTypeface(Typeface.create("sans", z ? 1 : 0));
        this.paint.setTextAlign(align);
        if (str == null) {
            str = "";
        }
        canvas.drawText(str, f, f2, this.paint);
    }

    private void drawMultiline(Canvas canvas, String str, float f, float f2, float f3, float f4, int i, float f5) {
        this.paint.setTextSize(sp(f4));
        this.paint.setTypeface(Typeface.create("sans", 0));
        this.paint.setTextAlign(Paint.Align.LEFT);
        this.paint.setColor(i);
        String[] split = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String str2 : split) {
            if (this.paint.measureText(sb.length() == 0 ? str2 : String.valueOf(sb) + " " + str2) > f3 && sb.length() > 0) {
                canvas.drawText(sb.toString(), f, f2, this.paint);
                f2 += f5;
                sb.setLength(0);
                sb.append(str2);
            } else {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(str2);
            }
        }
        if (sb.length() > 0) {
            canvas.drawText(sb.toString(), f, f2, this.paint);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean z = false;
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.downX = motionEvent.getX();
                this.downY = motionEvent.getY();
                this.lastTouchX = this.downX;
                this.moved = false;
                if (this.page == 0 && this.timelineRect.contains(this.downX, this.downY)) {
                    z = true;
                }
                this.timelineGesture = z;
                if (this.timelineGesture) {
                    if (!this.timelineScroller.isFinished()) {
                        this.timelineScroller.abortAnimation();
                    }
                    recycleVelocityTracker();
                    this.velocityTracker = VelocityTracker.obtain();
                    this.velocityTracker.addMovement(motionEvent);
                }
                return true;
            case 1:
                if (this.timelineGesture && this.velocityTracker != null) {
                    this.velocityTracker.addMovement(motionEvent);
                    this.velocityTracker.computeCurrentVelocity(1000, this.maximumFlingVelocity);
                    float xVelocity = this.velocityTracker.getXVelocity();
                    if (this.moved && Math.abs(xVelocity) >= this.minimumFlingVelocity) {
                        this.timelineScroller.fling(Math.round(this.timelineScrollX), 0, Math.round((-xVelocity) * 1.35f), 0, 0, Math.max(0, Math.round(this.timelineContentWidth - this.timelineRect.width())), 0, 0);
                        postInvalidateOnAnimation();
                    }
                }
                if (!this.moved) {
                    handleTap(motionEvent.getX(), motionEvent.getY());
                }
                recycleVelocityTracker();
                this.timelineGesture = false;
                return true;
            case 2:
                if (this.timelineGesture) {
                    if (this.velocityTracker != null) {
                        this.velocityTracker.addMovement(motionEvent);
                    }
                    if (Math.abs(motionEvent.getX() - this.downX) > this.touchSlop) {
                        this.moved = true;
                    }
                    if (this.moved) {
                        this.timelineScrollX -= (motionEvent.getX() - this.lastTouchX) * 1.25f;
                        clampTimelineScroll();
                        postInvalidateOnAnimation();
                    }
                    this.lastTouchX = motionEvent.getX();
                } else if (distance(this.downX, this.downY, motionEvent.getX(), motionEvent.getY()) > dp(10.0f)) {
                    this.moved = true;
                }
                return true;
            case 3:
                this.moved = false;
                this.timelineGesture = false;
                recycleVelocityTracker();
                return true;
            default:
                return super.onTouchEvent(motionEvent);
        }
    }

    @Override // android.view.View
    public void computeScroll() {
        super.computeScroll();
        if (this.timelineScroller.computeScrollOffset()) {
            this.timelineScrollX = this.timelineScroller.getCurrX();
            clampTimelineScroll();
            postInvalidateOnAnimation();
        }
    }

    private void recycleVelocityTracker() {
        if (this.velocityTracker != null) {
            this.velocityTracker.recycle();
            this.velocityTracker = null;
        }
    }

    private void handleTap(float f, float f2) {
        int i;
        if(this.fifthNavRect.contains(f,f2)){this.actions.openWebPage("report");return;}
        if(this.thirdNavRect.contains(f,f2)){this.actions.openWebPage("sleep");return;}
        if(this.page==0&&this.permissionGranted&&!this.loading&&this.sleepGapRect.contains(f,f2)&&selectedDay<days.size()){this.actions.showSleepCandidates(days.get(selectedDay).dayStart,gapStart,gapEnd);return;}
        if (this.refreshRect.contains(f, f2)) {
            this.actions.refreshData();
            return;
        }
        if (!this.permissionGranted && this.permissionRect.contains(f, f2)) {
            this.actions.openUsageAccessSettings();
            return;
        }
        if (this.firstNavRect.contains(f, f2)) {
            this.page = 0;
            invalidate();
            return;
        }
        if (this.secondNavRect.contains(f, f2)) {
            this.page = 1;
            this.appListPage = 0;
            invalidate();
            return;
        }
        for (int i2 = 0; i2 < this.dateRects.length; i2++) {
            if (this.dateRects[i2].contains(f, f2) && i2 < this.days.size()) {
                this.selectedDay = i2;
                this.appListPage = 0;
                this.needsTimelinePosition = true;
                invalidate();
                return;
            }
        }
        if (this.page == 1) {
            for (int i3 = 0; i3 < this.weekBarRects.length; i3++) {
                if (this.weekBarRects[i3].contains(f, f2) && i3 < this.days.size()) {
                    this.selectedDay = i3;
                    this.appListPage = 0;
                    this.needsTimelinePosition = true;
                    invalidate();
                    return;
                }
            }
            if (this.selectedDay < this.days.size()) {
                i = Math.max(1, ((filteredApps(this.days.get(this.selectedDay)).size() + this.appsPerPage) - 1) / this.appsPerPage);
            } else {
                i = 1;
            }
            for (int row = 0; row < appRowRects.size(); row++) {
                if (appRowRects.get(row).contains(f, f2) && this.selectedDay < this.days.size()) {
                    this.actions.showAppSessions(appRowItems.get(row), this.days.get(this.selectedDay).dayStart);
                    return;
                }
            }
            if (this.previousPageRect.contains(f, f2) && this.appListPage > 0) {
                this.appListPage--;
                invalidate();
                return;
            } else if (this.nextPageRect.contains(f, f2) && this.appListPage < i - 1) {
                this.appListPage++;
                invalidate();
                return;
            }
        }
        if (this.page == 0 && this.timelineRect.contains(f, f2) && this.selectedDay < this.days.size()) {
            Models.DayRecord dayRecord = this.days.get(this.selectedDay);
            int max = Math.max(0, Math.min(23, (int) Math.floor(((f - this.timelineRect.left) + this.timelineScrollX) / dp(64.0f))));
            long hourStart = dayRecord.dayStart + (max * HOUR_MS);
            // 막대에는 10분 이상 앱만 그리지만, 상세에는 그 시간의 사용을 모두 묶어서 보여 준다.
            List<Models.Session> inHour = new ArrayList<>();
            for (Models.Session session : dayRecord.sessions) {
                if (session.start < hourStart + HOUR_MS && session.end > hourStart) {
                    inHour.add(session);
                }
            }
            this.actions.showAppsAt(hourStart, inHour);
        }
    }

    private void clampTimelineScroll() {
        this.timelineScrollX = clamp(this.timelineScrollX, 0.0f, Math.max(0.0f, this.timelineContentWidth - this.timelineRect.width()));
    }

    void setPage(int value){this.page=value==1?1:0;invalidate();}
    private long pendingDayStart=-1;
    // 데이터가 아직 없으면 다음 setData에서 이 날짜를 고른다.
    void selectDayStart(long dayStart){pendingDayStart=dayStart;for(int n=0;n<days.size();n++)if(days.get(n).dayStart==dayStart){selectedDay=n;appListPage=0;needsTimelinePosition=true;invalidate();return;}}
    boolean backToTimeline(){if(page!=0){page=0;invalidate();return true;}return false;}
    void setSleepRecords(org.json.JSONArray records){sleepRecords=records;invalidate();}
    private boolean hasSleepRecord(long start,long end){for(int n=0;n<sleepRecords.length();n++){org.json.JSONObject r=sleepRecords.optJSONObject(n);if(r!=null&&"confirmed".equals(r.optString("state"))&&r.optLong("start")<end&&r.optLong("end")>start)return true;}return false;}
    private long[] unusedGap(Models.DayRecord dayRecord){
        if(selectedDay>0&&selectedDay<days.size()){Models.DayRecord before=days.get(selectedDay-1);if(before.lastUse>0&&dayRecord.firstUse>before.lastUse)return new long[]{before.lastUse,dayRecord.firstUse};}
        return new long[]{-1,-1};
    }

    private String buildUnusedNote(Models.DayRecord dayRecord) {
        if (this.selectedDay > 0 && this.selectedDay < this.days.size()) {
            Models.DayRecord dayRecord2 = this.days.get(this.selectedDay - 1);
            if (dayRecord2.lastUse > 0 && dayRecord.firstUse > dayRecord2.lastUse) {
                long j = dayRecord.firstUse - dayRecord2.lastUse;
                return "미사용  " + this.timeFormat.format(new Date(dayRecord2.lastUse)) + " → " + this.timeFormat.format(new Date(dayRecord.firstUse)) + "  ·  " + formatDuration(j);
            }
            return "미사용 구간  —";
        }
        return "미사용 구간  —";
    }

    private static List<Models.AppTotal> filteredApps(Models.DayRecord dayRecord) {
        List<Models.AppTotal> out=new ArrayList<Models.AppTotal>();for(Models.AppTotal a:dayRecord.totals)if(a.duration>=MIN_VISIBLE_APP)out.add(a);
        if(out.isEmpty()&&!dayRecord.totals.isEmpty())out.addAll(dayRecord.totals.subList(0,Math.min(3,dayRecord.totals.size())));return out;
    }

    private static String appFilterCaption(Models.DayRecord day,int count){for(Models.AppTotal a:day.totals)if(a.duration>=MIN_VISIBLE_APP)return "10분 이상 · "+count+"개";return count>0?"10분 미만 · 상위 "+count+"개":"10분 이상 · 0개";}

    // "3번 · 14:03–14:21 · 22:10 +1": 2분 안에 다시 연 것은 한 번으로 센다.
    private String formatBlocks(List<Models.UseBlock> blocks) {
        StringBuilder sb = new StringBuilder().append(blocks.size()).append("번");
        int shown = Math.min(2, blocks.size());
        for (int i = 0; i < shown; i++) {
            Models.UseBlock block = blocks.get(i);
            String start = this.timeFormat.format(new Date(block.start)), end = this.timeFormat.format(new Date(block.end));
            sb.append(" · ").append(start.equals(end) ? start : start + "–" + end);
        }
        if (blocks.size() > shown) {
            sb.append(" +").append(blocks.size() - shown);
        }
        return sb.toString();
    }

    private String ellipsizeToWidth(String str, float f, float f2) {
        this.paint.setTextSize(sp(f2));
        this.paint.setTypeface(Typeface.create("sans", 0));
        if (this.paint.measureText(str) <= f) {
            return str;
        }
        while (str.length() > 1 && this.paint.measureText(str + "…") > f) {
            str = str.substring(0, str.length() - 1);
        }
        return str + "…";
    }

    private static String ellipsize(String str, int i) {
        if (str == null) {
            return "";
        }
        if (str.length() <= i) {
            return str;
        }
        return str.substring(0, Math.max(1, i - 1)) + "…";
    }

    private String valueTime(long j) {
        return j < 0 ? "—" : this.timeFormat.format(new Date(j));
    }

    private static long sumAppDuration(Models.DayRecord dayRecord) {
        Iterator<Models.AppTotal> it = dayRecord.totals.iterator();
        long j = 0;
        while (it.hasNext()) {
            j += it.next().duration;
        }
        return j;
    }

    private static String formatDuration(long j) {
        if (j <= 0) {
            return "0분";
        }
        long max = Math.max(1L, Math.round(j / 60000.0d));
        long j2 = max / 60;
        long j3 = max % 60;
        return j2 == 0 ? max + "분" : j3 == 0 ? j2 + "시간" : j2 + "시간 " + j3 + "분";
    }

    private float textWidth(String str, float f) {
        this.paint.setTextSize(sp(f));
        this.paint.setTypeface(Typeface.create("sans", 0));
        return this.paint.measureText(str);
    }

    private static float distance(float f, float f2, float f3, float f4) {
        float f5 = f3 - f;
        float f6 = f4 - f2;
        return (float) Math.sqrt((f5 * f5) + (f6 * f6));
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private float dp(float f) {
        return f * getResources().getDisplayMetrics().density;
    }

    private float sp(float f) {
        return f * getResources().getDisplayMetrics().scaledDensity;
    }

    private static void setStartOfDay(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
    }
}
