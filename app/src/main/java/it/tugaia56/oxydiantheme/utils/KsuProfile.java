package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

/**
 * KernelSU (Next): le app senza root hanno di serie i moduli "smontati" (Superuser > app > "Smonta moduli"), quindi non vedono
 * i file degli overlay del Theme (/product/overlay montato dal modulo): il sistema li ha attivi ma l'app non riesce ad aprirli
 * ("Failed to load asset path" in logcat) e la traduzione/i colori non compaiono. Qui si spegne lo smontaggio per le app che
 * hanno un overlay del Theme. Con Magisk o senza ksud non fa niente.
 */
public final class KsuProfile {
    private static final String KSUD = "/data/adb/ksu/bin/ksud";

    private KsuProfile() {}

    /** Vero se ksud c'e' (KernelSU / KernelSU Next). */
    public static boolean available() {
        try { return Shell.cmd("[ -x " + KSUD + " ]").exec().isSuccess(); } catch (Throwable t) { return false; }
    }

    /**
     * Spegne "Smonta moduli" per l'app, se serve. Non tocca le app con il root concesso (non vengono smontate) e quelle
     * gia' a posto. Torna true se ha cambiato il profilo (l'app deve ripartire per vedere gli overlay).
     */
    public static boolean allowOverlays(String pkg) {
        try {
            if (pkg == null || pkg.isEmpty() || "android".equals(pkg)) return false;
            if (!Shell.cmd("[ -x " + KSUD + " ]").exec().isSuccess()) return false;
            String json = String.join("", Shell.cmd(KSUD + " profile get " + pkg + " 2>/dev/null").exec().getOut());
            if (json.isEmpty()) return false;
            JSONObject o = new JSONObject(json);
            if (o.optBoolean("allow_su", false)) return false;
            JSONObject nr = o.optJSONObject("non_root");
            if (nr != null && !nr.optBoolean("use_default", true) && !nr.optBoolean("umount_modules", true)) return false;
            int uid = o.optInt("uid", -1);
            if (uid < 0) return false;
            String set = "{\"key\":\"" + pkg + "\",\"uid\":" + uid
                    + ",\"allow_su\":false,\"non_root\":{\"use_default\":false,\"umount_modules\":false}}";
            boolean ok = Shell.cmd("echo '" + set + "' | " + KSUD + " profile set -").exec().isSuccess();
            OxyLog.log("KsuProfile: " + pkg + (ok ? " smontaggio moduli spento" : " impostazione non riuscita"));
            return ok;
        } catch (Throwable t) {
            return false;
        }
    }
}
