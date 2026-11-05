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

    /** Scrive l'accento scelto al posto di quello di sistema/predefinito scritto fisso nei sorgenti. */
    private static void substituteAccent(String source) {
        // @*android:color/accent_material_dark fuori dai processi hookati resta il teal di sistema:
        // si scrive l'accento reale scelto. Il prefisso "@*" puo' comparire doppio nei sorgenti.
        String accent = String.format("#%08X", ThemePrefs.accentColor());
        Shell.cmd("find \"" + source + "/res\" -type f -name '*.xml'"
                + " -exec sed -i -E 's|(@\\*)+android:color/accent_material_dark|" + accent + "|g' {} +").exec();
        // molti temi hanno l'accento predefinito (#908dff) scritto fisso, anche con trasparenza (#4d908dff):
        // se l'accento scelto e' un altro, lo si sostituisce (la trasparenza resta)
        String hex = String.format("%06X", ThemePrefs.accentColor() & 0xFFFFFF);
        if (!hex.equals("908DFF")) {
            Shell.cmd("find \"" + source + "/res\" -type f -name '*.xml'"
                    + " -exec sed -i -E 's/#([0-9A-Fa-f]{2})908[dD][fF][fF]/#\\1" + hex + "/g;"
                    + " s/#908[dD][fF][fF]([^0-9A-Fa-f]|$)/#" + hex + "\\1/g' {} +").exec();
        }
    }

    /** "1b2029" -> "[1][bB][2][0][2][9]": confronto senza distinzione fra maiuscole e minuscole (sed senza il flag I). */
    private static String ci(int rgb) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.format("%06x", rgb & 0xFFFFFF).toCharArray()) {
            if (Character.isLetter(c)) sb.append('[').append(Character.toLowerCase(c)).append(Character.toUpperCase(c)).append(']');
            else sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Con uno sfondo diverso da quello predefinito, i colori della tavolozza predefinita (Midnight_Slate) scritti fissi nei
     * sorgenti (anche con trasparenza) diventano quelli dello sfondo scelto. Si passa da segnaposto per non sostituire due volte.
     */
    private static void substituteBackground(String source) {
        java.util.Map<Integer, Integer> m = it.tugaia56.oxydiantheme.ui.BackgroundRemap.get(it.tugaia56.oxydiantheme.ThemeApp.getAppContext());
        if (m.isEmpty()) return;
        StringBuilder sed = new StringBuilder();
        int i = 0;
        for (java.util.Map.Entry<Integer, Integer> e : m.entrySet()) {
            String c = ci(e.getKey());
            sed.append("s/#([0-9A-Fa-f]{2})").append(c).append("([^0-9A-Fa-f]|$)/#\\1@@").append(i).append("@@\\2/g;");
            sed.append("s/#").append(c).append("([^0-9A-Fa-f]|$)/#@@").append(i).append("@@\\1/g;");
            i++;
        }
        i = 0;
        for (java.util.Map.Entry<Integer, Integer> e : m.entrySet()) {
            sed.append("s/@@").append(i).append("@@/").append(String.format("%06X", e.getValue() & 0xFFFFFF)).append("/g;");
            i++;
        }
        Shell.cmd("find \"" + source + "/res\" -type f -name '*.xml' -exec sed -i -E '" + sed + "' {} +").exec();
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
        return buildNamedInBatch(targetPackage, assetDir, name, optionAssetPaths, null);
    }

    /** @param extraValuesXml se non nullo, e' scritto come res/values/Obsidian.xml (colori/dimensioni scelti dall'utente) */
    public static boolean buildNamedInBatch(String targetPackage, String assetDir, String name,
                                            List<String> optionAssetPaths, String extraValuesXml) throws IOException {
        return buildNamedInBatch(targetPackage, assetDir, name, optionAssetPaths, extraValuesXml, 0);
    }

    public static boolean buildNamedInBatch(String targetPackage, String assetDir, String name,
                                            List<String> optionAssetPaths, String extraValuesXml,
                                            int iconSizeDp) throws IOException {
        return buildNamedInBatch(targetPackage, assetDir, name, optionAssetPaths, extraValuesXml, iconSizeDp, null);
    }

    /**
     * @param iconSizeDp se diverso da 0 e da 15 (misura stock), le icone (vettori da 15dp) vengono ridimensionate
     * @param iconColor  se non nullo, il nero dei disegni (con la sua trasparenza) diventa questo colore: SystemUI mostra i
     *                   colori scritti nei disegni delle icone Wi-Fi/segnale, non serve nessun hook (provato su OxygenOS 16)
     */
    public static boolean buildNamedInBatch(String targetPackage, String assetDir, String name,
                                            List<String> optionAssetPaths, String extraValuesXml,
                                            int iconSizeDp, Integer iconColor) throws IOException {
        String cacheRoot = ModuleConstants.TEMP_CACHE_DIR + "/" + targetPackage;
        String source = cacheRoot + "/" + name;

        copyAssets("CompileOnDemand/" + targetPackage + "/" + assetDir);
        Shell.cmd("mkdir -p \"" + cacheRoot + "\"",
                "mv -f \"" + ModuleConstants.DATA_DIR + "/CompileOnDemand/" + targetPackage + "/" + assetDir + "\" \"" + source + "\"").exec();
        substituteAccent(source);
        substituteBackground(source);

        if (optionAssetPaths != null) {
            for (String opt : optionAssetPaths) {
                copyAssets(opt);
                String o = ModuleConstants.DATA_DIR + "/" + opt + "/res";
                Shell.cmd("cd \"" + o + "\" && find . -type f | while read -r f; do"
                        + " d=\"" + source + "/res/$(dirname \"$f\")\"; b=$(basename \"$f\"); n=${b%.*};"
                        + " rm -f \"$d/$n\".*; done",
                        // colori gia' definiti dalla base e ridefiniti dall'opzione: si tolgono dalla base (sarebbe un doppione)
                        "for f in \"" + o + "\"/values/*.xml; do [ -f \"$f\" ] || continue; "
                        + "for n in $(grep -o '<color name=\"[^\"]*\"' \"$f\" | sed 's/<color name=\"//;s/\"//'); do "
                        + "find \"" + source + "/res\" -type f -name '*.xml' -path '*values*' -exec sed -i \"/<color name=\\\"$n\\\"/d\" {} + ; "
                        + "done; done",
                        "mkdir -p \"" + source + "/res\" && cp -rf \"" + o + "/.\" \"" + source + "/res/\"").exec();
            }
        }

        // anche i file delle opzioni (es. pacchetto icone) hanno l'accento predefinito scritto fisso
        if (optionAssetPaths != null && !optionAssetPaths.isEmpty()) substituteAccent(source);

        if ("com.android.systemui".equals(targetPackage) && assetDir.startsWith("WIFI_")) {
            // Android puro (verificato su crDroid, Android 17): SystemUI ha 4 livelli ic_wifi_0..3 (e ic_wifi_N_error) invece dei
            // 5 ic_wifi_signal_0..4 di OxygenOS. Il livello 0 dello stile (nessuna barra) non serve: 1..4 diventano 0..3.
            // I file con i nomi di OxygenOS restano: dove i nomi non esistono l'overlay li ignora.
            Shell.cmd("d=\"" + source + "/res/drawable\"; for i in 0 1 2 3; do j=$((i+1)); "
                    + "[ -f \"$d/ic_wifi_signal_$j.xml\" ] || continue; "
                    + "cp -f \"$d/ic_wifi_signal_$j.xml\" \"$d/ic_wifi_$i.xml\"; "
                    + "cp -f \"$d/ic_wifi_signal_$j.xml\" \"$d/ic_wifi_${i}_error.xml\"; done").exec();
            // SystemUI disegna la tessera Wi-Fi con queste stesse icone e accetta solo disegni che iniziano con <vector>: lo stile
            // li avvolge in un <inset> (margini minimi) e la schermata di modifica dei tasti rapidi andava in crash. Si toglie.
            Shell.cmd("d=\"" + source + "/res/drawable\"; for f in \"$d\"/ic_wifi_[0-3].xml \"$d\"/ic_wifi_[0-3]_error.xml; do "
                    + "[ -f \"$f\" ] && sed -i -E 's#<inset[^>]*>##; s#</inset>##' \"$f\"; done").exec();
        }

        if ("com.android.systemui".equals(targetPackage) && assetDir.startsWith("SIG_")) {
            // Android puro (verificato su crDroid, Android 17): il segnale mobile usa ic_mobile_N_4_bar e ic_mobile_N_5_bar (N = barre
            // accese, anche con la variante _error) invece dei 5 stat_signal_signal_lte_single_0..4 di OxygenOS. I 5 livelli dello
            // stile (0..4) si distribuiscono su quelli di Android: 4 barre = 0..4, 5 barre = 0..5 (livello ~ N*4/5).
            // I file con i nomi di OxygenOS restano: dove i nomi non esistono l'overlay li ignora.
            Shell.cmd("d=\"" + source + "/res/drawable\"; "
                    + "for n in 0 1 2 3 4; do [ -f \"$d/stat_signal_signal_lte_single_$n.xml\" ] || continue; "
                    + "cp -f \"$d/stat_signal_signal_lte_single_$n.xml\" \"$d/ic_mobile_${n}_4_bar.xml\"; "
                    + "cp -f \"$d/stat_signal_signal_lte_single_$n.xml\" \"$d/ic_mobile_${n}_4_bar_error.xml\"; done; "
                    + "for m in 0 1 2 3 4 5; do n=$(( (m*4+2)/5 )); [ -f \"$d/stat_signal_signal_lte_single_$n.xml\" ] || continue; "
                    + "cp -f \"$d/stat_signal_signal_lte_single_$n.xml\" \"$d/ic_mobile_${m}_5_bar.xml\"; "
                    + "cp -f \"$d/stat_signal_signal_lte_single_$n.xml\" \"$d/ic_mobile_${m}_5_bar_error.xml\"; done").exec();
            // SystemUI accetta solo disegni che iniziano con <vector> (come per il Wi-Fi): si toglie l'<inset> dello stile
            Shell.cmd("d=\"" + source + "/res/drawable\"; for f in \"$d\"/ic_mobile_[0-5]_[45]_bar.xml \"$d\"/ic_mobile_[0-5]_[45]_bar_error.xml; do "
                    + "[ -f \"$f\" ] || continue; sed -i -z -E 's#<inset[^>]*>##; s#</inset>##' \"$f\"; "   // -z: il tag <inset> va su due righe
                    // alcuni stili dichiarano xmlns:android solo sull'<inset>: tolto quello, va messo sul <vector>
                    + "grep -q 'xmlns:android' \"$f\" || sed -i -z -E 's#<vector([[:space:]])#<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\\1#' \"$f\"; done").exec();
        }

        if ("com.android.settings".equals(targetPackage) && assetDir.startsWith("ICP_")) {
            // Android puro (Android 17): la pagina principale di Impostazioni usa icone con altri nomi (ic_homepage_*) rispetto a quelle
            // di OxygenOS del pack: si copiano le icone del pack sui nomi di Android. I nomi di OxygenOS restano (dove non esistono si ignorano).
            String map = "network:ic_settings_wireless connected_device:ic_devices_other notification:ic_notifications "
                    + "sound:ic_volume_up_24dp display:ic_settings_display_white wallpaper:ic_settings_wallpaper_white apps:ic_apps "
                    + "storage:ic_storage_white battery:ic_settings_battery_white privacy:ic_settings_privacy location:ic_settings_location "
                    + "security:ic_settings_security_white safety_center:ic_settings_safety_center emergency:ic_settings_emergency "
                    + "accounts:ic_settings_accounts accessibility:ic_settings_accessibility support:ic_support_tips "
                    + "system_dashboard:ic_settings_system_dashboard_white about:ic_settings_about_device";
            Shell.cmd("d=\"" + source + "/res/drawable\"; for m in " + map + "; do n=${m%%:*}; o=${m#*:}; "
                    + "[ -f \"$d/$o.xml\" ] && cp -f \"$d/$o.xml\" \"$d/ic_homepage_$n.xml\"; done").exec();
        }

        if (iconSizeDp > 0 && iconSizeDp != 15) {
            // i vettori delle icone sono da 15dp: si riscrive larghezza e altezza della radice
            String expr = "s/(android:(width|height)=\")15(\\.0+)?(dp|dip)\"/\\1" + iconSizeDp + ".0dp\"/g";
            Shell.cmd("find \"" + source + "/res/drawable\" -type f -name '*.xml' -exec sed -i -E '" + expr + "' {} +").exec();
        }

        if ("com.android.systemui".equals(targetPackage) && iconSizeDp > 0 && iconSizeDp != 15
                && (assetDir.startsWith("WIFI_") || assetDir.startsWith("SIG_"))) {
            // Android puro (Android 17): le icone Wi-Fi e segnale hanno una misura fissa decisa da SystemUI (in sp, 15 / 12), non dal
            // disegno: si ridefiniscono qui (su OxygenOS questi nomi non esistono e l'overlay li ignora)
            int h = Math.max(1, Math.round(iconSizeDp * 0.8f));
            String dims = assetDir.startsWith("WIFI_")
                    ? "<dimen name=\"status_bar_wifi_signal_size\">" + iconSizeDp + "dp</dimen>"
                      + "<dimen name=\"status_bar_wifi_signal_height_updated\">" + h + "dp</dimen>"
                    : "<dimen name=\"status_bar_mobile_signal_size\">" + iconSizeDp + "dp</dimen>"
                      + "<dimen name=\"status_bar_mobile_signal_size_updated\">" + h + "dp</dimen>"
                      + "<dimen name=\"signal_icon_size\">" + iconSizeDp + "dp</dimen>";
            Shell.cmd("mkdir -p \"" + source + "/res/values\"; printf '%s\n' '<?xml version=\"1.0\" encoding=\"utf-8\"?>' '<resources>' '"
                    + dims + "' '</resources>' > \"" + source + "/res/values/oxy_icon_size.xml\"").exec();
        }

        if (iconColor != null) {
            // il nero dei disegni (anche con trasparenza: #66000000 = barre spente) diventa il colore scelto
            String rgb = String.format("%06X", iconColor & 0xFFFFFF);
            Shell.cmd("find \"" + source + "/res\" -type f -name '*.xml' -path '*drawable*'"
                    + " -exec sed -i -E 's/#([0-9A-Fa-f]{2})000000([^0-9A-Fa-f]|$)/#\\1" + rgb + "\\2/g' {} +").exec();
        }

        if (extraValuesXml != null) {
            // colori gia' presenti nei sorgenti (base o opzioni) e ridefiniti qui: si tolgono da la' (doppioni)
            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("<color name=\"([^\"]+)\"").matcher(extraValuesXml);
            StringBuilder dedupe = new StringBuilder();
            while (mm.find()) {
                dedupe.append("find \"").append(source).append("/res\" -type f -name '*.xml' -path '*values*' ")
                        .append("-exec sed -i \"/<color name=\\\"").append(mm.group(1)).append("\\\"/d\" {} + ; ");
            }
            if (dedupe.length() > 0) Shell.cmd(dedupe.toString()).exec();
            Shell.cmd("mkdir -p \"" + source + "/res/values\"",
                    "printf '%s' '" + extraValuesXml.replace("'", "'\\''") + "' > \"" + source + "/res/values/Obsidian.xml\"").exec();
            // Stessi valori anche per la modalita' notte: se il bersaglio ha una variante notte e qui c'e'
            // solo quella base, il sistema usa la sua e il colore scelto non si vede.
            Shell.cmd("mkdir -p \"" + source + "/res/values-night\"",
                    "cp -f \"" + source + "/res/values/Obsidian.xml\" \"" + source + "/res/values-night/Obsidian.xml\"").exec();
        }

        // Sempre: se la variante "notte" dell'overlay e' uguale a quella base, aapt2 la toglie, ma se l'app ha una sua variante
        // "notte" (piu' specifica) vince quella dell'app e il colore dell'overlay non si vede (es. titolo di OneSync dopo l'aggiornamento).
        OverlayCompiler.keepNightDuplicates = true;
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
                // Il file va riscritto sul posto (con un file nuovo l'unione montata continua a mostrare quello vecchio).
                // Ma SystemUI & co tengono aperto l'APK: se lo si riscrive mentre l'overlay e' acceso, leggono dati
                // sbagliati e vanno in crash ("File res/drawable/... not found"). Quindi prima si spegne l'overlay e si
                // aspetta che il processo lo lasci; verra' riacceso a fine lavoro.
                "[ -d " + ModuleConstants.LIVE_UPPER_DIR + " ] && { OV=" + namedPackage(name) + "; "
                        + "if cmd overlay list 2>/dev/null | grep -q \"^\\[x\\] $OV\\$\"; then "
                        + "cmd overlay disable --user 0 $OV >/dev/null 2>&1; sleep "
                        + (("com.android.systemui".equals(targetPackage) || "android".equals(targetPackage)) ? "3" : "1")
                        + "; fi; cp -f " + signed + " " + inSystem + " 2>/dev/null; }",
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
