package com.babymeal.app;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 统一的视觉语言：暖橘 + 奶油白，圆角大卡片，柔和描边 */
public final class UI {

    public static final int BG = 0xFFFFF9F4;
    public static final int CARD = 0xFFFFFFFF;
    public static final int LINE = 0xFFF0E4D8;
    public static final int TEXT = 0xFF3D2E26;
    public static final int TEXT2 = 0xFF8B7565;
    public static final int TEXT3 = 0xFFB5A395;
    public static int PRIMARY = 0xFFFF8A4C;
    public static int PRIMARY_SOFT = 0xFFFFF0E6;
    public static int ACCENT = 0xFF7FB069;
    public static int ACCENT_SOFT = 0xFFEDF5E8;
    public static int YELLOW = 0xFFF5B942;
    public static int YELLOW_SOFT = 0xFFFFF6E0;
    public static int PINK = 0xFFE88C9A;
    public static int PINK_SOFT = 0xFFFDEDF0;
    public static int BLUE = 0xFF6FA8DC;
    public static int BLUE_SOFT = 0xFFEDF4FB;

    public static int primarySoft() {
        return PRIMARY_SOFT;
    }

    public static int accentSoft() {
        return ACCENT_SOFT;
    }

    public static int yellowSoft() {
        return YELLOW_SOFT;
    }

    public static int pinkSoft() {
        return PINK_SOFT;
    }

    public static int blueSoft() {
        return BLUE_SOFT;
    }

    private static Typeface regular;
    private static Typeface bold;

    private UI() {
    }

    public static void init(Typeface reg, Typeface bd) {
        regular = reg;
        bold = bd;
    }

    public static Typeface reg() {
        return regular == null ? Typeface.DEFAULT : regular;
    }

    public static Typeface bd() {
        return bold == null ? Typeface.DEFAULT_BOLD : bold;
    }

    public static int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                Resources.getSystem().getDisplayMetrics());
    }

    public static int sp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v,
                Resources.getSystem().getDisplayMetrics());
    }

    /** 纯色圆角背景 */
    public static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    public static GradientDrawable roundStroke(int fill, int stroke, int strokeWidthDp, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(strokeWidthDp), stroke);
        return g;
    }

    /** 卡片：白底 + 极淡描边 + 大圆角 */
    public static GradientDrawable card(float radiusDp) {
        return roundStroke(CARD, LINE, 1, radiusDp);
    }

    /** 顶部渐晕容器 */
    public static GradientDrawable headerGradient() {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.BL_TR,
                new int[]{0xFFFFF3E9, 0xFFFFF9F4});
        return g;
    }

    public static TextView text(String s, float sizeSp, int color, boolean isBold) {
        TextView t = new TextView(app());
        t.setText(s);
        t.setTextSize(sizeSp);
        t.setTextColor(color);
        t.setTypeface(isBold ? bd() : reg());
        t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(3), 1f);
        return t;
    }

    public static TextView label(String s, float sizeSp, int color, boolean isBold) {
        TextView t = text(s, sizeSp, color, isBold);
        return t;
    }

    /** 小胶囊标签 */
    public static TextView chip(String s, int textColor, int bgColor) {
        TextView t = text(s, 11.5f, textColor, false);
        t.setBackground(round(bgColor, 20));
        int ph = dp(9), pv = dp(5);
        t.setPadding(ph, pv, ph, pv);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    public static LinearLayout row() {
        LinearLayout l = new LinearLayout(app());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout column() {
        LinearLayout l = new LinearLayout(app());
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams match(int h) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h);
    }

    public static void margin(View v, int l, int t, int r, int b) {
        View parent = v;
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) v.getLayoutParams();
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        v.setLayoutParams(p);
    }

    public static void setMargins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
    }

    /** 分隔细线 */
    public static View divider() {
        View v = new View(app());
        v.setBackgroundColor(LINE);
        v.setLayoutParams(match(dp(1)));
        return v;
    }

    public static void pressable(View v, float scale) {
        v.setOnTouchListener((view, ev) -> {
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(scale).scaleY(scale).setDuration(90).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(130).start();
                    break;
            }
            return false;
        });
    }

    public static int alpha(int color, float a) {
        int A = (int) (255 * a);
        return Color.argb(A, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static android.content.Context appContext;

    public static void setContext(android.content.Context c) {
        appContext = c.getApplicationContext();
    }

    public static android.content.Context app() {
        return appContext;
    }

    /** 类别配色 */
    public static int catColor(String cat) {
        switch (cat) {
            case "staple": return YELLOW;
            case "meat": return PRIMARY;
            case "veg": return ACCENT;
            case "soup": return BLUE;
            default: return PINK;
        }
    }

    public static int catSoft(String cat) {
        switch (cat) {
            case "staple": return YELLOW_SOFT;
            case "meat": return PRIMARY_SOFT;
            case "veg": return ACCENT_SOFT;
            case "soup": return BLUE_SOFT;
            default: return PINK_SOFT;
        }
    }
}
