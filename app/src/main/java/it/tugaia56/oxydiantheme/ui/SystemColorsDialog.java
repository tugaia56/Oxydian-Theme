package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Colori di sistema: accento e sfondo scelti tra i preset di OOS Theme (overlay su "android").
 * Salva le scelte come opzioni dell'overlay e usa l'accento anche per i temi delle app.
 */
final class SystemColorsDialog {
    static final String PKG = "android";
    private static final String BASE = "CompileOnDemand/android/OPT/";

    interface Listener { void onSaved(int accentColor); }

    private SystemColorsDialog() {}

    private static class Swatch {
        final String name;   // cartella negli asset ("" = predefinito)
        final int color;
        Swatch(String name, int color) { this.name = name; this.color = color; }
    }

    private static int parseColor(Context ctx, String path, String resName, int fallback) {
        try (InputStream in = ctx.getAssets().open(path)) {
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("name=\"" + resName + "\">\\s*(#[0-9a-fA-F]{6,8})\\s*<").matcher(s);
            if (m.find()) return Color.parseColor(m.group(1)) | 0xFF000000;
        } catch (Exception ignored) {}
        return fallback;
    }

    private static List<Swatch> load(Context ctx, String group, String file, String resName) {
        List<Swatch> out = new ArrayList<>();
        try {
            String[] names = ctx.getAssets().list(BASE + group);
            if (names == null) return out;
            Arrays.sort(names);
            for (String n : names) {
                if (n.equals("title.txt")) continue;
                int c = parseColor(ctx, BASE + group + "/" + n + "/res/values/" + file, resName, 0xFF808080);
                out.add(new Swatch(n, c));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static GradientDrawable circle(int fill, boolean selected, boolean stock, int accent) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(selected ? 7 : 2, selected ? Color.WHITE : (stock ? accent : 0x55FFFFFF));
        return g;
    }

    private static void section(Context ctx, LinearLayout box, int titleRes, List<Swatch> items,
                                String[] selection, int idx, int pad) {
        TextView t = new TextView(ctx);
        t.setText(titleRes);
        t.setTextColor(ThemePrefs.accentColor());
        t.setTextSize(15);
        t.setPadding(0, pad, 0, pad / 3);
        box.addView(t);

        TextView name = new TextView(ctx);
        name.setTextColor(ctx.getColor(R.color.text_dim));
        name.setTextSize(13);

        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(6);
        float d = ctx.getResources().getDisplayMetrics().density;
        int cell = (int) (44 * d), m = (int) (4 * d);
        // la prima casella e' "predefinito" (nessun overlay per quel colore)
        List<Swatch> all = new ArrayList<>();
        all.add(new Swatch("", 0xFF2A2A30));
        all.addAll(items);
        all.add(new Swatch(OptionGroupsDialog.CUSTOM, ThemePrefs.getCustomColor(PKG + "_" + (idx == 0 ? "accent" : "background"))));
        Runnable refresh = () -> {
            for (int i = 0; i < grid.getChildCount(); i++) {
                Swatch s = all.get(i);
                grid.getChildAt(i).setBackground(circle(s.color, s.name.equals(selection[idx]), s.name.isEmpty(), ThemePrefs.accentColor()));
            }
            name.setText(selection[idx].isEmpty() ? ctx.getString(R.string.options_default)
                    : selection[idx].equals(OptionGroupsDialog.CUSTOM)
                    ? ctx.getString(R.string.opt_custom) + String.format(" #%06X",
                            ThemePrefs.getCustomColor(PKG + "_" + (idx == 0 ? "accent" : "background")) & 0xFFFFFF)
                    : selection[idx].replace('_', ' '));
        };
        for (Swatch s : all) {
            View v = new View(ctx);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(m, m, m, m);
            v.setLayoutParams(lp);
            v.setOnClickListener(x -> {
                if (s.name.equals(OptionGroupsDialog.CUSTOM)) {
                    String key = PKG + "_" + (idx == 0 ? "accent" : "background");
                    ColorPickDialog.show(ctx, ThemePrefs.getCustomColor(key), R.string.opt_custom, color -> {
                        ThemePrefs.setCustomColor(key, color);
                        selection[idx] = OptionGroupsDialog.CUSTOM;
                        refresh.run();
                    });
                    return;
                }
                selection[idx] = s.name;
                refresh.run();
            });
            grid.addView(v);
        }
        box.addView(grid);
        box.addView(name);
        refresh.run();
    }

    /** Ripubblica le scelte attuali come proprieta' di sistema (all'avvio dell'app e dopo ogni cambio). */
    static void republish(Context ctx) {
        String a = ThemePrefs.getOption(PKG, "accent"), b = ThemePrefs.getOption(PKG, "background");
        Integer accent = null, bgc = null;
        for (Swatch sw : load(ctx, "accent", "type1a.xml", "accent_material_dark")) if (!a.isEmpty() && sw.name.equals(a)) accent = sw.color;
        for (Swatch sw : load(ctx, "background", "type1b.xml", "background_dark")) if (!b.isEmpty() && sw.name.equals(b)) bgc = sw.color;
        if (OptionGroupsDialog.CUSTOM.equals(a)) accent = ThemePrefs.getCustomColor(PKG + "_accent");
        if (OptionGroupsDialog.CUSTOM.equals(b)) bgc = ThemePrefs.getCustomColor(PKG + "_background");
        it.tugaia56.oxydiantheme.utils.ThemeProps.publish(accent, bgc);
    }

    static void show(Context ctx, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        List<Swatch> acc = load(ctx, "accent", "type1a.xml", "accent_material_dark");
        List<Swatch> bg = load(ctx, "background", "type1b.xml", "background_dark");
        final String[] sel = {ThemePrefs.getOption(PKG, "accent"), ThemePrefs.getOption(PKG, "background")};

        section(ctx, box, R.string.sys_accent, acc, sel, 0, pad);
        section(ctx, box, R.string.sys_background, bg, sel, 1, pad);

        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.btn_system_colors)
                .setView(sv)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    ThemePrefs.setOption(PKG, "accent", sel[0]);
                    ThemePrefs.setOption(PKG, "background", sel[1]);
                    int c = ThemePrefs.accentColor();
                    for (Swatch s : acc) if (s.name.equals(sel[0])) c = s.color;
                    if (OptionGroupsDialog.CUSTOM.equals(sel[0])) c = ThemePrefs.getCustomColor(PKG + "_accent");
                    if (sel[0].isEmpty()) c = ThemePrefs.DEFAULT_ACCENT;
                    ThemePrefs.setAccentColor(c);
                    ThemePrefs.setDarkShadowEnabled(PKG, !(sel[0].isEmpty() && sel[1].isEmpty() && ThemePrefs.getOption(PKG, "progress").isEmpty()));
                    Integer bgColor = null;
                    for (Swatch sw : bg) if (!sel[1].isEmpty() && sw.name.equals(sel[1])) bgColor = sw.color;
                    if (OptionGroupsDialog.CUSTOM.equals(sel[1])) bgColor = ThemePrefs.getCustomColor(PKG + "_background");
                    it.tugaia56.oxydiantheme.utils.ThemeProps.publish(sel[0].isEmpty() ? null : c, bgColor);
                    listener.onSaved(c);
                }));
    }
}
