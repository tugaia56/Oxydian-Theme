package it.tugaia56.oxydiantheme.utils;

import com.topjohnwu.superuser.Shell;

import java.io.IOException;

import it.tugaia56.oxydiantheme.ThemeApp;

/**
 * Crea (e tiene aggiornato) il modulo root "oxydian_theme": module.prop, script di avvio e la
 * cartella "store" con gli APK da montare. Funziona con KernelSU e Magisk; al primo avvio dopo
 * la creazione serve un riavvio perché il gestore dei moduli lo veda.
 */
public final class ModuleSetup {
    private ModuleSetup() {}

    /** @return true se il modulo e' appena stato creato (serve un riavvio per attivarlo) */
    public static boolean ensure() throws IOException {
        boolean existed = Shell.cmd("[ -f " + ModuleConstants.MODULE_DIR + "/module.prop ]").exec().isSuccess();
        FileUtil.copyAssets("module");
        String src = ModuleConstants.DATA_DIR + "/module";
        Shell.cmd(
                "mkdir -p " + ModuleConstants.STORE_DIR,
                "cp -f " + src + "/module.prop " + ModuleConstants.MODULE_DIR + "/module.prop",
                "cp -f " + src + "/post-fs-data.sh " + ModuleConstants.MODULE_DIR + "/post-fs-data.sh",
                "cp -f " + src + "/service.sh " + ModuleConstants.MODULE_DIR + "/service.sh",
                "chmod 755 " + ModuleConstants.MODULE_DIR + "/post-fs-data.sh " + ModuleConstants.MODULE_DIR + "/service.sh",
                "chown -R 0:0 " + ModuleConstants.MODULE_DIR
        ).exec();
        return !existed;
    }

    public static boolean hasRoot() {
        try {
            return Shell.getShell().isRoot();
        } catch (Throwable t) {
            return false;
        }
    }
}
