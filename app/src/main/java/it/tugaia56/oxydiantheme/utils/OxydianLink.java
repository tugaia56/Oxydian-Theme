package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

/**
 * Collegamento con l'app Oxydian: il suo "Colore Pulsante Recenti" con scelta Accento e' un overlay creato al volo con il
 * colore scritto dentro, quindi non segue da solo i cambi di accento. Qui lo si rifa' col colore nuovo (overlay e copia che
 * il modulo di Oxydian rimette ad ogni avvio). Chiamare fuori dal thread principale.
 */
public final class OxydianLink {
    private static final String PREFS = "/data/user_de/0/it.tugaia56.oxydian/shared_prefs/it.tugaia56.oxydian_preferences.xml";
    private static final String POST_EXEC = "/data/adb/modules/Obsidian/post-exec.sh";

    private OxydianLink() {}

    public static void refreshRecentsButton(int accent) {
        String hex = String.format("0x%08X", 0xFFFFFFFFL & (accent | 0xFF000000));
        Shell.cmd("P=" + PREFS + "; X=" + POST_EXEC + "; "
                + "grep -q 'LAUNCHER_RECENTS_BTN_COLOR_on\" value=\"true\"' $P && "
                + "grep -q 'LAUNCHER_RECENTS_BTN_COLOR_use_accent\" value=\"true\"' $P && "
                + "! grep -q 'DST_ACCENT1_on\" value=\"true\"' $P && [ -f $X ] && { "
                + "sed -i -E '/LAUNCHER_RECENTS/ s/ 0x[0-9A-Fa-f]{8}$/ " + hex + "/' $X; "
                + "grep 'cmd overlay fabricate.*LAUNCHER_RECENTS' $X | sh; }; true").exec();
    }
}
