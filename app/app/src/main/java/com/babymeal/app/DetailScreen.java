package com.babymeal.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 菜品详情 */
public class DetailScreen {

    private final MainActivity act;
    private final Store store;
    private final Dish dish;
    private LinearLayout root;

    public DetailScreen(MainActivity act, Dish dish) {
        this.act = act;
        this.store = act.store;
        this.dish = dish;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        root.addView(hero());
        root.addView(titleBlock());
        root.addView(ingredientCard());
        root.addView(stepCard());
        root.addView(nutritionCard());
        root.addView(contributionCard());
        if (dish.tip != null && !dish.tip.isEmpty()) root.addView(tipCard());
        root.addView(bottomInfo());
        root.addView(spacer(UI.dp(24)));
        return root;
    }

    private View spacer(int h) {
        View v = new View(act);
        v.setLayoutParams(UI.match(h));
        return v;
    }

    /* 顶部大图 + 返回 */
    private View hero() {
        android.widget.FrameLayout box = new android.widget.FrameLayout(act);
        box.setLayoutParams(UI.match(UI.dp(272)));

        ImageView iv = new ImageView(act);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(UI.primarySoft());
        android.graphics.Bitmap bmp = Images.load(act, dish);
        if (bmp != null) {
            iv.setImageBitmap(Images.centerCrop(bmp, 1.42f));
        }
        box.addView(iv, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, UI.dp(272)));

        // 底部渐白过渡，让大图自然衔接正文
        View fade = new View(act);
        GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00FFF9F4, 0x00FFF9F4, 0x66FFF9F4, 0xFFFFF9F4});
        fade.setBackground(fg);
        android.widget.FrameLayout.LayoutParams fp = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, UI.dp(110));
        fp.gravity = Gravity.BOTTOM;
        box.addView(fade, fp);

        // 返回按钮浮层
        TextView back = UI.text("‹", 26f, UI.TEXT, false);
        back.setGravity(Gravity.CENTER);
        back.setBackground(UI.round(0xF5FFFFFF, 20));
        back.setTypeface(UI.bd());
        android.widget.FrameLayout.LayoutParams bp = new android.widget.FrameLayout.LayoutParams(
                UI.dp(38), UI.dp(38));
        bp.leftMargin = UI.dp(16);
        bp.topMargin = UI.dp(16);
        box.addView(back, bp);
        UI.pressable(back, 0.92f);
        back.setOnClickListener(v -> act.closeDetail());
        return box;
    }

    private View titleBlock() {
        LinearLayout b = UI.column();
        b.setBackgroundColor(UI.BG);
        b.setPadding(UI.dp(20), UI.dp(18), UI.dp(20), UI.dp(4));

        LinearLayout chips = UI.row();
        TextView cat = UI.chip(MenuEngine.catEmoji(dish.cat) + " " + MenuEngine.catName(dish.cat),
                UI.catColor(dish.cat) == UI.YELLOW ? 0xFF9A7412 : UI.catColor(dish.cat),
                UI.catSoft(dish.cat));
        chips.addView(cat);
        TextView age = UI.chip("👶 " + MenuEngine.ageName(dish.minAge()) + "起",
                0xFF8A6239, UI.yellowSoft());
        LinearLayout.LayoutParams ap = UI.wrap();
        ap.leftMargin = UI.dp(6);
        chips.addView(age, ap);
        if (!dish.kcal.isEmpty()) {
            TextView k = UI.chip(dish.kcal, UI.TEXT2, 0xFFF3EAE2);
            LinearLayout.LayoutParams kp = UI.wrap();
            kp.leftMargin = UI.dp(6);
            chips.addView(k, kp);
        }
        b.addView(chips);

        if (!dish.meals.isEmpty()) {
            LinearLayout mealRow = UI.row();
            mealRow.addView(UI.text("适合时段", 11.5f, UI.TEXT3, false));
            for (String m : dish.meals) {
                TextView mc = UI.chip(m, 0xFF8A6239, UI.yellowSoft());
                mc.setTextSize(10.5f);
                LinearLayout.LayoutParams mp = UI.wrap();
                mp.leftMargin = UI.dp(6);
                mealRow.addView(mc, mp);
            }
            LinearLayout.LayoutParams mrp = UI.wrap();
            mrp.topMargin = UI.dp(10);
            b.addView(mealRow, mrp);
        }

        TextView name = UI.text(dish.name, 25f, UI.TEXT, true);
        LinearLayout.LayoutParams np = UI.matchWrap();
        np.topMargin = UI.dp(12);
        b.addView(name, np);

        // 收藏
        LinearLayout favRow = UI.row();
        TextView fav = UI.chip(store.isFav(dish.id) ? "♥ 已收藏" : "♡ 收藏",
                store.isFav(dish.id) ? Color.WHITE : UI.PINK,
                store.isFav(dish.id) ? UI.PINK : UI.PINK_SOFT);
        final TextView favRef = fav;
        fav.setOnClickListener(v -> {
            boolean added = store.toggleFav(dish.id);
            favRef.setText(added ? "♥ 已收藏" : "♡ 收藏");
            favRef.setTextColor(added ? Color.WHITE : UI.PINK);
            favRef.setBackground(UI.round(added ? UI.PINK : UI.PINK_SOFT, 20));
            act.toast(added ? "已加入收藏 ❤" : "已取消收藏");
        });
        UI.pressable(fav, 0.94f);
        favRow.addView(fav);
        LinearLayout.LayoutParams fp = UI.wrap();
        fp.topMargin = UI.dp(12);
        b.addView(favRow, fp);
        return b;
    }

    /* 食材明细 */
    private View ingredientCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(8));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(14);
        card.setLayoutParams(cp);

        LinearLayout head = UI.row();
        head.addView(UI.text("食材明细", 16f, UI.TEXT, true), UI.lp(0, UI.dp(22), 1f));
        head.addView(UI.text(dish.ingredients.size() + " 种食材", 11.5f, UI.TEXT3, false));
        card.addView(head);

        for (Dish.Ingredient i : dish.ingredients) {
            LinearLayout row = UI.row();
            View dot = new View(act);
            dot.setBackground(UI.round(UI.catColor(dish.cat), 4));
            LinearLayout.LayoutParams dp = UI.lp(UI.dp(5), UI.dp(5));
            dp.rightMargin = UI.dp(10);
            row.addView(dot, dp);

            row.addView(UI.text(i.name, 13.5f, UI.TEXT, false), UI.lp(0, UI.dp(20), 1f));
            row.addView(UI.text(i.amount + i.unit, 13f, UI.TEXT2, false));

            LinearLayout.LayoutParams rp = UI.match(UI.dp(34));
            card.addView(row, rp);
        }
        return card;
    }

    /* 做法 */
    private View stepCard() {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(14));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        LinearLayout head = UI.row();
        head.addView(UI.text("做法步骤", 16f, UI.TEXT, true), UI.lp(0, UI.dp(22), 1f));
        head.addView(UI.chip(dish.steps.size() + " 步", 0xFFB26B32, UI.primarySoft()));
        card.addView(head);

        for (int s = 0; s < dish.steps.size(); s++) {
            LinearLayout row = UI.row();
            row.setGravity(Gravity.TOP);

            TextView num = UI.text(String.valueOf(s + 1), 12f, Color.WHITE, true);
            num.setGravity(Gravity.CENTER);
            num.setBackground(UI.round(UI.PRIMARY, 20));
            LinearLayout.LayoutParams np = UI.lp(UI.dp(22), UI.dp(22));
            np.topMargin = UI.dp(1);
            np.rightMargin = UI.dp(12);
            row.addView(num, np);

            TextView body = UI.text(dish.steps.get(s), 13.8f, 0xFF4A3B32, false);
            body.setLineSpacing(UI.dp(5), 1f);
            row.addView(body, UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams rp = UI.matchWrap();
            rp.topMargin = UI.dp(s == 0 ? 14 : 12);
            card.addView(row, rp);
        }
        return card;
    }

    /* 营养 */
    private View nutritionCard() {
        LinearLayout card = UI.column();
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF1F8EC, 0xFFF8FCF5});
        bg.setCornerRadius(UI.dp(18));
        bg.setStroke(UI.dp(1), 0xFFE2EFD8);
        card.setBackground(bg);
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(16));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        LinearLayout head = UI.row();
        head.addView(UI.text("✨ 补充的营养", 16f, 0xFF3F5A31, true), UI.lp(0, UI.dp(22), 1f));
        card.addView(head);

        // 主要营养素圆点
        if (!dish.keyNutrients.isEmpty()) {
            LinearLayout ns = new LinearLayout(act);
            ns.setOrientation(LinearLayout.HORIZONTAL);
            for (String n : dish.keyNutrients) {
                TextView chip = UI.chip(n, 0xFF3F7A2E, 0xFFFFFFFF);
                chip.setTypeface(UI.bd());
                LinearLayout.LayoutParams np = UI.wrap();
                np.rightMargin = UI.dp(6);
                np.topMargin = UI.dp(10);
                ns.addView(chip, np);
            }
            card.addView(ns);
        }

        TextView body = UI.text(dish.nutrition, 13.2f, 0xFF4B6040, false);
        body.setLineSpacing(UI.dp(6), 1f);
        LinearLayout.LayoutParams bp = UI.matchWrap();
        bp.topMargin = UI.dp(12);
        card.addView(body, bp);

        return card;
    }

    /* 这道菜的营养贡献：把食材换算成具体营养素含量 */
    private View contributionCard() {
        double[] nut = Nutrition.ofDish(dish);
        int age = store.age();

        LinearLayout card = UI.column();
        card.setBackground(UI.card(18));
        card.setPadding(UI.dp(18), UI.dp(16), UI.dp(18), UI.dp(12));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        LinearLayout head = UI.row();
        head.addView(UI.text("这道菜补什么", 16f, UI.TEXT, true), UI.lp(0, UI.dp(22), 1f));
        head.addView(UI.chip("按食材换算", UI.TEXT3, 0xFFF3EAE2));
        card.addView(head);

        TextView sub = UI.text("以下是这道菜能提供的实际含量，括号内为占 " +
                MenuEngine.ageName(age) + " 每日推荐量的比例", 11f, UI.TEXT3, false);
        LinearLayout.LayoutParams sp = UI.matchWrap();
        sp.topMargin = UI.dp(6);
        card.addView(sub, sp);

        // 只展示有贡献的项，按占比从高到低排序
        java.util.List<String> keys = new java.util.ArrayList<>();
        for (String k : Nutrition.KEYS) {
            if (nut[Nutrition.indexOf(k)] > 0) keys.add(k);
        }
        if (keys.isEmpty()) {
            TextView t = UI.text("这道菜的食材暂时没有对应的营养数据。", 12f, UI.TEXT3, false);
            LinearLayout.LayoutParams tp = UI.matchWrap();
            tp.topMargin = UI.dp(12);
            card.addView(t, tp);
            return card;
        }
        keys.sort((a, b) -> Double.compare(
                Nutrition.ratio(nut, b, age), Nutrition.ratio(nut, a, age)));

        for (String k : keys) {
            double v = nut[Nutrition.indexOf(k)];
            double r = Nutrition.ratio(nut, k, age);
            card.addView(nutRow(k, v, r));
        }
        return card;
    }

    private View nutRow(String key, double value, double ratio) {
        LinearLayout row = UI.row();
        row.setPadding(0, UI.dp(9), 0, UI.dp(9));

        row.addView(UI.text(Nutrition.emoji(key), 12f, UI.TEXT, false), UI.lp(UI.dp(22), UI.dp(18)));
        row.addView(UI.text(key, 12.5f, UI.TEXT, false), UI.lp(UI.dp(66), UI.dp(18)));

        // 进度细条
        LinearLayout track = UI.row();
        track.setBackground(UI.round(0xFFF6ECE3, 4));
        int color = ratio >= 0.15 ? 0xFF7FB069 : 0xFFE0B76A;
        View fill = new View(act);
        fill.setBackground(UI.round(color, 4));
        float r = (float) Math.min(1.0, ratio);
        track.addView(fill, UI.lp(0, UI.dp(6), Math.max(0.02f, r)));
        View rest = new View(act);
        track.addView(rest, UI.lp(0, UI.dp(6), Math.max(0f, 1 - r)));
        LinearLayout.LayoutParams tp = UI.lp(0, UI.dp(6), 1f);
        tp.rightMargin = UI.dp(10);
        row.addView(track, tp);

        TextView v = UI.text(Nutrition.fmt(value) + Nutrition.unit(key) +
                "  (" + Math.round(ratio * 100) + "%)", 11f, ratio >= 0.15 ? 0xFF5C7A4A : UI.TEXT3, false);
        v.setGravity(Gravity.RIGHT);
        row.addView(v);
        return row;
    }

    /* 小贴士 */
    private View tipCard() {
        LinearLayout card = UI.row();
        card.setGravity(Gravity.TOP);
        card.setBackground(UI.roundStroke(UI.YELLOW_SOFT, 0xFFF5E3BC, 1, 18));
        card.setPadding(UI.dp(16), UI.dp(14), UI.dp(16), UI.dp(14));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.leftMargin = UI.dp(16);
        cp.rightMargin = UI.dp(16);
        cp.topMargin = UI.dp(12);
        card.setLayoutParams(cp);

        card.addView(UI.text("💡", 15f, UI.TEXT, false));
        LinearLayout col = UI.column();
        TextView h = UI.text("妈妈小贴士", 13f, 0xFF8A6239, true);
        col.addView(h);
        TextView body = UI.text(dish.tip, 13f, 0xFF6B5544, false);
        body.setLineSpacing(UI.dp(5), 1f);
        LinearLayout.LayoutParams bp = UI.matchWrap();
        bp.topMargin = UI.dp(6);
        col.addView(body, bp);
        LinearLayout.LayoutParams cpp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cpp.leftMargin = UI.dp(10);
        card.addView(col, cpp);
        return card;
    }

    private View bottomInfo() {
        TextView t = UI.text("菜单与食材用量基于 1-3 岁宝宝常见食量估算，请按宝宝实际食量调整；\n首次尝试新食材请少量并观察 3 天。",
                11.5f, UI.TEXT3, false);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(UI.dp(4), 1f);
        LinearLayout.LayoutParams p = UI.matchWrap();
        p.topMargin = UI.dp(20);
        p.leftMargin = UI.dp(28);
        p.rightMargin = UI.dp(28);
        t.setLayoutParams(p);
        return t;
    }
}
