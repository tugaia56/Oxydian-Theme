package it.tugaia56.oxydiantheme.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/** Colora pulsanti e interruttori con l'accento scelto (il tema Material dell'app ha un colore fisso). */
final class Tint {
    private Tint() {}

    private static boolean isLight(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) > 150;
    }

    private static int darken(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[2] *= 0.35f;
        return Color.HSVToColor(hsv);
    }

    static void sw(MaterialSwitch s) {
        int acc = ThemePrefs.accentColor();
        int[][] st = {{android.R.attr.state_checked}, {}};
        s.setTrackTintList(new ColorStateList(st, new int[]{acc, 0xFF36343B}));
        s.setThumbTintList(new ColorStateList(st, new int[]{Color.WHITE, 0xFF938F99}));
        s.setTrackDecorationTintList(new ColorStateList(st, new int[]{acc, 0xFF938F99}));
    }

    static void button(MaterialButton b) {
        int acc = ThemePrefs.accentColor();
        if (b.getStrokeWidth() > 0) {
            // con bordo: testo accento, bordo neutro come prima
            b.setTextColor(acc);
            b.setRippleColor(ColorStateList.valueOf(acc & 0x33FFFFFF | 0x33000000));
        } else if (b.getId() == it.tugaia56.oxydiantheme.R.id.btn_apply) {
            b.setBackgroundTintList(ColorStateList.valueOf(acc));
            b.setTextColor(isLight(acc) ? 0xFF1B1B1B : Color.WHITE);
        } else {
            b.setTextColor(acc);
        }
    }

    /** Applica a tutti i pulsanti e interruttori sotto una vista. */
    static void tree(View v) {
        if (v instanceof MaterialSwitch) sw((MaterialSwitch) v);
        else if (v instanceof MaterialButton) button((MaterialButton) v);
        else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) tree(g.getChildAt(i));
        }
    }
}
