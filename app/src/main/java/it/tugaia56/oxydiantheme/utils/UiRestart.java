package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

/** Riavvio dell'interfaccia: barra di stato/pannelli (SystemUI) e anche il launcher, che tiene in memoria i colori vecchi. */
public final class UiRestart {
    private UiRestart() {}

    /** Chiamare fuori dal thread principale. */
    public static void run() {
        Shell.cmd("killall com.android.launcher", "killall com.android.systemui").exec();
    }
}
