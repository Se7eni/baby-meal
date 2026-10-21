package com.babymeal.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.util.LruCache;

import java.io.InputStream;

/** 菜品图加载 + 圆形 / 圆角裁切 */
public final class Images {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(48) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private Images() {
    }

    public static Bitmap load(Context ctx, Dish d) {
        Bitmap b = CACHE.get(d.id);
        if (b != null) return b;
        try (InputStream is = ctx.getAssets().open(d.imageAsset())) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.ARGB_8888;
            b = BitmapFactory.decodeStream(is, null, o);
            if (b != null) CACHE.put(d.id, b);
        } catch (Exception ignored) {
        }
        return b;
    }

    public static Bitmap roundCorners(Bitmap src, float radiusPx) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(0, 0, w, h), radiusPx, radiusPx, p);
        return out;
    }

    public static Bitmap circle(Bitmap src) {
        if (src == null) return null;
        int s = Math.min(src.getWidth(), src.getHeight());
        Bitmap sq = Bitmap.createBitmap(src, (src.getWidth() - s) / 2, (src.getHeight() - s) / 2, s, s);
        Bitmap out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new BitmapShader(sq, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        c.drawCircle(s / 2f, s / 2f, s / 2f, p);
        return out;
    }

    /** 居中裁切到目标宽高比 */
    public static Bitmap centerCrop(Bitmap src, float targetRatio) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();
        float ratio = w / (float) h;
        if (Math.abs(ratio - targetRatio) < 0.01f) return src;
        int nw, nh;
        if (ratio > targetRatio) {
            nh = h;
            nw = (int) (h * targetRatio);
        } else {
            nw = w;
            nh = (int) (w / targetRatio);
        }
        int x = (w - nw) / 2, y = (h - nh) / 2;
        Bitmap out = Bitmap.createBitmap(src, x, y, nw, nh);
        if (out.getWidth() > 1200) {
            Matrix m = new Matrix();
            float sc = 1200f / out.getWidth();
            m.postScale(sc, sc);
            out = Bitmap.createBitmap(out, 0, 0, out.getWidth(), out.getHeight(), m, true);
        }
        return out;
    }
}
