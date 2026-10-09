package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/** Opacita' dell'effetto tocco (l'onda che compare quando si preme qualcosa). */
final class RippleDialog {
    interface Listener { void onSaved(); }

    private RippleDialog() {}

    static String summary(Context ctx) {
        int p = ThemePrefs.getRippleAlpha();
        return p == 0 ? ctx.getString(R.string.options_default) : p + "%";
    }

    static void show(Context ctx, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, 0);

        TextView hint = new TextView(ctx);
        hint.setText(R.string.ripple_hint);
        hint.setTextColor(ctx.getColor(R.color.text_dim));
        hint.setTextSize(13);
        box.addView(hint);

        final int[] value = {ThemePrefs.getRippleAlpha() == 0 ? 50 : ThemePrefs.getRippleAlpha()};
        TextView label = new TextView(ctx);
        label.setTextColor(ThemePrefs.accentColor());
        label.setTextSize(18);
        label.setPadding(0, pad, 0, pad / 2);
        label.setText(value[0] + "%");
        box.addView(label);

        SeekBar bar = new SeekBar(ctx);
        bar.setMax(85);                 // 5% .. 90%
        bar.setProgress(value[0] - 5);
        bar.setProgressTintList(ColorStateList.valueOf(ThemePrefs.accentColor()));
        bar.setThumbTintList(ColorStateList.valueOf(ThemePrefs.accentColor()));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                value[0] = p + 5;
                label.setText(value[0] + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(bar);

        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.card_ripple)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.options_default, (dlg, w) -> {
                    ThemePrefs.setRippleAlpha(0);
                    listener.onSaved();
                })
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    ThemePrefs.setRippleAlpha(value[0]);
                    listener.onSaved();
                }));
    }
}
