package it.tugaia56.oxydiantheme.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ModuleSetup;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.overlay.compiler.ThemeCompiler;

/**
 * Schermata unica: elenco delle app installate per cui c'e' un tema incluso, con un interruttore
 * ciascuna e Applica. Applica compila le selezionate, toglie le altre e (se serve) riaccende.
 */
public class MainActivity extends AppCompatActivity {

    /** Una riga: tema di un'app (cartella APP) oppure un bersaglio Dark Shadow (cartella con sigla). */
    private static class AppEntry {
        final String pkg;
        final String label;
        final String dir;
        final String name;
        final boolean ds;
        boolean enabled;
        AppEntry(String pkg, String label, boolean enabled) {
            this(pkg, label, "APP", ThemeCompiler.overlayName(pkg), false, enabled);
        }
        AppEntry(String pkg, String label, String dir, boolean enabled) {
            this(pkg, label, dir, "DS_" + dir, true, enabled);
        }
        private AppEntry(String pkg, String label, String dir, String name, boolean ds, boolean enabled) {
            this.pkg = pkg; this.label = label; this.dir = dir; this.name = name; this.ds = ds; this.enabled = enabled;
        }
        String overlay() { return ThemeCompiler.namedPackage(name); }
        void saveEnabled() { if (ds) ThemePrefs.setDarkShadowEnabled(pkg, enabled); else ThemePrefs.setAppEnabled(pkg, enabled); }
        String builtSig() { return ds ? ThemePrefs.getDarkShadowBuilt(pkg) : ThemePrefs.getBuiltSignature(pkg); }
        void setBuilt(String sig) { if (ds) ThemePrefs.setDarkShadowBuilt(pkg, sig); else ThemePrefs.setBuiltSignature(pkg, sig); }
        void clearBuilt() { if (ds) ThemePrefs.setDarkShadowBuilt(pkg, ""); else ThemePrefs.clearBuiltSignature(pkg); }
    }

    /** Bersagli Dark Shadow: pacchetto, cartella negli asset. */
    private static final String[][] DS_TARGETS = {
            {"com.android.settings", "SST"}, {"com.android.systemui", "SUT"}, {"com.android.launcher", "LT"},
            {"com.oneplus.calculator", "CA"}, {"com.oneplus.deskclock", "DC"}, {"com.oneplus.gallery", "GL"},
            {"com.oneplus.oshare", "OS"}, {"com.oplus.apprecover", "AR"}, {"com.oplus.camera", "CM"},
            {"com.oplus.contentportal", "CP"}, {"com.oplus.eyeprotect", "EP"}, {"com.oplus.games", "GA"},
            {"com.oplus.wirelesssettings", "WS"}, {"com.heytap.browser", "BR"},
            {"com.coloros.floatassistant", "FA"}, {"com.coloros.video", "VT"},
            {"android", "SYS"},
    };

    private final List<AppEntry> mApps = new ArrayList<>();
    private final List<AppEntry> mVisible = new ArrayList<>(); // senza "Colori di sistema" (ha la sua scheda)
    private RecyclerView mList;
    private TextView mStatus;
    private android.widget.ImageButton mWarn;
    private int mWarnCount = 0;
    private Button mApply;
    private boolean mBusy = false;

    // Stato di ogni app, con gli stessi colori di Substratum
    private static final int ST_NONE = 0, ST_ACTIVE = 1, ST_DISABLED = 2, ST_REBOOT = 3, ST_INVALID = 4;
    private final Map<String, Integer> mState = new HashMap<>();

    private final androidx.activity.result.ActivityResultLauncher<String> mBackupSave =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null) return;
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    out.write(ThemePrefs.exportJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    Toast.makeText(this, R.string.backup_saved, Toast.LENGTH_LONG).show();
                } catch (Throwable t) {
                    Toast.makeText(this, R.string.backup_error, Toast.LENGTH_LONG).show();
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<String[]> mBackupLoad =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
                    String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (ThemePrefs.importJson(text)) {
                        Toast.makeText(this, R.string.backup_restored, Toast.LENGTH_LONG).show();
                        recreate();
                    } else {
                        Toast.makeText(this, R.string.backup_error, Toast.LENGTH_LONG).show();
                    }
                } catch (Throwable t) {
                    Toast.makeText(this, R.string.backup_error, Toast.LENGTH_LONG).show();
                }
            });

    private void backupDialog() {
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.card_backup)
                .setMessage(R.string.backup_message)
                .setPositiveButton(R.string.backup_save, (d, w) -> mBackupSave.launch("OxydianTheme-backup.json"))
                .setNeutralButton(R.string.backup_restore, (d, w) -> mBackupLoad.launch(new String[]{"application/json", "text/plain", "*/*"}))
                .setNegativeButton(R.string.legend_close, null));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mList = findViewById(R.id.list);
        mStatus = findViewById(R.id.status);
        mWarn = findViewById(R.id.btn_warn);
        mWarn.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFFFC107));
        mWarn.setOnClickListener(v -> showWarning());
        mApply = findViewById(R.id.btn_apply);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(new Adapter());

        mApply.setOnClickListener(v -> runSelected(ACT_ACTIVATE));
        findViewById(R.id.btn_disable).setOnClickListener(v -> runSelected(ACT_DISABLE));
        findViewById(R.id.btn_remove).setOnClickListener(v -> runSelected(ACT_REMOVE));
        android.widget.ImageButton restart = findViewById(R.id.btn_restart_ui);
        restart.setImageTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        restart.setOnClickListener(v -> {
            Toast.makeText(this, R.string.restart_ui_toast, Toast.LENGTH_SHORT).show();
            new Thread(() -> Shell.cmd("killall com.android.systemui").exec()).start();
        });
        setupCards();
        if (savedInstanceState == null) mTab = TAB_IDS[Math.max(0, Math.min(3, ThemePrefs.getDefaultTab()))];
        setupBottomNav();
        buildInfoPage();
        SystemColorsDialog.republish(this);
        IconColorDialog.republish();
        ((Button) findViewById(R.id.btn_all)).setOnClickListener(v -> setAll(true));
        ((Button) findViewById(R.id.btn_none)).setOnClickListener(v -> setAll(false));

        loadApps();
        setupModuleAsync();
        Tint.tree(findViewById(android.R.id.content));
    }

    // ── Schede dell'intestazione ─────────────────────────────────────────────

    private final List<View> mCards = new ArrayList<>();
    private TextView mSubAccent, mSubBg, mSubWifi, mSubSignal, mSubNav, mSubSettings;
    private TextView mSubPinNum, mSubPinBg, mSubRipple, mSubDefaultTab;

    private View makeCard(int titleRes, TextView[] subOut, Runnable onClick) {
        float d = getResources().getDisplayMetrics().density;
        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (14 * d);
        card.setPadding(pad, pad, pad, pad);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        int m = (int) (4 * d);
        lp.setMargins(m, m, m, m);
        card.setLayoutParams(lp);
        card.setClickable(true);
        card.setFocusable(true);
        TextView t = new TextView(this);
        t.setText(titleRes);
        t.setTextColor(getColor(R.color.text));
        t.setTextSize(15);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView sub = new TextView(this);
        sub.setTextColor(getColor(R.color.text_dim));
        sub.setTextSize(12);
        sub.setPadding(0, (int) (4 * d), 0, 0);
        card.addView(t);
        card.addView(sub);
        card.setOnClickListener(v -> onClick.run());
        subOut[0] = sub;
        mCards.add(card);
        return card;
    }

    private TextView mSubProgress, mSubActivity;
    private static final String SYSTEMUI_PKG = "com.android.systemui";

    private void setupCards() {
        android.widget.LinearLayout r1 = findViewById(R.id.cards_row1);
        android.widget.LinearLayout r2 = findViewById(R.id.cards_row2);
        android.widget.LinearLayout r3 = findViewById(R.id.cards_row3);
        android.widget.LinearLayout r4 = findViewById(R.id.cards_row4);
        TextView[] s = new TextView[1];
        // Pagina Colori
        r1.addView(makeCard(R.string.card_accent, s, () -> openColors(0)));
        mSubAccent = s[0];
        r1.addView(makeCard(R.string.card_background, s, () -> openColors(1)));
        mSubBg = s[0];
        android.widget.LinearLayout r6 = findViewById(R.id.cards_row6);
        r6.addView(makeCard(R.string.card_ripple, s, () -> RippleDialog.show(this, () -> {
            syncEnabled(SystemColorsDialog.PKG);
            refreshCards();
            mList.getAdapter().notifyDataSetChanged();
            onApply(SystemColorsDialog.PKG, false);
        })));
        mSubRipple = s[0];
        View sp6 = new View(this);
        sp6.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        r6.addView(sp6);
        // Pagina Mods
        android.widget.LinearLayout r5 = findViewById(R.id.cards_row5);
        r2.addView(makeCard(R.string.card_progress, s, () -> openGroups(SystemColorsDialog.PKG, new String[]{"progress"}, R.string.card_progress)));
        mSubProgress = s[0];
        r2.addView(makeCard(R.string.card_activity, s, this::toggleHideActivity));
        mSubActivity = s[0];
        r3.addView(makeCard(R.string.card_pin_num, s, () -> openGroups(SYSTEMUI_PKG, new String[]{"pinnum"}, R.string.card_pin_num)));
        mSubPinNum = s[0];
        r3.addView(makeCard(R.string.card_pin_bg, s, () -> openGroups(SYSTEMUI_PKG, new String[]{"pinbg"}, R.string.card_pin_bg)));
        mSubPinBg = s[0];
        r4.addView(makeCard(R.string.card_wifi, s, () -> openStyles("wifi")));
        mSubWifi = s[0];
        r4.addView(makeCard(R.string.card_signal, s, () -> openStyles("signal")));
        mSubSignal = s[0];
        r5.addView(makeCard(R.string.card_nav, s, () -> openStyles("nav")));
        mSubNav = s[0];
        r5.addView(makeCard(R.string.card_settings, s, () -> openStyles("settings")));
        mSubSettings = s[0];
        refreshCards();
    }

    // ── Barra di navigazione in basso ────────────────────────────────────────

    private int mTab = R.id.nav_themes;

    private static final int[] TAB_IDS = {R.id.nav_themes, R.id.nav_colors, R.id.nav_icons, R.id.nav_info};

    private void defaultTabDialog() {
        String[] names = {getString(R.string.tab_themes), getString(R.string.tab_colors), getString(R.string.tab_icons), getString(R.string.tab_info)};
        final int[] sel = {ThemePrefs.getDefaultTab()};
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.card_default_tab)
                .setSingleChoiceItems(names, sel[0], (d, w) -> sel[0] = w)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    ThemePrefs.setDefaultTab(sel[0]);
                    refreshCards();
                }));
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        nav.setSelectedItemId(mTab);
        showTab(mTab);
        tintBottomNav();
    }

    private void showTab(int id) {
        mTab = id;
        findViewById(R.id.page_themes).setVisibility(id == R.id.nav_themes ? View.VISIBLE : View.GONE);
        findViewById(R.id.page_colors).setVisibility(id == R.id.nav_colors ? View.VISIBLE : View.GONE);
        findViewById(R.id.page_icons).setVisibility(id == R.id.nav_icons ? View.VISIBLE : View.GONE);
        findViewById(R.id.page_info).setVisibility(id == R.id.nav_info ? View.VISIBLE : View.GONE);
        int title = id == R.id.nav_colors ? R.string.tab_colors : id == R.id.nav_icons ? R.string.tab_icons
                : id == R.id.nav_info ? R.string.tab_info : R.string.title_apps;
        ((TextView) findViewById(R.id.title)).setText(title);
        updateNotice();
    }

    private void tintBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView nav = findViewById(R.id.bottom_nav);
        if (nav == null) return;
        int acc = ThemePrefs.accentColor();
        int[][] st = {{android.R.attr.state_checked}, {}};
        android.content.res.ColorStateList c = new android.content.res.ColorStateList(st, new int[]{acc, 0xFF9E9E9E});
        nav.setItemIconTintList(c);
        nav.setItemTextColor(c);
        nav.setItemActiveIndicatorColor(android.content.res.ColorStateList.valueOf((acc & 0xFFFFFF) | 0x33000000));
    }

    // ── Pagina Info: come funziona + legenda dei colori ──────────────────────

    private void buildInfoPage() {
        android.widget.LinearLayout box = findViewById(R.id.info_box);
        box.removeAllViews();
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        TextView[] sub = new TextView[1];
        row.addView(makeCard(R.string.card_info, sub, this::showLegend));
        sub[0].setText(R.string.card_info_sub);
        row.addView(makeCard(R.string.restart_ui, sub, () -> {
            Toast.makeText(this, R.string.restart_ui_toast, Toast.LENGTH_SHORT).show();
            new Thread(() -> Shell.cmd("killall com.android.systemui").exec()).start();
        }));
        sub[0].setText(R.string.card_restart_sub);
        box.addView(row);
        android.widget.LinearLayout row2 = new android.widget.LinearLayout(this);
        row2.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row2.addView(makeCard(R.string.card_update, sub, () -> SettingsPage.checkUpdates(this)));
        sub[0].setText(R.string.card_update_sub);
        row2.addView(makeCard(R.string.card_backup, sub, this::backupDialog));
        sub[0].setText(R.string.card_backup_sub);
        box.addView(row2);
        android.widget.LinearLayout row3 = new android.widget.LinearLayout(this);
        row3.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row3.addView(makeCard(R.string.card_about, sub, () -> SettingsPage.about(this)));
        sub[0].setText(R.string.card_about_sub);
        row3.addView(makeCard(R.string.card_credits, sub, () -> SettingsPage.credits(this)));
        sub[0].setText(R.string.card_credits_sub);
        box.addView(row3);
        android.widget.LinearLayout row4 = new android.widget.LinearLayout(this);
        row4.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row4.addView(makeCard(R.string.card_default_tab, sub, this::defaultTabDialog));
        mSubDefaultTab = sub[0];
        View sp4 = new View(this);
        sp4.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        row4.addView(sp4);
        box.addView(row4);
        TextView ver = new TextView(this);
        String v = "";
        try { v = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        ver.setText("Oxydian Theme " + v);
        ver.setTextColor(getColor(R.color.text_dim));
        ver.setTextSize(12);
        ver.setPadding(0, (int) (24 * getResources().getDisplayMetrics().density), 0, 0);
        box.addView(ver);
        refreshCards();
    }

    /** Nasconde / mostra le frecce di entrata e uscita dell'attivita' di rete: un tocco e si applica. */
    private void toggleHideActivity() {
        boolean on = ThemePrefs.getOption(SYSTEMUI_PKG, "icons").isEmpty();
        ThemePrefs.setOption(SYSTEMUI_PKG, "icons", on ? "Nascondi_entrata_uscita" : "");
        syncEnabled(SYSTEMUI_PKG);
        refreshCards();
        mList.getAdapter().notifyDataSetChanged();
        onApply(SYSTEMUI_PKG, false);
    }

    /** Il tema di un bersaglio (colori di sistema / SystemUI) si accende da solo se si sceglie qualcosa. */
    private void syncEnabled(String pkg) {
        boolean any;
        if (SystemColorsDialog.PKG.equals(pkg)) {
            any = !ThemePrefs.getOption(pkg, "accent").isEmpty() || !ThemePrefs.getOption(pkg, "background").isEmpty()
                    || !ThemePrefs.getOption(pkg, "progress").isEmpty() || ThemePrefs.getRippleAlpha() > 0;
            ThemePrefs.setDarkShadowEnabled(pkg, any);
        } else {
            any = !ThemePrefs.getOption(pkg, "pinnum").isEmpty() || !ThemePrefs.getOption(pkg, "pinbg").isEmpty()
                    || !ThemePrefs.getOption(pkg, "icons").isEmpty();
            if (any) ThemePrefs.setDarkShadowEnabled(pkg, true);
        }
        for (AppEntry e : mApps) if (e.ds && e.pkg.equals(pkg)) e.enabled = ThemePrefs.isDarkShadowEnabled(pkg);
    }

    private void openGroups(String pkg, String[] groups, int titleRes) {
        OptionGroupsDialog.show(this, pkg, groups, titleRes, () -> {
            syncEnabled(pkg);
            refreshCards();
            mList.getAdapter().notifyDataSetChanged();
            onApply(pkg, false);
        });
    }

    /** Pallino del colore a sinistra del testo. */
    private void setDot(TextView t, int color) {
        float d = getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        g.setColor(color | 0xFF000000);
        g.setStroke((int) (1.5f * d), 0x88FFFFFF);
        int size = (int) (16 * d);
        g.setSize(size, size);
        g.setBounds(0, 0, size, size);
        t.setCompoundDrawablesRelative(g, null, null, null);
        t.setCompoundDrawablePadding((int) (8 * d));
        t.setGravity(android.view.Gravity.CENTER_VERTICAL);
    }

    private void openStyles(String mode) {
        startActivity(new android.content.Intent(this, StyleActivity.class).putExtra("mode", mode));
    }

    private void openColors(int which) {
        SystemColorsDialog.show(this, which, color -> {
            for (AppEntry e : mApps) {
                if (e.ds && SystemColorsDialog.PKG.equals(e.pkg)) e.enabled = ThemePrefs.isDarkShadowEnabled(e.pkg);
            }
            mStatus.setTextColor(color);
            Tint.tree(findViewById(android.R.id.content));
            refreshCards();
            mList.getAdapter().notifyDataSetChanged();
            Toast.makeText(this, R.string.accent_hint, Toast.LENGTH_LONG).show();
            onApply(SystemColorsDialog.PKG, false);
        });
    }

    private static String prettyPack(String n) {
        switch (n) {
            case "pui_v1": return "PUI v1";
            case "pui_v2": return "PUI v2";
            case "pui_v3": return "PUI v3";
            case "hos": return "HOS";
            case "oos": return "OOS";
            case "oos_stock": return "OOS Stock";
            default: return pretty(n);
        }
    }

    private static String pretty(String n) {
        if (n == null || n.isEmpty()) return "";
        if (n.equals("oneui")) return "One UI";
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1));
        }
        return sb.toString();
    }

    /** Bordo accento e scelte attuali nelle schede. */
    private void refreshCards() {
        if (mStatus != null) mStatus.setTextColor(ThemePrefs.accentColor());
        tintBottomNav();
        float d = getResources().getDisplayMetrics().density;
        for (View c : mCards) {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setColor(getColor(R.color.card));
            g.setCornerRadius(14 * d);
            g.setStroke((int) (1.5f * d), ThemePrefs.accentColor());
            c.setBackground(g);
        }
        String def = getString(R.string.options_default);
        String acc = ThemePrefs.getOption(SystemColorsDialog.PKG, "accent");
        String bg = ThemePrefs.getOption(SystemColorsDialog.PKG, "background");
        mSubAccent.setText(acc.isEmpty() ? def : OptionGroupsDialog.CUSTOM.equals(acc) ? getString(R.string.opt_custom) : pretty(acc));
        mSubBg.setText(bg.isEmpty() ? def : OptionGroupsDialog.CUSTOM.equals(bg) ? getString(R.string.opt_custom) : pretty(bg));
        Integer ca = SystemColorsDialog.currentAccent(this), cb = SystemColorsDialog.currentBg(this);
        setDot(mSubAccent, ca != null ? ca : ThemePrefs.DEFAULT_ACCENT);
        setDot(mSubBg, cb != null ? cb : 0xFF1B2029);
        String w = ThemePrefs.getStyle("WIFI1"), sg = ThemePrefs.getStyle("SIG1");
        mSubWifi.setText(w.isEmpty() ? def : pretty(w));
        mSubSignal.setText(sg.isEmpty() ? def : pretty(sg));
        String nv = ThemePrefs.getStyle("NAV1");
        mSubNav.setText(nv.isEmpty() ? def : pretty(nv));
        mSubRipple.setText(RippleDialog.summary(this));
        if (mSubDefaultTab != null) {
            String[] names = {getString(R.string.tab_themes), getString(R.string.tab_colors), getString(R.string.tab_icons), getString(R.string.tab_info)};
            mSubDefaultTab.setText(names[Math.max(0, Math.min(3, ThemePrefs.getDefaultTab()))]);
        }
        mSubProgress.setText(OptionGroupsDialog.summary(this, SystemColorsDialog.PKG, "progress"));
        mSubPinNum.setText(OptionGroupsDialog.summary(this, SYSTEMUI_PKG, "pinnum"));
        mSubPinBg.setText(OptionGroupsDialog.summary(this, SYSTEMUI_PKG, "pinbg"));
        boolean hideOn = !ThemePrefs.getOption(SYSTEMUI_PKG, "icons").isEmpty();
        mSubActivity.setText(hideOn ? getString(R.string.state_on_full) : getString(R.string.state_off));
        mSubActivity.setTextColor(hideOn ? ThemePrefs.accentColor() : getColor(R.color.text_dim));
        setDot(mSubActivity, hideOn ? ThemePrefs.accentColor() : 0xFF555A66);
        String ic = ThemePrefs.getStyle("ICON1");
        mSubSettings.setText(ic.isEmpty() ? def : prettyPack(ic));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSubAccent != null) refreshCards();
        refreshStates();
    }

    /** Legge lo stato reale di ogni overlay (attivo, solo installato, da riavviare, non valido). */
    private void refreshStates() {
        new Thread(() -> {
            Map<String, Integer> st = new HashMap<>();
            try {
                Set<String> store = new HashSet<>();
                for (String f : Shell.cmd("ls " + it.tugaia56.oxydiantheme.utils.ModuleConstants.STORE_DIR).exec().getOut())
                    store.add(f.trim().replace(".apk", ".overlay"));
                Set<String> live = new HashSet<>();
                for (String f : Shell.cmd("ls " + it.tugaia56.oxydiantheme.utils.ModuleConstants.SYSTEM_OVERLAY_DIR + " | grep " + ThemeCompiler.PREFIX).exec().getOut())
                    live.add(f.trim().replace(".apk", ".overlay"));
                Map<String, String> sys = new HashMap<>();
                for (String line : Shell.cmd("cmd overlay list | grep " + ThemeCompiler.PREFIX).exec().getOut()) {
                    line = line.trim();
                    if (line.length() > 4) sys.put(line.substring(3).trim(), line.substring(0, 3));
                }
                for (AppEntry e : new ArrayList<>(mApps)) {
                    String ov = e.overlay();
                    String m = sys.get(ov);
                    int s;
                    if ("[x]".equals(m)) s = ST_ACTIVE;
                    else if ("[ ]".equals(m)) s = ST_DISABLED;
                    else if (m != null && live.contains(ov)) s = ST_INVALID;  // "---" ma l'APK c'e': davvero non valido
                    else if (store.contains(ov)) s = ST_REBOOT;  // APK pronto, il sistema non lo conosce ancora
                    else if (m != null) s = ST_DISABLED;         // tolto dallo store, il sistema lo ricorda fino al riavvio
                    else s = ST_NONE;
                    st.put(e.name, s);
                }
            } catch (Throwable ignored) {}
            new Handler(Looper.getMainLooper()).post(() -> {
                mState.clear();
                mState.putAll(st);
                mList.getAdapter().notifyDataSetChanged();
            });
        }).start();
    }

    // ── Legenda dei colori ───────────────────────────────────────────────────

    private android.widget.ScrollView scroll(View v) {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(v);
        return sv;
    }

    private void showLegend() {
        int[][] rows = {
                {ThemePrefsAccent(), R.string.state_active, R.string.legend_active_desc},
                {getColor(R.color.state_disabled), R.string.state_disabled, R.string.legend_disabled_desc},
                {getColor(R.color.state_not_active), R.string.state_reboot, R.string.legend_reboot_desc},
                {getColor(R.color.state_invalid), R.string.state_invalid, R.string.legend_invalid_desc},
                {getColor(R.color.state_not_installed), R.string.state_none, R.string.legend_none_desc},
        };
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView how = new TextView(this);
        how.setText(R.string.notice);
        how.setTextColor(getColor(R.color.text));
        how.setTextSize(14);
        box.addView(how);
        TextView legendTitle = new TextView(this);
        legendTitle.setText(R.string.legend_title);
        legendTitle.setTextColor(ThemePrefs.accentColor());
        legendTitle.setTextSize(15);
        legendTitle.setPadding(0, pad, 0, 0);
        box.addView(legendTitle);
        for (int[] r : rows) {
            TextView name = new TextView(this);
            name.setText(r[1]);
            name.setTextColor(r[0]);
            name.setTextSize(16);
            name.setPadding(0, pad / 2, 0, 0);
            TextView desc = new TextView(this);
            desc.setText(r[2]);
            desc.setTextColor(getColor(R.color.text_dim));
            desc.setTextSize(13);
            box.addView(name);
            box.addView(desc);
        }
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.info_title)
                .setView(scroll(box))
                .setPositiveButton(R.string.legend_close, null));
    }

    private static int ThemePrefsAccent() {
        return ThemePrefs.accentColor();
    }

    // ── Modulo root ──────────────────────────────────────────────────────────

    private void setupModuleAsync() {
        mStatus.setText("…");
        new Thread(() -> {
            String msg;
            if (!ModuleSetup.hasRoot()) {
                msg = getString(R.string.status_no_root);
            } else {
                try {
                    msg = ModuleSetup.ensure() ? getString(R.string.status_reboot) : getString(R.string.status_ok);
                } catch (Throwable t) {
                    msg = String.valueOf(t);
                }
            }
            final String m = msg;
            new Handler(Looper.getMainLooper()).post(() -> mStatus.setText(m));
        }).start();
    }

    // ── Elenco ───────────────────────────────────────────────────────────────

    private void loadApps() {
        mApps.clear();
        PackageManager pm = getPackageManager();
        try {
            String[] dirs = getAssets().list("CompileOnDemand");
            if (dirs != null) for (String pkg : dirs) {
                String[] sub = getAssets().list("CompileOnDemand/" + pkg);
                if (sub == null || !Arrays.asList(sub).contains("APP")) continue;
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    mApps.add(new AppEntry(pkg, String.valueOf(pm.getApplicationLabel(ai)), ThemePrefs.isAppEnabled(pkg)));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // app non installata: nessuna riga
                }
            }
        } catch (Exception ignored) {}
        mApps.sort(Comparator.comparing(e -> e.label.toLowerCase(Locale.ROOT)));
        List<AppEntry> dsList = new ArrayList<>();
        for (String[] t : DS_TARGETS) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(t[0], 0);
                dsList.add(new AppEntry(t[0], "android".equals(t[0]) ? getString(R.string.system_colors) : String.valueOf(pm.getApplicationLabel(ai)), t[1],
                        ThemePrefs.isDarkShadowEnabled(t[0])));
            } catch (PackageManager.NameNotFoundException ignored) {
                // bersaglio non presente su questo telefono
            }
        }
        mApps.addAll(dsList);
        mApps.sort(Comparator.comparing(e -> e.label.toLowerCase(Locale.ROOT)));
        mVisible.clear();
        for (AppEntry e : mApps) if (!(e.ds && SystemColorsDialog.PKG.equals(e.pkg))) mVisible.add(e);
        mList.getAdapter().notifyDataSetChanged();
        updateNotice();
    }

    private void updateNotice() {
        int selected = 0;
        for (AppEntry e : mApps) if (e.enabled) selected++;
        mWarnCount = selected;
        if (mWarn != null) mWarn.setVisibility(selected > 10 && mTab == R.id.nav_themes ? View.VISIBLE : View.GONE);
    }

    private void showWarning() {
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warn_title)
                .setMessage(getString(R.string.many_warning, mWarnCount))
                .setPositiveButton(android.R.string.ok, null));
    }

    /** Voci spuntate (solo per la prossima azione: si svuotano dopo Attiva / Disattiva / Rimuovi). */
    private final Set<String> mSelected = new HashSet<>();

    private void setAll(boolean on) {
        if (mBusy) return;
        mSelected.clear();
        if (on) for (AppEntry e : mVisible) mSelected.add(e.name);
        mList.getAdapter().notifyDataSetChanged();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView name, pkg, stat;
            final Button opt;
            final android.widget.CheckBox sw;
            VH(View v) {
                super(v);
                opt = v.findViewById(R.id.opt);
                name = v.findViewById(R.id.name);
                pkg = v.findViewById(R.id.pkg);
                stat = v.findViewById(R.id.stat);
                sw = v.findViewById(R.id.sw);
            }
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppEntry e = mVisible.get(pos);
            h.name.setText(e.label);
            h.pkg.setText(e.pkg);
            Integer stObj = mState.get(e.name);
            int stv = stObj == null ? ST_NONE : stObj;
            int color; int label;
            switch (stv) {
                case ST_ACTIVE:   color = it.tugaia56.oxydiantheme.utils.ThemePrefs.accentColor(); label = R.string.state_active; break;
                case ST_DISABLED: color = getColor(R.color.state_disabled); label = R.string.state_disabled; break;
                case ST_REBOOT:   color = getColor(R.color.state_not_active); label = R.string.state_reboot; break;
                case ST_INVALID:  color = getColor(R.color.state_invalid); label = R.string.state_invalid; break;
                default:          color = getColor(R.color.state_not_installed); label = R.string.state_none; break;
            }
            h.name.setTextColor(color);
            h.stat.setTextColor(color);
            h.stat.setText(getString(label));
            h.sw.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
            h.sw.setOnCheckedChangeListener(null);
            h.sw.setChecked(mSelected.contains(e.name));
            h.sw.setOnCheckedChangeListener((b, checked) -> {
                if (checked) mSelected.add(e.name); else mSelected.remove(e.name);
            });
            boolean hasOpt = !e.ds && !optionGroups(e.pkg).isEmpty();
            h.opt.setVisibility(hasOpt ? View.VISIBLE : View.GONE);
            Tint.button((com.google.android.material.button.MaterialButton) h.opt);
            h.opt.setOnClickListener(v -> showOptions(e));
            // tocco sulla riga = spunta (come in Substratum)
            h.itemView.setOnClickListener(v -> h.sw.toggle());
        }

        @Override
        public int getItemCount() { return mVisible.size(); }
    }

    // ── Opzioni dei temi ─────────────────────────────────────────────────────

    private static class OptGroup {
        String id, title;
        List<String> choices = new ArrayList<>();
    }

    private final Map<String, List<OptGroup>> mOptCache = new HashMap<>();

    private List<OptGroup> optionGroups(String pkg) {
        List<OptGroup> cached = mOptCache.get(pkg);
        if (cached != null) return cached;
        List<OptGroup> out = new ArrayList<>();
        mOptCache.put(pkg, out);
        try {
            String base = "CompileOnDemand/" + pkg + "/OPT";
            String[] groups = getAssets().list(base);
            if (groups == null) return out;
            Arrays.sort(groups);
            for (String g : groups) {
                String[] items = getAssets().list(base + "/" + g);
                if (items == null) continue;
                OptGroup og = new OptGroup();
                og.id = g;
                og.title = g;
                Arrays.sort(items);
                for (String item : items) {
                    if (item.equals("title.txt")) {
                        try (java.io.InputStream in = getAssets().open(base + "/" + g + "/title.txt")) {
                            og.title = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
                        }
                    } else {
                        og.choices.add(item);
                    }
                }
                if (!og.choices.isEmpty()) out.add(og);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** Scelte attuali sotto forma di testo (entra nella firma: se cambiano, il tema va rifatto). */
    private String optsKey(AppEntry e) {
        StringBuilder sb = new StringBuilder();
        if (e.ds && SystemColorsDialog.PKG.equals(e.pkg)) sb.append("|ripple=").append(ThemePrefs.getRippleAlpha());
        for (OptGroup g : optionGroups(e.pkg)) {
            String c = ThemePrefs.getOption(e.pkg, g.id);
            sb.append('|').append(g.id).append('=').append(c);
            if (OptionGroupsDialog.CUSTOM.equals(c)) sb.append(String.format("#%08X", ThemePrefs.getCustomColor(e.pkg + "_" + g.id)));
        }
        return sb.toString();
    }

    private static String hex8(int c) { return String.format("#%08X", c); }

    /** Colori a scelta libera del PIN (stesse risorse che creava Oxydian), o null se non servono. */
    private String customValuesXml(AppEntry e) {
        if (e.ds && SystemColorsDialog.PKG.equals(e.pkg)) {
            StringBuilder body = new StringBuilder();
            if (OptionGroupsDialog.CUSTOM.equals(ThemePrefs.getOption(e.pkg, "accent")))
                body.append(CustomPalette.accentBody(this, ThemePrefs.getCustomColor(e.pkg + "_accent")));
            if (OptionGroupsDialog.CUSTOM.equals(ThemePrefs.getOption(e.pkg, "background")))
                body.append(CustomPalette.backgroundBody(this, ThemePrefs.getCustomColor(e.pkg + "_background")));
            int rp = ThemePrefs.getRippleAlpha();
            if (rp > 0) {
                // onda del tocco: colore dell'accento con l'opacita' scelta
                Integer acc = SystemColorsDialog.currentAccent(this);
                int base = (acc != null ? acc : ThemePrefs.accentColor()) & 0xFFFFFF;
                String hex = String.format("#%02X%06X", Math.round(rp * 2.55f), base);
                String cleaned = body.toString().replaceAll("<color name=\"ripple_material_(dark|light)\">[^<]*</color>", "");
                body.setLength(0);
                body.append(cleaned);
                body.append("<color name=\"ripple_material_dark\">").append(hex).append("</color>")
                    .append("<color name=\"ripple_material_light\">").append(hex).append("</color>");
            }
            return body.length() == 0 ? null : "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>" + body + "</resources>";
        }
        if (!e.ds || !"com.android.systemui".equals(e.pkg)) return null;
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n");
        boolean any = false;
        if (OptionGroupsDialog.CUSTOM.equals(ThemePrefs.getOption(e.pkg, "pinbg"))) {
            int rgb = ThemePrefs.getCustomColor(e.pkg + "_pinbg") & 0xFFFFFF;
            String[][] m = {
                    {"coui_numeric_keyboard_border_color", hex8(0xFF000000 | rgb)},
                    {"coui_numeric_keyboard_inner_gradient_color_1", hex8(0x80000000 | rgb)},
                    {"coui_numeric_keyboard_inner_gradient_color_2", hex8(0x80000000 | rgb)},
                    {"coui_numeric_keyboard_upper_inner_shadow_color", hex8(0xFF000000 | rgb)},
                    {"coui_numeric_keyboard_outer_gradient_color_1", hex8(0xCC000000 | rgb)},
                    {"coui_numeric_keyboard_outer_gradient_color_2", hex8(0x40000000 | rgb)},
                    {"coui_numeric_keyboard_outer_gradient_color_3", hex8(0x21000000 | rgb)},
                    {"coui_simple_lock_transparent_filled_rectangle_icon_color", hex8(0xFF000000 | rgb)},
                    {"coui_simple_lock_transparent_outlined_rectangle_icon_color", "#33FFFFFF"},
                    {"coui_numeric_keyboard_dark_word_text_normal_color", hex8(0xFF000000 | rgb)},
                    {"coui_numeric_keyboard_dark_word_text_normal_light_color", hex8(0xFF000000 | rgb)},
            };
            for (String[] c : m) sb.append("    <color name=\"").append(c[0]).append("\">").append(c[1]).append("</color>\n");
            any = true;
        }
        if (OptionGroupsDialog.CUSTOM.equals(ThemePrefs.getOption(e.pkg, "pinnum"))) {
            int rgb = ThemePrefs.getCustomColor(e.pkg + "_pinnum") & 0xFFFFFF;
            sb.append("    <color name=\"coui_numeric_keyboard_number_color\">").append(hex8(0xFF000000 | rgb)).append("</color>\n");
            any = true;
        }
        return any ? sb.append("</resources>").toString() : null;
    }

    /** Percorsi negli asset delle scelte da sovrapporre alla base. */
    private List<String> optionPaths(AppEntry e) {
        List<String> out = new ArrayList<>();
        for (OptGroup g : optionGroups(e.pkg)) {
            String c = ThemePrefs.getOption(e.pkg, g.id);
            if (!c.isEmpty() && g.choices.contains(c)) out.add("CompileOnDemand/" + e.pkg + "/OPT/" + g.id + "/" + c);
        }
        return out;
    }

    private void showOptions(AppEntry e) {
        List<OptGroup> groups = optionGroups(e.pkg);
        if (groups.isEmpty()) return;
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        for (OptGroup g : groups) {
            TextView t = new TextView(this);
            t.setText(g.title);
            t.setTextColor(ThemePrefs.accentColor());
            t.setTextSize(15);
            t.setPadding(0, pad / 2, 0, pad / 4);
            box.addView(t);
            android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
            String cur = ThemePrefs.getOption(e.pkg, g.id);
            List<String> all = new ArrayList<>();
            all.add("");
            all.addAll(g.choices);
            for (String c : all) {
                android.widget.RadioButton rb = new android.widget.RadioButton(this);
                rb.setText(c.isEmpty() ? getString(R.string.options_default) : c.replace('_', ' '));
                rb.setId(View.generateViewId());
                rb.setChecked(c.equals(cur));
                rb.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
                final String choice = c;
                rb.setOnClickListener(v -> {
                    ThemePrefs.setOption(e.pkg, g.id, choice);
                    for (int i = 0; i < rg.getChildCount(); i++) {
                        android.widget.RadioButton o = (android.widget.RadioButton) rg.getChildAt(i);
                        o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : getColor(R.color.text));
                    }
                });
                rg.addView(rb);
            }
            for (int i = 0; i < rg.getChildCount(); i++) {
                android.widget.RadioButton o = (android.widget.RadioButton) rg.getChildAt(i);
                o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : getColor(R.color.text));
            }
            box.addView(rg);
        }
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(box);
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.options_title) + " – " + e.label)
                .setView(sv)
                .setPositiveButton(android.R.string.ok, null));
        Toast.makeText(this, R.string.options_note, Toast.LENGTH_SHORT).show();
    }

    // ── Applica ──────────────────────────────────────────────────────────────

    /** Stato di ogni overlay di tema nel sistema: nome completo -> "[x]", "[ ]" oppure "---". */
    private static Map<String, String> overlayStates() {
        Map<String, String> out = new HashMap<>();
        try {
            for (String line : Shell.cmd("cmd overlay list | grep " + ThemeCompiler.PREFIX).exec().getOut()) {
                line = line.trim();
                if (line.length() > 4) out.put(line.substring(3).trim(), line.substring(0, 3));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /** Overlay di tema attualmente abilitati nel sistema (nomi completi). */
    private static Set<String> enabledOverlays() {
        Set<String> out = new HashSet<>();
        try {
            for (String line : Shell.cmd("cmd overlay list | grep " + ThemeCompiler.PREFIX).exec().getOut()) {
                line = line.trim();
                if (line.startsWith("[x]")) out.add(line.substring(3).trim());
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static final int ACT_ACTIVATE = 0, ACT_DISABLE = 1, ACT_REMOVE = 2;

    /** Azione sulle voci spuntate nella lista. */
    private void runSelected(int action) {
        if (mBusy) return;
        List<AppEntry> targets = new ArrayList<>();
        for (AppEntry e : mVisible) if (mSelected.contains(e.name)) targets.add(e);
        if (targets.isEmpty()) {
            Toast.makeText(this, R.string.select_first, Toast.LENGTH_SHORT).show();
            return;
        }
        runAction(targets, action);
    }

    /** Per le schede (colori, PIN, icone...): applica solo la voce Dark Shadow indicata secondo il suo stato. */
    private void onApply(String onlyPkg, boolean ignored) {
        List<AppEntry> t = new ArrayList<>();
        for (AppEntry e : mApps) if (e.ds && onlyPkg.equals(e.pkg)) t.add(e);
        if (t.isEmpty()) return;
        runAction(t, t.get(0).enabled ? ACT_ACTIVATE : ACT_DISABLE);
    }

    private void setButtonsEnabled(boolean on) {
        mApply.setEnabled(on);
        findViewById(R.id.btn_disable).setEnabled(on);
        findViewById(R.id.btn_remove).setEnabled(on);
    }

    private void runAction(final List<AppEntry> targets, final int action) {
        if (mBusy) return;
        mBusy = true;
        setButtonsEnabled(false);
        Toast.makeText(this, R.string.working, Toast.LENGTH_SHORT).show();
        for (AppEntry e : targets) {
            e.enabled = action == ACT_ACTIVATE;   // stato voluto: acceso solo dopo Attiva
            e.saveEnabled();
        }
        final List<AppEntry> all = new ArrayList<>(mApps);
        new Thread(() -> {
            int failed = 0;
            Set<String> before = enabledOverlays();
            Map<String, String> stateBefore = overlayStates();
            List<String> toEnable = new ArrayList<>();
            String signature = signature();
            List<String> refresh = new ArrayList<>(), toDisable = new ArrayList<>(), toRemove = new ArrayList<>();
            boolean batchOpen = false;
            try {
                ModuleSetup.ensure();
                for (AppEntry e : targets) {
                    boolean active = before.contains(e.overlay());
                    if (action != ACT_ACTIVATE) {
                        if (active) toDisable.add(e.overlay());
                        if (action == ACT_REMOVE) {
                            toRemove.add(e.name);
                            e.clearBuilt();
                        }
                        continue;
                    }
                    boolean known = "[ ]".equals(stateBefore.get(e.overlay()));
                    if (known) toEnable.add(e.overlay());
                    // gia' compilata (attiva o solo spenta) con lo stesso accento/versione: si salta
                    if ((active || known) && (signature + optsKey(e)).equals(e.builtSig())) continue;
                    if (!batchOpen) { ThemeCompiler.beginBatch(); batchOpen = true; }
                    try {
                        if (ThemeCompiler.buildNamedInBatch(e.pkg, e.dir, e.name, optionPaths(e), customValuesXml(e))) {
                            failed++;
                        } else {
                            e.setBuilt(signature + optsKey(e));
                            if (active) refresh.add(e.overlay());
                        }
                    } catch (Throwable t) {
                        android.util.Log.e("OxyTheme", "build " + e.name + " failed", t);
                        failed++;
                    }
                }
            } catch (Throwable t) {
                android.util.Log.e("OxyTheme", "action failed", t);
            } finally {
                if (batchOpen) ThemeCompiler.endBatch(refresh);
            }
            ThemeCompiler.disable(toDisable);
            if (!toEnable.isEmpty())
                it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil.enableOverlays(toEnable.toArray(new String[0]));
            ThemeCompiler.removeNamedApks(toRemove);
            // elenco dei temi da accendere ad ogni avvio (lo legge il modulo)
            List<String> wanted = new ArrayList<>();
            for (AppEntry e : all) if (e.enabled) wanted.add(e.overlay());
            ThemeCompiler.writeEnabledList(wanted);

            Set<String> after = enabledOverlays();
            int notYetActive = 0;
            if (action == ACT_ACTIVATE) for (AppEntry e : targets) if (!after.contains(e.overlay())) notYetActive++;
            final int f = failed, n = notYetActive, removedCount = toRemove.size();
            new Handler(Looper.getMainLooper()).post(() -> {
                mBusy = false;
                setButtonsEnabled(true);
                mSelected.clear();
                String msg;
                if (f > 0) msg = getString(R.string.failed_n, f);
                else if (n > 0) msg = getString(R.string.applied_reboot);
                else msg = getString(R.string.applied);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                refreshStates();
                updateNotice();
                if (removedCount > 0) {
                    Dialogs.show(MainActivity.this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle(R.string.off_title)
                            .setMessage(R.string.off_message)
                            .setPositiveButton(android.R.string.ok, null));
                }
            });
        }).start();
    }

    /** Cambia se si reinstalla l'app o si cambia accento: allora gli overlay vanno rifatti. */
    private String signature() {
        long updated = 0;
        try {
            updated = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime;
        } catch (Exception ignored) {}
        return updated + ":" + String.format("%08X", ThemePrefs.accentColor());
    }
}
