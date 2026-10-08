package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
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
 * Colori di sistema: accento e sfondo scelti tra i preset di Dark Shadow Theme (overlay su "android").
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
        final float d = ctx.getResources().getDisplayMetrics().density;
        final String key = PKG + "_" + (idx == 0 ? "accent" : "background");
        // elenco completo: Predefinito, tutti i preset (pallino + nome), Personalizzato
        final List<Swatch> all = new ArrayList<>();
        all.add(new Swatch(OptionGroupsDialog.CUSTOM, ThemePrefs.getCustomColor(key)));
        all.addAll(items);
        all.add(new Swatch("", idx == 0 ? ThemePrefs.DEFAULT_ACCENT : 0xFF1B2029)); // Predefinito, in fondo

        final List<TextView> labels = new ArrayList<>();
        final List<View> dots = new ArrayList<>();
        final List<View> radios = new ArrayList<>();
        Runnable refresh = () -> {
            int acc = ThemePrefs.accentColor();
            for (int i = 0; i < all.size(); i++) {
                Swatch s = all.get(i);
                boolean sel = s.name.equals(selection[idx]);
                labels.get(i).setTextColor(sel ? acc : ctx.getColor(R.color.text));
                int fill = s.name.equals(OptionGroupsDialog.CUSTOM) ? ThemePrefs.getCustomColor(key) : s.color;
                dots.get(i).setBackground(circle(fill, sel, s.name.isEmpty(), acc));
                ((android.widget.RadioButton) radios.get(i)).setChecked(sel);
            }
        };
        for (int i = 0; i < all.size(); i++) {
            final Swatch s = all.get(i);
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
            row.setClickable(true);

            View dot = new View(ctx);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams((int) (26 * d), (int) (26 * d));
            dlp.setMarginEnd((int) (14 * d));
            row.addView(dot, dlp);

            TextView label = new TextView(ctx);
            label.setTextSize(16);
            String text;
            if (s.name.isEmpty()) text = ctx.getString(R.string.options_default);
            else if (s.name.equals(OptionGroupsDialog.CUSTOM)) {
                text = ctx.getString(R.string.opt_custom) + String.format(" #%06X", ThemePrefs.getCustomColor(key) & 0xFFFFFF);
            } else text = s.name.replace('_', ' ');
            label.setText(text);
            row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.RadioButton rb = new android.widget.RadioButton(ctx);
            rb.setClickable(false);
            rb.setFocusable(false);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
            row.addView(rb);

            labels.add(label);
            dots.add(dot);
            radios.add(rb);
            row.setOnClickListener(x -> {
                if (s.name.equals(OptionGroupsDialog.CUSTOM)) {
                    ColorPickDialog.show(ctx, ThemePrefs.getCustomColor(key), R.string.opt_custom, color -> {
                        ThemePrefs.setCustomColor(key, color);
                        selection[idx] = OptionGroupsDialog.CUSTOM;
                        label.setText(ctx.getString(R.string.opt_custom) + String.format(" #%06X", color & 0xFFFFFF));
                        refresh.run();
                    });
                    return;
                }
                selection[idx] = s.name;
                refresh.run();
            });
            box.addView(row);
        }
        refresh.run();
    }

    /** Nome della cartella del preset corrispondente a un nome qualsiasi (maiuscole e spazi ignorati), o null. */
    static String matchPreset(Context ctx, String group, String name) {
        if (name == null) return null;
        String want = name.trim().replace(' ', '_');
        try {
            String[] names = ctx.getAssets().list(BASE + group);
            if (names == null) return null;
            for (String n : names) if (!n.equals("title.txt") && n.equalsIgnoreCase(want)) return n;
        } catch (Exception ignored) {}
        return null;
    }

    /** Riporta le scelte salvate (come se si fosse premuto OK nella finestra): accento dell'app, tema acceso, proprieta' di sistema. */
    static void commitFromPrefs(Context ctx) {
        String a = ThemePrefs.getOption(PKG, "accent"), b = ThemePrefs.getOption(PKG, "background");
        Integer ea = effAccent(ctx);
        int c = a.isEmpty() ? ThemePrefs.DEFAULT_ACCENT : (ea != null ? ea : ThemePrefs.accentColor());
        ThemePrefs.setAccentColor(c);
        ThemePrefs.setDarkShadowEnabled(PKG, !(a.isEmpty() && b.isEmpty() && ThemePrefs.getOption(PKG, "progress").isEmpty()));
        republish(ctx);
    }

    /** Accento effettivo: quello scelto con la saturazione impostata (null = predefinito). */
    static Integer effAccent(Context ctx) {
        Integer a = currentAccent(ctx);
        if (a == null) return null;
        int sat = ThemePrefs.getAccSat();
        if (sat == 100) return a;
        return 0xFF000000 | CustomPalette.tweakColor(a & 0xFFFFFF, sat, 0, false, 0f);
    }

    /** Sfondo effettivo: quello scelto con saturazione, luminosita' e nero puro (null = predefinito). */
    static Integer effBg(Context ctx) {
        Integer b = currentBg(ctx);
        if (b == null) return null;
        int sat = ThemePrefs.getBgSat(), light = ThemePrefs.getBgLight();
        boolean pitch = ThemePrefs.isBgPitch();
        if (sat == 100 && light == 0 && !pitch) return b;
        float l0 = CustomPalette.bgDarkLightness(CustomPalette.baseBody(ctx, "background",
                ThemePrefs.getOption(PKG, "background"), ThemePrefs.getCustomColor(PKG + "_background")));
        return 0xFF000000 | CustomPalette.tweakColor(b & 0xFFFFFF, sat, light, pitch, Math.max(l0, 0f));
    }

    /** Accento scelto (null = predefinito). */
    static Integer currentAccent(Context ctx) {
        String a = ThemePrefs.getOption(PKG, "accent");
        if (a.isEmpty()) return null;
        if (OptionGroupsDialog.CUSTOM.equals(a)) return ThemePrefs.getCustomColor(PKG + "_accent");
        for (Swatch sw : load(ctx, "accent", "type1a.xml", "accent_material_dark")) if (sw.name.equals(a)) return sw.color;
        return null;
    }

    /** Sfondo di sistema scelto (null = predefinito). */
    static Integer currentBg(Context ctx) {
        String b = ThemePrefs.getOption(PKG, "background");
        if (b.isEmpty()) return null;
        if (OptionGroupsDialog.CUSTOM.equals(b)) return ThemePrefs.getCustomColor(PKG + "_background");
        for (Swatch sw : load(ctx, "background", "type1b.xml", "background_dark")) if (sw.name.equals(b)) return sw.color;
        return null;
    }

    static void republish(Context ctx) {
        String a = ThemePrefs.getOption(PKG, "accent"), b = ThemePrefs.getOption(PKG, "background");
        Integer accent = null, bgc = null;
        for (Swatch sw : load(ctx, "accent", "type1a.xml", "accent_material_dark")) if (!a.isEmpty() && sw.name.equals(a)) accent = sw.color;
        for (Swatch sw : load(ctx, "background", "type1b.xml", "background_dark")) if (!b.isEmpty() && sw.name.equals(b)) bgc = sw.color;
        if (OptionGroupsDialog.CUSTOM.equals(a)) accent = ThemePrefs.getCustomColor(PKG + "_accent");
        if (OptionGroupsDialog.CUSTOM.equals(b)) bgc = ThemePrefs.getCustomColor(PKG + "_background");
        Integer ea = effAccent(ctx), eb = effBg(ctx);
        it.tugaia56.oxydiantheme.utils.ThemeProps.publish(ea != null ? ea : accent, eb != null ? eb : bgc);
    }

    private static TextView note(Context ctx, int textRes) {
        TextView t = new TextView(ctx);
        t.setText(textRes);
        t.setTextColor(ctx.getColor(R.color.text_dim));
        t.setTextSize(12);
        t.setPadding(0, (int) (10 * ctx.getResources().getDisplayMetrics().density), 0, 0);
        return t;
    }

    /** Cursore con titolo e valore: min..max a passi di step; il valore scelto torna in value[0]. */
    private static void slider(Context ctx, LinearLayout box, int titleRes, int min, int max, int step,
                               int[] value, boolean signed, int def) {
        float d = ctx.getResources().getDisplayMetrics().density;
        LinearLayout head = new LinearLayout(ctx);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(0, (int) (14 * d), 0, 0);
        TextView label = new TextView(ctx);
        label.setTextColor(ctx.getColor(R.color.text));
        label.setTextSize(15);
        head.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView reset = new TextView(ctx);
        reset.setText(R.string.tweak_reset);
        reset.setTextColor(ThemePrefs.accentColor());
        reset.setTextSize(14);
        reset.setPadding((int) (12 * d), (int) (6 * d), (int) (4 * d), (int) (6 * d));
        head.addView(reset);
        box.addView(head);
        android.widget.SeekBar bar = new android.widget.SeekBar(ctx);
        bar.setMax((max - min) / step);
        bar.setProgress((value[0] - min) / step);
        int acc = ThemePrefs.accentColor();
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(acc));
        bar.setThumbTintList(android.content.res.ColorStateList.valueOf(acc));
        Runnable show = () -> label.setText(ctx.getString(titleRes) + ": "
                + (signed && value[0] > 0 ? "+" : "") + value[0] + (signed ? "" : "%"));
        show.run();
        bar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar s, int p, boolean user) {
                value[0] = min + p * step;
                show.run();
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
            @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
        });
        reset.setOnClickListener(v -> bar.setProgress((def - min) / step));
        box.addView(bar);
    }

    /** @param which 0 = solo accento, 1 = solo sfondo */
    static void show(Context ctx, int which, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        List<Swatch> acc = load(ctx, "accent", "type1a.xml", "accent_material_dark");
        List<Swatch> bg = load(ctx, "background", "type1b.xml", "background_dark");
        final String[] sel = {ThemePrefs.getOption(PKG, "accent"), ThemePrefs.getOption(PKG, "background")};

        if (which == 0) section(ctx, box, R.string.sys_accent, acc, sel, 0, pad);
        else section(ctx, box, R.string.sys_background, bg, sel, 1, pad);

        // ritocchi (valgono con un colore scelto, non con Predefinito)
        final int[] accSat = {ThemePrefs.getAccSat()};
        final int[] bgSat = {ThemePrefs.getBgSat()};
        final int[] bgLight = {ThemePrefs.getBgLight()};
        final boolean[] pitch = {ThemePrefs.isBgPitch()};
        if (which == 0) {
            slider(ctx, box, R.string.tweak_sat_accent, 0, 200, 5, accSat, false, 100);
        } else {
            com.google.android.material.materialswitch.MaterialSwitch sw =
                    new com.google.android.material.materialswitch.MaterialSwitch(ctx);
            sw.setText(R.string.tweak_pitch);
            sw.setTextColor(ctx.getColor(R.color.text));
            sw.setChecked(pitch[0]);
            Tint.sw(sw);
            sw.setPadding(0, (int) (14 * d), 0, 0);
            sw.setOnCheckedChangeListener((b, on) -> pitch[0] = on);
            box.addView(sw);
            slider(ctx, box, R.string.tweak_sat_bg, 0, 200, 5, bgSat, false, 100);
            slider(ctx, box, R.string.tweak_light_bg, -10, 10, 1, bgLight, true, 0);
        }
        box.addView(note(ctx, which == 0 ? R.string.tweak_note_accent : R.string.tweak_note));

        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(which == 0 ? R.string.card_accent : R.string.card_background)
                .setView(sv)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    ThemePrefs.setOption(PKG, "accent", sel[0]);
                    ThemePrefs.setOption(PKG, "background", sel[1]);
                    if (which == 0) {
                        ThemePrefs.setAccSat(accSat[0]);
                    } else {
                        ThemePrefs.setBgSat(bgSat[0]);
                        ThemePrefs.setBgLight(bgLight[0]);
                        ThemePrefs.setBgPitch(pitch[0]);
                    }
                    int c = ThemePrefs.accentColor();
                    for (Swatch s : acc) if (s.name.equals(sel[0])) c = s.color;
                    if (OptionGroupsDialog.CUSTOM.equals(sel[0])) c = ThemePrefs.getCustomColor(PKG + "_accent");
                    if (sel[0].isEmpty()) c = ThemePrefs.DEFAULT_ACCENT;
                    Integer tweakedAccent = effAccent(ctx);
                    if (tweakedAccent != null && !sel[0].isEmpty()) c = tweakedAccent;
                    ThemePrefs.setAccentColor(c);
                    ThemePrefs.setDarkShadowEnabled(PKG, !(sel[0].isEmpty() && sel[1].isEmpty() && ThemePrefs.getOption(PKG, "progress").isEmpty()));
                    Integer bgColor = null;
                    for (Swatch sw : bg) if (!sel[1].isEmpty() && sw.name.equals(sel[1])) bgColor = sw.color;
                    if (OptionGroupsDialog.CUSTOM.equals(sel[1])) bgColor = ThemePrefs.getCustomColor(PKG + "_background");
                    Integer tweakedBg = effBg(ctx);
                    if (tweakedBg != null && !sel[1].isEmpty()) bgColor = tweakedBg;
                    it.tugaia56.oxydiantheme.utils.ThemeProps.publish(sel[0].isEmpty() ? null : c, bgColor);
                    listener.onSaved(c);
                }));
    }
}
