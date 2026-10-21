package com.babymeal.app;

import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 买菜清单：把今日所有菜的食材按类别汇总 */
public class ShoppingScreen {

    private final MainActivity act;
    private final MenuEngine.Day day;
    private final java.util.Set<String> checked = new java.util.HashSet<>();
    private LinearLayout root;
    private LinearLayout listBox;
    private TextView progress;

    /** 食材分类归属 */
    private static final String[][] GROUPS = {
            {"🥩", "肉蛋水产", "牛腩|牛肉末|猪里脊|猪肉末|鸡腿|鸡腿肉|鸡胸肉|鸡肝|猪肝|排骨|虾仁|鲜虾仁|三文鱼|鳕鱼|龙利鱼|鸡蛋|蛋黄|鸡蛋清|鸡蛋黄"},
            {"🥬", "蔬菜", "南瓜|胡萝卜|土豆|西兰花|菠菜|青菜|小白菜|鸡毛菜|包菜|西葫芦|茄子|长茄子|冬瓜|山药|铁棍山药|洋葱|番茄|玉米|甜玉米粒|甜玉米|鲜香菇|干香菇|鲜蘑菇|木耳|干木耳|紫菜|蒜|葱|姜|红枣|紫薯|红薯|苹果|香蕉|蓝莓|牛油果|柠檬|青菜叶"},
            {"🍚", "主食干货", "大米|小米|面粉|低筋面粉|宝宝面条|宝宝意面|馄饨皮|即食燕麦片|玉米淀粉|淀粉|酵母|宝宝酱油|芝麻酱|淡干虾皮"},
            {"🥛", "奶与油", "配方奶|全脂牛奶|无糖原味酸奶|奶酪|黄油|植物油"},
    };

    public ShoppingScreen(MainActivity act, MenuEngine.Day day) {
        this.act = act;
        this.day = day;
    }

    public View build() {
        root = UI.column();
        root.setBackgroundColor(UI.BG);
        root.addView(header());
        listBox = UI.column();
        listBox.setPadding(UI.dp(16), 0, UI.dp(16), UI.dp(30));
        listBox.setLayoutParams(UI.matchWrap());
        root.addView(listBox);
        buildList();
        return root;
    }

    private View header() {
        LinearLayout h = UI.column();
        h.setBackground(UI.headerGradient());
        h.setPadding(UI.dp(20), UI.dp(18), UI.dp(20), UI.dp(16));

        LinearLayout top = UI.row();
        TextView back = UI.text("‹  返回", 15f, UI.TEXT2, false);
        back.setPadding(0, UI.dp(4), UI.dp(12), UI.dp(4));
        top.addView(back, UI.lp(0, UI.dp(26), 1f));
        UI.pressable(back, 0.95f);
        back.setOnClickListener(v -> act.closeOverlay());
        h.addView(top);

        TextView t = UI.text("🛒 今日买菜清单", 24f, UI.TEXT, true);
        LinearLayout.LayoutParams tp = UI.wrap();
        tp.topMargin = UI.dp(10);
        h.addView(t, tp);

        progress = UI.text("", 12f, UI.TEXT2, false);
        LinearLayout.LayoutParams pp = UI.wrap();
        pp.topMargin = UI.dp(8);
        h.addView(progress, pp);
        return h;
    }

    private void buildList() {
        if (listBox == null) return;
        listBox.removeAllViews();

        // 汇总
        Map<String, int[]> agg = new LinkedHashMap<>(); // name|unit -> [amount]
        for (Dish d : day.allDishes()) {
            for (Dish.Ingredient i : d.ingredients) {
                String key = i.name + "|" + i.unit;
                int v;
                try {
                    v = Integer.parseInt(i.amount);
                } catch (Exception e) {
                    v = 0;
                }
                int[] cur = agg.get(key);
                if (cur == null) agg.put(key, new int[]{v});
                else cur[0] += v;
            }
        }

        Map<String, List<String>> grouped = new LinkedHashMap<>();
        for (String[] g : GROUPS) grouped.put(g[1], new ArrayList<>());

        List<String> leftovers = new ArrayList<>();
        List<String> keys = new ArrayList<>(agg.keySet());
        for (String key : keys) {
            String name = key.split("\\|")[0];
            String group = null;
            for (String[] g : GROUPS) {
                for (String n : g[2].split("\\|")) {
                    if (n.equals(name)) {
                        group = g[1];
                        break;
                    }
                }
                if (group != null) break;
            }
            if (group == null) leftovers.add(key);
            else grouped.get(group).add(key);
        }
        if (!leftovers.isEmpty()) grouped.put("其他", leftovers);

        int total = keys.size();
        totalItems = total;
        updateProgress();

        for (String[] g : GROUPS) {
            List<String> list = grouped.get(g[1]);
            if (list == null || list.isEmpty()) continue;
            listBox.addView(sectionTitle(g[0], g[1], list.size()));
            listBox.addView(itemsCard(list, agg));
        }
        if (!leftovers.isEmpty()) {
            listBox.addView(sectionTitle("🧂", "其他", leftovers.size()));
            listBox.addView(itemsCard(leftovers, agg));
        }

        TextView tip = UI.text("📝 已按今日 " + day.allDishes().size() + " 道菜汇总，共 " + total +
                " 种食材。勾选已买到的，慢慢逛逛超市吧～", 11.5f, UI.TEXT3, false);
        tip.setGravity(Gravity.CENTER);
        tip.setLineSpacing(UI.dp(4), 1f);
        LinearLayout.LayoutParams tp = UI.matchWrap();
        tp.topMargin = UI.dp(20);
        listBox.addView(tip, tp);
    }

    private void updateProgress() {
        int total = totalItems;
        int done = checked.size();
        if (progress != null) {
            progress.setText("已完成 " + done + " / " + total + " 项" + (done == total && total > 0 ? "　全部买齐啦 ✅" : ""));
        }
    }

    private int totalItems;

    private int countTotal() {
        return totalItems;
    }

    private View sectionTitle(String emoji, String name, int count) {
        LinearLayout row = UI.row();
        LinearLayout.LayoutParams rp = UI.matchWrap();
        rp.topMargin = UI.dp(20);
        row.setLayoutParams(rp);
        row.addView(UI.text(emoji + "  " + name, 14.5f, UI.TEXT, true), UI.lp(0, UI.dp(20), 1f));
        row.addView(UI.text(count + " 项", 11.5f, UI.TEXT3, false));
        return row;
    }

    private View itemsCard(List<String> keys, Map<String, int[]> agg) {
        LinearLayout card = UI.column();
        card.setBackground(UI.card(16));
        card.setPadding(UI.dp(16), UI.dp(4), UI.dp(16), UI.dp(4));
        LinearLayout.LayoutParams cp = UI.matchWrap();
        cp.topMargin = UI.dp(10);
        card.setLayoutParams(cp);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String[] parts = key.split("\\|");
            String name = parts[0];
            String unit = parts.length > 1 ? parts[1] : "";
            int amount = agg.get(key)[0];
            boolean on = checked.contains(key);

            LinearLayout row = UI.row();
            row.setPadding(0, UI.dp(13), 0, UI.dp(13));

            TextView box = UI.text(on ? "✓" : "", 10.5f, android.graphics.Color.WHITE, true);
            box.setGravity(Gravity.CENTER);
            box.setBackground(on ? UI.round(UI.ACCENT, 7) : UI.roundStroke(android.graphics.Color.TRANSPARENT, 0xFFDCCDBF, 1, 7));
            row.addView(box, UI.lp(UI.dp(20), UI.dp(20)));

            TextView nm = UI.text(name, 14f, on ? UI.TEXT3 : UI.TEXT, false);
            if (on) nm.setPaintFlags(nm.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            LinearLayout.LayoutParams np = UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            np.leftMargin = UI.dp(12);
            row.addView(nm, np);

            String amt = amount > 0 ? (amount + unit) : "适量";
            TextView am = UI.text(amt, 13f, on ? UI.TEXT3 : UI.TEXT2, false);
            if (on) am.setPaintFlags(am.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(am);

            final String k = key;
            row.setOnClickListener(v -> {
                if (checked.contains(k)) checked.remove(k);
                else checked.add(k);
                buildList();
            });
            card.addView(row);
            if (i < keys.size() - 1) card.addView(UI.divider());
        }
        return card;
    }
}
