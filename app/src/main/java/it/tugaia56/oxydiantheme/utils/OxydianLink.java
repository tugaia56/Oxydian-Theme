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

    /**
     * Spegne lo sfondo DST di Oxydian (colori di sistema opachi con priorita' massima): coprirebbe uno sfondo con trasparenza
     * scelto qui. Lo spegne nelle preferenze di Oxydian, toglie i suoi overlay e le righe salvate per il riavvio. Va rifatto
     * dall'interno di Oxydian se lo si vuole riaccendere. Chiamare fuori dal thread principale.
     */
    public static void disableDstBackground() {
        inGlobalMounts("P=" + PREFS + "; X=" + POST_EXEC + "; "
                + "if grep -q 'name=\"DST_BACKGROUND_on\" value=\"true\"' $P 2>/dev/null; then "
                + "am force-stop it.tugaia56.oxydian; sleep 1; "   // prima si chiude Oxydian: potrebbe riscrivere le sue preferenze
                + "sed -i 's/name=\"DST_BACKGROUND_on\" value=\"true\"/name=\"DST_BACKGROUND_on\" value=\"false\"/' $P; "
                + "for n in $(cmd overlay list --user 0 | sed -n 's/^\\[.\\] com.android.shell:\\(ObsidianComponentBACKGROUND_[0-9]*\\)$/\\1/p'); do "
                + "cmd overlay disable --user 0 com.android.shell:$n; done; "
                + "[ -f $X ] && sed -i '/ObsidianComponentBACKGROUND_/d' $X; "
                + "fi; true");
    }

    /**
     * La shell root dell'app eredita la sua vista del disco, dove Android nasconde i dati delle altre app: le preferenze di
     * Oxydian "non esistevano" e queste modifiche non venivano mai fatte (es. il pulsante delle recenti restava col vecchio
     * accento). Si esegue nella vista di sistema (quella di init).
     */
    private static void inGlobalMounts(String script) {
        Shell.cmd("nsenter -t 1 -m sh -c '" + script.replace("'", "'\\''") + "'").exec();
    }
}
