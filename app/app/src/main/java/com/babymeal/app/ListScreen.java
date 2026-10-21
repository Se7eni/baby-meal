package com.babymeal.app;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 「菜单大全」页 */
public class ListScreen {

    private final MainActivity act;
    private final Store store;
    private LinearLayout root;
    private LinearLayout listBox;
    private String cat = "all";
    private boolean onlyMyAge = false;
    private String query = "";
    private LinearLayout chipRow;
    private HorizontalScrollView chipScroll;

    public ListScreen(MainActivity act) {
        this.act = act;
        this.store = act.store;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        root.addView(header());
        root.addView(searchBox());
        chipRow = UI.row();
        chipRow.setPadding(UI.dp(16), UI.dp(1), UI.dp(10), UI.dp(1));
        chipScroll = new HorizontalScrollView(act);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setFillViewport(false);
        chipScroll.addView(chipRow, new android.widget.FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams crp = UI.matchWrap();
        crp.topMargin = UI.dp(12);
        chipScroll.setLayoutParams(crp);
        root.addView(chipScroll);
        buildChips();

        listBox = UI.column();
        listBox.setPadding(UI.dp(16), UI.dp(12), UI.dp(16), UI.dp(24));
        listBox.setLayoutParams(UI.matchWrap());
        root.addView(listBox);

        refresh();
        return root;
    }

    private View header() {
        LinearLayout h = UI.column();
        h.setBackground(UI.headerGradient());
        h.setPadding(UI.dp(20), UI.dp(18), UI.dp(20), UI.dp(4));
        h.addView(UI.text("菜单大全", 26f, UI.TEXT, true));
        LinearLayout r = UI.row();
        TextView sub = UI.text("共 " + DishRepo.all().size() + " 道适合 1-3 岁宝宝的做法", 12.5f, UI.TEXT2, false);
        LinearLayout.LayoutParams sp = UI.lp(0, UI.dp(20), 1f);
        sp.topMargin = UI.dp(8);
        r.addView(sub, sp);

        TextView ageChip = UI.chip("👶 " + MenuEngine.ageName(store.age()), 0xFFB26B32, UI.primarySoft());
        ageChip.setOnClickListener(v -> act.openProfile());
        r.addView(ageChip);
        LinearLayout.LayoutParams rp = UI.matchWrap();
        rp.topMargin = UI.dp(8);
        h.addView(r, rp);
        return h;
    }

    private View searchBox() {
        LinearLayout box = UI.row();
        box.setBackground(UI.roundStroke(UI.CARD, UI.LINE, 1, 14));
        box.setPadding(UI.dp(14), 0, UI.dp(14), 0);
        LinearLayout.LayoutParams bp = UI.match(UI.dp(44));
        bp.leftMargin = UI.dp(16);
        bp.rightMargin = UI.dp(16);
        bp.topMargin = UI.dp(14);
        box.setLayoutParams(bp);

        box.addView(UI.text("🔍", 13f, UI.TEXT3, false));
        EditText et = new EditText(act);
        et.setHint("搜菜名 or 食材，如「牛肉」「南瓜」");
        et.setHintTextColor(UI.TEXT3);
        et.setTextColor(UI.TEXT);
        et.setTextSize(13.5f);
        et.setTypeface(UI.reg());
        et.setBackground(null);
        et.setSingleLine(true);
        et.setPadding(UI.dp(10), 0, 0, 0);
        et.setIncludeFontPadding(false);
        LinearLayout.LayoutParams ep = UI.lp(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        box.addView(et, ep);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                refresh();
            }
        });
        return box;
    }

    private void buildChips() {
        chipRow.removeAllViews();
        addChip("全部", "all");
        addChip("🍚 主食", "staple");
        addChip("🍖 荤菜", "meat");
        addChip("🥬 素菜", "veg");
        addChip("🍲 汤羹", "soup");
        addChip("🍪 点心", "snack");
    }

    private void addChip(String label, String key) {
        boolean on = cat.equals(key);
        TextView c = UI.chip(label, on ? Color.WHITE : UI.TEXT2,
                on ? UI.PRIMARY : UI.CARD);
        if (!on) c.setBackground(UI.roundStroke(UI.CARD, UI.LINE, 1, 20));
        LinearLayout.LayoutParams p = UI.wrap();
        p.rightMargin = UI.dp(7);
        chipRow.addView(c, p);
        UI.pressable(c, 0.94f);
        c.setOnClickListener(v -> {
            cat = key;
            buildChips();
            refresh();
            if (chipScroll != null) {
                c.post(() -> chipScroll.smoothScrollTo(
                        Math.max(0, c.getLeft() - UI.dp(16)), 0));
            }
        });
    }

    private void refresh() {
        if (listBox == null) return;
        listBox.removeAllViews();

        List<Dish> res = new ArrayList<>();
        for (Dish d : DishRepo.all()) {
            if (!cat.equals("all") && !d.cat.equals(cat)) continue;
            if (onlyMyAge && !d.fitsAge(store.age())) continue;
            if (!DishRepo.matches(d, query)) continue;
            res.add(d);
        }

        LinearLayout bar = UI.row();
        bar.addView(UI.text(res.size() + " 道菜", 12f, UI.TEXT2, false), UI.lp(0, UI.dp(18), 1f));
        TextView filter = UI.chip(onlyMyAge ? "✓ 仅适合当前月龄" : "仅看适合当前月龄",
                onlyMyAge ? 0xFF5C7A4A : UI.TEXT2,
                onlyMyAge ? UI.ACCENT_SOFT : UI.CARD);
        if (!onlyMyAge) filter.setBackground(UI.roundStroke(UI.CARD, UI.LINE, 1, 20));
        filter.setOnClickListener(v -> {
            onlyMyAge = !onlyMyAge;
            refresh();
        });
        UI.pressable(filter, 0.94f);
        bar.addView(filter);
        listBox.addView(bar);

        if (res.isEmpty()) {
            LinearLayout empty = UI.column();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, UI.dp(50), 0, UI.dp(50));
            empty.addView(UI.text("🍽", 34f, UI.TEXT3, false));
            TextView t = UI.text("没找到相关菜谱\n换个关键词试试吧", 13f, UI.TEXT3, false);
            t.setGravity(Gravity.CENTER);
            t.setLineSpacing(UI.dp(6), 1f);
            LinearLayout.LayoutParams tp = UI.wrap();
            tp.topMargin = UI.dp(12);
            empty.addView(t, tp);
            listBox.addView(empty);
            return;
        }

        // 两列网格
        LinearLayout rowBox = null;
        for (int i = 0; i < res.size(); i++) {
            if (i % 2 == 0) {
                rowBox = UI.row();
                LinearLayout.LayoutParams rp = UI.matchWrap();
                rp.topMargin = UI.dp(10);
                listBox.addView(rowBox, rp);
            }
            final Dish d = res.get(i);
            View card = gridCard(d);
            LinearLayout.LayoutParams cp = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cp.rightMargin = (i % 2 == 0) ? UI.dp(5) : 0;
            cp.leftMargin = (i % 2 == 0) ? 0 : UI.dp(5);
            rowBox.addView(card, cp);
        }
        // 补齐占位，保持两列宽度一致
        if (res.size() % 2 == 1) {
            View ph = new View(act);
            rowBox.addView(ph, UI.lp(0, UI.dp(1), 1f));
        }
    }

    private View gridCard(Dish d) {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(16));
        card.setPadding(UI.dp(8), UI.dp(8), UI.dp(8), UI.dp(10));
        card.setClipToOutline(true);
        UI.pressable(card, 0.97f);
        card.setOnClickListener(v -> act.openDetail(d));

        ImageView iv = new ImageView(act);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(UI.round(UI.primarySoft(), 12));
        android.graphics.Bitmap bmp = Images.load(act, d);
        if (bmp != null) iv.setImageBitmap(Images.roundCorners(bmp, UI.dp(12)));
        card.addView(iv, UI.match(UI.dp(104)));

        TextView n = UI.text(d.name, 13.5f, UI.TEXT, true);
        n.setMaxLines(1);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = UI.matchWrap();
        np.topMargin = UI.dp(8);
        np.leftMargin = UI.dp(2);
        card.addView(n, np);

        LinearLayout meta = UI.row();
        meta.setPadding(UI.dp(2), 0, UI.dp(2), 0);
        TextView c = UI.chip(MenuEngine.catName(d.cat),
                UI.catColor(d.cat) == UI.YELLOW ? 0xFF9A7412 : UI.catColor(d.cat), UI.catSoft(d.cat));
        c.setTextSize(10f);
        meta.addView(c);

        TextView age = UI.text(MenuEngine.ageName(d.minAge()) + "+", 10f, UI.TEXT3, false);
        LinearLayout.LayoutParams ap = UI.wrap();
        ap.leftMargin = UI.dp(6);
        meta.addView(age, ap);

        if (store.isFav(d.id)) {
            meta.addView(new View(act), UI.lp(0, UI.dp(1), 1f));
            meta.addView(UI.text("♥", 11f, UI.PINK, false));
        }
        LinearLayout.LayoutParams mp = UI.matchWrap();
        mp.topMargin = UI.dp(7);
        card.addView(meta, mp);
        return card;
    }
}
