package it.tugaia56.oxydiantheme.utils.overlay.compiler;

import static it.tugaia56.oxydiantheme.utils.FileUtil.copyAssets;
import static it.tugaia56.oxydiantheme.utils.helper.BinaryInstaller.symLinkBinaries;
import static it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil.disableOverlays;
import static it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil.enableOverlays;

import com.topjohnwu.superuser.Shell;

import java.io.IOException;
import java.util.List;

import it.tugaia56.oxydiantheme.utils.ModuleConstants;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Compila gli overlay di tema e li mette nello "store" del modulo oxydian_theme.
 * Per compilarne molti insieme: beginBatch() una volta, buildInBatch(...) per ognuno,
 * endBatch(...) una volta (le operazioni lente si fanno una volta sola, non per ogni app).
 */
public final class ThemeCompiler {
    public static final String PREFIX = "OxydianThemeComponent";
    private static final String ASSET_DIR = "APP";

    private ThemeCompiler() {}

    /** Nome dell'overlay di un'app (il pacchetto senza punti; "apk" romperebbe il regex del compilatore). */
    public static String overlayName(String targetPackage) {
        return "App_" + targetPackage.replace('.', '_').replace("apk", "ap_k");
    }

    public static String overlayPackage(String targetPackage) {
        return PREFIX + overlayName(targetPackage) + ".overlay";
    }

    /** Nome completo di un overlay con nome libero (es. WIFI1). */
    public static String namedPackage(String name) {
        return PREFIX + name + ".overlay";
    }

    /** Elenco dei temi (app e Dark Shadow) da accendere ad ogni avvio: il modulo accende solo questi. */
    public static void writeEnabledList(List<String> overlayNames) {
        StringBuilder sb = new StringBuilder("printf '%s\n'");
        for (String n : overlayNames) sb.append(" '").append(n).append("'");
        sb.append(" > ").append(ModuleConstants.MODULE_DIR).append("/enabled.list");
        if (overlayNames.isEmpty()) sb = new StringBuilder(": > " + ModuleConstants.MODULE_DIR + "/enabled.list");
        Shell.cmd(sb.toString(), "chmod 644 " + ModuleConstants.MODULE_DIR + "/enabled.list").exec();
    }

    /** Toglie dallo store gli overlay con nome libero indicati. */
    public static void removeNamedApks(List<String> names) {
        if (names == null || names.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String n : names)
            sb.append("rm -f ").append(ModuleConstants.STORE_DIR).append('/').append(PREFIX).append(n).append(".apk 2>/dev/null; ");
        Shell.cmd(sb.toString().trim()).exec();
    }

    // ── Giro completo ────────────────────────────────────────────────────────

    public static void beginBatch() {
        symLinkBinaries();
        Shell.cmd("rm -rf " + ModuleConstants.TEMP_OVERLAY_DIR,
                "rm -rf " + ModuleConstants.DATA_DIR + "/CompileOnDemand",
                "mkdir -p " + ModuleConstants.TEMP_CACHE_DIR + " " + ModuleConstants.UNSIGNED_UNALIGNED_DIR
                        + " " + ModuleConstants.UNSIGNED_DIR + " " + ModuleConstants.SIGNED_DIR
                        + " " + ModuleConstants.STORE_DIR,
                // le cartelle le crea il root: l'app (che firma gli APK) deve poterci scrivere
                "chmod -R 777 " + ModuleConstants.TEMP_DIR).exec();
    }

    /** @return true se la compilazione e' fallita */
    public static boolean buildInBatch(String targetPackage) throws IOException {
        return buildNamedInBatch(targetPackage, ASSET_DIR, overlayName(targetPackage));
    }

    /** Sorgente in assets/CompileOnDemand/&lt;pacchetto&gt;/&lt;assetDir&gt;/res, nome overlay a scelta. */
    public static boolean buildNamedInBatch(String targetPackage, String assetDir, String name) throws IOException {
        return buildNamedInBatch(targetPackage, assetDir, name, null);
    }

    /** Come sopra, con in piu' le scelte dell'utente: cartelle "res" sopra la base (i file con lo
     *  stesso nome risorsa, anche con estensione diversa, vengono sostituiti). */
    public static boolean buildNamedInBatch(String targetPackage, String assetDir, String name,
                                            List<String> optionAssetPaths) throws IOException {
        String cacheRoot = ModuleConstants.TEMP_CACHE_DIR + "/" + targetPackage;
        String source = cacheRoot + "/" + name;

        copyAssets("CompileOnDemand/" + targetPackage + "/" + assetDir);
        // @*android:color/accent_material_dark fuori dai processi hookati resta il teal di sistema:
        // si scrive l'accento reale scelto. Il prefisso "@*" puo' comparire doppio nei sorgenti.
        String accent = String.format("#%08X", ThemePrefs.accentColor());
        String moved = ModuleConstants.DATA_DIR + "/CompileOnDemand/" + targetPackage + "/" + assetDir;
        Shell.cmd("mkdir -p \"" + cacheRoot + "\"",
                "mv -f \"" + moved + "\" \"" + source + "\"",
                "find \"" + source + "/res\" -type f -name '*.xml'"
                        + " -exec sed -i -E 's|(@\\*)+android:color/accent_material_dark|" + accent + "|g' {} +").exec();

        if (optionAssetPaths != null) {
            for (String opt : optionAssetPaths) {
                copyAssets(opt);
                String o = ModuleConstants.DATA_DIR + "/" + opt + "/res";
                Shell.cmd("cd \"" + o + "\" && find . -type f | while read -r f; do"
                        + " d=\"" + source + "/res/$(dirname \"$f\")\"; b=$(basename \"$f\"); n=${b%.*};"
                        + " rm -f \"$d/$n\".*; done",
                        "mkdir -p \"" + source + "/res\" && cp -rf \"" + o + "/.\" \"" + source + "/res/\"").exec();
            }
        }

        if (OverlayCompiler.createManifest(name, targetPackage, source)) return true;
        if (OverlayCompiler.runAapt(source, targetPackage)) return true;
        if (OverlayCompiler.zipAlign(ModuleConstants.UNSIGNED_UNALIGNED_DIR + "/" + name + "-unsigned-unaligned.apk")) return true;
        if (OverlayCompiler.apkSigner(ModuleConstants.UNSIGNED_DIR + "/" + name + "-unsigned.apk")) return true;

        String apkName = PREFIX + name + ".apk";
        String signed = ModuleConstants.SIGNED_DIR + "/" + apkName;
        String inStore = ModuleConstants.STORE_DIR + "/" + apkName;
        String inSystem = ModuleConstants.LIVE_UPPER_DIR + "/" + apkName;
        // Lo store e' quello che il modulo monta ad ogni avvio; la copia in /product/overlay serve
        // all'effetto immediato (se l'unione e' gia' montata) e puo' anche fallire senza danni.
        Shell.cmd("cp -f " + signed + " " + inStore,
                "chmod 644 " + inStore,
                "[ -d " + ModuleConstants.LIVE_UPPER_DIR + " ] && cp -f " + signed + " " + inSystem + " 2>/dev/null",
                "chmod 644 " + inSystem + " 2>/dev/null").exec();
        return false;
    }

    /** @param refresh pacchetti (overlay) gia' attivi che vanno spenti e riaccesi per ricaricare l'APK */
    public static void endBatch(List<String> refreshOverlayPackages) {
        if (refreshOverlayPackages == null || refreshOverlayPackages.isEmpty()) return;
        String[] names = refreshOverlayPackages.toArray(new String[0]);
        disableOverlays(names);
        enableOverlays(names);
    }

    /** Spegne gli overlay indicati (nomi completi) */
    public static void disable(List<String> overlayPackages) {
        if (overlayPackages == null || overlayPackages.isEmpty()) return;
        disableOverlays(overlayPackages.toArray(new String[0]));
    }

    /** Toglie dal telefono gli APK delle app indicate (solo lo store: la copia viva resta fino al riavvio, cosi' il sistema vede l'overlay spento e non \"non valido\"). */
    public static void removeApks(List<String> targetPackages) {
        if (targetPackages == null || targetPackages.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (String pkg : targetPackages) {
            String apk = PREFIX + overlayName(pkg) + ".apk";
            sb.append("rm -f ").append(ModuleConstants.STORE_DIR).append('/').append(apk)
              .append(" 2>/dev/null; ");
        }
        Shell.cmd(sb.toString().trim()).exec();
    }
}
