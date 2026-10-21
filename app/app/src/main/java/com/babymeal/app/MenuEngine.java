package com.babymeal.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 每日菜单生成器
 *
 * 设计要点：
 * 1. 以「日期 + 月龄段」为随机种子 → 同一天重复打开结果一致
 * 2. 记录最近 N 天吃过的菜，生成时降权 / 排除，做到「尽量不重复」
 * 3. 每天 5 餐：早餐、午餐、下午加餐、晚餐、睡前加餐
 * 4. **营养缺口驱动**：每选一道菜，都先算「现在最缺哪项营养」，
 *    再优先选能补上缺口的那道。这样一桌菜自然覆盖 12 项关键营养素，
 *    而不是靠类别数量凑出来。
 * 5. 「换一换」只换某一餐，且换出来的菜当天不重复，同样考虑营养缺口。
 */
public final class MenuEngine {

    public static final int SLOT_BREAKFAST = 0;
    public static final int SLOT_LUNCH = 1;
    public static final int SLOT_SNACK = 2;
    public static final int SLOT_DINNER = 3;
    public static final int SLOT_BEDTIME = 4;

    public static final String[] SLOT_NAMES = {"早餐", "午餐", "下午加餐", "晚餐", "睡前加餐"};
    public static final String[] SLOT_TIMES = {"7:30 - 8:30", "11:30 - 12:30", "15:00 - 15:30", "17:30 - 18:30", "20:00 - 20:30"};
    public static final String[] SLOT_EMOJI = {"🌅", "🍚", "🍎", "🌙", "🥛"};

    /** 每个餐位的菜量配额：按类别控制数量 */
    // [staple, meat, veg, soup, snack]
    private static final int[][] PLAN = {
            // 早餐
            {1, 0, 0, 0, 0},
            // 午餐
            {1, 1, 1, 0, 0},
            // 加餐
            {0, 0, 0, 0, 1},
            // 晚餐
            {1, 0, 1, 1, 0},
            // 睡前
            {0, 0, 0, 0, 1},
    };

    private static final String[] CATS = {"staple", "meat", "veg", "soup", "snack"};

    private static final int HISTORY_DAYS = 5;

    public static class Slot {
        public final int index;
        public final List<Dish> dishes = new ArrayList<>();

        Slot(int index) {
            this.index = index;
        }

        public String name() {
            return SLOT_NAMES[index];
        }

        public String time() {
            return SLOT_TIMES[index];
        }

        public String emoji() {
            return SLOT_EMOJI[index];
        }

        public int kcalTotal() {
            int t = 0;
            for (Dish d : dishes) t += parseKcal(d.kcal);
            return t;
        }

        /** 这一餐主要提供了哪几项营养（供菜卡上显示） */
        public List<String> mainNutrients() {
            double[] sum = new double[Nutrition.KEYS.length];
            for (Dish d : dishes) {
                double[] one = Nutrition.ofDish(d);
                for (int i = 0; i < sum.length; i++) sum[i] += one[i];
            }
            List<String> out = new ArrayList<>();
            for (String k : Nutrition.KEYS) {
                if (sum[Nutrition.indexOf(k)] > 0) out.add(k);
            }
            return out;
        }
    }

    public static class Day {
        public final String dateKey;
        public final int age;
        public final List<Slot> slots = new ArrayList<>();

        Day(String dateKey, int age) {
            this.dateKey = dateKey;
            this.age = age;
        }

        public List<Dish> allDishes() {
            List<Dish> r = new ArrayList<>();
            for (Slot s : slots) r.addAll(s.dishes);
            return r;
        }

        /** 全天营养摄入 */
        public double[] intake() {
            return Nutrition.ofDay(this);
        }

        /** 均衡分 0-100 */
        public int score() {
            return Nutrition.balanceScore(intake(), age);
        }
    }

    private MenuEngine() {
    }

    /* ------------------------------------------------------------------ */

    /** 生成某天完整菜单。history 为最近几天吃过的菜 id（越近越靠后） */
    public static Day generate(String dateKey, int age, List<String> history, int salt) {
        Day day = new Day(dateKey, age);
        long seed = hash(dateKey + "#" + age + "#" + salt);

        Map<String, Integer> used = new HashMap<>();
        for (String id : history) used.merge(id, 1, Integer::sum);

        Set<String> usedToday = new HashSet<>();
        double[] acc = new double[Nutrition.KEYS.length];   // 已累计营养

        for (int s = 0; s < 5; s++) {
            Slot slot = new Slot(s);
            day.slots.add(slot);
            int[] plan = PLAN[s];
            Random rnd = new Random(seed * 31 + s * 7919L);

            for (int c = 0; c < CATS.length; c++) {
                for (int n = 0; n < plan[c]; n++) {
                    Dish picked = pick(CATS[c], age, rnd, used, usedToday, acc);
                    if (picked != null) {
                        slot.dishes.add(picked);
                        usedToday.add(picked.id);
                        addInto(acc, Nutrition.ofDish(picked));
                    }
                }
            }
            if (slot.dishes.isEmpty()) {
                Dish d = pick("snack", age, rnd, used, usedToday, acc);
                if (d != null) {
                    slot.dishes.add(d);
                    usedToday.add(d.id);
                    addInto(acc, Nutrition.ofDish(d));
                }
            }
        }
        return day;
    }

    /** 只重抽某一餐 */
    public static Slot regenerateSlot(Day day, int slotIndex, List<String> history, int salt) {
        Slot old = day.slots.get(slotIndex);
        List<Dish> others = day.allDishes();
        others.removeAll(old.dishes);

        // 其他餐已经提供的营养
        double[] acc = new double[Nutrition.KEYS.length];
        for (Dish d : others) addInto(acc, Nutrition.ofDish(d));

        Set<String> usedToday = new HashSet<>();
        for (Dish d : others) usedToday.add(d.id);

        Map<String, Integer> used = new HashMap<>();
        for (String id : history) used.merge(id, 1, Integer::sum);

        Slot slot = new Slot(slotIndex);
        int[] plan = PLAN[slotIndex];
        Random rnd = new Random(hash(day.dateKey + "#" + slotIndex + "#" + salt + "#" + System.nanoTime()));
        for (int c = 0; c < CATS.length; c++) {
            for (int n = 0; n < plan[c]; n++) {
                Dish picked = pick(CATS[c], day.age, rnd, used, usedToday, acc);
                if (picked != null) {
                    slot.dishes.add(picked);
                    usedToday.add(picked.id);
                    addInto(acc, Nutrition.ofDish(picked));
                }
            }
        }
        if (slot.dishes.isEmpty()) {
            Dish d = pick("snack", day.age, rnd, used, usedToday, acc);
            if (d != null) slot.dishes.add(d);
        }
        day.slots.set(slotIndex, slot);
        return slot;
    }

    /**
     * 在某类别里抽取一道菜。
     * 权重 = 历史降权 × 月龄贴合 × 随机抖动 × **营养缺口匹配度**
     *
     * 营养缺口匹配度：算出当前 12 项营养各自的缺口率（1 - 已摄入/目标），
     * 这道菜能补的那些项，缺口越大的权重越高。这样算法会主动去补短板。
     */
    private static Dish pick(String cat, int age, Random rnd, Map<String, Integer> history,
                             Set<String> usedToday, double[] acc) {
        List<Dish> pool = new ArrayList<>();
        for (Dish d : DishRepo.all()) {
            if (!d.cat.equals(cat)) continue;
            if (!d.fitsAge(age)) continue;
            if (usedToday.contains(d.id)) continue;
            pool.add(d);
        }
        // 池子太小（比如当前月龄下该类别可选的菜只剩 1-2 道）时，放宽到相邻月龄段
        if (pool.size() < 3) {
            for (Dish d : DishRepo.all()) {
                if (!d.cat.equals(cat)) continue;
                if (usedToday.contains(d.id)) continue;
                if ((d.fitsAge(age + 1) || d.fitsAge(age - 1)) && !pool.contains(d)) {
                    pool.add(d);
                }
            }
        }
        if (pool.isEmpty()) {
            for (Dish d : DishRepo.all()) {
                if (d.cat.equals(cat) && !usedToday.contains(d.id)) pool.add(d);
            }
        }
        if (pool.isEmpty()) return null;

        double[] gap = gaps(acc, age);   // 各项缺口率 0..1

        double[] w = new double[pool.size()];
        double total = 0;
        for (int i = 0; i < pool.size(); i++) {
            Dish d = pool.get(i);
            // 历史降权
            int cnt = history.getOrDefault(d.id, 0);
            double weight = cnt > 0 ? 1.0 / (1.0 + cnt * 3.0) : 1.6;
            // 随机抖动
            weight *= 0.85 + rnd.nextDouble() * 0.3;
            // 营养缺口匹配
            weight *= 1.0 + 2.4 * fitScore(Nutrition.ofDish(d), gap);
            // 稀缺营养素加成
            weight *= 1.0 + SCARCE_BONUS * scarceBoost(Nutrition.ofDish(d), gap);
            w[i] = weight;
            total += weight;
        }
        if (total <= 0) return pool.get(rnd.nextInt(pool.size()));

        double r = rnd.nextDouble() * total;
        for (int i = 0; i < pool.size(); i++) {
            r -= w[i];
            if (r <= 0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    /** 各项缺口率：1 - 已摄入/目标，截断到 0..1（已超标的记为 0） */
    private static double[] gaps(double[] acc, int age) {
        double[] g = new double[Nutrition.KEYS.length];
        for (int i = 0; i < g.length; i++) {
            double t = Nutrition.target(Nutrition.KEYS[i], age);
            g[i] = t <= 0 ? 0 : Math.max(0, Math.min(1, 1 - acc[i] / t));
        }
        return g;
    }

    /**
     * 稀缺营养素加成强度，0..1。
     *
     * 维生素 D 天然只存在于三文鱼、蛋黄、干香菇等极少数食材里，几乎不可能
     * 靠「随便配几道菜」碰到；而 10μg/天 的目标又是按「日照 + 补充剂」总量
     * 给出的。如果一个宝宝每天的三餐都绕开这几样食材，维 D 就会长期接近零。
     *
     * 所以这里给维 D 单独加一条加权通道：缺口越大、这道菜在这一项上越可观，
     * 被选中的倾向就越强。系数取 4 是离线模拟试出来的——再往上（6、8）
     * 提升不明显，反而会让菜单反复出现鱼类。
     */
    private static final double SCARCE_BONUS = 4.0;

    private static double scarceBoost(double[] dish, double[] gap) {
        int i = Nutrition.indexOf(Nutrition.VIT_D);
        if (i < 0 || i >= gap.length || gap[i] <= 0) return 0;
        double strength = Math.min(1, dish[i] / magnitude(Nutrition.VIT_D));
        return gap[i] * strength;
    }

    /** 这道菜对当前缺口的补足程度，0..1 */
    private static double fitScore(double[] dish, double[] gap) {
        double sum = 0;
        double maxGap = 0.001;
        for (int i = 0; i < gap.length; i++) maxGap = Math.max(maxGap, gap[i]);
        for (int i = 0; i < dish.length && i < gap.length; i++) {
            if (gap[i] <= 0) continue;
            // 该菜在这一项上的「贡献强度」：100mg 级别的营养素 50mg 算满分
            double strength = Math.min(1, dish[i] / magnitude(Nutrition.KEYS[i]));
            sum += gap[i] * strength;
        }
        return Math.min(1, sum / maxGap / 2.0);
    }

    /** 每项营养素的「一份有效贡献」量级，用于归一化 */
    private static double magnitude(String key) {
        switch (key) {
            case Nutrition.PROTEIN: return 8;
            case Nutrition.IRON: return 1.5;
            case Nutrition.ZINC: return 1.0;
            case Nutrition.CALCIUM: return 80;
            case Nutrition.IODINE: return 25;
            case Nutrition.DHA: return 80;
            case Nutrition.VIT_A: return 120;
            case Nutrition.VIT_C: return 12;
            case Nutrition.VIT_D: return 1.2;
            case Nutrition.VIT_B: return 0.18;
            case Nutrition.FIBER: return 1.2;
            case Nutrition.POTASSIUM: return 200;
            default: return 1;
        }
    }

    private static void addInto(double[] acc, double[] one) {
        for (int i = 0; i < acc.length && i < one.length; i++) acc[i] += one[i];
    }

    /** 完全随机「来一份」：不受日期约束 */
    public static Day random(int age, List<String> history) {
        long salt = (long) (Math.random() * 1000000);
        return generate("random", age, history, (int) (salt % 100000));
    }

    public static int parseKcal(String s) {
        if (s == null) return 0;
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') b.append(c);
        }
        if (b.length() == 0) return 0;
        try {
            return Integer.parseInt(b.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /* ---------------------- 存取 ---------------------- */

    /** "d1,d2|d3,d4|..." 每餐用 | 分隔，餐内用 , 分隔 */
    public static String pack(Day day) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < day.slots.size(); i++) {
            if (i > 0) b.append('|');
            Slot s = day.slots.get(i);
            for (int j = 0; j < s.dishes.size(); j++) {
                if (j > 0) b.append(',');
                b.append(s.dishes.get(j).id);
            }
        }
        return b.toString();
    }

    public static Day unpack(String dateKey, int age, String packed) {
        Day day = new Day(dateKey, age);
        String[] slots = packed.split("\\|", -1);
        for (int i = 0; i < 5; i++) {
            Slot s = new Slot(i);
            if (i < slots.length && !slots[i].isEmpty()) {
                for (String id : slots[i].split(",")) {
                    Dish d = DishRepo.byId(id);
                    if (d != null) s.dishes.add(d);
                }
            }
            day.slots.add(s);
        }
        return day;
    }

    /** 用 id 列表构造一天的菜单（用于历史记录等） */
    public static Day fromIds(String dateKey, int age, List<String> ids) {
        Day day = new Day(dateKey, age);
        for (int i = 0; i < 5; i++) day.slots.add(new Slot(i));
        return day;
    }

    private static long hash(String s) {
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        return Math.abs(h);
    }

    /* ---------------------- 展示辅助 ---------------------- */

    public static String catName(String cat) {
        switch (cat) {
            case "staple": return "主食";
            case "meat": return "荤菜";
            case "veg": return "素菜";
            case "soup": return "汤羹";
            case "snack": return "点心";
            default: return "其他";
        }
    }

    public static String catEmoji(String cat) {
        switch (cat) {
            case "staple": return "🍚";
            case "meat": return "🍖";
            case "veg": return "🥬";
            case "soup": return "🍲";
            case "snack": return "🍪";
            default: return "🍽";
        }
    }

    public static String ageName(int age) {
        switch (age) {
            case 1: return "1-1.5岁";
            case 2: return "1.5-2岁";
            case 3: return "2-3岁";
            default: return "1-3岁";
        }
    }

    public static String ageHint(int age) {
        switch (age) {
            case 1: return "质地细腻、小块软烂，不加盐不加糖";
            case 2: return "可以吃小丁小条，练习咀嚼，清淡少盐";
            default: return "接近家庭餐，但仍需少盐少油、切小块";
        }
    }

    /** 合并所有食材为购物清单 */
    public static Map<String, Integer> shoppingList(List<Dish> dishes) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Dish d : dishes) {
            for (Dish.Ingredient i : d.ingredients) {
                String key = i.name + "|" + i.unit;
                try {
                    int v = (int) Double.parseDouble(i.amount);
                    m.merge(key, v, Integer::sum);
                } catch (Exception e) {
                    m.merge(key, 0, Integer::sum);
                }
            }
        }
        return m;
    }
}
