package com.babymeal.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** 「今日营养报告」页：12 项关键营养素的达标情况 + 具体补救建议 */
public class NutritionScreen {

    private final MainActivity act;
    private final Store store;
    private final MenuEngine.Day day;
    private LinearLayout root;

    public NutritionScreen(MainActivity act, MenuEngine.Day day) {
        this.act = act;
        this.store = act.store;
        this.day = day;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        root.addView(header());
        root.addView(scoreCard());
        root.addView(listCard());
        root.addView(gapCard());
        root.addView(milkNote());
        root.addView(disclaimer());
        View s = new View(act);
        s.setLayoutParams(UI.match(UI.dp(28)));
        root.addView(s);
        return root;
    }

    /* ---------------- 头部 ---------------- */

    private View header() {
        LinearLayout h = UI.column();
        h.setBackground(UI.headerGradient());
        h.setPadding(UI.dp(20), UI.dp(16), UI.dp(20), UI.dp(8));

        LinearLayout bar = UI.row();
        TextView back = UI.text("‹", 26f, UI.TEXT, false);
        back.setTypeface(UI.bd());
        back.setGravity(Gravity.CENTER);
        back.setBackground(UI.round(0xF2FFFFFF, 18));
        LinearLayout.LayoutParams bp = UI.lp(UI.dp(36), UI.dp(36));
        bar.addView(back, bp);
        UI.pressable(back, 0.92f);
        back.setOnClickListener(v -> act.closeOverlay());
        bar.addView(new View(act), UI.lp(0, UI.dp(1), 1f));
        h.addView(bar);

        TextView t = UI.text("今日营养报告", 24f, UI.TEXT, true);
        LinearLayout.LayoutParams tp = UI.wrap();
        tp.topMargin = UI.dp(12);
        h.addView(t, tp);

        TextView sub = UI.text(MenuEngine.ageName(store.age()) + " 每日推荐量 · 仅统计菜品贡献，不含奶和水果", 12f, UI.TEXT2, false);
        sub.setLineSpacing(UI.dp(3), 1f);
        LinearLayout.LayoutParams sp = UI.matchWrap();
        sp.topMargin = UI.dp(7);
        h.addView(sub, sp);
        return h;
    }

    /* ---------------- 均衡分 ---------------- */

    private View scoreCard() {
        double[] intake = day.intake();
        int age = store.age();
        int score = Nutrition.balanceScore(intake, age);

        LinearLayout card = UI.column();
        card.setBackground(UI.card(20));
        card.setPadding(UI.dp(20), UI.dp(18), UI.dp(20), UI.dp(18));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        LinearLayout top = UI.row();
        LinearLayout left = UI.column();
        TextView num = UI.text(String.valueOf(score), 40f, scoreColor(score), true);
        // 40sp 的「100」比「98」宽出一截，若用 wrap_content 会被父容器压掉最后一位，
        // 显示成「1」。固定最小宽度（足够放下三位数）+ 居中，两位与三位都对齐。
        LinearLayout.LayoutParams np = UI.wrap();
        np.width = UI.dp(74);
        num.setGravity(Gravity.CENTER);
        num.setIncludeFontPadding(false);
        left.addView(num, np);
        TextView lbl = UI.text("均衡分", 11f, UI.TEXT3, false);
        lbl.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = UI.wrap();
        lp.width = UI.dp(74);
        lp.topMargin = UI.dp(-2);
        left.addView(lbl, lp);
        top.addView(left, UI.wrap());

        LinearLayout right = UI.column();
        TextView word = UI.text(Nutrition.scoreWord(score), 16f, scoreColor(score), true);
        right.addView(word);
        int good = 0, ok = 0, low = 0;
        for (String k : Nutrition.KEYS) {
            int lv = Nutrition.level(Nutrition.ratio(intake, k, age));
            if (lv == Nutrition.LEVEL_GOOD) good++;
            else if (lv == Nutrition.LEVEL_OK) ok++;
            else low++;
        }
        TextView detail = UI.text("达标 " + good + " 项 · 基本够 " + ok + " 项 · 偏少 " + low + " 项", 12f, UI.TEXT2, false);
        LinearLayout.LayoutParams dp = UI.matchWrap();
        dp.topMargin = UI.dp(7);
        right.addView(detail, dp);
        TextView hint = UI.text(summaryWord(good, low), 11.5f, UI.TEXT3, false);
        hint.setLineSpacing(UI.dp(3), 1f);
        LinearLayout.LayoutParams hp = UI.matchWrap();
        hp.topMargin = UI.dp(5);
        right.addView(hint, hp);

        LinearLayout.LayoutParams rp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = UI.dp(18);
        top.addView(right, rp);
        card.addView(top);

        // 分段色条
        LinearLayout bar = UI.row();
        bar.setBackground(UI.round(0xFFF6ECE3, 6));
        addSeg(bar, good, 0xFF4E9A54);
        addSeg(bar, ok, 0xFFD99A2B);
        addSeg(bar, low, 0xFFE06B5C);
        LinearLayout.LayoutParams bp = UI.match(UI.dp(7));
        bp.topMargin = UI.dp(16);
        card.addView(bar, bp);

        return card;
    }

    private void addSeg(LinearLayout bar, int n, int color) {
        if (n <= 0) return;
        View v = new View(act);
        v.setBackgroundColor(color);
        bar.addView(v, UI.lp(0, LinearLayout.LayoutParams.MATCH_PARENT, n));
    }

    private int scoreColor(int s) {
        if (s >= 90) return 0xFF4E9A54;
        if (s >= 75) return 0xFF6A9A48;
        if (s >= 55) return 0xFFD99A2B;
        return 0xFFE06B5C;
    }

    private String summaryWord(int good, int low) {
        if (low == 0) return "这一桌基本覆盖了宝宝每天需要的全部关键营养，很省心。";
        if (good >= 9) return "整体不错，只有个别项稍有欠缺，看下面的建议补一下就好。";
        if (good >= 6) return "有几项明显偏少，可以点「换一换」把对应的一餐换成更补的菜。";
        return "缺口比较明显，建议按下面建议调整，或重新生成一桌。";
    }

    /* ---------------- 12 项明细 ---------------- */

    private View listCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(20));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(6));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        LinearLayout head = UI.row();
        head.addView(UI.text("关键营养素", 15f, UI.TEXT, true), UI.lp(0, UI.dp(22), 1f));
        head.addView(UI.chip("达标线 60%", UI.TEXT3, 0xFFF3EAE2));
        card.addView(head);

        double[] intake = day.intake();
        int age = store.age();
        for (String k : Nutrition.KEYS) {
            card.addView(row(k, intake, age));
        }
        return card;
    }

    private View row(String key, double[] intake, int age) {
        LinearLayout box = UI.column();
        box.setPadding(0, UI.dp(11), 0, UI.dp(11));

        double ratio = Nutrition.ratio(intake, key, age);
        int lv = Nutrition.level(ratio);
        int color = Nutrition.levelColor(lv);

        LinearLayout top = UI.row();
        top.addView(UI.text(Nutrition.emoji(key), 13f, UI.TEXT, false));
        TextView nm = UI.text(key, 13.5f, UI.TEXT, true);
        LinearLayout.LayoutParams np = UI.wrap();
        np.leftMargin = UI.dp(7);
        top.addView(nm, np);

        TextView pct = UI.text(Math.round(ratio * 100) + "%", 12f, color, true);
        LinearLayout.LayoutParams pp = UI.wrap();
        pp.leftMargin = UI.dp(8);
        top.addView(pct, pp);

        top.addView(new View(act), UI.lp(0, UI.dp(1), 1f));

        TextView val = UI.text(Nutrition.fmt(intake[Nutrition.indexOf(key)]) + " / " +
                Nutrition.fmt(Nutrition.target(key, age)) + " " + Nutrition.unit(key), 11f, UI.TEXT3, false);
        top.addView(val);

        TextView badge = UI.chip(Nutrition.levelText(lv), color, Nutrition.levelSoft(lv));
        badge.setTextSize(10f);
        LinearLayout.LayoutParams lvp = UI.wrap();
        lvp.leftMargin = UI.dp(7);
        top.addView(badge, lvp);

        box.addView(top);

        // 进度条（满格 = 100% 推荐量，达标线 60% 处画一条刻度）
        LinearLayout track = UI.row();
        track.setBackground(UI.round(0xFFF6ECE3, 5));
        View fill = new View(act);
        fill.setBackground(UI.round(color, 5));
        float r = (float) Math.min(1.0, ratio);
        track.addView(fill, UI.lp(0, UI.dp(7), Math.max(0.015f, r)));
        View rest = new View(act);
        track.addView(rest, UI.lp(0, UI.dp(7), Math.max(0f, 1 - r)));
        LinearLayout.LayoutParams tp = UI.match(UI.dp(7));
        tp.topMargin = UI.dp(8);
        box.addView(track, tp);

        return box;
    }

    /* ---------------- 缺口建议 ---------------- */

    private View gapCard() {
        double[] intake = day.intake();
        int age = store.age();
        List<String> gaps = Nutrition.gaps(intake, age, 4);

        LinearLayout card = UI.column();
        card.setBackground(UI.roundStroke(0xFFFFF8EC, 0xFFF3E0BE, 1, 20));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(16));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        if (gaps.isEmpty()) {
            card.setBackground(UI.roundStroke(0xFFF3FAF0, 0xFFDDEDD5, 1, 20));
            card.addView(UI.text("✅ 今天没有明显缺口", 15f, 0xFF3F7A2E, true));
            TextView t = UI.text("12 项关键营养素都在达标线上，继续保持这个搭配节奏就好。", 12f, 0xFF5C7A4A, false);
            t.setLineSpacing(UI.dp(4), 1f);
            LinearLayout.LayoutParams tp = UI.matchWrap();
            tp.topMargin = UI.dp(7);
            card.addView(t, tp);
            return card;
        }

        card.addView(UI.text("💡 今天这样补上", 15f, 0xFF8A6239, true));
        TextView sub = UI.text("缺口从大到小排列，可以点「换一换」把对应的一餐调整一下", 11.5f, 0xFFA08878, false);
        LinearLayout.LayoutParams sp = UI.matchWrap();
        sp.topMargin = UI.dp(6);
        card.addView(sub, sp);

        for (String g : gaps) {
            LinearLayout r = UI.row();
            r.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams rp = UI.matchWrap();
            rp.topMargin = UI.dp(11);
            r.setLayoutParams(rp);

            TextView dot = UI.text(Nutrition.emoji(g), 12f, UI.TEXT, false);
            r.addView(dot, UI.lp(UI.dp(20), UI.wrap().height));
            LinearLayout col = UI.column();
            TextView name = UI.text("缺" + g, 12.5f, 0xFF8A6239, true);
            col.addView(name);
            TextView adv = UI.text(Nutrition.fixAdvice(g), 11.5f, 0xFF7A6555, false);
            adv.setLineSpacing(UI.dp(3), 1f);
            LinearLayout.LayoutParams ap = UI.matchWrap();
            ap.topMargin = UI.dp(3);
            col.addView(adv, ap);
            LinearLayout.LayoutParams cp2 = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cp2.leftMargin = UI.dp(6);
            r.addView(col, cp2);
            card.addView(r);
        }
        return card;
    }

    /* ---------------- 奶与补充剂提示 ---------------- */

    private View milkNote() {
        LinearLayout card = UI.column();
        card.setBackground(UI.roundStroke(UI.BLUE_SOFT, 0xFFD6E4F0, 1, 18));
        card.setPadding(UI.dp(16), UI.dp(14), UI.dp(16), UI.dp(14));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        card.addView(UI.text("🥛 还差的部分这样补齐", 13.5f, 0xFF3F6B8A, true));
        TextView t = UI.text(
                "1-3 岁宝宝每天还需要 400-500ml 奶（母乳、配方奶或全脂牛奶），"
                        + "能补上这里约一半的钙和蛋白质缺口。\n"
                        + "维生素 D 天然只存在于少数食物（三文鱼、蛋黄、干香菇）里，"
                        + "单靠吃菜很难吃够——10μg 这个推荐量本身就是按「日照 + 补充剂」总量给的。"
                        + "所以这一项偏低是正常的，请遵医嘱每天补 400IU 滴剂。\n"
                        + "每周吃 2-3 次海带或紫菜，碘基本就够了。",
                11.5f, 0xFF4F6E85, false);
        t.setLineSpacing(UI.dp(5), 1f);
        LinearLayout.LayoutParams tp = UI.matchWrap();
        tp.topMargin = UI.dp(8);
        card.addView(t, tp);
        return card;
    }

    private View disclaimer() {
        TextView t = UI.text("营养数据参考《中国食物成分表》，为常见品种平均值，用于判断「够不够」的量级。\n每个宝宝食量不同，本报告不构成医学或营养处方建议。",
                10.5f, UI.TEXT3, false);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(UI.dp(4), 1f);
        LinearLayout.LayoutParams p = UI.matchWrap();
        p.leftMargin = UI.dp(28);
        p.rightMargin = UI.dp(28);
        p.topMargin = UI.dp(16);
        t.setLayoutParams(p);
        return t;
    }
}
