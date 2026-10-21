package com.babymeal.app;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** 「今日菜单」页 */
public class TodayScreen {

    private final MainActivity act;
    private final Store store;
    private LinearLayout root;
    private MenuEngine.Day day;

    public TodayScreen(MainActivity act) {
        this.act = act;
        this.store = act.store;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        render();
        return root;
    }

    private void loadDay() {
        String key = Store.todayKey();
        int age = store.age();
        String saved = store.savedMenu(key);
        if (saved != null) {
            day = MenuEngine.unpack(key, age, saved);
            // 如果月龄变了，重新生成
            if (day.age != age) {
                day = null;
            }
        }
        if (day == null) {
            day = MenuEngine.generate(key, age, store.history(), 0);
            store.saveMenu(key, MenuEngine.pack(day));
            store.recordDay(key, day.allDishes());
        }
    }

    private void render() {
        if (day == null) loadDay();
        root.removeAllViews();

        root.addView(header());
        root.addView(menuCard());
        root.addView(actionRow());
        root.addView(progressCard());
        root.addView(footer());

        View spacer = new View(act);
        root.addView(spacer, UI.match(UI.dp(24)));
    }

    /* ---------------- 头部 ---------------- */

    private View header() {
        LinearLayout h = UI.column();
        h.setBackground(UI.headerGradient());
        h.setPadding(UI.dp(20), UI.dp(18), UI.dp(20), UI.dp(6));

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String greet = hour < 10 ? "早上好" : hour < 14 ? "中午好" : hour < 18 ? "下午好" : "晚上好";
        String name = store.babyName();
        String title = name.isEmpty() ? "今天宝宝吃什么？" : name + "今天吃什么？";

        LinearLayout top = UI.row();
        top.addView(UI.text(greet + " ☀️", 12.5f, UI.TEXT2, false), UI.lp(0, UI.dp(18), 1f));
        TextView ageChip = UI.chip("👶 " + MenuEngine.ageName(store.age()), 0xFFB26B32, UI.primarySoft());
        ageChip.setOnClickListener(v -> act.openProfile());
        UI.pressable(ageChip, 0.94f);
        top.addView(ageChip);
        h.addView(top);

        TextView t = UI.text(title, 26f, UI.TEXT, true);
        LinearLayout.LayoutParams tp = UI.wrap();
        tp.topMargin = UI.dp(10);
        h.addView(t, tp);

        String dateStr = String.format(Locale.CHINA, "%d 月 %d 日 · %s",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH), weekName(c.get(Calendar.DAY_OF_WEEK)));
        TextView sub = UI.text(dateStr + "　" + MenuEngine.ageHint(store.age()), 12.5f, UI.TEXT2, false);
        LinearLayout.LayoutParams sp = UI.wrap();
        sp.topMargin = UI.dp(8);
        h.addView(sub, sp);

        return h;
    }

    /* ---------------- 菜单卡 ---------------- */

    private View menuCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(20));
        card.setPadding(UI.dp(14), UI.dp(10), UI.dp(14), UI.dp(14));
        card.setElevation(UI.dp(2));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(14);
        card.setLayoutParams(cp);

        for (int i = 0; i < day.slots.size(); i++) {
            final int idx = i;
            card.addView(slotView(day.slots.get(i), idx));
        }
        return card;
    }

    private View slotView(MenuEngine.Slot slot, int index) {
        LinearLayout wrap = UI.column();
        int topMargin = index == 0 ? UI.dp(8) : UI.dp(16);

        // 餐次标题行
        LinearLayout head = UI.row();
        head.setPadding(UI.dp(4), 0, UI.dp(4), 0);

        TextView emoji = UI.text(slot.emoji(), 16f, UI.TEXT, false);
        head.addView(emoji);

        LinearLayout nameBox = UI.column();
        TextView nm = UI.text(slot.name(), 15f, UI.TEXT, true);
        TextView tm = UI.text(slot.time(), 11f, UI.TEXT3, false);
        nameBox.addView(nm);
        LinearLayout.LayoutParams tmp = UI.wrap();
        tmp.topMargin = UI.dp(2);
        nameBox.addView(tm, tmp);
        LinearLayout.LayoutParams nbp = UI.wrap();
        nbp.leftMargin = UI.dp(8);
        head.addView(nameBox, nbp);

        head.addView(new View(act), UI.lp(0, UI.dp(1), 1f));

        int kcal = slot.kcalTotal();
        if (kcal > 0) {
            head.addView(UI.text("≈" + kcal + " kcal", 11f, UI.TEXT3, false));
        }

        TextView change = UI.chip("换一换", 0xFFB26B32, UI.primarySoft());
        LinearLayout.LayoutParams chp = UI.wrap();
        chp.leftMargin = UI.dp(8);
        head.addView(change, chp);
        UI.pressable(change, 0.93f);
        change.setOnClickListener(v -> {
            MenuEngine.regenerateSlot(day, index, store.history(), (int) (Math.random() * 99999));
            store.saveMenu(day.dateKey, MenuEngine.pack(day));
            render();
        });

        LinearLayout.LayoutParams hp = UI.matchWrap();
        hp.topMargin = topMargin;
        wrap.addView(head, hp);

        // 菜品行
        LinearLayout dishes = UI.row();
        dishes.setPadding(UI.dp(4), 0, UI.dp(4), 0);
        for (Dish d : slot.dishes) {
            dishes.addView(dishTile(d));
        }
        LinearLayout.LayoutParams dp = UI.matchWrap();
        dp.topMargin = UI.dp(9);
        wrap.addView(dishes, dp);

        return wrap;
    }

    private View dishTile(Dish d) {
        LinearLayout tile = UI.column();
        LinearLayout.LayoutParams tp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.rightMargin = UI.dp(8);
        tile.setLayoutParams(tp);
        UI.pressable(tile, 0.96f);
        tile.setOnClickListener(v -> act.openDetail(d));

        ImageView iv = new ImageView(act);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(UI.round(UI.primarySoft(), 14));
        android.graphics.Bitmap bmp = Images.load(act, d);
        if (bmp != null) iv.setImageBitmap(Images.roundCorners(bmp, UI.dp(14)));
        tile.addView(iv, UI.match(UI.dp(84)));

        TextView n = UI.text(d.name, 12.5f, UI.TEXT, true);
        n.setMaxLines(1);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = UI.matchWrap();
        np.topMargin = UI.dp(6);
        tile.addView(n, np);

        TextView c = UI.text(MenuEngine.catName(d.cat) + d.emoji, 10.5f, UI.TEXT3, false);
        LinearLayout.LayoutParams cpp = UI.matchWrap();
        cpp.topMargin = UI.dp(2);
        tile.addView(c, cpp);

        return tile;
    }

    /* ---------------- 操作按钮 ---------------- */

    private View actionRow() {
        LinearLayout row = UI.row();
        LinearLayout.LayoutParams rp = UI.matchWrap();
        rp.leftMargin = UI.dp(16);
        rp.rightMargin = UI.dp(16);
        rp.topMargin = UI.dp(14);
        row.setLayoutParams(rp);

        // 主按钮：重新生成今日菜单
        LinearLayout main = UI.row();
        main.setBackground(UI.round(UI.PRIMARY, 16));
        main.setGravity(Gravity.CENTER);
        main.setPadding(0, UI.dp(15), 0, UI.dp(15));
        TextView mt = UI.text("🎲  重新生成今日菜单", 14.5f, Color.WHITE, true);
        main.addView(mt);
        LinearLayout.LayoutParams mp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        mp.rightMargin = UI.dp(10);
        row.addView(main, mp);
        UI.pressable(main, 0.97f);
        main.setOnClickListener(v -> {
            MenuEngine.Day prev = day;
            day = MenuEngine.generate(day.dateKey, store.age(), store.history(),
                    (int) (Math.random() * 99999));
            store.saveMenu(day.dateKey, MenuEngine.pack(day));
            store.recordDay(day.dateKey, day.allDishes());
            render();
            MenuEngine.Day now = day;
            act.toastWithUndo("已换一桌新菜 🍽", "撤销", () -> act.restoreDay(now, prev));
        });

        // 次按钮：购物清单
        LinearLayout sec = UI.row();
        sec.setBackground(UI.roundStroke(UI.CARD, UI.LINE, 1, 16));
        sec.setGravity(Gravity.CENTER);
        sec.setPadding(UI.dp(16), UI.dp(15), UI.dp(16), UI.dp(15));
        sec.addView(UI.text("🛒 买菜清单", 13.5f, UI.TEXT, false));
        row.addView(sec);
        UI.pressable(sec, 0.96f);
        sec.setOnClickListener(v -> act.openShopping(day));

        return row;
    }

    /* ---------------- 今日营养概览 ---------------- */

    private View progressCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(16));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(14);
        card.setLayoutParams(cp);

        double[] intake = day.intake();
        int age = store.age();
        int score = Nutrition.balanceScore(intake, age);

        // 标题行：均衡分 + 查看全部
        LinearLayout head = UI.row();
        LinearLayout ttl = UI.column();
        ttl.addView(UI.text("今日营养", 15f, UI.TEXT, true));
        TextView sc = UI.text("均衡分 " + score + " · " + Nutrition.scoreWord(score), 11.5f, scoreColor(score), false);
        LinearLayout.LayoutParams scp = UI.wrap();
        scp.topMargin = UI.dp(4);
        ttl.addView(sc, scp);
        head.addView(ttl, UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView more = UI.chip("营养报告 ›", 0xFFB26B32, UI.primarySoft());
        head.addView(more);
        UI.pressable(more, 0.94f);
        more.setOnClickListener(v -> act.openNutrition(day));
        card.addView(head);

        // 12 项营养素小格（2 行 × 6 列）
        LinearLayout grid = UI.column();
        LinearLayout.LayoutParams gp = UI.matchWrap();
        gp.topMargin = UI.dp(12);
        card.addView(grid, gp);

        for (int r = 0; r < 2; r++) {
            LinearLayout line = UI.row();
            for (int c = 0; c < 6; c++) {
                int idx = r * 6 + c;
                if (idx >= Nutrition.KEYS.length) break;
                line.addView(nutCell(Nutrition.KEYS[idx], intake, age));
            }
            LinearLayout.LayoutParams lp = UI.matchWrap();
            if (r > 0) lp.topMargin = UI.dp(10);
            grid.addView(line, lp);
        }

        // 缺口提示
        java.util.List<String> gaps = Nutrition.gaps(intake, age, 3);
        TextView note;
        if (gaps.isEmpty()) {
            note = UI.text("✅ 12 项关键营养今天都在达标线上", 11.5f, 0xFF4E9A54, false);
        } else {
            StringBuilder b = new StringBuilder("还差一点：");
            for (int i = 0; i < gaps.size(); i++) {
                if (i > 0) b.append("、");
                b.append(gaps.get(i));
            }
            b.append("　点「营养报告」看怎么补");
            note = UI.text(b.toString(), 11.5f, 0xFFC0842B, false);
        }
        LinearLayout.LayoutParams np = UI.matchWrap();
        np.topMargin = UI.dp(14);
        card.addView(note, np);

        // 全天热量条
        int total = 0;
        for (MenuEngine.Slot s : day.slots) total += s.kcalTotal();
        int target = age == 1 ? 900 : age == 2 ? 1100 : 1300;
        View bar = progressBar(total, target);
        LinearLayout.LayoutParams bp = UI.matchWrap();
        bp.topMargin = UI.dp(14);
        card.addView(bar, bp);

        return card;
    }

    /** 一个营养素小格：图标 + 名称 + 环形进度 */
    private View nutCell(String key, double[] intake, int age) {
        double ratio = Nutrition.ratio(intake, key, age);
        int lv = Nutrition.level(ratio);
        int color = Nutrition.levelColor(lv);

        LinearLayout cell = UI.column();
        cell.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cell.setLayoutParams(lp);

        TextView ic = UI.text(Nutrition.emoji(key), 15f, UI.TEXT, false);
        ic.setGravity(Gravity.CENTER);
        cell.addView(ic);

        TextView n = UI.text(shortName(key), 9.5f, UI.TEXT2, false);
        n.setGravity(Gravity.CENTER);
        n.setMaxLines(1);
        LinearLayout.LayoutParams np = UI.wrap();
        np.topMargin = UI.dp(3);
        cell.addView(n, np);

        // 百分比 + 底色条
        TextView p = UI.text(Math.round(Math.min(ratio, 1.0) * 100) + "%", 10.5f, color, true);
        p.setGravity(Gravity.CENTER);
        p.setBackground(UI.round(Nutrition.levelSoft(lv), 6));
        p.setPadding(UI.dp(4), UI.dp(2), UI.dp(4), UI.dp(2));
        LinearLayout.LayoutParams pp = UI.wrap();
        pp.topMargin = UI.dp(4);
        cell.addView(p, pp);
        return cell;
    }

    private String shortName(String key) {
        switch (key) {
            case Nutrition.PROTEIN: return "蛋白";
            case Nutrition.IRON: return "铁";
            case Nutrition.ZINC: return "锌";
            case Nutrition.CALCIUM: return "钙";
            case Nutrition.IODINE: return "碘";
            case Nutrition.DHA: return "DHA";
            case Nutrition.VIT_A: return "维A";
            case Nutrition.VIT_C: return "维C";
            case Nutrition.VIT_D: return "维D";
            case Nutrition.VIT_B: return "B族";
            case Nutrition.FIBER: return "纤维";
            case Nutrition.POTASSIUM: return "钾";
            default: return key;
        }
    }

    private int scoreColor(int s) {
        if (s >= 90) return 0xFF4E9A54;
        if (s >= 75) return 0xFF6A9A48;
        if (s >= 55) return 0xFFD99A2B;
        return 0xFFE06B5C;
    }

    private View progressBar(int value, int target) {
        LinearLayout box = UI.column();
        LinearLayout labels = UI.row();
        labels.addView(UI.text("全天能量", 12f, UI.TEXT2, false), UI.lp(0, UI.dp(16), 1f));
        labels.addView(UI.text(value + " / " + target + " kcal", 12f, UI.PRIMARY, true));
        box.addView(labels);

        LinearLayout track = UI.row();
        track.setBackground(UI.round(0xFFF6ECE3, 8));
        float ratio = Math.min(1f, value / (float) target);
        View fill = new View(act);
        fill.setBackground(UI.round(UI.PRIMARY, 8));
        track.addView(fill, UI.lp(0, UI.dp(8), Math.max(0.02f, ratio)));
        View rest = new View(act);
        track.addView(rest, UI.lp(0, UI.dp(8), Math.max(0f, 1 - ratio)));
        LinearLayout.LayoutParams tp = UI.match(UI.dp(8));
        tp.topMargin = UI.dp(8);
        box.addView(track, tp);
        return box;
    }

    private View footer() {
        TextView t = UI.text("💡 每个宝宝的食量和口味都不同，菜单仅供参考\n按需增减，注意观察过敏反应", 11.5f, UI.TEXT3, false);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(UI.dp(4), 1f);
        LinearLayout.LayoutParams p = UI.matchWrap();
        p.topMargin = UI.dp(22);
        t.setLayoutParams(p);
        return t;
    }

    static String weekName(int d) {
        switch (d) {
            case Calendar.MONDAY: return "周一";
            case Calendar.TUESDAY: return "周二";
            case Calendar.WEDNESDAY: return "周三";
            case Calendar.THURSDAY: return "周四";
            case Calendar.FRIDAY: return "周五";
            case Calendar.SATURDAY: return "周六";
            default: return "周日";
        }
    }
}
