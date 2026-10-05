package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/** Finestra a scelta singola per uno o piu' gruppi di opzioni (cartelle OPT/&lt;gruppo&gt; negli asset). */
final class OptionGroupsDialog {
    interface Listener { void onSaved(); }

    private OptionGroupsDialog() {}

    static String pretty(String n) {
        if (n == null || n.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1));
        }
        return sb.toString();
    }

    /** Scelte di un gruppo (nomi delle cartelle), nell'ordine alfabetico. */
    static List<String> choices(Context ctx, String pkg, String group) {
        List<String> out = new ArrayList<>();
        try {
            String[] items = ctx.getAssets().list("CompileOnDemand/" + pkg + "/OPT/" + group);
            if (items != null) {
                Arrays.sort(items);
                for (String i : items) if (!i.equals("title.txt")) out.add(i);
            }
        } catch (Exception ignored) {}
        return out;
    }

    static String title(Context ctx, String pkg, String group) {
        try (InputStream in = ctx.getAssets().open("CompileOnDemand/" + pkg + "/OPT/" + group + "/title.txt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return group;
        }
    }

    /** Testo riassuntivo delle scelte attuali (per la scheda). */
    static String summary(Context ctx, String pkg, String... groups) {
        StringBuilder sb = new StringBuilder();
        for (String g : groups) {
            String c = ThemePrefs.getOption(pkg, g);
            if (c.isEmpty() || !choices(ctx, pkg, g).contains(c)) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(pretty(c));
        }
        return sb.length() == 0 ? ctx.getString(R.string.options_default) : sb.toString();
    }

    static void show(Context ctx, String pkg, String[] groups, int titleRes, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        if (java.util.Arrays.asList(groups).contains("progress")) {
            // anteprima: i tre cerchi di sistema (piccolo, medio, grande) col disegno in uso adesso
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, pad / 2, 0, pad / 2);
            int[] styles = {android.R.attr.progressBarStyleSmall, android.R.attr.progressBarStyle, android.R.attr.progressBarStyleLarge};
            for (int st : styles) {
                android.widget.ProgressBar pb = new android.widget.ProgressBar(ctx, null, st);
                pb.setIndeterminate(true);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd(pad);
                row.addView(pb, lp);
            }
            box.addView(row);
        }
        final java.util.Map<String, String> initial = new java.util.HashMap<>();
        for (String g : groups) initial.put(g, ThemePrefs.getOption(pkg, g));
        for (String g : groups) {
            List<String> ch = choices(ctx, pkg, g);
            if (ch.isEmpty()) continue;
            if (groups.length > 1) {
                TextView t = new TextView(ctx);
                t.setText(title(ctx, pkg, g));
                t.setTextColor(ThemePrefs.accentColor());
                t.setTextSize(15);
                t.setPadding(0, pad / 2, 0, pad / 4);
                box.addView(t);
            }
            RadioGroup rg = new RadioGroup(ctx);
            String cur = ThemePrefs.getOption(pkg, g);
            List<String> all = new ArrayList<>();
            all.add("");
            all.addAll(ch);
            for (String c : all) {
                RadioButton rb = new RadioButton(ctx);
                rb.setText(c.isEmpty() ? ctx.getString(R.string.options_default) : pretty(c));
                rb.setId(View.generateViewId());
                rb.setChecked(c.equals(cur));
                rb.setButtonTintList(ColorStateList.valueOf(ThemePrefs.accentColor()));
                rb.setOnClickListener(v -> {
                    ThemePrefs.setOption(pkg, g, c);
                    for (int i = 0; i < rg.getChildCount(); i++) {
                        RadioButton o = (RadioButton) rg.getChildAt(i);
                        o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : ctx.getColor(R.color.text));
                    }
                });
                rg.addView(rb);
            }
            for (int i = 0; i < rg.getChildCount(); i++) {
                RadioButton o = (RadioButton) rg.getChildAt(i);
                o.setTextColor(o.isChecked() ? ThemePrefs.accentColor() : ctx.getColor(R.color.text));
            }
            box.addView(rg);
        }
        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(titleRes)
                .setView(sv)
                .setNegativeButton(android.R.string.cancel, (dlg, w) -> {
                    for (String g : groups) ThemePrefs.setOption(pkg, g, initial.get(g));
                })
                .setPositiveButton(android.R.string.ok, (dlg, w) -> listener.onSaved()));
    }
}
