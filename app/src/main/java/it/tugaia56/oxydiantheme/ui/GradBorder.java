package it.tugaia56.oxydiantheme.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

/**
 * Bordo sfumato delle schede: legge la scelta fatta in Oxydian > Mods > Varie > "Bordo sfumato", che Oxydian scrive in
 * proprieta' di sistema (persist.obsidian.grad_*): acceso, colore 1 (vuoto = accento), colore 2, colore 3 (vuoto = solo
 * due colori; "accent" = accento del Theme), spessore in mezzi dp, direzione (0 diagonale, 1 sx>dx, 2 dx>sx, 3 alto>basso,
 * 4 basso>alto, 5 radiale). Spento o senza secondo colore: {@link #card} restituisce null e restano i bordi normali.
 */
final class GradBorder {

    private GradBorder() {}

    private static String prop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            return (String) sp.getMethod("get", String.class, String.class).invoke(null, key, "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static Integer parse(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return (int) Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Colore di una voce: numero ARGB, "accent" = accento del Theme, vuoto = nessuno. */
    private static Integer color(String v, int accent) {
        if ("accent".equals(v)) return accent;
        Integer c = parse(v);
        return c == null ? null : (c | 0xFF000000);
    }

    /** Sfondo della scheda con bordo sfumato, oppure null se l'opzione e' spenta. */
    static Drawable card(int fill, float radius, float density, int accent) {
        if (!"1".equals(prop("persist.obsidian.grad_on"))) return null;
        Integer c1 = color(prop("persist.obsidian.grad_c1"), accent);
        Integer c2 = color(prop("persist.obsidian.grad_c2"), accent);
        Integer c3 = color(prop("persist.obsidian.grad_c3"), accent);
        if (c2 == null && c3 == null) return null;
        int first = c1 != null ? c1 : accent;
        int[] colors;
        if (c2 != null && c3 != null) colors = new int[]{first, c2, c3};
        else colors = new int[]{first, c2 != null ? c2 : c3};
        Integer hw = parse(prop("persist.obsidian.grad_w"));
        int halfDp = hw != null && hw > 0 ? hw : 3;
        Integer dir = parse(prop("persist.obsidian.grad_dir"));
        return new Ring(fill, radius, Math.max(1f, halfDp * 0.5f * density), colors, dir != null ? dir : 0);
    }

    private static final class Ring extends Drawable {
        private final Paint mFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float mRadius, mWidth;
        private final int[] mColors;
        private final int mDir;
        private final RectF mR = new RectF();

        Ring(int fill, float radius, float width, int[] colors, int dir) {
            mFill.setColor(fill);
            mRing.setStyle(Paint.Style.STROKE);
            mRing.setStrokeWidth(width);
            mRadius = radius;
            mWidth = width;
            mColors = colors;
            mDir = dir;
        }

        @Override public void draw(@NonNull Canvas c) {
            float half = mWidth / 2f;
            mR.set(getBounds());
            mR.inset(half, half);
            c.drawRoundRect(mR, mRadius, mRadius, mFill);
            mRing.setShader(shader(mR, mColors, mDir));
            float rr = Math.max(0f, mRadius - half);
            c.drawRoundRect(mR, rr, rr, mRing);
        }

        @Override public void setAlpha(int alpha) {
            mFill.setAlpha(alpha);
            mRing.setAlpha(alpha);
        }

        @Override public void setColorFilter(ColorFilter cf) {}

        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    private static Shader shader(RectF r, int[] colors, int dir) {
        float cx = r.centerX(), cy = r.centerY();
        switch (dir) {
            case 1: return new LinearGradient(r.left, cy, r.right, cy, colors, null, Shader.TileMode.CLAMP);
            case 2: return new LinearGradient(r.right, cy, r.left, cy, colors, null, Shader.TileMode.CLAMP);
            case 3: return new LinearGradient(cx, r.top, cx, r.bottom, colors, null, Shader.TileMode.CLAMP);
            case 4: return new LinearGradient(cx, r.bottom, cx, r.top, colors, null, Shader.TileMode.CLAMP);
            case 5: {
                float rad = (float) Math.hypot(r.width() / 2f, r.height() / 2f);
                return new RadialGradient(cx, cy, Math.max(1f, rad), colors, null, Shader.TileMode.CLAMP);
            }
            default: return new LinearGradient(r.left, r.top, r.right, r.bottom, colors, null, Shader.TileMode.CLAMP);
        }
    }
}
