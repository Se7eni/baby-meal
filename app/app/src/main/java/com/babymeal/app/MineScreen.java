package com.babymeal.app;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 「我的」页：宝宝档案 + 收藏 + 一周记录 */
public class MineScreen {

    private final MainActivity act;
    private final Store store;
    private LinearLayout root;

    public MineScreen(MainActivity act) {
        this.act = act;
        this.store = act.store;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        refresh();
        return root;
    }

    private void refresh() {
        if (root == null) return;
        root.removeAllViews();
        root.addView(headerCard());
        root.addView(ageCard());
        root.addView(favSection());
        root.addView(statsCard());
        root.addView(aboutCard());
        root.addView(spacer(UI.dp(24)));
    }

    private View spacer(int h) {
        View v = new View(act);
        v.setLayoutParams(UI.match(h));
        return v;
    }

    /* 顶部宝宝卡 */
    private View headerCard() {
        LinearLayout box = UI.column();
        box.setBackground(UI.headerGradient());
        box.setPadding(UI.dp(20), UI.dp(22), UI.dp(20), UI.dp(16));

        LinearLayout row = UI.row();
        LinearLayout avatar = UI.column();
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(UI.round(UI.primarySoft(), 30));
        avatar.addView(UI.text("👶", 30f, UI.TEXT, false));
        row.addView(avatar, UI.lp(UI.dp(64), UI.dp(64)));

        LinearLayout info = UI.column();
        String name = store.babyName();
        TextView n = UI.text(name.isEmpty() ? "宝宝" : name, 20f, UI.TEXT, true);
        info.addView(n);
        TextView a = UI.text(MenuEngine.ageName(store.age()) + " · " + MenuEngine.ageHint(store.age()),
                12f, UI.TEXT2, false);
        a.setMaxLines(2);
        LinearLayout.LayoutParams ap = UI.matchWrap();
        ap.topMargin = UI.dp(7);
        info.addView(a, ap);

        LinearLayout.LayoutParams ip = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        ip.leftMargin = UI.dp(16);
        row.addView(info, ip);
        box.addView(row);
        return box;
    }

    /* 月龄选择 */
    private View ageCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(18));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(16);
        card.setLayoutParams(cp);

        card.addView(UI.text("宝宝月龄", 15f, UI.TEXT, true));
        TextView sub = UI.text("切换后菜单会按新阶段重新生成，切块大小和调味建议也会变化", 11.5f, UI.TEXT3, false);
        sub.setLineSpacing(UI.dp(3), 1f);
        LinearLayout.LayoutParams sp = UI.matchWrap();
        sp.topMargin = UI.dp(6);
        card.addView(sub, sp);

        LinearLayout row = UI.row();
        LinearLayout.LayoutParams rp = UI.matchWrap();
        rp.topMargin = UI.dp(14);
        card.addView(row, rp);

        String[] names = {"1-1.5岁", "1.5-2岁", "2-3岁"};
        String[] subs = {"细腻软烂", "小丁小条", "接近家庭餐"};
        for (int i = 0; i < 3; i++) {
            final int age = i + 1;
            boolean on = store.age() == age;
            LinearLayout opt = UI.column();
            opt.setGravity(Gravity.CENTER);
            opt.setPadding(0, UI.dp(12), 0, UI.dp(12));
            opt.setBackground(on ? UI.round(UI.PRIMARY, 14) : UI.roundStroke(0xFFFFFCF9, UI.LINE, 1, 14));
            opt.addView(UI.text(names[i], 13.5f, on ? Color.WHITE : UI.TEXT, true));
            TextView s2 = UI.text(subs[i], 10.5f, on ? 0xFFFFE6D9 : UI.TEXT3, false);
            LinearLayout.LayoutParams sp2 = UI.wrap();
            sp2.topMargin = UI.dp(4);
            opt.addView(s2, sp2);
            LinearLayout.LayoutParams op = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            op.rightMargin = i < 2 ? UI.dp(8) : 0;
            row.addView(opt, op);
            UI.pressable(opt, 0.95f);
            opt.setOnClickListener(v -> {
                store.setAge(age);
                act.onAgeChanged();
                refresh();
                act.toast("已切换到 " + MenuEngine.ageName(age));
            });
        }
        return card;
    }

    /* 收藏 */
    private View favSection() {
        LinearLayout box = UI.column();
        LinearLayout.LayoutParams bp = UI.matchWrap();
        bp.leftMargin = UI.dp(16);
        bp.rightMargin = UI.dp(16);
        bp.topMargin = UI.dp(20);
        box.setLayoutParams(bp);

        LinearLayout head = UI.row();
        head.addView(UI.text("我的收藏", 16f, UI.TEXT, true), UI.lp(0, UI.dp(22), 1f));
        List<String> favs = new ArrayList<>(store.favorites());
        head.addView(UI.text(favs.size() + " 道", 12f, UI.TEXT3, false));
        box.addView(head);

        if (favs.isEmpty()) {
            LinearLayout empty = UI.column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, UI.dp(26), 0, UI.dp(26));
            empty.setBackground(UI.card(16));
            empty.addView(UI.text("♡", 26f, UI.TEXT3, false));
            TextView t = UI.text("还没有收藏的菜\n在菜谱详情页点「收藏」就能存下来", 12f, UI.TEXT3, false);
            t.setGravity(Gravity.CENTER);
            t.setLineSpacing(UI.dp(4), 1f);
            LinearLayout.LayoutParams tp = UI.wrap();
            tp.topMargin = UI.dp(10);
            empty.addView(t, tp);
            box.addView(empty);
            return box;
        }

        // 横向滚动
        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = UI.row();
        row.setPadding(0, UI.dp(2), 0, UI.dp(2));
        for (String id : favs) {
            Dish d = DishRepo.byId(id);
            if (d == null) continue;
            LinearLayout tile = UI.column();
            LinearLayout.LayoutParams tp = UI.wrap();
            tp.rightMargin = UI.dp(10);
            tile.setLayoutParams(tp);
            UI.pressable(tile, 0.96f);
            tile.setOnClickListener(v -> act.openDetail(d));

            ImageView iv = new ImageView(act);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            android.graphics.Bitmap bmp = Images.load(act, d);
            if (bmp != null) iv.setImageBitmap(Images.roundCorners(bmp, UI.dp(13)));
            tile.addView(iv, UI.lp(UI.dp(112), UI.dp(86)));

            TextView n = UI.text(d.name, 12f, UI.TEXT, true);
            n.setMaxLines(1);
            n.setEllipsize(android.text.TextUtils.TruncateAt.END);
            n.setWidth(UI.dp(112));
            LinearLayout.LayoutParams np = UI.wrap();
            np.topMargin = UI.dp(7);
            tile.addView(n, np);
            row.addView(tile);
        }
        hs.addView(row);
        box.addView(hs);
        return box;
    }

    private View statsCard() {
        LinearLayout card = UI.row();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(8), UI.dp(18), UI.dp(8), UI.dp(18));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(16);
        card.setLayoutParams(cp);

        card.addView(statCell(String.valueOf(store.allRecordedIds().size()), "吃过 / 安排过的菜"));
        card.addView(vline());
        card.addView(statCell(String.valueOf(DishRepo.all().size()), "菜谱总数"));
        card.addView(vline());
        card.addView(statCell(String.valueOf(store.favorites().size()), "收藏"));
        return card;
    }

    private View vline() {
        View v = new View(act);
        v.setBackgroundColor(UI.LINE);
        v.setLayoutParams(UI.lp(UI.dp(1), UI.dp(34)));
        return v;
    }

    private View statCell(String num, String label) {
        LinearLayout c = UI.column();
        c.setGravity(Gravity.CENTER);
        c.addView(UI.text(num, 20f, UI.PRIMARY, true));
        TextView l = UI.text(label, 10.5f, UI.TEXT3, false);
        LinearLayout.LayoutParams lp = UI.wrap();
        lp.topMargin = UI.dp(5);
        c.addView(l, lp);
        LinearLayout.LayoutParams pp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        c.setLayoutParams(pp);
        return c;
    }

    private View aboutCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(6), UI.dp(18), UI.dp(6));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(16);
        card.setLayoutParams(cp);

        card.addView(settingRow("🛒", "买菜清单", "按今日菜单汇总食材", v -> act.openShopping(act.currentDay)));
        card.addView(UI.divider());
        card.addView(settingRow("📊", "今日营养报告", "12 项关键营养素的达标情况", v -> act.openNutrition(act.currentDay)));
        card.addView(UI.divider());
        card.addView(settingRow("🎲", "随机来一桌", "不看日期，随机搭配一顿", v -> act.randomMenu()));
        card.addView(UI.divider());
        card.addView(settingRow("ℹ️", "关于", "宝宝吃什么 v1.2.0", null));
        return card;
    }

    private View settingRow(String emoji, String title, String sub, View.OnClickListener click) {
        LinearLayout row = UI.row();
        row.setPadding(0, UI.dp(14), 0, UI.dp(14));
        row.addView(UI.text(emoji, 15f, UI.TEXT, false), UI.lp(UI.dp(26), UI.dp(20)));
        LinearLayout col = UI.column();
        col.addView(UI.text(title, 14f, UI.TEXT, false));
        TextView s = UI.text(sub, 11f, UI.TEXT3, false);
        LinearLayout.LayoutParams sp = UI.wrap();
        sp.topMargin = UI.dp(3);
        col.addView(s, sp);
        row.addView(col, UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (click != null) {
            row.addView(UI.text("›", 18f, UI.TEXT3, false));
            UI.pressable(row, 0.98f);
            row.setOnClickListener(click);
        }
        return row;
    }
}
