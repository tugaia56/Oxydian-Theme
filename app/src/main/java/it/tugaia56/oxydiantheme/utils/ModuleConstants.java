package it.tugaia56.oxydiantheme.utils;

import it.tugaia56.oxydiantheme.ThemeApp;

/**
 * Percorsi di Oxydian Theme. Tutto il lavoro di compilazione avviene nella cartella privata
 * dell'app (nessun permesso di archiviazione); gli APK finiti vanno nello "store" del modulo
 * oxydian_theme, che lo script di avvio del modulo monta in /product/overlay.
 */
public final class ModuleConstants {
    public static final String MODULE_ID  = "oxydian_theme";
    public static final String MODULE_DIR = "/data/adb/modules/" + MODULE_ID;
    /** Gli APK compilati che il modulo monta e abilita ad ogni avvio (= l'insieme scelto dall'utente). */
    public static final String STORE_DIR  = MODULE_DIR + "/store";
    /** Overlay reale del sistema (sola lettura): qui si copia solo per l'effetto immediato. */
    public static final String SYSTEM_OVERLAY_DIR = "/product/overlay";
    /** Cartella "upper" dell'unione montata dal modulo: scrivere qui rende il file subito visibile in /product/overlay */
    public static final String LIVE_UPPER_DIR = "/mnt/oxydian_theme_ovl/upper";

    public static final String DATA_DIR = ThemeApp.getAppContext().getFilesDir().getAbsolutePath();
    public static final String BIN_DIR  = ThemeApp.getAppContext().getDataDir() + "/bin";
    public static final String TEMP_DIR = DATA_DIR + "/work";
    public static final String TEMP_OVERLAY_DIR       = TEMP_DIR + "/overlays";
    public static final String TEMP_CACHE_DIR         = TEMP_OVERLAY_DIR + "/cache";
    public static final String UNSIGNED_UNALIGNED_DIR = TEMP_OVERLAY_DIR + "/unsigned_unaligned";
    public static final String UNSIGNED_DIR           = TEMP_OVERLAY_DIR + "/unsigned";
    public static final String SIGNED_DIR             = TEMP_OVERLAY_DIR + "/signed";

    public static final String METADATA_OVERLAY_PARENT = "OVERLAY_PARENT";
    public static final String METADATA_OVERLAY_TARGET = "OVERLAY_TARGET";
    public static final String METADATA_THEME_VERSION  = "THEME_VERSION";
    public static final String METADATA_THEME_CATEGORY = "THEME_CATEGORY";
    public static final String OVERLAY_CATEGORY_PREFIX = "it.tugaia56.oxydian.theme.category.";

    public static final String FRAMEWORK_DIR = "/system/framework/framework-res.apk";

    private ModuleConstants() {}
}
