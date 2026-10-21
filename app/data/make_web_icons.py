"""生成 PWA 图标：从与安卓版同一套几何图形绘制，输出多种尺寸。

iOS 的 apple-touch-icon 不能有透明通道，所以这里都铺满背景、不裁圆角
（iOS 会自己加圆角遮罩）。maskable 版本留出 10% 安全边距。
"""
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))     # app/data
BASE = os.path.abspath(os.path.join(HERE, "..", "..", "web", "icons"))
SS = 4
VIEW = 108.0
BG_TOP = (126, 88, 194)
BG_BOTTOM = (86, 56, 148)


def draw_icon(size, content_scale=1.28, pad_ratio=0.0):
    S = int(size * SS)
    img = Image.new("RGB", (S, S), BG_TOP)
    k = S / VIEW

    # 竖向渐变背景
    grad = Image.new("RGB", (1, S))
    gd = ImageDraw.Draw(grad)
    for y in range(S):
        t = y / max(1, S - 1)
        gd.point((0, y), fill=(
            int(BG_TOP[0] + (BG_BOTTOM[0] - BG_TOP[0]) * t),
            int(BG_TOP[1] + (BG_BOTTOM[1] - BG_TOP[1]) * t),
            int(BG_TOP[2] + (BG_BOTTOM[2] - BG_TOP[2]) * t),
        ))
    img.paste(grad.resize((S, S)))

    d = ImageDraw.Draw(img)
    CX, CY = 54.0, 54.0
    sc = content_scale * (1.0 - pad_ratio)

    def P(x, y):
        return ((CX + (x - CX) * sc) * k, (CY + (y - CY) * sc) * k)

    def R(x0, y0, x1, y1):
        return [P(x0, y0)[0], P(x0, y0)[1], P(x1, y1)[0], P(x1, y1)[1]]

    # 碗
    d.chord(R(30, 56, 78, 80), 180, 360, fill=(255, 255, 255))
    d.ellipse(R(30, 52, 78, 61), fill=(255, 255, 255))
    # 汤面
    d.ellipse(R(34, 54, 74, 60), fill=(255, 224, 199))
    d.chord(R(34, 54, 74, 76), 180, 360, fill=(255, 224, 199))
    # 食物
    d.ellipse(R(38, 38, 50, 50), fill=(255, 180, 67))
    d.ellipse(R(53.5, 33.5, 66.5, 46.5), fill=(140, 199, 82))
    d.ellipse(R(50.5, 28.5, 57.5, 35.5), fill=(255, 122, 69))
    # 勺子
    d.line([P(70, 34), P(84, 20)], fill=(255, 255, 255),
           width=max(1, int(3 * k * sc)))
    d.ellipse(R(63, 26, 73, 36), fill=(255, 255, 255))

    return img.resize((size, size), Image.LANCZOS)


def main():
    os.makedirs(BASE, exist_ok=True)
    # 普通图标：铺满，iOS 自己加圆角
    for sz in (120, 152, 167, 180, 192, 512):
        draw_icon(sz).save(os.path.join(BASE, f"icon-{sz}.png"), "PNG", optimize=True)
    draw_icon(180).save(os.path.join(BASE, "apple-touch-icon-180.png"), "PNG", optimize=True)
    # maskable：内容缩到 80%，四周留安全区
    draw_icon(512, pad_ratio=0.2).save(os.path.join(BASE, "maskable-512.png"), "PNG", optimize=True)
    print("icons done:", sorted(os.listdir(BASE)))


if __name__ == "__main__":
    main()
