package com.babymeal.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends android.app.Activity {

    Store store;
    MainActivity act;
    FrameLayout contentBox;
    LinearLayout navBar;
    ScrollView scroll;
    MenuEngine.Day currentDay;

    private final List<View> stack = new ArrayList<>();
    private int tab = 0;
    private TextView[] tabLabels = new TextView[3];
    private LinearLayout[] tabCells = new LinearLayout[3];

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        act = this;
        UI.setContext(this);
        Typeface reg = Typeface.createFromAsset(getAssets(), "fonts/deng.ttf");
        Typeface bd = Typeface.createFromAsset(getAssets(), "fonts/dengb.ttf");
        UI.init(reg, bd);

        store = new Store(this);
        DishRepo.load(this);

        // 状态栏文字深色
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        LinearLayout rootView = UI.column();
        rootView.setBackgroundColor(UI.BG);
        rootView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        contentBox = new FrameLayout(this);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        rootView.addView(contentBox, cp);

        navBar = buildNav();
        rootView.addView(navBar, UI.match(UI.dp(58)));

        setContentView(rootView);

        showTab(0);
    }

    /* ---------------- 底部导航 ---------------- */

    private LinearLayout buildNav() {
        LinearLayout nav = UI.row();
        nav.setBackground(UI.roundStroke(0xFFFFFFFF, UI.LINE, 1, 0));
        nav.setElevation(UI.dp(8));
        String[] icons = {"🍽", "📖", "👶"};
        String[] names = {"今日菜单", "菜单大全", "我的"};
        for (int i = 0; i < 3; i++) {
            LinearLayout cell = UI.column();
            cell.setGravity(Gravity.CENTER);
            TextView ic = UI.text(icons[i], 18f, UI.TEXT, false);
            ic.setGravity(Gravity.CENTER);
            TextView lb = UI.text(names[i], 10.5f, UI.TEXT3, false);
            lb.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = UI.wrap();
            lp.topMargin = UI.dp(2);
            cell.addView(ic);
            cell.addView(lb, lp);
            LinearLayout.LayoutParams cpp = UI.lp(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            nav.addView(cell, cpp);
            tabCells[i] = cell;
            tabLabels[i] = lb;
            final int idx = i;
            cell.setOnClickListener(v -> showTab(idx));
            UI.pressable(cell, 0.96f);
        }
        return nav;
    }

    private void paintNav() {
        for (int i = 0; i < 3; i++) {
            boolean on = i == tab;
            tabLabels[i].setTextColor(on ? UI.PRIMARY : UI.TEXT3);
            tabLabels[i].setTypeface(on ? UI.bd() : UI.reg());
            tabCells[i].setAlpha(on ? 1f : 0.82f);
        }
    }

    void showTab(int idx) {
        tab = idx;
        paintNav();
        View v;
        switch (idx) {
            case 0:
                TodayScreen ts = new TodayScreen(this);
                v = ts.build();
                currentDay = tsDay(ts);
                break;
            case 1:
                v = new ListScreen(this).build();
                break;
            default:
                v = new MineScreen(this).build();
                break;
        }
        stack.clear();
        stack.add(v);
        setContent(v, false);
    }

    private MenuEngine.Day tsDay(TodayScreen ts) {
        String key = Store.todayKey();
        String saved = store.savedMenu(key);
        if (saved != null) return MenuEngine.unpack(key, store.age(), saved);
        return MenuEngine.generate(key, store.age(), store.history(), 0);
    }

    void onAgeChanged() {
        String key = Store.todayKey();
        // 清掉今天旧的存档，按新月龄重新生成
        MenuEngine.Day d = MenuEngine.generate(key, store.age(), store.history(), 0);
        store.saveMenu(key, MenuEngine.pack(d));
        store.recordDay(key, d.allDishes());
        showTab(0);
    }

    void randomMenu() {
        tab = 0;
        paintNav();
        String key = Store.todayKey();
        MenuEngine.Day d = MenuEngine.generate(key, store.age(), store.history(),
                (int) (Math.random() * 999999));
        store.saveMenu(key, MenuEngine.pack(d));
        store.recordDay(key, d.allDishes());
        showTab(0);
        toast("已随机搭配新的一桌 🎲");
    }

    /* ---------------- 页面栈 ---------------- */

    private void setContent(View v, boolean animate) {
        contentBox.removeAllViews();
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(v, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentBox.addView(scroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (animate) {
            scroll.setAlpha(0f);
            scroll.setTranslationY(UI.dp(14));
            scroll.animate().alpha(1f).translationY(0).setDuration(220).start();
        }
        scroll.scrollTo(0, 0);
    }

    void push(View v) {
        stack.add(v);
        setContent(v, true);
    }

    void pop() {
        if (stack.size() > 1) {
            stack.remove(stack.size() - 1);
            setContent(stack.get(stack.size() - 1), false);
        } else {
            showTab(tab);
        }
    }

    void openDetail(Dish d) {
        push(new DetailScreen(this, d).build());
    }

    void openShopping(MenuEngine.Day day) {
        if (day == null) day = currentDay;
        if (day == null) return;
        push(new ShoppingScreen(this, day).build());
    }

    void openNutrition(MenuEngine.Day day) {
        if (day == null) day = currentDay;
        if (day == null) return;
        push(new NutritionScreen(this, day).build());
    }

    void openProfile() {
        // 切到「我的」并滚动到月龄设置
        showTab(2);
    }

    void closeDetail() {
        pop();
    }

    void closeOverlay() {
        pop();
    }

    @Override
    public void onBackPressed() {
        if (stack.size() > 1) {
            pop();
            return;
        }
        if (tab != 0) {
            showTab(0);
            return;
        }
        super.onBackPressed();
    }

    void toast(String s) {
        android.widget.Toast t = android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT);
        t.show();
    }

    /* ---------------- 自定义 Toast：带撤销按钮 ---------------- */

    void restoreDay(MenuEngine.Day now, MenuEngine.Day prev) {
        store.saveMenu(prev.dateKey, MenuEngine.pack(prev));
        showTab(0);
        toast("已恢复原来的菜单");
    }

    void toastWithUndo(String msg, String action, Runnable undo) {
        android.widget.Toast t = new android.widget.Toast(this);
        LinearLayout box = UI.row();
        box.setPadding(UI.dp(4), 0, UI.dp(4), 0);
        TextView m = UI.text(msg, 13f, android.graphics.Color.WHITE, false);
        box.addView(m, UI.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView a = UI.text(action, 13f, 0xFFFFC98A, true);
        LinearLayout.LayoutParams ap = UI.wrap();
        ap.leftMargin = UI.dp(14);
        box.addView(a, ap);
        a.setOnClickListener(v -> {
            t.cancel();
            undo.run();
        });
        t.setView(box);
        t.setDuration(android.widget.Toast.LENGTH_LONG);
        t.show();
    }
}
