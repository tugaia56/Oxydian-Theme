package it.tugaia56.oxydiantheme.ui;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

import it.tugaia56.oxydiantheme.utils.SystemVariant;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Su Android puro (15/16) il sistema calcola da solo la tavolozza dei colori (Material You) con
 * overlay che vincono sui nostri. Quindi l'accento si passa al sistema stesso come "colore di
 * partenza" (impostazione theme_customization_overlay_packages): il pannello rapido e il resto di
 * SystemUI lo seguono. Su OxygenOS non si usa.
 */
public final class AospTheme {
    private static final String KEY = "theme_customization_overlay_packages";
    private static final String SOURCE = "android.theme.customization.color_source";
    private static final String PALETTE = "android.theme.customization.system_palette";
    private static final String ACCENT = "android.theme.customization.accent_color";
    private static final String STYLE = "android.theme.customization.theme_style";

    private AospTheme() {}

    /** True se la variante in uso e' Android puro (non OxygenOS). */
    public static boolean isAosp() {
        String v = SystemVariant.effective();
        return SystemVariant.A16.equals(v) || SystemVariant.A15.equals(v);
    }

    /**
     * @param enabled true = usa l'accento scelto; false = torna ai colori dello sfondo del telefono
     * Chiamare fuori dal thread principale.
     */
    public static void apply(Context ctx, boolean enabled) {
        try {
            if (!isAosp()) return;
            String current = String.join("", Shell.cmd("settings get secure " + KEY).exec().getOut()).trim();
            if (current.isEmpty() || current.equals("null")) current = "{}";
            // copia dell'impostazione originale, una volta sola, per poterla rimettere
            if (ThemePrefs.getAospBackup().isEmpty() && !current.contains(PALETTE)) ThemePrefs.setAospBackup(current);
            JSONObject o = new JSONObject(current);
            Integer accent = enabled ? SystemColorsDialog.effAccent(ctx) : null;
            if (accent != null) {
                String hex = String.format("%06X", accent & 0xFFFFFF);
                o.put(SOURCE, "preset");
                o.put(PALETTE, hex);
                o.put(ACCENT, hex);
                // Tonal Spot ammorbidisce i colori; Vibrant resta piu' fedele a quello scelto
                o.put(STYLE, ThemePrefs.getAccSat() < 90 ? "TONAL_SPOT" : "VIBRANT");
            } else {
                o.remove(PALETTE);
                o.remove(ACCENT);
                o.put(SOURCE, "home_wallpaper");
                String old = "TONAL_SPOT";
                try { old = new JSONObject(ThemePrefs.getAospBackup().isEmpty() ? "{}" : ThemePrefs.getAospBackup()).optString(STYLE, "TONAL_SPOT"); } catch (Throwable ignored) {}
                o.put(STYLE, old);
            }
            o.put("_applied_timestamp", System.currentTimeMillis());
            Shell.cmd("settings put secure " + KEY + " '" + o.toString().replace("'", "") + "'").exec();
        } catch (Throwable ignored) {}
    }
}
