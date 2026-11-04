package it.tugaia56.oxydiantheme.ui;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.util.Collections;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ModuleConstants;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil;
import it.tugaia56.oxydiantheme.utils.overlay.compiler.ThemeCompiler;

/**
 * Card di Impostazioni su Android puro (crDroid, ecc.): piene (di serie), trasparenti (colore delle card con trasparenza,
 * lo fa AospTheme) oppure con bordo (overlay DS_SSB che ridisegna le card con sfondo e accento del Theme).
 */
final class SettingsCards {
    static final String FULL = "", GLASS = "glass", BORDER = "border", GLASS_BORDER = "glassborder";
    static final String OVERLAY_NAME = "DS_SSTB";
    private static final String ASSET_DIR = "SSB", ASSET_DIR_GLASS = "SSBG";

    private SettingsCards() {}

    static String label(Context ctx, String mode) {
        if (GLASS.equals(mode)) return ctx.getString(R.string.scards_glass);
        if (BORDER.equals(mode)) return ctx.getString(R.string.scards_border);
        if (GLASS_BORDER.equals(mode)) return ctx.getString(R.string.scards_glassborder);
        return ctx.getString(R.string.scards_full);
    }

    /** Nome completo dell'overlay, da tenere nell'elenco di quelli da accendere ad ogni avvio quando e' in uso. */
    static String overlayPackage() {
        return ThemeCompiler.namedPackage(OVERLAY_NAME);
    }

    /** Stili con l'overlay del bordo (DS_SSTB). */
    static boolean usesBorder(String mode) { return BORDER.equals(mode) || GLASS_BORDER.equals(mode); }

    static void show(Activity act, Runnable onDone) {
        final String[] modes = {FULL, GLASS, BORDER, GLASS_BORDER};
        String cur = ThemePrefs.getSettingsCards();
        int checked = GLASS.equals(cur) ? 1 : BORDER.equals(cur) ? 2 : GLASS_BORDER.equals(cur) ? 3 : 0;
        CharSequence[] names = {act.getString(R.string.scards_full), act.getString(R.string.scards_glass), act.getString(R.string.scards_border), act.getString(R.string.scards_glassborder)};
        Dialogs.show(act, new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_scards)
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    ThemePrefs.setSettingsCards(modes[which]);
                    if (onDone != null) onDone.run();
                    final Context app = act.getApplicationContext();
                    Toast.makeText(act, R.string.working, Toast.LENGTH_SHORT).show();
                    new Thread(() -> {
                        boolean needsReboot = apply(app);
                        if (needsReboot) act.runOnUiThread(() -> Toast.makeText(act, R.string.scards_reboot, Toast.LENGTH_LONG).show());
                    }).start();
                })
                .setNegativeButton(android.R.string.cancel, null));
    }

    /**
     * Mette in pratica la scelta (fuori dal thread principale): costruisce/accende o spegne l'overlay del bordo e rifa' i
     * colori di sistema (le card trasparenti sono un colore). Vero se serve riavviare (un overlay nuovo si registra solo all'avvio).
     */
    static boolean apply(Context app) {
        String mode = ThemePrefs.getSettingsCards();
        String pkg = overlayPackage();
        String list = ModuleConstants.MODULE_DIR + "/enabled.list";
        boolean needsReboot = false;
        if (usesBorder(mode)) {
            try {
                ThemeCompiler.beginBatch();
                boolean registeredBefore = registered(pkg);
                boolean failed = ThemeCompiler.buildNamedInBatch("com.android.settings", GLASS_BORDER.equals(mode) ? ASSET_DIR_GLASS : ASSET_DIR, OVERLAY_NAME);
                if (!failed) {
                    Shell.cmd("grep -qx '" + pkg + "' " + list + " 2>/dev/null || echo '" + pkg + "' >> " + list).exec();
                    if (registeredBefore) {
                        OverlayUtil.enableOverlays(pkg);
                        ThemeCompiler.endBatch(Collections.singletonList(pkg));
                    } else {
                        needsReboot = true;
                    }
                }
            } catch (Throwable t) {
                android.util.Log.e("OxyTheme", "settings cards border failed", t);
            }
        } else if (registered(pkg)) {
            OverlayUtil.disableOverlays(pkg);
            Shell.cmd("sed -i '/^" + pkg + "$/d' " + list + " 2>/dev/null").exec();
        }
        // le card trasparenti sono un colore di sistema: si rifanno i colori
        AospTheme.apply(app, ThemePrefs.isDarkShadowEnabled(SystemColorsDialog.PKG));
        return needsReboot;
    }

    private static boolean registered(String pkg) {
        return !Shell.cmd("cmd overlay list --user 0 2>/dev/null | grep -F '" + pkg + "'").exec().getOut().isEmpty();
    }
}
