package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
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
        int fill = sprite ? a(mAccent, 0x59) : on ? a(mAccent, full ? 0xFF : 0x59) : a(mFg, full ? 0x99 : 0x22);
        p.setColor(fill);
        c.drawCircle(cx, cy, rad - sw / 2f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setColor(on ? mAccent : a(mFg, 0x99));
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
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1f * d);
            p.setColor(0xFFB0B0B0);
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
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(a(mFg, 0x66));
            c.drawCircle(cx, cy, rad - 3f * d, p);
        }
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter cf) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
