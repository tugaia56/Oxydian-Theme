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

    private static final int COUNT = 40;

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Testo/icona sopra l'accento: scuro se l'accento e' chiaro, chiaro se e' scuro. */
    private static int onColor(int c) {
        double lum = (0.2126 * ((c >> 16) & 255) + 0.7152 * ((c >> 8) & 255) + 0.0722 * (c & 255)) / 255.0;
        return lum > 0.5 ? mix(c, 0xFF000000, 0.85f) : mix(c, 0xFFFFFFFF, 0.9f);
    }

    /**
     * I colori scelti si scrivono sopra quelli di Material You (overlay creati al volo, con la
     * priorita' piu' alta): riquadri, cursori, pulsanti e superfici di Impostazioni/pannello.
     * accent null = li toglie.
     */
    static void applyFixedColors(Integer accent, Integer bg) {
        applyFixedColors(accent, bg, false);
    }

    /**
     * @param rolesOnly true su OxygenOS: solo i colori "di ruolo" della palette dinamica (pulsanti, selettori,
     *                  contenitori) che app come Calendar e Traduttore leggono direttamente dal sistema e che un
     *                  tema per singola app non puo' cambiare. Testo e icone sopra l'accento: bianchi.
     */
    static void applyFixedColors(Integer accent, Integer bg, boolean rolesOnly) {
        // Il sistema rifa' i suoi colori qualche secondo dopo il cambio di impostazione e si rimette davanti: si aspetta
        if (!rolesOnly) android.os.SystemClock.sleep(6000);
        java.util.List<String> cmds = new java.util.ArrayList<>();
        java.util.LinkedHashMap<String, Integer> m = new java.util.LinkedHashMap<>();
        if (accent != null) {
            int a = 0xFF000000 | accent, on = onColor(a), w = 0xFFFFFFFF;
            m.put("system_primary_dark", a);
            m.put("system_on_primary_dark", rolesOnly ? w : on);
            m.put("system_primary_container_dark", a);
            m.put("system_on_primary_container_dark", w);
            // un po' piu' scuro dell'accento: nelle app che usano il contenitore secondario come sfondo della voce selezionata
            // e il colore primario per l'icona (es. KernelSU), a tinta uguale l'icona sparirebbe
            m.put("system_secondary_container_dark", mix(a, 0xFF000000, 0.35f));
            m.put("system_on_secondary_container_dark", w);
            if (!rolesOnly) {
                m.put("system_surface_tint_dark", a);
                m.put("system_control_activated_dark", a);
                m.put("system_shade_active_dark", a);
                m.put("system_on_shade_active_dark", on);
            }
        }
        if (bg != null) {
            int b = 0xFF000000 | bg, w = 0xFFFFFFFF;
            m.put("system_background_dark", b);
            m.put("system_surface_dark", b);
            m.put("system_surface_dim_dark", b);
            m.put("system_surface_container_lowest_dark", mix(b, 0xFF000000, 0.4f));
            m.put("system_surface_container_low_dark", mix(b, w, 0.03f));
            m.put("system_surface_container_dark", mix(b, w, 0.06f));
            m.put("system_surface_container_high_dark", mix(b, w, 0.09f));
            m.put("system_surface_container_highest_dark", mix(b, w, 0.12f));
            m.put("system_surface_bright_dark", mix(b, w, 0.15f));
            m.put("system_surface_variant_dark", mix(b, w, 0.12f));
            m.put("system_shade_inactive_dark", mix(b, w, 0.10f));
            m.put("system_surface_effect_1_dark", mix(b, w, 0.10f));
        }
        int i = 0;
        for (java.util.Map.Entry<String, Integer> e : m.entrySet()) {
            long v = e.getValue() & 0xFFFFFFFFL;
            cmds.add("cmd overlay fabricate --target android --name oxyfix" + i + " android:color/" + e.getKey() + " 0x1c " + v);
            // l'identificativo dell'overlay creato dipende da chi lancia il comando (da "adb shell" e' com.android.shell,
            // all'avvio dal modulo puo' essere diverso): si legge dall'elenco invece di darlo per scontato
            cmds.add("P=$(cmd overlay list 2>/dev/null | grep -o \"[A-Za-z0-9_.]*:oxyfix" + i + "$\" | head -1); "
                    + "[ -n \"$P\" ] || P=com.android.shell:oxyfix" + i + "; echo \"fixedcolors oxyfix" + i + " -> $P\"");
            // disable+enable prima di set-priority: senza, a volte resta dietro ai colori del sistema
            cmds.add("cmd overlay disable --user 0 $P");
            cmds.add("cmd overlay enable --user 0 $P");
            cmds.add("cmd overlay set-priority $P highest");
            i++;
        }
        for (; i < COUNT; i++) cmds.add("cmd overlay disable --user 0 com.android.shell:oxyfix" + i);
        Shell.cmd(cmds.toArray(new String[0])).exec();
        // dopo un riavvio gli overlay creati al volo spariscono: il modulo rilancia questo elenco ad ogni avvio
        String file = it.tugaia56.oxydiantheme.utils.ModuleConstants.MODULE_DIR + "/fixedcolors.sh";
        if (m.isEmpty()) {
            Shell.cmd("rm -f " + file).exec();
        } else {
            StringBuilder sb = new StringBuilder("printf '%s\n'");
            for (String c : cmds) sb.append(" '").append(c).append("'");
            sb.append(" > ").append(file);
            Shell.cmd(sb.toString(), "chmod 755 " + file).exec();
            // subito dopo "Installa" il sistema rifa' i suoi colori e si rimette davanti a questi: si ripassa un
            // paio di volte, in background, dopo qualche secondo
            Shell.cmd("nohup sh -c 'sleep 8; sh " + file + "; sleep 20; sh " + file + "' >/dev/null 2>&1 &").exec();
        }
    }

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
            if (!isAosp()) {
                // OxygenOS: niente impostazioni di sistema, solo i colori di ruolo dell'accento scelto
                applyFixedColors(enabled ? SystemColorsDialog.effAccent(ctx) : null, null, true);
                return;
            }
            String current = String.join("", Shell.cmd("settings get secure " + KEY).exec().getOut()).trim();
            if (current.isEmpty() || current.equals("null")) current = "{}";
            // copia dell'impostazione originale, una volta sola, per poterla rimettere
            if (ThemePrefs.getAospBackup().isEmpty() && !current.contains(PALETTE)) ThemePrefs.setAospBackup(current);
            JSONObject o = new JSONObject(current);
            Integer accent = enabled ? SystemColorsDialog.effAccent(ctx) : null;
            Integer bg = enabled ? SystemColorsDialog.effBg(ctx) : null;
            if (accent != null || bg != null) {
                // Lo sfondo grande del pannello lo calcola il sistema dal colore di partenza: si parte dallo sfondo scelto
                String seed = String.format("%06X", (bg != null ? bg : accent) & 0xFFFFFF);
                o.put(SOURCE, "preset");
                o.put(PALETTE, seed);
                o.put(ACCENT, accent != null ? String.format("%06X", accent & 0xFFFFFF) : seed);
                // Tonal Spot riproduce lo sfondo scelto quasi identico; Vibrant lo renderebbe piu' vivace
                o.put(STYLE, bg != null || ThemePrefs.getAccSat() < 90 ? "TONAL_SPOT" : "VIBRANT");
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
            applyFixedColors(accent, bg);
        } catch (Throwable ignored) {}
    }
}
