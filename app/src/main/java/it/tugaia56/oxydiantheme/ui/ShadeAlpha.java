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
        if (percent <= 40) return ctx.getString(R.string.shade_strong);
        if (percent < 100) return ctx.getString(R.string.shade_light);
        return ctx.getString(R.string.shade_solid);
    }

    /** Ripubblica la scelta salvata (all'avvio dell'app): la proprieta' sopravvive al riavvio, ma cosi' resta sempre coerente. */
    static void republish() {
        ThemeProps.publishShadeAlpha(ThemePrefs.getShadeAlpha());
    }

    static void show(Activity act, Runnable onDone) {
        int cur = ThemePrefs.getShadeAlpha();
        int checked = cur <= 40 ? 2 : cur < 100 ? 1 : 0;
        CharSequence[] names = {act.getString(R.string.shade_solid), act.getString(R.string.shade_light), act.getString(R.string.shade_strong)};
        Dialogs.show(act, new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_shade)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    int p = LEVELS[which];
                    ThemePrefs.setShadeAlpha(p);
                    if (onDone != null) onDone.run();
                    new Thread(() -> {
                        ThemeProps.publishShadeAlpha(p);
                        if (p < 100 && !AospTheme.hasOxydianModule())
                            act.runOnUiThread(() -> Toast.makeText(act, R.string.shade_need_module, Toast.LENGTH_LONG).show());
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null));
    }
}
