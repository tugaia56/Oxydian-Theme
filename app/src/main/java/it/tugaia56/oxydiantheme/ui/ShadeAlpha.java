package it.tugaia56.oxydiantheme.ui;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.ThemeProps;

/** Sfondo del pannello rapido e delle notifiche su Android puro: pieno, trasparente o molto trasparente (lo fa il modulo Oxydian). */
final class ShadeAlpha {
    private static final int[] LEVELS = {100, 65, 40};

    private ShadeAlpha() {}

    static String label(Context ctx, int percent) {
        if (ThemePrefs.getShadeTint()) return ctx.getString(R.string.shade_accent_label, percent);
        if (percent <= 40) return ctx.getString(R.string.shade_strong);
        if (percent < 100) return ctx.getString(R.string.shade_light);
        return ctx.getString(R.string.shade_solid);
    }

    /** Ripubblica la scelta salvata (all'avvio dell'app): la proprieta' sopravvive al riavvio, ma cosi' resta sempre coerente. */
    static void republish() {
        ThemeProps.publishShadeTint(ThemePrefs.getShadeTint());
        ThemeProps.publishShadeAlpha(ThemePrefs.getShadeAlpha());
    }

    /** Sfondo con l'accento: cursore dell'opacita' (5%..90%), come "Opacita' tocco". */
    private static void accentDialog(Activity act, Runnable onDone) {
        float d = act.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        android.widget.LinearLayout box = new android.widget.LinearLayout(act);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, 0);
        android.widget.TextView hint = new android.widget.TextView(act);
        hint.setText(R.string.shade_accent_hint);
        hint.setTextColor(act.getColor(R.color.text_dim));
        hint.setTextSize(13);
        box.addView(hint);
        final int[] value = {ThemePrefs.getShadeTint() ? ThemePrefs.getShadeAlpha() : 50};
        android.widget.TextView label = new android.widget.TextView(act);
        label.setTextColor(ThemePrefs.accentColor());
        label.setTextSize(18);
        label.setPadding(0, pad, 0, pad / 2);
        label.setText(value[0] + "%");
        box.addView(label);
        android.widget.SeekBar bar = new android.widget.SeekBar(act);
        bar.setMax(85);
        bar.setProgress(Math.max(0, Math.min(85, value[0] - 5)));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean user) {
                value[0] = p + 5;
                label.setText(value[0] + "%");
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });
        box.addView(bar);
        Dialogs.show(act, new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_shade)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    final int p = value[0];
                    ThemePrefs.setShadeTint(true);
                    ThemePrefs.setShadeAlpha(p);
                    if (onDone != null) onDone.run();
                    new Thread(() -> {
                        ThemeProps.publishShadeTint(true);
                        ThemeProps.publishShadeAlpha(p);
                        if (!AospTheme.hasOxydianModule())
                            act.runOnUiThread(() -> Toast.makeText(act, R.string.shade_need_module, Toast.LENGTH_LONG).show());
                    }).start();
                }));
    }

    static void show(Activity act, Runnable onDone) {
        int cur = ThemePrefs.getShadeAlpha();
        int checked = ThemePrefs.getShadeTint() ? 3 : cur <= 40 ? 2 : cur < 100 ? 1 : 0;
        CharSequence[] names = {act.getString(R.string.shade_solid), act.getString(R.string.shade_light), act.getString(R.string.shade_strong), act.getString(R.string.shade_accent)};
        Dialogs.show(act, new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_shade)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    if (which == 3) { accentDialog(act, onDone); return; }
                    int p = LEVELS[which];
                    ThemePrefs.setShadeTint(false);
                    ThemePrefs.setShadeAlpha(p);
                    if (onDone != null) onDone.run();
                    new Thread(() -> {
                        ThemeProps.publishShadeTint(false);
                        ThemeProps.publishShadeAlpha(p);
                        if (p < 100 && !AospTheme.hasOxydianModule())
                            act.runOnUiThread(() -> Toast.makeText(act, R.string.shade_need_module, Toast.LENGTH_LONG).show());
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null));
    }
}
