package com.babymeal.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 一道菜 */
public class Dish {
    public String id;
    public String name;
    public String cat;          // staple / meat / veg / soup / snack
    public int[] ages;          // 1=12-18月 2=18-24月 3=24-36月
    public List<Ingredient> ingredients = new ArrayList<>();
    public List<String> steps = new ArrayList<>();
    public String nutrition;
    public String tip;
    public String emoji;
    public String kcal;
    public List<String> tags = new ArrayList<>();
    public List<String> meals = new ArrayList<>();
    public List<String> keyNutrients = new ArrayList<>();

    public boolean fitsAge(int age) {
        for (int a : ages) if (a == age) return true;
        return false;
    }

    /** 图片资源文件名（assets/dishes/{id}.webp） */
    public String imageAsset() {
        return "dishes/" + id + ".webp";
    }

    public int minAge() {
        int m = 9;
        for (int a : ages) if (a < m) m = a;
        return m == 9 ? 1 : m;
    }

    public static Dish fromJson(JSONObject o) throws Exception {
        Dish d = new Dish();
        d.id = o.getString("id");
        d.name = o.getString("name");
        d.cat = o.getString("cat");
        JSONArray a = o.getJSONArray("age");
        d.ages = new int[a.length()];
        for (int i = 0; i < a.length(); i++) d.ages[i] = a.getInt(i);
        JSONArray ing = o.getJSONArray("ingredients");
        for (int i = 0; i < ing.length(); i++) {
            JSONArray one = ing.getJSONArray(i);
            if (one.length() >= 3) {
                d.ingredients.add(new Ingredient(
                        one.optString(0, ""),
                        String.valueOf(one.opt(1)),
                        one.optString(2, "")));
            } else if (one.length() == 2) {
                d.ingredients.add(new Ingredient(one.optString(0, ""),
                        String.valueOf(one.opt(1)), ""));
            }
        }
        JSONArray st = o.getJSONArray("steps");
        for (int i = 0; i < st.length(); i++) d.steps.add(st.getString(i));
        d.nutrition = o.optString("nutrition", "");
        d.tip = o.optString("tip", "");
        d.emoji = o.optString("emoji", "🍽");
        d.kcal = o.optString("kcal", "");
        JSONArray tg = o.optJSONArray("tags");
        if (tg != null) for (int i = 0; i < tg.length(); i++) d.tags.add(tg.getString(i));
        JSONArray ml = o.optJSONArray("meals");
        if (ml != null) for (int i = 0; i < ml.length(); i++) d.meals.add(ml.getString(i));
        JSONArray kn = o.optJSONArray("keyNutrients");
        if (kn != null) for (int i = 0; i < kn.length(); i++) d.keyNutrients.add(kn.getString(i));
        return d;
    }

    public static class Ingredient {
        public final String name;
        public final String amount;
        public final String unit;

        public Ingredient(String name, String amount, String unit) {
            this.name = name;
            this.amount = amount;
            this.unit = unit;
        }

        public String display() {
            if (amount == null || amount.isEmpty()) return name;
            return name + "  " + amount + unit;
        }
    }
}
