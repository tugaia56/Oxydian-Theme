package it.tugaia56.oxydiantheme.ui;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.ThemeProps;
import it.tugaia56.oxydiantheme.utils.UiRestart;

/**
 * Stile delle notifiche su Android puro (crDroid, ecc.): la lista e' quella di Oxydian (gli stessi id), la scelta viene
 * pubblicata come proprieta' di sistema e il disegno lo fa il modulo Oxydian (colori: accento e sfondo del Theme).
 */
final class NotifStyle {
    private NotifStyle() {}

    static String label(Context ctx, String id) {
        if (id == null || id.isEmpty()) return ctx.getString(R.string.notif_style_default);
        String[] ids = ctx.getResources().getStringArray(R.array.notif_style_ids);
        String[] names = ctx.getResources().getStringArray(R.array.notif_style_names);
        for (int i = 0; i < ids.length && i < names.length; i++) if (ids[i].equals(id)) return names[i];
        return id;
    }

    /** Ripubblica la scelta salvata (all'avvio dell'app). */
    static void republish() {
        ThemeProps.publishNotifStyle(ThemePrefs.getNotifStyle());
    }

    static void show(Activity act, Runnable onDone) {
        final String[] ids = act.getResources().getStringArray(R.array.notif_style_ids);
        String[] names = act.getResources().getStringArray(R.array.notif_style_names);
        final CharSequence[] items = new CharSequence[ids.length + 1];
        items[0] = act.getString(R.string.notif_style_default);
        for (int i = 0; i < ids.length && i < names.length; i++) items[i + 1] = names[i];
        String cur = ThemePrefs.getNotifStyle();
        int checked = 0;
        for (int i = 0; i < ids.length; i++) if (ids[i].equals(cur)) checked = i + 1;
        Dialogs.show(act, new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_notifstyle)
                .setSingleChoiceItems(items, checked, (d, which) -> {
                    d.dismiss();
                    final String id = which == 0 ? "" : ids[which - 1];
                    ThemePrefs.setNotifStyle(id);
                    if (onDone != null) onDone.run();
                    Toast.makeText(act, R.string.restart_ui_toast, Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        ThemeProps.publishNotifStyle(id);
                        if (!id.isEmpty() && !AospTheme.hasOxydianModule())
                            act.runOnUiThread(() -> Toast.makeText(act, R.string.shade_need_module, Toast.LENGTH_LONG).show());
                        // le notifiche prendono lo stile quando vengono disegnate: si riavvia l'interfaccia (una volta)
                        UiRestart.run();
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null));
    }
}
