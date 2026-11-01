package it.tugaia56.oxydiantheme.ui;

import android.content.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Gli overlay delle app sono scritti con la tavolozza dello sfondo predefinito (Midnight_Slate): dove usano un
 * riferimento ai colori del tema seguono lo sfondo scelto da soli, ma i colori scritti fissi (soprattutto quelli
 * con trasparenza, che non si possono scrivere come riferimento) restano quelli di Midnight_Slate. Qui si calcola
 * la corrispondenza "colore di Midnight_Slate -> colore dello sfondo scelto", che il compilatore applica ai sorgenti.
 * Vuota se lo sfondo e' quello predefinito o con trasparenza (quelli li gestiscono altre parti del tema).
 */
public final class BackgroundRemap {
    private static final Pattern COLOR = Pattern.compile("<color name=\"([^\"]+)\">#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})</color>");

    private BackgroundRemap() {}

    private static boolean active() {
        String b = ThemePrefs.getOption(SystemColorsDialog.PKG, "background");
        return !b.isEmpty() && !SystemColorsDialog.isTransparentBg(b) && !"Midnight_Slate".equals(b);
    }

    private static Map<String, Integer> rgbByName(String body) {
        Map<String, Integer> m = new LinkedHashMap<>();
        Matcher mm = COLOR.matcher(body);
        while (mm.find()) {
            String h = mm.group(2);
            m.put(mm.group(1), Integer.parseInt(h.substring(h.length() - 6), 16));
        }
        return m;
    }

    /** rgb (24 bit) del colore di Midnight_Slate -> rgb dello sfondo scelto; vuota se non serve. */
    public static Map<Integer, Integer> get(Context ctx) {
        Map<Integer, Integer> out = new LinkedHashMap<>();
        try {
            if (!active()) return out;
            String choice = ThemePrefs.getOption(SystemColorsDialog.PKG, "background");
            String chosen = CustomPalette.baseBody(ctx, "background", choice, ThemePrefs.getCustomColor(SystemColorsDialog.PKG + "_background"));
            if (ThemePrefs.getBgSat() != 100 || ThemePrefs.getBgLight() != 0)
                chosen = CustomPalette.tweakBody(chosen, ThemePrefs.getBgSat(), ThemePrefs.getBgLight());
            Map<String, Integer> from = rgbByName(CustomPalette.baseBody(ctx, "background", "Midnight_Slate", 0));
            Map<String, Integer> to = rgbByName(chosen);
            for (Map.Entry<String, Integer> e : from.entrySet()) {
                Integer t = to.get(e.getKey());
                int f = e.getValue();
                if (t == null || t == f || f == 0xFFFFFF || f == 0x000000) continue;
                if (!out.containsKey(f)) out.put(f, t);
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /** Parte della firma di compilazione: cambia quando cambia lo sfondo (vuota con lo sfondo predefinito). */
    public static String key() {
        if (!active()) return "";
        String b = ThemePrefs.getOption(SystemColorsDialog.PKG, "background");
        StringBuilder sb = new StringBuilder(":bg=").append(b);
        if (it.tugaia56.oxydiantheme.ui.OptionGroupsDialog.CUSTOM.equals(b))
            sb.append(String.format("#%08X", ThemePrefs.getCustomColor(SystemColorsDialog.PKG + "_background")));
        sb.append(',').append(ThemePrefs.getBgSat()).append(',').append(ThemePrefs.getBgLight());
        return sb.toString();
    }
}
