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
    };

    private final List<AppEntry> mApps = new ArrayList<>();
    private RecyclerView mList;
    private TextView mStatus, mNotice;
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
        mNotice = findViewById(R.id.notice);
        mApply = findViewById(R.id.btn_apply);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(new Adapter());

        mApply.setOnClickListener(v -> onApply());
        findViewById(R.id.btn_legend).setOnClickListener(v -> showLegend());
        findViewById(R.id.btn_styles).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, StyleActivity.class)));
        ((Button) findViewById(R.id.btn_all)).setOnClickListener(v -> setAll(true));
        ((Button) findViewById(R.id.btn_none)).setOnClickListener(v -> setAll(false));

        loadApps();
        setupModuleAsync();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.legend_title)
                .setView(box)
                .setPositiveButton(R.string.legend_close, null)
                .show();
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
                dsList.add(new AppEntry(t[0], String.valueOf(pm.getApplicationLabel(ai)), t[1],
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
        String notice = getString(R.string.notice);
        if (selected > 10) notice += "\n\n" + getString(R.string.many_warning, selected);
        mNotice.setText(notice);
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
            final MaterialSwitch sw;
            VH(View v) {
                super(v);
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
            h.stat.setText(label);
            h.sw.setOnCheckedChangeListener(null);
            h.sw.setChecked(e.enabled);
            h.sw.setOnCheckedChangeListener((b, checked) -> {
                e.enabled = checked;
                e.saveEnabled();
                updateNotice();
            });
            h.itemView.setOnClickListener(v -> h.sw.toggle());
        }

        @Override
        public int getItemCount() { return mApps.size(); }
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

    private void onApply() {
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
                    boolean active = before.contains(e.overlay());
                    if (!e.enabled) {
                        if (active) toDisable.add(e.overlay());
                        toRemove.add(e.name);
                        e.clearBuilt();
                        continue;
                    }
                    if ("[ ]".equals(stateBefore.get(e.overlay()))) toEnable.add(e.overlay());
                    // gia' compilata e attiva con lo stesso accento/versione: si salta
                    if (active && signature.equals(e.builtSig())) continue;
                    if (!batchOpen) { ThemeCompiler.beginBatch(); batchOpen = true; }
                    try {
                        if (ThemeCompiler.buildNamedInBatch(e.pkg, e.dir, e.name)) {
                            failed++;
                        } else {
                            e.setBuilt(signature);
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
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle(R.string.off_title)
                            .setMessage(R.string.off_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
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
