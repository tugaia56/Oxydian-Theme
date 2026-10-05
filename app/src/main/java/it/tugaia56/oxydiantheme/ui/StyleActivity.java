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
    private static final String SETTINGS = "com.android.settings";
    private static final String[] TARGET_PKG = {SYSTEMUI, SYSTEMUI, SYSTEMUI, SETTINGS, "it.tugaia56.oxydian"};
    private static final String[] SLOTS = {"WIFI1", "SIG1", "NAV1", "ICON1", "ICON2"};
    /** indice "insieme" (Wi-Fi + segnale) nelle sezioni */
    private static final int BOTH = 9;
    private static final String[] PREFIXES = {"WIFI_", "SIG_", "NAV_", "ICP_", "ICP_"};

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
    private final boolean[] mOpen = new boolean[10]; // sezioni: Wi-Fi+segnale, Wi-Fi, segnale (indice = slot)
    private final String[] mChoice = new String[5];
    private boolean mBusy = false;
    private String mMode = "wifi";
    private Button mApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_styles);
        mApply = findViewById(R.id.btn_apply);
        String m = getIntent().getStringExtra("mode");
        if (m != null) mMode = m;
        Button optBtn = findViewById(R.id.btn_icon_options);
        optBtn.setVisibility("settings".equals(mMode) ? View.VISIBLE : View.GONE);
        optBtn.setOnClickListener(v -> showIconOptions());
        android.widget.ImageButton back = findViewById(R.id.btn_back);
        back.setImageTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        back.setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.title)).setText("nav".equals(mMode) ? R.string.card_nav : "settings".equals(mMode) ? R.string.card_settings : R.string.card_wifi);
        if ("nav".equals(mMode)) mOpen[2] = true;
        if ("settings".equals(mMode)) mOpen[3] = true;
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        for (int i = 0; i < SLOTS.length; i++) mChoice[i] = ThemePrefs.getStyle(SLOTS[i]);
        buildRows();
        refreshShown();
        list.setAdapter(new Adapter());
        mApply.setOnClickListener(v -> onApply());
        Tint.tree(findViewById(android.R.id.content));
    }

    private void buildRows() {
        String[] dirs = new String[0];
        try {
            String[] l = getAssets().list("CompileOnDemand/" + SYSTEMUI);
            String[] l2 = getAssets().list("CompileOnDemand/" + SETTINGS);
            List<String> both = new ArrayList<>();
            if (l != null) both.addAll(Arrays.asList(l));
            if (l2 != null) both.addAll(Arrays.asList(l2));
            dirs = both.toArray(new String[0]);
        } catch (Exception ignored) {}
        Arrays.sort(dirs);
        // Sezione "insieme": gli stili presenti sia per Wi-Fi sia per segnale
        if ("wifi".equals(mMode)) mRows.add(new Row(true, BOTH, null, getString(R.string.section_both)));
        if ("wifi".equals(mMode)) mRows.add(new Row(false, BOTH, "", getString(R.string.style_none)));
        List<String> all = Arrays.asList(dirs);
        for (String d : dirs) {
            if (!d.startsWith(PREFIXES[0])) continue;
            String name = d.substring(PREFIXES[0].length());
            if ("wifi".equals(mMode) && all.contains(PREFIXES[1] + name)) mRows.add(new Row(false, BOTH, name, pretty(name)));
        }
        int[] titles = {R.string.section_wifi, R.string.section_signal, R.string.section_nav, R.string.card_settings};
        for (int s = 0; s < SLOTS.length; s++) {
            if (s == 4) continue; // ICON2 (icona di Oxydian in Impostazioni) segue ICON1
            boolean inMode = s <= 1 ? "wifi".equals(mMode) : s == 2 ? "nav".equals(mMode) : "settings".equals(mMode);
            if (!inMode) continue;
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
        if (slot == BOTH) {
            return mChoice[0].isEmpty() || !mChoice[0].equals(mChoice[1]) ? "" : pretty(mChoice[0]);
        }
        return mChoice[slot].isEmpty() ? "" : pretty(mChoice[slot]);
    }

    private static String pretty(String n) {
        if (n.equals("oneui")) return "One UI";
        switch (n) {
            case "pui_v1": return "PUI v1";
            case "pui_v2": return "PUI v2";
            case "pui_v3": return "PUI v3";
            case "hos": return "HOS";
            case "oos": return "OOS";
            case "oos_stock": return "OOS Stock";
            default: break;
        }
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
            boolean sel = r.slot == BOTH
                    ? (r.style.equals(mChoice[0]) && r.style.equals(mChoice[1]))
                    : r.style.equals(mChoice[r.slot]);
            TextView nameView = h.itemView.findViewById(R.id.name);
            nameView.setText(r.label);
            nameView.setTextColor(sel ? ThemePrefs.accentColor() : getColor(R.color.text));
            fillPreviews(h.itemView.findViewById(R.id.previews), r, sel);
            RadioButton rb = h.itemView.findViewById(R.id.radio);
            rb.setChecked(sel);
            h.itemView.setOnClickListener(v -> {
                if (mBusy) return;
                if (r.slot == BOTH) { mChoice[0] = r.style; mChoice[1] = r.style; }
                else mChoice[r.slot] = r.style;
                refreshShown();
                notifyDataSetChanged();
            });
        }

        @Override public int getItemCount() { return mShown.size(); }
    }

    /** Anteprima: le icone dello stile (stesse risorse che finiscono nell'overlay), colorate come la barra. */
    private void fillPreviews(android.widget.LinearLayout box, Row r, boolean selected) {
        box.removeAllViews();
        if (r.style.isEmpty()) { box.setVisibility(View.GONE); return; }
        box.setVisibility(View.VISIBLE);
        List<String> names = new ArrayList<>();
        if (r.slot == 0 || r.slot == BOTH) {
            for (int i : r.slot == 0 ? new int[]{1, 2, 3, 4} : new int[]{2, 4}) names.add("pv_wifi_" + r.style + "_" + i);
        }
        if (r.slot == 1 || r.slot == BOTH) {
            for (int i : r.slot == 1 ? new int[]{1, 2, 3, 4} : new int[]{2, 4}) names.add("pv_sig_" + r.style + "_" + i);
        }
        if (r.slot == 2) {
            for (String part : new String[]{"back", "home", "recent"}) names.add("pv_nav_" + r.style + "_" + part);
        }
        if (r.slot == 3) {
            for (String part : new String[]{"wifi", "wallpaper", "battery", "about"}) names.add("pv_set_" + r.style + "_" + part);
        }
        float d = getResources().getDisplayMetrics().density;
        int size = (int) (22 * d), gap = (int) (10 * d);
        int tint = selected ? ThemePrefs.accentColor() : getColor(R.color.text);
        for (String n : names) {
            int id = getResources().getIdentifier(n, "drawable", getPackageName());
            if (id == 0) continue;
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(id);
            if (r.slot != 3) {
                iv.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
            } else if ("oos".equals(r.style)) {
                // OOS: icone bianche dentro un cerchio con l'accento
                iv.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(0x00000000);
                circle.setStroke((int) (1.5f * d), ThemePrefs.accentColor());
                iv.setBackground(circle);
                int pp = (int) (4 * d);
                iv.setPadding(pp, pp, pp, pp);
            } else if (!"oos_stock".equals(r.style)) {
                // PUI e HOS: tinta con l'accento. OOS Stock: colori originali
                iv.setImageTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
            }
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(gap);
            box.addView(iv, lp);
        }
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

    // ── Pack icone Impostazioni: risorse dinamiche e opzioni ─────────────────

    private static final int OPT_BG_COLOR = 0, OPT_SOLID = 1, OPT_SHAPE = 2, OPT_ICON_COLOR = 3;

    private String iconOptsKey() {
        return ThemePrefs.getIconOpt("bgcolor", 0) + "," + ThemePrefs.getIconOpt("solid", 0) + ","
                + ThemePrefs.getIconOpt("shape", 0) + "," + ThemePrefs.getIconOpt("iconcolor", 0);
    }

    private static String colorHex(int choice) {
        switch (choice) {
            case 0: return String.format("#%08X", ThemePrefs.accentColor());
            case 1: return "#FFFFFF";
            default: return "#000000";
        }
    }

    /** Stesse risorse (res/values/Obsidian.xml) che costruiva Oxydian per ogni pack. */
    private String settingsValuesXml(String pack) {
        int bgColor = ThemePrefs.getIconOpt("bgcolor", 0);
        boolean solid = ThemePrefs.getIconOpt("solid", 0) == 1;
        int shape = ThemePrefs.getIconOpt("shape", 0);
        int iconColor = ThemePrefs.getIconOpt("iconcolor", 0);
        String head = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n";
        if ("oos_stock".equals(pack)) {
            return head + "    <color name=\"bg_color\">#00000000</color>\n"
                    + "    <color name=\"solid_bg_color\">#00000000</color>\n"
                    + "    <dimen name=\"top_left\">0dp</dimen>\n    <dimen name=\"top_right\">0dp</dimen>\n"
                    + "    <dimen name=\"bottom_left\">0dp</dimen>\n    <dimen name=\"bottom_right\">0dp</dimen>\n</resources>";
        }
        if (pack.startsWith("pui_")) {
            return head + "    <color name=\"monet_color\">" + colorHex(iconColor) + "</color>\n</resources>";
        }
        StringBuilder sb = new StringBuilder(head);
        sb.append("    <color name=\"bg_color\">").append(colorHex(bgColor)).append("</color>\n");
        sb.append("    <color name=\"solid_bg_color\">").append(solid ? colorHex(bgColor) : "#00000000").append("</color>\n");
        sb.append("    <color name=\"icon_color\">").append(colorHex(iconColor)).append("</color>\n");
        String[] c;
        switch (shape) {
            case 1: c = new String[]{"15dp", "15dp", "15dp", "15dp"}; break;
            case 2: c = new String[]{"4.0dp", "4.0dp", "4.0dp", "4.0dp"}; break;
            case 3: c = new String[]{"90.0dp", "90.0dp", "90.0dp", "24.0dp"}; break;
            case 4: c = new String[]{"2.0dp", "14.0dp", "14.0dp", "2.0dp"}; break;
            default: c = new String[]{"18dp", "18dp", "18dp", "18dp"}; break;
        }
        sb.append("    <dimen name=\"top_left\">").append(c[0]).append("</dimen>\n");
        sb.append("    <dimen name=\"top_right\">").append(c[1]).append("</dimen>\n");
        sb.append("    <dimen name=\"bottom_left\">").append(c[2]).append("</dimen>\n");
        sb.append("    <dimen name=\"bottom_right\">").append(c[3]).append("</dimen>\n");
        return sb.append("</resources>").toString();
    }

    private void addGroup(android.widget.LinearLayout box, int titleRes, String optKey, int[] entryRes, int pad) {
        TextView t = new TextView(this);
        t.setText(titleRes);
        t.setTextColor(ThemePrefs.accentColor());
        t.setTextSize(15);
        t.setPadding(0, pad / 2, 0, pad / 4);
        box.addView(t);
        android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
        int cur = ThemePrefs.getIconOpt(optKey, 0);
        for (int i = 0; i < entryRes.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(entryRes[i]);
            rb.setId(View.generateViewId());
            rb.setChecked(i == cur);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
            final int idx = i;
            rb.setOnClickListener(v -> {
                ThemePrefs.setIconOpt(optKey, idx);
                for (int k = 0; k < rg.getChildCount(); k++) {
                    android.widget.RadioButton o = (android.widget.RadioButton) rg.getChildAt(k);
                    o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : getColor(R.color.text));
                }
            });
            rg.addView(rb);
        }
        for (int k = 0; k < rg.getChildCount(); k++) {
            android.widget.RadioButton o = (android.widget.RadioButton) rg.getChildAt(k);
            o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : getColor(R.color.text));
        }
        box.addView(rg);
    }

    private void showIconOptions() {
        String pack = mChoice[3];
        if (pack.isEmpty() || "oos_stock".equals(pack)) {
            Toast.makeText(this, pack.isEmpty() ? R.string.icopt_pick_first : R.string.icopt_none, Toast.LENGTH_LONG).show();
            return;
        }
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        int[] colors = {R.string.icopt_accent, R.string.icopt_white, R.string.icopt_black};
        if (!pack.startsWith("pui_")) {
            addGroup(box, R.string.icopt_bg_color, "bgcolor", colors, pad);
            addGroup(box, R.string.icopt_solid, "solid", new int[]{R.string.icopt_no, R.string.icopt_yes}, pad);
            addGroup(box, R.string.icopt_shape, "shape", new int[]{R.string.icopt_circle, R.string.icopt_squircle,
                    R.string.icopt_rounded, R.string.icopt_teardrop, R.string.icopt_diamond}, pad);
        }
        addGroup(box, R.string.icopt_icon_color, "iconcolor", colors, pad);
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(box);
        Dialogs.show(this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.icopt_title)
                .setView(sv)
                .setPositiveButton(android.R.string.ok, null));
    }

    private void onApply() {
        if (mBusy) return;
        mBusy = true;
        mApply.setEnabled(false);
        Toast.makeText(this, R.string.working, Toast.LENGTH_SHORT).show();
        final String[] choice = mChoice.clone();
        choice[4] = choice[3];
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
                for (int i = 0; i < SLOTS.length; i++) {
                    if (i == 4) {
                        // l'icona di Oxydian dentro Impostazioni: solo se Oxydian e' installato
                        try { getPackageManager().getPackageInfo(TARGET_PKG[4], 0); }
                        catch (Exception notInstalled) { continue; }
                    }
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
                    String sig = choice[i] + ":" + accent + ((i == 3 || i == 4) ? ":" + iconOptsKey() : "");
                    if ("[ ]".equals(st)) toEnable.add(ov);
                    if (active && sig.equals(ThemePrefs.getStyleBuilt(slot))) continue;
                    if (!batchOpen) { ThemeCompiler.beginBatch(); batchOpen = true; }
                    try {
                        if (ThemeCompiler.buildNamedInBatch(TARGET_PKG[i], PREFIXES[i] + choice[i], slot, null,
                                (i == 3 || i == 4) ? settingsValuesXml(choice[i]) : null)) {
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
                    Dialogs.show(StyleActivity.this, new com.google.android.material.dialog.MaterialAlertDialogBuilder(StyleActivity.this)
                            .setTitle(R.string.off_title)
                            .setMessage(R.string.styles_off_note)
                            .setPositiveButton(android.R.string.ok, null));
                }
            });
        }).start();
    }
}
