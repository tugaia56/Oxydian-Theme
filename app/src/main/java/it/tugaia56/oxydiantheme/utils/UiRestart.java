package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

/** Riavvio dell'interfaccia: barra di stato/pannelli (SystemUI) e anche il launcher, che tiene in memoria i colori vecchi. */
public final class UiRestart {
    private UiRestart() {}

    /** Chiamare fuori dal thread principale. */
    public static void run() {
        OxyLog.log("UiRestart: killall launcher + systemui (+ launcher3)");
        Shell.cmd("killall com.android.launcher", "killall com.android.systemui").exec();
        // crDroid / Android puro: i Recenti, il tasto indietro (pillola "Recents") e i gesti li gestisce il Quickstep di
        // com.android.launcher3, che dopo il riavvio di SystemUI resta scollegato finche' non riparte anche lui.
        // Si riavvia dopo SystemUI (con una breve attesa); se il processo non esiste il comando non fa nulla.
        Shell.cmd("sleep 3; killall com.android.launcher3 2>/dev/null").exec();
        OxyLog.log("UiRestart: done");
    }
}
