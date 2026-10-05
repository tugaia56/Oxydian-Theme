package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

/**
 * Pubblica come proprieta' di sistema i colori scelti (accento e sfondo): le legge Oxydian (hook e
 * interfaccia) cosi' le sue parti seguono la stessa scelta. Valore null = predefinito.
 */
public final class ThemeProps {
    private ThemeProps() {}

    public static void publish(Integer accent, Integer bg) {
        StringBuilder sb = new StringBuilder();
        put(sb, "persist.oxytheme.accent", accent == null ? "" : String.format("%08x", accent));
        put(sb, "persist.oxytheme.bg", bg == null ? "" : String.format("%08x", bg));
        // stesse proprieta' usate dai "preload fallback" di Oxydian nei processi senza prefs
        put(sb, "persist.obsidian.dst.a1_on", accent == null ? "0" : "1");
        put(sb, "persist.obsidian.dst.a1", accent == null ? "" : String.valueOf(accent));
        put(sb, "persist.obsidian.dst.bg_on", bg == null ? "0" : "1");
        put(sb, "persist.obsidian.dst.bg", bg == null ? "" : String.valueOf(bg));
        Shell.cmd(sb.toString()).submit();
    }

    private static void put(StringBuilder sb, String key, String value) {
        sb.append("resetprop ").append(key).append(" \"").append(value).append("\"; ");
    }
}
