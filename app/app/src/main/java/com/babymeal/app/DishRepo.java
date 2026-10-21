package com.babymeal.app;

import android.content.Context;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 菜谱仓库：一次性从 assets 读入 68 道菜 */
public final class DishRepo {

    private static List<Dish> ALL;
    private static Map<String, Dish> BY_ID;

    private DishRepo() {
    }

    public static synchronized void load(Context ctx) {
        if (ALL != null) return;
        List<Dish> list = new ArrayList<>();
        try (InputStream is = ctx.getAssets().open("dishes.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                try {
                    list.add(Dish.fromJson(arr.getJSONObject(i)));
                } catch (Exception e) {
                    android.util.Log.e("DishRepo", "skip bad dish #" + i, e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed loading dishes.json", e);
        }
        Map<String, Dish> m = new LinkedHashMap<>();
        for (Dish d : list) m.put(d.id, d);
        ALL = list;
        BY_ID = m;
    }

    public static List<Dish> all() {
        return ALL == null ? new ArrayList<>() : ALL;
    }

    public static Dish byId(String id) {
        return BY_ID == null ? null : BY_ID.get(id);
    }

    /** 菜名转拼音首字母用于搜索 - 简化：支持菜名/食材直接包含匹配 */
    public static boolean matches(Dish d, String q) {
        if (q == null || q.trim().isEmpty()) return true;
        String s = q.trim().toLowerCase();
        if (d.name.toLowerCase().contains(s)) return true;
        for (Dish.Ingredient i : d.ingredients) {
            if (i.name.toLowerCase().contains(s)) return true;
        }
        for (String t : d.tags) {
            if (t.toLowerCase().contains(s)) return true;
        }
        return false;
    }
}
