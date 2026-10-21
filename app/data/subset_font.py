"""把字体裁剪到 App 实际用到的字符集，体积从 31MB 降到 ~1MB"""
import glob
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)

BASE = os.path.dirname(os.path.abspath(__file__))
SRC_MAIN = os.path.abspath(os.path.join(BASE, "..", "app", "src", "main"))
ASSETS = os.path.join(SRC_MAIN, "assets")
FONTS = os.path.join(ASSETS, "fonts")

chars = set()

# 1. 菜品数据 JSON（含所有菜名、食材、做法、营养、贴士）
with open(os.path.join(ASSETS, "dishes.json"), encoding="utf-8") as f:
    chars |= set(f.read())

# 2. 所有 Java 源码的中文字面量 + XML 资源
for j in glob.glob(os.path.join(SRC_MAIN, "java", "**", "*.java"), recursive=True):
    with open(j, encoding="utf-8") as f:
        chars |= set(f.read())
for x in glob.glob(os.path.join(SRC_MAIN, "res", "**", "*.xml"), recursive=True):
    with open(x, encoding="utf-8") as f:
        chars |= set(f.read())

# 3. 常见中文字符兜底（用户输入搜索时可能键入任意汉字）
#    插入常用 3500 字的高频部分，保证搜索框不会出现豆腐块
common = (
    "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞"
    "宝宝吃什辅食月龄段岁鸡蛋牛奶米粉南瓜胡萝卜西兰花菠菜土豆红薯山药玉米豌豆豆腐鸡胸腿牛肉猪肉虾仁三文鱼鳕鱼肝菠菜油菜小白菜番茄洋葱蘑菇香米面条馒头粥羹汤软饭炒蒸煮炖烤拼盘食材明细做法步骤营养补充维素蛋白质铁锌钙碘DHA膳食纤维热量千卡克毫升勺个片张碗适量少许清水温开水和油盐糖酱醋葱姜蒜"
)
chars |= set(common)

# 4. 基础 ASCII、标点、常用符号
extra = (
    "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    " .,:;!?()[]{}<>/\\|-–—_+*=@#$%&'\"`~^，。、；：？！（）【】《》“”‘’…·"
    "¥￥℃°±×÷≈≥≤→←↑↓★☆♥♡✓✔"
    "\t\n"
)
chars |= set(extra)
chars = {c for c in chars if ord(c) > 31}

text_path = os.path.join(BASE, "_subset_chars.txt")
with open(text_path, "w", encoding="utf-8") as f:
    f.write("".join(sorted(chars)))
print("unique chars:", len(chars))

from fontTools import subset  # noqa: E402

for name in ("deng.ttf", "dengb.ttf"):
    orig = os.path.join(FONTS, name + ".orig")
    cur = os.path.join(FONTS, name)
    if not os.path.exists(orig):
        os.rename(cur, orig)          # 保留原文件，重复执行也不会二次裁剪
    src = orig
    args = [
        src,
        "--text-file=" + text_path,
        "--output-file=" + cur,
        "--layout-features=*",
        "--drop-tables=DSIG",
        "--no-hinting",
        "--desubroutinize",
        "--name-IDs=*",
        "--recalc-bounds",
    ]
    subset.main(args)
    print("%-10s %6d KB  <- %6d KB" % (
        name, os.path.getsize(cur) // 1024, os.path.getsize(src) // 1024))
