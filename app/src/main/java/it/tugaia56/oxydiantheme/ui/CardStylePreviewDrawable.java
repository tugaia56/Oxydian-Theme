package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Anteprima di uno stile di notifica o di toast (gruppi "notifstyle" e "toaststyle"): una piccola card disegnata come lo
 * stile vero, con accento e sfondo del Theme, sopra uno sfondo a sfumatura (cosi' si vedono le trasparenze).
 * Variante = nome della cartella dello stile (gli stessi disegni stanno nei file overlay).
 */
final class CardStylePreviewDrawable extends Drawable {
    private final String mGroup, mVariant;
    private final float d;
    private final int mAcc, mBg;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    CardStylePreviewDrawable(Context ctx, String group, String variant) {
        mGroup = group;
        mVariant = variant == null ? "" : variant;
        d = ctx.getResources().getDisplayMetrics().density;
        mAcc = ThemePrefs.accentColor() | 0xFF000000;
        Integer bg = SystemColorsDialog.currentBg(ctx);
        mBg = 0xFF000000 | (bg != null ? bg : 0x1B2029);
    }

    @Override public int getIntrinsicWidth() { return Math.round(104 * d); }
    @Override public int getIntrinsicHeight() { return Math.round(40 * d); }

    private static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }
    private static int darken(int c, float f) {
        return 0xFF000000 | (Math.round(((c >> 16) & 0xFF) * f) << 16) | (Math.round(((c >> 8) & 0xFF) * f) << 8) | Math.round((c & 0xFF) * f);
    }

    @Override
    public void draw(Canvas c) {
        float x = getBounds().left, y = getBounds().top, w = getIntrinsicWidth(), h = getIntrinsicHeight();
        // sfondo a sfumatura
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(x, y, x + w, y + h, new int[]{0xFF5B6BC8, 0xFF1B2A55, 0xFF8A5BB8}, null, Shader.TileMode.CLAMP));
        r.set(x, y, x + w, y + h);
        c.drawRoundRect(r, 10 * d, 10 * d, p);
        p.setShader(null);
        // la card (a sinistra e a destra un po' di sfondo)
        float cx = x + 6 * d, cy = y + 6 * d, cw = w - 12 * d, ch = h - 12 * d;
        float rad = Math.min(ch / 2f, ("toaststyle".equals(mGroup) ? 12 : 9) * d);
        if ("toaststyle".equals(mGroup)) toast(c, cx, cy, cw, ch, rad);
        else if ("dialogstyle".equals(mGroup)) dialog(c, cx, cy, cw, ch, Math.min(ch / 2f, 6 * d));
        else notif(c, cx, cy, cw, ch, rad);
        // due righe finte di testo
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFFFFFFF);
        r.set(cx + 10 * d, cy + ch / 2 - 5 * d, cx + 10 * d + 46 * d, cy + ch / 2 - 1.5f * d);
        c.drawRoundRect(r, 2 * d, 2 * d, p);
        p.setColor(0x99FFFFFF);
        r.set(cx + 10 * d, cy + ch / 2 + 1.5f * d, cx + 10 * d + 32 * d, cy + ch / 2 + 5 * d);
        c.drawRoundRect(r, 2 * d, 2 * d, p);
    }

    private void rr(Canvas c, float x, float y, float w, float h, float rad, int fill, float stroke, int strokeColor) {
        r.set(x, y, x + w, y + h);
        if (Color_alpha(fill) != 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawRoundRect(r, rad, rad, p);
        }
        if (stroke > 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setColor(strokeColor);
            r.inset(stroke / 2f, stroke / 2f);
            c.drawRoundRect(r, Math.max(0, rad - stroke / 2f), Math.max(0, rad - stroke / 2f), p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private static int Color_alpha(int color) { return (color >>> 24); }

    /** Sfumatura diagonale dal colore pieno a una versione piu' scura (come i preset del modulo). */
    private void gradient(Canvas c, float x, float y, float w, float h, float rad, int from, int to) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(x, y, x + w, y + h, from, to, Shader.TileMode.CLAMP));
        r.set(x, y, x + w, y + h);
        c.drawRoundRect(r, rad, rad, p);
        p.setShader(null);
    }

    private void notif(Canvas c, float x, float y, float w, float h, float rad) {
        switch (mVariant) {
            case "Trasparente_con_bordo_sottile": rr(c, x, y, w, h, rad, 0, d, mAcc); break;
            case "Trasparenza_alta_con_bordo": rr(c, x, y, w, h, rad, 0x40000000, 2 * d, mAcc); break;
            case "Trasparenza_media_con_bordo": rr(c, x, y, w, h, rad, 0x80000000, 2 * d, mAcc); break;
            case "Trasparenza_bassa_con_bordo": rr(c, x, y, w, h, rad, 0xBF000000, 2 * d, mAcc); break;
            case "Nero_con_bordo": rr(c, x, y, w, h, rad, 0xFF000000, 2 * d, mAcc); break;
            case "Sfondo_del_tema": rr(c, x, y, w, h, rad, mBg, 0, 0); break;
            case "Nero_pieno": rr(c, x, y, w, h, rad, 0xFF000000, 0, 0); break;
            case "Accento_pieno": rr(c, x, y, w, h, rad, mAcc, 0, 0); break;
            default: rr(c, x, y, w, h, rad, 0x30FFFFFF, 0, 0); break;   // predefinito / trasparente: solo un velo leggero
        }
    }

    private void dialog(Canvas c, float x, float y, float w, float h, float rad) {
        switch (mVariant) {
            case "Alta_trasparenza": rr(c, x, y, w, h, rad, 0x331B2029, 0, 0); break;
            case "Alta_trasparenza_con_bordo": rr(c, x, y, w, h, rad, 0x331B2029, 1.5f * d, mAcc); break;
            case "Bassa_trasparenza": rr(c, x, y, w, h, rad, 0xCC1B2029, 0, 0); break;
            case "Bassa_trasparenza_con_bordo": rr(c, x, y, w, h, rad, 0xCC1B2029, 1.5f * d, mAcc); break;
            case "Media_trasparenza": rr(c, x, y, w, h, rad, 0x801B2029, 0, 0); break;
            case "Media_trasparenza_con_bordo": rr(c, x, y, w, h, rad, 0x80000000, 1.5f * d, mAcc); break;
            case "Scuro": rr(c, x, y, w, h, rad, 0xFF000000 | Math.min(0xFFFFFF, mBg + 0x090A0D), 0, 0); break;
            case "Scuro_con_bordo": rr(c, x, y, w, h, rad, mBg, 1.5f * d, mAcc); break;
            default: rr(c, x, y, w, h, rad, 0x30FFFFFF, 0, 0); break;
        }
    }

    private void toast(Canvas c, float x, float y, float w, float h, float rad) {
        switch (mVariant) {
            case "Solido": rr(c, x, y, w, h, rad, mBg, 0, 0); break;
            case "Bordo_sottile": rr(c, x, y, w, h, rad, mBg, 1.5f * d, mAcc); break;
            case "Bordo_spesso": rr(c, x, y, w, h, rad, mBg, 4 * d, mAcc); break;
            case "Accento_pieno": rr(c, x, y, w, h, rad, mAcc, 0, 0); break;
            case "Semitrasparente": rr(c, x, y, w, h, rad, 0x80000000, 0, 0); break;
            case "Accento_sfumato": gradient(c, x, y, w, h, rad, mAcc, darken(mAcc, 0.6f)); break;
            case "Vetro":
                gradient(c, x, y, w, h, rad, mAcc, darken(mAcc, 0.7f));
                rr(c, x + d, y + d, w - 2 * d, h - 2 * d, Math.max(0, rad - d), 0x99FFFFFF, 0, 0);
                break;
            case "Grigio":
                gradient(c, x, y, w, h, rad, 0xFFF0F0F0, 0xFFBDBDBD);
                rr(c, x + 3 * d, y + 3 * d, w - 6 * d, h - 6 * d, Math.max(0, rad - 3 * d), mAcc, 0, 0);
                break;
            case "Accento_doppio":
                rr(c, x, y, w, h, rad, mAcc, 0, 0);
                rr(c, x, y + 6 * d, w, h - 12 * d, Math.max(0, rad - 6 * d), mBg, 0, 0);
                break;
            case "Neutro_trasparente": rr(c, x, y, w, h, rad, 0x99424242, 0, 0); break;
            case "Accento_con_velo":
                gradient(c, x, y, w, h, rad, alpha(mAcc, 0xCC), alpha(mAcc, 0x66));
                rr(c, x + 4 * d, y + 4 * d, w - 8 * d, h - 8 * d, Math.max(0, rad - 4 * d), 0x4D000000, 0, 0);
                break;
            case "Pillola_asimmetrica": {
                float big = rad, small = rad * 0.6f;
                pill(c, x, y, w, h, big, small, mAcc);
                pill(c, x + 4 * d, y + 4 * d, w - 8 * d, h - 8 * d, Math.max(0, big - 4 * d), Math.max(0, small - 4 * d), mBg);
                break;
            }
            default: rr(c, x, y, w, h, rad, 0x30FFFFFF, 0, 0); break;
        }
    }

    /** Angoli disuguali: alto-sinistra e basso-destra grandi, gli altri due piccoli. */
    private void pill(Canvas c, float x, float y, float w, float h, float big, float small, int color) {
        Path path = new Path();
        r.set(x, y, x + w, y + h);
        path.addRoundRect(r, new float[]{big, big, small, small, big, big, small, small}, Path.Direction.CW);
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        c.drawPath(path, p);
    }

    @Override public void setAlpha(int alpha) {}
    @Override public void setColorFilter(ColorFilter colorFilter) {}
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
