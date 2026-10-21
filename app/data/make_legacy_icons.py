"""生成 API 24/25 用的老式 PNG 图标（无自适应图标支持时的 fallback）。

背景取自 ic_launcher_background.xml 的渐变色，前景几何元素与
ic_launcher_foreground.xml 保持同一套坐标（108 视口），
按 4 倍超采样绘制后缩小，得到干净的抗锯齿边缘。

输出到 res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png
以及 ic_launcher_round.png（圆形裁切版）。
"""
import os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

SS = 4           # 超采样倍数
VIEW = 108.0     # 矢量视口
SIZES = {        # 目录 -> (边长dp, 缩放)
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# ic_launcher_background.xml 里用的紫色系渐变端点
BG_TOP = (126, 88, 194)
BG_BOTTOM = (86, 56, 148)


def v(p):
    """视口坐标 -> 超采样像素（图标只有中间 72dp 是内容区，但这里直接铺满）"""
    return p * SS


def draw_icon(size, round_mask=False):
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    k = S / VIEW

    # 背景：竖向渐变
    grad = Image.new("RGB", (1, S))
    gd = ImageDraw.Draw(grad)
    for y in range(S):
        t = y / max(1, S - 1)
        gd.point((0, y), fill=(
            int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * t),
            int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * t),
            int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * t),
        ))
    grad = grad.resize((S, S))

    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([0, 0, S - 1, S - 1], fill=255)
    else:
        r = int(S * 0.18)  # 圆角矩形
        md.rounded_rectangle([0, 0, S - 1, S - 1], radius=r, fill=255)
    img.paste(grad, (0, 0), mask)

    d = ImageDraw.Draw(img)

    # 前景整体放大并居中：图标内容不该只占中间一小块
    CONTENT_SCALE = 1.28
    CX, CY = 54.0, 54.0   # 内容视觉重心（矢量坐标系）

    def P(x, y):
        return ((CX + (x - CX) * CONTENT_SCALE) * k,
                (CY + (y - CY) * CONTENT_SCALE) * k)

    def R(x0, y0, x1, y1):
        return [P(x0, y0)[0], P(x0, y0)[1], P(x1, y1)[0], P(x1, y1)[1]]

    # 碗身：上半个椭圆（碗口）+ 下半个椭圆（碗底），两者共享碗口宽度，侧边自然收拢
    bowl = R(30, 56, 78, 80)
    d.chord(bowl, 180, 360, fill=(255, 255, 255, 255))   # 碗底（下半）
    d.ellipse(R(30, 52, 78, 61), fill=(255, 255, 255, 255))   # 碗口沿

    # 碗内汤面（浅橙），比碗口略小一圈
    d.ellipse(R(34, 54, 74, 60), fill=(255, 224, 199, 255))
    d.chord(R(34, 54, 74, 76), 180, 360, fill=(255, 224, 199, 255))

    # 食物：橙球、绿球、红点
    d.ellipse(R(38, 38, 50, 50), fill=(255, 180, 67, 255))
    d.ellipse(R(53.5, 33.5, 66.5, 46.5), fill=(140, 199, 82, 255))
    d.ellipse(R(50.5, 28.5, 57.5, 35.5), fill=(255, 122, 69, 255))

    # 勺子柄与勺头（白）
    d.line([P(70, 34), P(84, 20)], fill=(255, 255, 255, 255),
           width=max(1, int(3 * k * CONTENT_SCALE)))
    d.ellipse(R(63, 26, 73, 36), fill=(255, 255, 255, 255))

    return img.resize((size, size), Image.LANCZOS)


def main():
    for folder, size in SIZES.items():
        out = os.path.join(BASE, folder)
        os.makedirs(out, exist_ok=True)
        draw_icon(size, False).save(os.path.join(out, "ic_launcher.png"), "PNG", optimize=True)
        draw_icon(size, True).save(os.path.join(out, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"{folder}: {size}x{size} x2")


if __name__ == "__main__":
    main()
