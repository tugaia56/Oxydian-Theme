package it.tugaia56.oxydiantheme.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ModuleSetup;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil;
import it.tugaia56.oxydiantheme.utils.overlay.compiler.ThemeCompiler;

/**
 * Stili delle icone Wi-Fi e segnale mobile di SystemUI, come overlay (WIFI1 / SIG1).
 * Un solo stile per icona; "Predefinito" toglie l'overlay.
 */
public class StyleActivity extends AppCompatActivity {
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String[] SLOTS = {"WIFI1", "SIG1"};
    private static final String[] PREFIXES = {"WIFI_", "SIG_"};

    /** Riga: titolo di sezione (slot>=0, style==null e header) oppure stile. */
    private static class Row {
        final boolean header;
        final int slot;
        final String style; // "" = predefinito
        final String label;
        Row(boolean header, int slot, String style, String label) {
            this.header = header; this.slot = slot; this.style = style; this.label = label;
        }
    }

    private final List<Row> mRows = new ArrayList<>();
    private final List<Row> mShown = new ArrayList<>();
    private final boolean[] mOpen = new boolean[3]; // sezioni: Wi-Fi+segnale, Wi-Fi, segnale (indice = slot)
    private final String[] mChoice = new String[2];
    private boolean mBusy = false;
    private Button mApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_styles);
        mApply = findViewById(R.id.btn_apply);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        for (int i = 0; i < 2; i++) mChoice[i] = ThemePrefs.getStyle(SLOTS[i]);
        buildRows();
        refreshShown();
        list.setAdapter(new Adapter());
        mApply.setOnClickListener(v -> onApply());
    }

    private void buildRows() {
        String[] dirs = new String[0];
        try {
            String[] l = getAssets().list("CompileOnDemand/" + SYSTEMUI);
            if (l != null) dirs = l;
        } catch (Exception ignored) {}
        Arrays.sort(dirs);
        // Sezione "insieme": gli stili presenti sia per Wi-Fi sia per segnale
        mRows.add(new Row(true, 2, null, getString(R.string.section_both)));
        mRows.add(new Row(false, 2, "", getString(R.string.style_none)));
        List<String> all = Arrays.asList(dirs);
        for (String d : dirs) {
            if (!d.startsWith(PREFIXES[0])) continue;
            String name = d.substring(PREFIXES[0].length());
            if (all.contains(PREFIXES[1] + name)) mRows.add(new Row(false, 2, name, pretty(name)));
        }
        int[] titles = {R.string.section_wifi, R.string.section_signal};
        for (int s = 0; s < 2; s++) {
            mRows.add(new Row(true, s, null, getString(titles[s])));
            mRows.add(new Row(false, s, "", getString(R.string.style_none)));
            for (String d : dirs) {
                if (!d.startsWith(PREFIXES[s])) continue;
                String name = d.substring(PREFIXES[s].length());
                mRows.add(new Row(false, s, name, pretty(name)));
            }
        }
    }

    /** Solo le righe delle sezioni aperte (le intestazioni si vedono sempre). */
    private void refreshShown() {
        mShown.clear();
        for (Row r : mRows) if (r.header || mOpen[r.slot]) mShown.add(r);
    }

    private String currentLabel(int slot) {
        if (slot == 2) {
            return mChoice[0].isEmpty() || !mChoice[0].equals(mChoice[1]) ? "" : pretty(mChoice[0]);
        }
        return mChoice[slot].isEmpty() ? "" : pretty(mChoice[slot]);
    }

    private static String pretty(String n) {
        String[] parts = n.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1));
        }
        return sb.toString();
    }

    private class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override public int getItemViewType(int pos) { return mShown.get(pos).header ? 0 : 1; }

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == 0) {
                TextView t = new TextView(parent.getContext());
                t.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                int pad = (int) (12 * getResources().getDisplayMetrics().density);
                t.setPadding(pad, pad * 2, pad, pad / 2);
                t.setTextSize(15);
                t.setTextColor(it.tugaia56.oxydiantheme.utils.ThemePrefs.accentColor());
                return new RecyclerView.ViewHolder(t) {};
            }
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_style, parent, false)) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            Row r = mShown.get(pos);
            if (r.header) {
                String cur = currentLabel(r.slot);
                ((TextView) h.itemView).setText((mOpen[r.slot] ? "▾  " : "▸  ") + r.label
                        + (cur.isEmpty() ? "" : "  ·  " + cur));
                h.itemView.setOnClickListener(v -> {
                    mOpen[r.slot] = !mOpen[r.slot];
                    refreshShown();
                    notifyDataSetChanged();
                });
                return;
            }
            boolean sel = r.slot == 2
                    ? (r.style.equals(mChoice[0]) && r.style.equals(mChoice[1]))
                    : r.style.equals(mChoice[r.slot]);
            TextView nameView = h.itemView.findViewById(R.id.name);
            nameView.setText(r.label);
            nameView.setTextColor(sel ? ThemePrefs.accentColor() : getColor(R.color.text));
            RadioButton rb = h.itemView.findViewById(R.id.radio);
            rb.setChecked(sel);
            h.itemView.setOnClickListener(v -> {
                if (mBusy) return;
                if (r.slot == 2) { mChoice[0] = r.style; mChoice[1] = r.style; }
                else mChoice[r.slot] = r.style;
                refreshShown();
                notifyDataSetChanged();
            });
        }

        @Override public int getItemCount() { return mShown.size(); }
    }

    private static Map<String, String> states() {
        Map<String, String> out = new HashMap<>();
        try {
            for (String line : Shell.cmd("cmd overlay list | grep " + ThemeCompiler.PREFIX).exec().getOut()) {
                line = line.trim();
                if (line.length() > 4) out.put(line.substring(3).trim(), line.substring(0, 3));
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static boolean batchOpenFinal(List<String> a, List<String> b, List<String> c) {
        return !a.isEmpty() || !b.isEmpty() || !c.isEmpty();
    }

    private void onApply() {
        if (mBusy) return;
        mBusy = true;
        mApply.setEnabled(false);
        Toast.makeText(this, R.string.working, Toast.LENGTH_SHORT).show();
        final String[] choice = mChoice.clone();
        new Thread(() -> {
            int failed = 0;
            boolean removed = false, notActive = false;
            Map<String, String> before = states();
            List<String> refresh = new ArrayList<>(), toEnable = new ArrayList<>(),
                    toDisable = new ArrayList<>(), toRemove = new ArrayList<>();
            long updated = 0;
            try { updated = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime; } catch (Exception ignored) {}
            String accent = String.format("%08X", ThemePrefs.accentColor()) + ":" + updated;
            boolean batchOpen = false;
            try {
                ModuleSetup.ensure();
                for (int i = 0; i < 2; i++) {
                    String slot = SLOTS[i], ov = ThemeCompiler.namedPackage(slot);
                    String st = before.get(ov);
                    boolean active = "[x]".equals(st);
                    ThemePrefs.setStyle(slot, choice[i]);
                    if (choice[i].isEmpty()) {
                        if (active) toDisable.add(ov);
                        if (st != null) removed = true;
                        toRemove.add(slot);
                        ThemePrefs.setStyleBuilt(slot, "");
                        continue;
                    }
                    String sig = choice[i] + ":" + accent;
                    if ("[ ]".equals(st)) toEnable.add(ov);
                    if (active && sig.equals(ThemePrefs.getStyleBuilt(slot))) continue;
                    if (!batchOpen) { ThemeCompiler.beginBatch(); batchOpen = true; }
                    try {
                        if (ThemeCompiler.buildNamedInBatch(SYSTEMUI, PREFIXES[i] + choice[i], slot)) {
                            failed++;
                        } else {
                            ThemePrefs.setStyleBuilt(slot, sig);
                            if (active) refresh.add(ov);
                            else if (st == null) notActive = true;
                        }
                    } catch (Throwable t) {
                        failed++;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (batchOpen) ThemeCompiler.endBatch(refresh);
            }
            ThemeCompiler.disable(toDisable);
            if (!toEnable.isEmpty()) OverlayUtil.enableOverlays(toEnable.toArray(new String[0]));
            ThemeCompiler.removeNamedApks(toRemove);
            // la barra di stato non rilegge da sola le icone cambiate: la si riavvia (schermo di blocco per un attimo)
            if (batchOpenFinal(refresh, toEnable, toDisable)) Shell.cmd("killall com.android.systemui").exec();

            final int f = failed;
            final boolean fRemoved = removed, fNeedReboot = notActive;
            new Handler(Looper.getMainLooper()).post(() -> {
                mBusy = false;
                mApply.setEnabled(true);
                String msg;
                if (f > 0) msg = getString(R.string.failed_n, f);
                else if (fNeedReboot) msg = getString(R.string.applied_reboot);
                else msg = getString(R.string.applied);
                Toast.makeText(StyleActivity.this, msg, Toast.LENGTH_LONG).show();
                if (fRemoved) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(StyleActivity.this)
                            .setTitle(R.string.off_title)
                            .setMessage(R.string.styles_off_note)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });
        }).start();
    }
}
