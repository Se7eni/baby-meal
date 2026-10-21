package com.babymeal.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 本地存储：月龄、最近吃的菜、收藏、锁定菜单 */
public final class Store {

    private static final String P = "babymeal_v1";

    private final SharedPreferences sp;

    public Store(Context ctx) {
        sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    /* ---------- 月龄 ---------- */

    public int age() {
        int a = sp.getInt("age", 1);
        return (a >= 1 && a <= 3) ? a : 1;
    }

    public void setAge(int a) {
        sp.edit().putInt("age", a).apply();
    }

    /** 宝宝昵称，可为空 */
    public String babyName() {
        return sp.getString("baby_name", "");
    }

    public void setBabyName(String n) {
        sp.edit().putString("baby_name", n).apply();
    }

    /* ---------- 收藏 ---------- */

    public Set<String> favorites() {
        return new LinkedHashSet<>(sp.getStringSet("fav", new LinkedHashSet<>()));
    }

    public boolean isFav(String id) {
        return favorites().contains(id);
    }

    public boolean toggleFav(String id) {
        Set<String> s = favorites();
        boolean added;
        if (s.contains(id)) {
            s.remove(id);
            added = false;
        } else {
            s.add(id);
            added = true;
        }
        sp.edit().putStringSet("fav", s).apply();
        return added;
    }

    /* ---------- 历史（去重用） ---------- */

    public List<String> history() {
        // 格式：dateKey|id,id,id
        String raw = sp.getString("history", "");
        List<String> out = new ArrayList<>();
        if (raw.isEmpty()) return out;
        String[] days = raw.split(";");
        for (String d : days) {
            int i = d.indexOf('|');
            if (i < 0) continue;
            String ids = d.substring(i + 1);
            for (String id : ids.split(",")) {
                if (!id.isEmpty()) out.add(id);
            }
        }
        return out;
    }

    public void recordDay(String dateKey, List<Dish> dishes) {
        StringBuilder ids = new StringBuilder();
        for (Dish d : dishes) {
            if (ids.length() > 0) ids.append(',');
            ids.append(d.id);
        }
        List<String> entries = new ArrayList<>();
        String raw = sp.getString("history", "");
        if (!raw.isEmpty()) {
            for (String e : raw.split(";")) {
                if (e.isEmpty()) continue;
                if (e.startsWith(dateKey + "|")) continue;
                entries.add(e);
            }
        }
        entries.add(dateKey + "|" + ids);
        // 只保留最近 7 天
        while (entries.size() > 7) entries.remove(0);
        sp.edit().putString("history", join(entries, ";")).apply();
    }

    public Set<String> allRecordedIds() {
        return new LinkedHashSet<>(history());
    }

    /* ---------- 锁定 ---------- */

    public boolean isLocked(String dateKey) {
        return sp.getBoolean("lock_" + dateKey, false);
    }

    public void setLocked(String dateKey, boolean v) {
        sp.edit().putBoolean("lock_" + dateKey, v).apply();
    }

    /** 某天的菜单是否已经确定下来（有存档） */
    public String savedMenu(String dateKey) {
        return sp.getString("menu_" + dateKey, null);
    }

    public void saveMenu(String dateKey, String packed) {
        sp.edit().putString("menu_" + dateKey, packed).apply();
    }

    /* ---------- 提醒开关 ---------- */

    public boolean reminderOn() {
        return sp.getBoolean("reminder", true);
    }

    public void setReminder(boolean on) {
        sp.edit().putBoolean("reminder", on).apply();
    }

    public static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.CHINA, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String keyOf(Calendar c) {
        return String.format(Locale.CHINA, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String join(List<String> list, String sep) {
        StringBuilder b = new StringBuilder();
        for (String s : list) {
            if (b.length() > 0) b.append(sep);
            b.append(s);
        }
        return b.toString();
    }
}
