package it.tugaia56.oxydiantheme.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import it.tugaia56.oxydiantheme.ThemeApp;

public final class AppUtils {
    private AppUtils() {}

    /** Percorsi APK base + split del pacchetto: servono ad aapt2 come riferimento risorse (-I). */
    public static String[] getSplitLocations(String packageName) {
        try {
            ApplicationInfo info = ThemeApp.getAppContext().getPackageManager().getApplicationInfo(packageName, 0);
            String[] splits = info.splitSourceDirs;
            if (splits == null) return new String[]{info.sourceDir};
            String[] all = new String[splits.length + 1];
            all[0] = info.sourceDir;
            System.arraycopy(splits, 0, all, 1, splits.length);
            return all;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return new String[0];
    }
}
