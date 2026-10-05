package it.tugaia56.oxydiantheme.utils;

import android.content.Context;
import android.content.SharedPreferences;

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

    /** Colore scelto per le icone Wi-Fi e segnale (nome, "" = predefinito). */
    public static String getIconColor() { return sp().getString("icon_color_name", ""); }

    public static void setIconColor(String name) { sp().edit().putString("icon_color_name", name == null ? "" : name).commit(); }
}
