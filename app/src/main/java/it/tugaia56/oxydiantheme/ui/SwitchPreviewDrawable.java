package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Anteprima di uno stile di interruttore (spento e acceso) disegnata qui, senza applicare niente: stesso disegno dello
 * stile vero (overlay di sistema + Oxydian per gli interruttori OnePlus). Variante = nome della cartella dello stile.
 */
final class SwitchPreviewDrawable extends Drawable {
    private final String mVariant;
    private final float d;
    private final int mFg, mBg, mAccent;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    SwitchPreviewDrawable(Context ctx, String variant) {
        mVariant = variant == null ? "" : variant;
        d = ctx.getResources().getDisplayMetrics().density;
        mFg = ctx.getColor(R.color.text);
        mBg = ctx.getColor(R.color.card);
        mAccent = ThemePrefs.accentColor() | 0xFF000000;
    }

    @Override public int getIntrinsicWidth() { return Math.round((52 + 14 + 52) * d); }
    @Override public int getIntrinsicHeight() { return Math.round(30 * d); }

    private static int a(int color, int alpha) { return (alpha << 24) | (color & 0xFFFFFF); }

    @Override
    public void draw(Canvas c) {
        float top = getBounds().top, left = getBounds().left;
        one(c, left, top, false);
        one(c, left + (52 + 14) * d, top, true);
    }

    private void one(Canvas c, float x, float y, boolean on) {
        float w = 52 * d, h = 30 * d, cy = y + h / 2f;
        if (mVariant.isEmpty()) {
            // predefinito: barra piena e tondo bianco
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? mAccent : a(mFg, 0x44));
            r.set(x, cy - 14 * d, x + w, cy + 14 * d);
            c.drawRoundRect(r, 14 * d, 14 * d, p);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(on ? x + w - 14 * d : x + 14 * d, cy, 11 * d, p);
            return;
        }
        boolean ring = "Anello".equals(mVariant), onoff = mVariant.startsWith("ON_OFF"), btn = "Pulsante".equals(mVariant);
        if (ring || onoff || btn) { form(c, x, y, on, ring, onoff, btn); return; }
        if ("Levetta".equals(mVariant) || "LED".equals(mVariant) || "Tacche".equals(mVariant) || "Neon".equals(mVariant) || "Lucido_3D".equals(mVariant)) {
            float w0 = 52 * d, h0 = 28 * d, cy0 = y + 15 * d, rad0 = 11 * d;
            float cx0 = on ? x + w0 - 3 * d - rad0 : x + 3 * d + rad0;
            special(c, p, r, mVariant.toLowerCase(java.util.Locale.ROOT), on, x, x + w0, cy0, h0, cx0, rad0, mFg, mAccent, d);
            return;
        }
        boolean sprite = "Sprite".equals(mVariant);
        float th = 2f * d;
        float bl = x + 3 * d, br = x + w - 3 * d, rad = 11 * d;
        p.setStyle(Paint.Style.FILL);
        int bar = sprite ? (on ? mAccent : a(mFg, 0x40)) : ("Barra_accento".equals(mVariant) && on ? mAccent : mFg);
        p.setColor(bar);
        r.set(bl, cy - th / 2f, br, cy + th / 2f);
        c.drawRoundRect(r, th / 2f, th / 2f, p);
        float sw = (sprite ? 1f : 1.5f) * d;
        float cx = on ? br - rad : bl + rad;
        boolean full = "Pallino_pieno".equals(mVariant);
        int fill = sprite ? (on ? a(mAccent, 0x59) : a(mFg, 0x4D)) : on ? a(mAccent, full ? 0xFF : 0x59) : full ? mFg : a(mFg, 0x22);
        p.setColor(fill);
        c.drawCircle(cx, cy, rad - sw / 2f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setColor(on ? mAccent : full ? mFg : a(mFg, 0x99));
        c.drawCircle(cx, cy, rad - sw / 2f, p);
    }

    /** Stili a forma: anello, ON/OFF (anche verde e rosso), pulsante. */
    private void form(Canvas c, float x, float y, boolean on, boolean ring, boolean onoff, boolean btn) {
        float w = 52 * d, h = 28 * d, cy = y + 15 * d, left = x, right = x + w;
        int onC = mAccent, offC = a(mFg, 0x55);
        if ("ON_OFF_verde_rosso".equals(mVariant)) { onC = 0xFF34A853; offC = 0xFFD93025; }
        p.setStyle(Paint.Style.FILL);
        r.set(left, cy - h / 2f, right, cy + h / 2f);
        if (btn) {
            p.setColor(0xFF2A2D33);
            c.drawRoundRect(r, h / 2f, h / 2f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(a(mFg, 0x66));
            r.inset(0.75f * d, 0.75f * d);
            c.drawRoundRect(r, h / 2f, h / 2f, p);
        } else {
            p.setColor(on ? onC : offC);
            c.drawRoundRect(r, h / 2f, h / 2f, p);
        }
        float rad = 11 * d, cx = on ? right - 3 * d - rad : left + 3 * d + rad;
        if (ring) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f * d);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(cx, cy, rad - 1.5f * d, p);
        } else if (onoff) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFFFFFFF);
            c.drawCircle(cx, cy, rad - 1f * d, p);
            Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
            t.setColor(0xFFFFFFFF);
            t.setTextAlign(Paint.Align.CENTER);
            t.setFakeBoldText(true);
            t.setTextSize(h * 0.36f);
            float mid = on ? (left + (cx - rad)) / 2f : ((cx + rad) + right) / 2f;
            c.drawText(on ? "ON" : "OFF", mid, cy - (t.ascent() + t.descent()) / 2f, t);
        } else {
            p.setStyle(Paint.Style.FILL);
            p.setColor(on ? mAccent : 0xFF8A8F99);
            c.drawCircle(cx, cy, rad - 3f * d, p);
        }
    }


    /** Stili "di carattere": levetta, led, tacche, neon, lucido_3d. cx = centro del pallino, rad = suo raggio, h = altezza della barra. */
    static void special(Canvas c, Paint p, RectF r, String var, boolean on, float left, float right, float cy, float h,
                        float cx, float rad, int fg, int accent, float d) {
        p.setShader(null);
        switch (var) {
            case "levetta": {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF2A2D33);
                float bl = left + 4 * d, br = right - 4 * d, bt = cy - h / 2f + 2 * d, bb = cy + h / 2f - 2 * d;
                r.set(bl, bt, br, bb);
                c.drawRoundRect(r, 5 * d, 5 * d, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f * d);
                p.setColor((0x66 << 24) | (fg & 0xFFFFFF));
                r.inset(0.75f * d, 0.75f * d);
                c.drawRoundRect(r, 5 * d, 5 * d, p);
                float kw = (br - bl) / 2f - 3 * d, kh = bb - bt - 4 * d;
                float kx = Math.max(bl + 2 * d + kw / 2f, Math.min(br - 2 * d - kw / 2f, cx));
                r.set(kx - kw / 2f, cy - kh / 2f, kx + kw / 2f, cy + kh / 2f);
                p.setStyle(Paint.Style.FILL);
                p.setColor(on ? accent : 0xFF8A8F99);
                c.drawRoundRect(r, 4 * d, 4 * d, p);
                p.setColor(0x55FFFFFF);
                r.set(kx - kw / 2f + 3 * d, cy - kh / 2f + 1.5f * d, kx + kw / 2f - 3 * d, cy - kh / 2f + 3f * d);
                c.drawRect(r, p);
                break;
            }
            case "led": {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1.5f * d);
                p.setColor((0x44 << 24) | (fg & 0xFFFFFF));
                r.set(left + 0.75f * d, cy - 4 * d, right - 0.75f * d, cy + 4 * d);
                c.drawRoundRect(r, 4 * d, 4 * d, p);
                float rr = rad * 0.62f;
                p.setStyle(Paint.Style.FILL);
                if (on) {
                    float gr = Math.max(h / 2f, rr * 2f);
                    p.setColor(0xFF000000);
                    p.setShader(new RadialGradient(cx, cy, gr,
                            new int[]{(0xE6 << 24) | (accent & 0xFFFFFF), (0x80 << 24) | (accent & 0xFFFFFF),
                                    (0x30 << 24) | (accent & 0xFFFFFF), (accent & 0xFFFFFF)},
                            new float[]{0f, 0.35f, 0.7f, 1f}, Shader.TileMode.CLAMP));
                    c.drawCircle(cx, cy, gr, p);
                    p.setShader(null);
                }
                p.setColor(on ? accent : 0xFF4A4E56);
                c.drawCircle(cx, cy, rr, p);
                p.setColor(on ? 0xAAFFFFFF : 0x22FFFFFF);
                c.drawCircle(cx - rr * 0.3f, cy - rr * 0.3f, rr * 0.32f, p);
                break;
            }
            case "tacche": {
                int n = 9;
                float tw = 3 * d, th = 12 * d;
                float l0 = left + 3 * d + tw / 2f, r0 = right - 3 * d - tw / 2f;
                // l'indice percorre tutte le tacche: da quella di sinistra a quella di destra
                float c0 = left + 3 * d + rad, c1 = right - 3 * d - rad;
                float prog = c1 - c0 > 1f ? Math.max(0f, Math.min(1f, (cx - c0) / (c1 - c0))) : (on ? 1f : 0f);
                float mx = l0 + prog * (r0 - l0);
                p.setStyle(Paint.Style.FILL);
                for (int i = 0; i < n; i++) {
                    float x = l0 + (r0 - l0) * i / (n - 1);
                    boolean act = x <= mx + 0.5f * d;
                    p.setColor(act ? accent : ((0x44 << 24) | (fg & 0xFFFFFF)));
                    r.set(x - tw / 2f, cy - th / 2f, x + tw / 2f, cy + th / 2f);
                    c.drawRoundRect(r, tw / 2f, tw / 2f, p);
                }
                float bw = 4 * d, bh = Math.min(h - 2 * d, 16 * d);
                p.setColor(on ? 0xFFFFFFFF : ((0xCC << 24) | (fg & 0xFFFFFF)));
                r.set(mx - bw / 2f, cy - bh / 2f, mx + bw / 2f, cy + bh / 2f);
                c.drawRoundRect(r, bw / 2f, bw / 2f, p);
                break;
            }
            case "neon": {
                float ph = h - 10 * d;
                r.set(left + 5 * d, cy - ph / 2f, right - 5 * d, cy + ph / 2f);
                p.setStyle(Paint.Style.STROKE);
                if (on) {
                    // bordo senza alone: l'alone resta solo sul pallino
                    p.setStrokeWidth(2 * d);
                    p.setColor(accent);
                    c.drawRoundRect(r, ph / 2f, ph / 2f, p);
                } else {
                    p.setStrokeWidth(1.5f * d);
                    p.setColor((0x66 << 24) | (fg & 0xFFFFFF));
                    c.drawRoundRect(r, ph / 2f, ph / 2f, p);
                }
                float rr = rad - 5 * d;
                if (on) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor((0x33 << 24) | (accent & 0xFFFFFF));
                    c.drawCircle(cx, cy, rr, p);
                    p.setStyle(Paint.Style.STROKE);
                    float[] sw = {9 * d, 6 * d, 4 * d, 2.5f * d};
                    int[] al = {0x33, 0x55, 0x99, 0xFF};
                    for (int i = 0; i < 4; i++) {
                        p.setStrokeWidth(sw[i]);
                        p.setColor((al[i] << 24) | (accent & 0xFFFFFF));
                        c.drawCircle(cx, cy, rr, p);
                    }
                } else {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1.5f * d);
                    p.setColor((0x66 << 24) | (fg & 0xFFFFFF));
                    c.drawCircle(cx, cy, rr, p);
                }
                break;
            }
            case "lucido_3d": {
                float top = cy - h / 2f + d, bot = cy + h / 2f - d;
                int c1 = on ? mixc(accent, 0xFFFFFFFF, 0.30f) : 0xFF6A6F79;
                int c2 = on ? mixc(accent, 0xFF000000, 0.40f) : 0xFF2B2E34;
                r.set(left + d, top, right - d, bot);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFF000000);
                p.setShader(new LinearGradient(0, top, 0, bot, c1, c2, Shader.TileMode.CLAMP));
                c.drawRoundRect(r, (bot - top) / 2f, (bot - top) / 2f, p);
                p.setShader(null);
                // bordo e riflesso sul bordo alto
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1f * d);
                p.setColor(0x55000000);
                c.drawRoundRect(r, (bot - top) / 2f, (bot - top) / 2f, p);
                p.setColor(0x44FFFFFF);
                p.setStrokeWidth(1f * d);
                r.set(left + d + 11 * d, top + 1.5f * d, right - d - 11 * d, top + 1.5f * d);
                c.drawLine(r.left, r.top, r.right, r.top, p);
                // pallino come una sfera lucida con ombra
                float rr = rad - 2 * d;
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x55000000);
                c.drawCircle(cx, cy + 1.5f * d, rr, p);
                p.setColor(0xFF000000);
                p.setShader(new RadialGradient(cx - rr * 0.3f, cy - rr * 0.4f, rr * 1.4f,
                        new int[]{0xFFFFFFFF, 0xFFE3E6EB, 0xFF9CA2AC}, new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, rr, p);
                p.setShader(null);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(1f * d);
                p.setColor(0x44000000);
                c.drawCircle(cx, cy, rr, p);
                break;
            }
            default:
                break;
        }
    }

    private static int mixc(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | (Math.round(ar + (br - ar) * t) << 16) | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
