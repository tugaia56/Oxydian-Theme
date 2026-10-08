package it.tugaia56.oxydiantheme.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

import it.tugaia56.oxydiantheme.ThemeApp;

/** Impostazioni di Oxydian Theme (app attive, colore accento, firma delle build). */
public final class ThemePrefs {
    private static final String FILE = "theme_prefs";
    public static final int DEFAULT_ACCENT = 0xFF908DFF;

    private ThemePrefs() {}

    private static SharedPreferences sp() {
        return ThemeApp.getAppContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isAppEnabled(String pkg) { return sp().getBoolean("app_enabled_" + pkg, false); }

    public static void setAppEnabled(String pkg, boolean on) { sp().edit().putBoolean("app_enabled_" + pkg, on).commit(); }

    public static String getBuiltSignature(String pkg) { return sp().getString("app_built_" + pkg, ""); }

    public static void setBuiltSignature(String pkg, String sig) { sp().edit().putString("app_built_" + pkg, sig).commit(); }

    public static void clearBuiltSignature(String pkg) { sp().edit().remove("app_built_" + pkg).commit(); }

    /** Accento usato nei temi: scelto dall'utente (default viola Oxydian). */
    public static int accentColor() { return sp().getInt("accent", DEFAULT_ACCENT); }

    public static void setAccentColor(int color) { sp().edit().putInt("accent", color).commit(); }

    /** Stile scelto per un'icona (slot WIFI1 / SIG1); stringa vuota = nessuno. */
    public static String getStyle(String slot) { return sp().getString("style_" + slot, ""); }

    public static void setStyle(String slot, String style) { sp().edit().putString("style_" + slot, style == null ? "" : style).commit(); }

    public static String getStyleBuilt(String slot) { return sp().getString("style_built_" + slot, ""); }

    public static void setStyleBuilt(String slot, String sig) { sp().edit().putString("style_built_" + slot, sig).commit(); }

    public static boolean isDarkShadowEnabled(String pkg) { return sp().getBoolean("ds_enabled_" + pkg, false); }

    public static void setDarkShadowEnabled(String pkg, boolean on) { sp().edit().putBoolean("ds_enabled_" + pkg, on).commit(); }

    public static String getDarkShadowBuilt(String pkg) { return sp().getString("ds_built_" + pkg, ""); }

    public static void setDarkShadowBuilt(String pkg, String sig) { sp().edit().putString("ds_built_" + pkg, sig).commit(); }

    /** Scelta dell'utente in un gruppo di opzioni di un tema; stringa vuota = predefinito. */
    public static String getOption(String pkg, String group) { return sp().getString("opt_" + pkg + "_" + group, ""); }

    public static void setOption(String pkg, String group, String choice) { sp().edit().putString("opt_" + pkg + "_" + group, choice == null ? "" : choice).commit(); }

    /** Opzioni dei pack icone Impostazioni (colore/forma sfondo, colore icona). */
    public static int getIconOpt(String key, int def) { return sp().getInt("icopt_" + key, def); }

    public static void setIconOpt(String key, int value) { sp().edit().putInt("icopt_" + key, value).commit(); }

    /** Dimensione delle icone Wi-Fi e segnale in dp (15 = stock). */
    public static int getIconSize() { return sp().getInt("icon_size_dp", 15); }

    public static void setIconSize(int dp) { sp().edit().putInt("icon_size_dp", dp).commit(); }

    /** Colore scelto per l'icona Wi-Fi ("wifi") o del segnale mobile ("mobile"): nome, "" = predefinito. */
    public static String getIconColor(String kind) { return sp().getString("icon_color_" + kind, ""); }

    public static void setIconColor(String kind, String name) { sp().edit().putString("icon_color_" + kind, name == null ? "" : name).commit(); }

    /** Colore a scelta libera di un gruppo di opzioni (es. "pinbg", "pinnum"). */
    public static int getCustomColor(String key) { return sp().getInt("custom_" + key, 0xFFFFFFFF); }

    public static void setCustomColor(String key, int color) { sp().edit().putInt("custom_" + key, color).commit(); }

    // ── Backup e ripristino ──────────────────────────────────────────────────

    /** Tutte le scelte (overlay accesi, colori, opzioni) in un testo JSON. */
    public static String exportJson() {
        try {
            JSONObject all = new JSONObject();
            for (java.util.Map.Entry<String, ?> e : sp().getAll().entrySet()) {
                Object v = e.getValue();
                JSONObject o = new JSONObject();
                if (v instanceof Boolean) o.put("t", "b");
                else if (v instanceof Integer) o.put("t", "i");
                else if (v instanceof Long) o.put("t", "l");
                else if (v instanceof Float) o.put("t", "f");
                else o.put("t", "s");
                o.put("v", v);
                all.put(e.getKey(), o);
            }
            JSONObject root = new JSONObject();
            root.put("app", "oxydian-theme");
            root.put("version", 1);
            root.put("prefs", all);
            return root.toString(2);
        } catch (Exception ex) {
            return "";
        }
    }

    /** @return true se il file era un backup valido e le scelte sono state ripristinate */
    public static boolean importJson(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (!"oxydian-theme".equals(root.optString("app"))) return false;
            JSONObject all = root.getJSONObject("prefs");
            SharedPreferences.Editor ed = sp().edit();
            ed.clear();
            java.util.Iterator<String> it = all.keys();
            while (it.hasNext()) {
                String k = it.next();
                // le firme delle ultime build non valgono su un altro telefono/stato: si rifanno
                if (k.startsWith("ds_built_") || k.startsWith("app_built_") || k.startsWith("style_built_")) continue;
                JSONObject o = all.getJSONObject(k);
                switch (o.getString("t")) {
                    case "b": ed.putBoolean(k, o.getBoolean("v")); break;
                    case "i": ed.putInt(k, o.getInt("v")); break;
                    case "l": ed.putLong(k, o.getLong("v")); break;
                    case "f": ed.putFloat(k, (float) o.getDouble("v")); break;
                    default: ed.putString(k, o.getString("v")); break;
                }
            }
            ed.commit();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Wi-Fi e Segnale usano sempre lo stesso stile (interruttore nella pagina delle icone). */
    public static boolean isSameIcon() { return sp().getBoolean("same_icon", false); }

    public static void setSameIcon(boolean on) { sp().edit().putBoolean("same_icon", on).commit(); }

    /** Opacita' dell'effetto tocco (ripple) in percentuale; 0 = quella del preset. */
    public static int getRippleAlpha() { return sp().getInt("ripple_alpha", 0); }

    public static void setRippleAlpha(int pct) { sp().edit().putInt("ripple_alpha", pct).commit(); }

    /** Pagina che si apre all'avvio: 0 Overlay, 1 Colori, 2 Mods, 3 Impostazioni. */
    public static int getDefaultTab() { return sp().getInt("default_tab", 0); }

    public static void setDefaultTab(int tab) { sp().edit().putInt("default_tab", tab).commit(); }

    /** Colore icone navigazione cambiato: la pagina principale rifa l'overlay di SystemUI al ritorno. */
    public static boolean isNavPending() { return sp().getBoolean("nav_pending", false); }

    public static void setNavPending(boolean on) { sp().edit().putBoolean("nav_pending", on).commit(); }

    /** Ritocchi ai colori di sistema: saturazione accento/sfondo (%), luminosita' sfondo (punti %), nero puro. */
    public static int getAccSat() { return sp().getInt("tweak_acc_sat", 100); }
    public static void setAccSat(int v) { sp().edit().putInt("tweak_acc_sat", v).commit(); }
    public static int getBgSat() { return sp().getInt("tweak_bg_sat", 100); }
    public static void setBgSat(int v) { sp().edit().putInt("tweak_bg_sat", v).commit(); }
    public static int getBgLight() { return sp().getInt("tweak_bg_light", 0); }
    public static void setBgLight(int v) { sp().edit().putInt("tweak_bg_light", v).commit(); }
    public static boolean isBgPitch() { return sp().getBoolean("tweak_bg_pitch", false); }
    public static void setBgPitch(boolean on) { sp().edit().putBoolean("tweak_bg_pitch", on).commit(); }

    /** Integrazione con Tasker/MacroDroid (broadcast per cambiare i colori): spenta di default. */
    public static boolean isTaskerEnabled() { return sp().getBoolean("tasker_enabled", false); }
    public static void setTaskerEnabled(boolean on) { sp().edit().putBoolean("tasker_enabled", on).commit(); }

    /** Versione del sistema scelta per SystemUI ("" = automatica). */
    public static String getSystemVariant() { return sp().getString("system_variant", ""); }
    public static void setSystemVariant(String v) { sp().edit().putString("system_variant", v == null ? "" : v).commit(); }
}
