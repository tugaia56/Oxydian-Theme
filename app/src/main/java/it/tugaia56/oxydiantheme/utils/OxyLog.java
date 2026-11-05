package it.tugaia56.oxydiantheme.utils;

import android.util.Log;

import com.topjohnwu.superuser.Shell;

/**
 * Diario con l'ora (ms) dei passi lunghi del Theme (ricostruzioni, colori di sistema, riavvii dell'interfaccia): va in logcat (tag
 * OxyTheme) e in /data/local/tmp/oxydian_theme_steps.log, per capire cosa fa il Theme quando l'interfaccia si blocca.
 */
public final class OxyLog {
    private static final String FILE = "/data/local/tmp/oxydian_theme_steps.log";

    private OxyLog() {}

    public static void log(String msg) {
        String line = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.ROOT).format(new java.util.Date())
                + " [" + Thread.currentThread().getName() + "] " + msg;
        Log.i("OxyTheme", line);
        try {
            Shell.cmd("echo '" + line.replace("'", "") + "' >> " + FILE).submit();
        } catch (Throwable ignored) {}
    }
}
