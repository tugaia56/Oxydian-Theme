package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.ThemeProps;

/**
 * Bordo sfumato (stesse proprieta' di Oxydian: persist.obsidian.grad_*): acceso/spento e colori 2 e 3. Il colore 1 e'
 * sempre l'accento. Lo usano le schede del Theme e gli stili "con bordo sfumato" di notifiche, toast e dialog.
 */
final class GradBorderDialog {
    private GradBorderDialog() {}

    private static String prop(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            return (String) sp.getMethod("get", String.class, String.class).invoke(null, key, "");
        } catch (Throwable t) {
            return "";
        }
    }

    private static Integer parse(String s) {
        try {
            return s == null || s.isEmpty() || "accent".equals(s) ? null : (Integer) (int) Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String summary(Context ctx) {
        return "1".equals(prop("persist.obsidian.grad_on")) ? ctx.getString(R.string.gb_on) : ctx.getString(R.string.gb_off);
    }

    static void show(Context ctx, Runnable onSaved) {
        final float d = ctx.getResources().getDisplayMetrics().density;
        final int pad = (int) (20 * d);
        final int accent = ThemePrefs.accentColor();
        final boolean[] on = {"1".equals(prop("persist.obsidian.grad_on"))};
        final Integer[] c = {parse(prop("persist.obsidian.grad_c2")), parse(prop("persist.obsidian.grad_c3"))};
        if (c[0] == null) c[0] = 0xFF7C4DFF;

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        SwitchCompat sw = new SwitchCompat(ctx);
        sw.setText(R.string.gb_switch);
        sw.setTextColor(ctx.getColor(R.color.text));
        sw.setChecked(on[0]);
        sw.setOnCheckedChangeListener((b, v) -> on[0] = v);
        box.addView(sw);
        TextView hint = new TextView(ctx);
        hint.setText(R.string.gb_hint);
        hint.setTextColor(ctx.getColor(R.color.text));
        hint.setAlpha(0.7f);
        hint.setPadding(0, pad / 2, 0, pad / 2);
        box.addView(hint);
        box.addView(colorRow(ctx, ctx.getString(R.string.gb_c2), c, 0, d));
        box.addView(colorRow(ctx, ctx.getString(R.string.gb_c3), c, 1, d));

        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.card_gradborder)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    final boolean o = on[0];
                    final Integer c2 = c[0], c3 = c[1];
                    new Thread(() -> {
                        ThemeProps.publishGradBorder(o, c2, c3);
                        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                        if (onSaved != null) new android.os.Handler(android.os.Looper.getMainLooper()).post(onSaved);
                    }).start();
                }));
    }

    private static LinearLayout colorRow(Context ctx, String label, Integer[] c, int idx, float d) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (6 * d), 0, (int) (6 * d));
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextColor(ctx.getColor(R.color.text));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button pick = new Button(ctx);
        pick.setAllCaps(false);
        pick.setTextColor(0xFFFFFFFF);
        Runnable paint = () -> {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(10 * d);
            if (c[idx] != null) {
                g.setColor(c[idx] | 0xFF000000);
                pick.setText(String.format("#%06X", c[idx] & 0xFFFFFF));
            } else {
                g.setColor(0x00000000);
                g.setStroke((int) (1.5f * d), ThemePrefs.accentColor());
                pick.setText(R.string.gb_none);
            }
            pick.setBackground(g);
        };
        paint.run();
        pick.setOnClickListener(v -> ColorPickDialog.show(ctx, c[idx] != null ? c[idx] : 0xFF7C4DFF, R.string.opt_custom, color -> {
            c[idx] = color | 0xFF000000;
            paint.run();
        }));
        row.addView(pick, new LinearLayout.LayoutParams((int) (130 * d), (int) (44 * d)));
        if (idx == 1) {
            Button none = new Button(ctx);
            none.setAllCaps(false);
            none.setText("✕");
            none.setOnClickListener(v -> { c[idx] = null; paint.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int) (48 * d), (int) (44 * d));
            lp.setMarginStart((int) (6 * d));
            row.addView(none, lp);
        }
        return row;
    }
}
