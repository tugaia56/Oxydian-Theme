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
    private RecyclerView mList;
    private TextView mStatus;
    private android.widget.ImageButton mWarn;
    private int mWarnCount = 0;
    private Button mApply;
    private boolean mBusy = false;

    // Stato di ogni app, con gli stessi colori di Substratum
    private static final int ST_NONE = 0, ST_ACTIVE = 1, ST_DISABLED = 2, ST_REBOOT = 3, ST_INVALID = 4;
    private final Map<String, Integer> mState = new HashMap<>();

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

        mApply.setOnClickListener(v -> onApply(null, false));
        findViewById(R.id.btn_remove_off).setOnClickListener(v -> onApply(null, true));
        findViewById(R.id.btn_info).setOnClickListener(v -> showLegend());
        ((android.widget.ImageButton) findViewById(R.id.btn_info)).setImageTintList(
                android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        setupCards();
        ((Button) findViewById(R.id.btn_all)).setOnClickListener(v -> setAll(true));
        ((Button) findViewById(R.id.btn_none)).setOnClickListener(v -> setAll(false));

        loadApps();
        setupModuleAsync();
        Tint.tree(findViewById(android.R.id.content));
    }

    // ── Schede dell'intestazione ─────────────────────────────────────────────

    private final List<View> mCards = new ArrayList<>();
    private TextView mSubColors, mSubWifi, mSubNav, mSubSettings;

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

    private void setupCards() {
        android.widget.LinearLayout r1 = findViewById(R.id.cards_row1);
        android.widget.LinearLayout r2 = findViewById(R.id.cards_row2);
        TextView[] s = new TextView[1];
        r1.addView(makeCard(R.string.card_colors, s, this::openColors));
        mSubColors = s[0];
        r1.addView(makeCard(R.string.card_wifi, s, () -> openStyles("wifi")));
        mSubWifi = s[0];
        r2.addView(makeCard(R.string.card_nav, s, () -> openStyles("nav")));
        mSubNav = s[0];
        r2.addView(makeCard(R.string.card_settings, s,
                () -> Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()));
        mSubSettings = s[0];
        mSubSettings.setText(R.string.coming_soon);
        refreshCards();
    }

    private void openStyles(String mode) {
        startActivity(new android.content.Intent(this, StyleActivity.class).putExtra("mode", mode));
    }

    private void openColors() {
        SystemColorsDialog.show(this, color -> {
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
        ((android.widget.ImageButton) findViewById(R.id.btn_info)).setImageTintList(
                android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
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
        StringBuilder col = new StringBuilder();
        if (!acc.isEmpty()) col.append(getString(R.string.sys_accent)).append(' ').append(pretty(acc));
        if (!bg.isEmpty()) {
            if (col.length() > 0) col.append(" · ");
            col.append(getString(R.string.sys_background)).append(' ').append(pretty(bg));
        }
        mSubColors.setText(col.length() == 0 ? def : col.toString());
        String w = ThemePrefs.getStyle("WIFI1"), sg = ThemePrefs.getStyle("SIG1");
        if (w.isEmpty() && sg.isEmpty()) mSubWifi.setText(def);
        else if (w.equals(sg)) mSubWifi.setText(pretty(w));
        else mSubWifi.setText("Wi-Fi " + (w.isEmpty() ? "–" : pretty(w)) + " · " + getString(R.string.sys_signal)
                + " " + (sg.isEmpty() ? "–" : pretty(sg)));
        String nv = ThemePrefs.getStyle("NAV1");
        mSubNav.setText(nv.isEmpty() ? def : pretty(nv));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSubColors != null) refreshCards();
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
        mList.getAdapter().notifyDataSetChanged();
        updateNotice();
    }

    private void updateNotice() {
        int selected = 0;
        for (AppEntry e : mApps) if (e.enabled) selected++;
        mWarnCount = selected;
        if (mWarn != null) mWarn.setVisibility(selected > 10 ? View.VISIBLE : View.GONE);
    }

    private void showWarning() {
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.warn_title)
                .setMessage(getString(R.string.many_warning, mWarnCount))
                .setPositiveButton(android.R.string.ok, null));
    }

    private void setAll(boolean on) {
        if (mBusy) return;
        for (AppEntry e : mApps) { e.enabled = on; e.saveEnabled(); }
        mList.getAdapter().notifyDataSetChanged();
        updateNotice();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView name, pkg, stat;
            final Button opt;
            final MaterialSwitch sw;
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
            AppEntry e = mApps.get(pos);
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
            boolean pending = (e.enabled && (stv == ST_NONE || stv == ST_DISABLED || stv == ST_INVALID))
                    || (!e.enabled && stv == ST_ACTIVE);
            h.stat.setText(pending ? getString(label) + "  •  " + getString(R.string.pending) : getString(label));
            Tint.sw(h.sw);
            h.sw.setOnCheckedChangeListener(null);
            h.sw.setChecked(e.enabled);
            h.sw.setOnCheckedChangeListener((b, checked) -> {
                e.enabled = checked;
                e.saveEnabled();
                updateNotice();
            });
            boolean hasOpt = !optionGroups(e.pkg).isEmpty();
            h.opt.setVisibility(hasOpt ? View.VISIBLE : View.GONE);
            Tint.button((com.google.android.material.button.MaterialButton) h.opt);
            h.opt.setOnClickListener(v -> showOptions(e));
            h.itemView.setOnClickListener(null);
            h.itemView.setClickable(false);
        }

        @Override
        public int getItemCount() { return mApps.size(); }
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
        for (OptGroup g : optionGroups(e.pkg)) sb.append('|').append(g.id).append('=').append(ThemePrefs.getOption(e.pkg, g.id));
        return sb.toString();
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

    /** @param onlyName se non nullo, applica solo la voce con questo pacchetto bersaglio (le altre restano come sono) */
    private void onApply(String onlyPkg, boolean removeOff) {
        if (mBusy) return;
        mBusy = true;
        mApply.setEnabled(false);
        Toast.makeText(this, R.string.working, Toast.LENGTH_SHORT).show();
        final List<AppEntry> apps = new ArrayList<>(mApps);
        new Thread(() -> {
            int failed = 0;
            Set<String> before = enabledOverlays();
            Map<String, String> stateBefore = overlayStates();
            List<String> toEnable = new ArrayList<>(); // conosciuti dal sistema ma spenti
            String signature = signature();
            List<String> refresh = new ArrayList<>();
            List<String> toDisable = new ArrayList<>();
            List<String> toRemove = new ArrayList<>();
            boolean batchOpen = false;
            try {
                ModuleSetup.ensure();
                for (AppEntry e : apps) {
                    if (onlyPkg != null && !(e.ds && onlyPkg.equals(e.pkg))) continue;
                    boolean active = before.contains(e.overlay());
                    if (!e.enabled) {
                        if (active) toDisable.add(e.overlay());
                        toRemove.add(e.name);
                        e.clearBuilt();
                        continue;
                    }
                    if ("[ ]".equals(stateBefore.get(e.overlay()))) toEnable.add(e.overlay());
                    // gia' compilata e attiva con lo stesso accento/versione: si salta
                    if (active && (signature + optsKey(e)).equals(e.builtSig())) continue;
                    if (!batchOpen) { ThemeCompiler.beginBatch(); batchOpen = true; }
                    try {
                        if (ThemeCompiler.buildNamedInBatch(e.pkg, e.dir, e.name, optionPaths(e))) {
                            failed++;
                        } else {
                            e.setBuilt(signature + optsKey(e));
                            if (active) refresh.add(e.overlay());
                        }
                    } catch (Throwable t) {
                        failed++;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (batchOpen) ThemeCompiler.endBatch(refresh);
            }
            final int turnedOff = toDisable.size();
            final int removedCount = toRemove.size();
            ThemeCompiler.disable(toDisable);
            if (!toEnable.isEmpty())
                it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil.enableOverlays(toEnable.toArray(new String[0]));
            ThemeCompiler.removeNamedApks(toRemove);

            Set<String> after = enabledOverlays();
            int notYetActive = 0, selected = 0;
            for (AppEntry e : apps) {
                if (!e.enabled) continue;
                selected++;
                if (!after.contains(e.overlay())) notYetActive++;
            }
            final int f = failed, n = notYetActive, sel = selected;
            new Handler(Looper.getMainLooper()).post(() -> {
                mBusy = false;
                mApply.setEnabled(true);
                String msg;
                if (f > 0) msg = getString(R.string.failed_n, f);
                else if (n > 0) msg = getString(R.string.applied_reboot);
                else msg = getString(R.string.applied);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                refreshStates();
                if (turnedOff > 0) {
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
