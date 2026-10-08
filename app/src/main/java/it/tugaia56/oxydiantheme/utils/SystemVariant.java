package it.tugaia56.oxydiantheme.utils;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import it.tugaia56.oxydiantheme.R;

/**
 * Versione del sistema per cui compilare l'overlay di SystemUI (le risorse cambiano da una versione
 * all'altra, e su Android puro mancano quelle OnePlus). Scelta manuale oppure automatica.
 */
public final class SystemVariant {
    /** Valori salvati: "" = automatico, oppure uno di questi. */
    public static final String OOS16 = "oos16", OOS15 = "oos15", A16 = "a16", A15 = "a15";

    private static String sAuto;

    private SystemVariant() {}

    /** Nome da mostrare per un valore salvato. */
    public static String label(Context ctx, String value) {
        if (value == null || value.isEmpty()) return ctx.getString(R.string.variant_auto);
        switch (value) {
            case OOS16: return "OxygenOS 16";
            case OOS15: return "OxygenOS 15";
            case A16: return "Android 16";
            case A15: return "Android 15";
            default: return value;
        }
    }

    /** Variante riconosciuta dal telefono (chiamare fuori dal thread principale). */
    public static synchronized String detect() {
        if (sAuto != null) return sAuto;
        int sdk = android.os.Build.VERSION.SDK_INT;
        boolean oplus = false;
        try {
            String v = String.join("", Shell.cmd("getprop ro.build.version.oplusrom").exec().getOut()).trim();
            oplus = !v.isEmpty();
        } catch (Throwable ignored) {}
        if (!oplus) {
            // ROM di terze parti su telefoni OnePlus (es. crDroid): il produttore e' lo stesso, ma mancano le app di sistema OnePlus
            for (String pkg : new String[]{"com.oplus.wirelesssettings", "com.oneplus.calculator", "com.oplus.camera", "com.oplus.games"}) {
                try {
                    if (Shell.cmd("pm path " + pkg).exec().isSuccess()) { oplus = true; break; }
                } catch (Throwable ignored) {}
            }
        }
        if (sdk >= 36) sAuto = oplus ? OOS16 : A16;
        else sAuto = oplus ? OOS15 : A15;
        return sAuto;
    }

    /** Variante da usare davvero: la scelta salvata, oppure quella riconosciuta (fuori dal thread principale). */
    public static String effective() {
        String saved = ThemePrefs.getSystemVariant();
        return saved.isEmpty() ? detect() : saved;
    }

    /** Suffisso della cartella negli asset: vuoto per OxygenOS 16 (la base). */
    public static String dirSuffix(String variant) {
        switch (variant) {
            case OOS15: return "_OOS15";
            case A16: return "_A16";
            case A15: return "_A15";
            default: return "";
        }
    }
}
