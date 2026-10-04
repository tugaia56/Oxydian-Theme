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
}
