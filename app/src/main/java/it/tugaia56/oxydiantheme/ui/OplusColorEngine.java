package it.tugaia56.oxydiantheme.ui;

import android.graphics.Color;

import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

/**
 * OxygenOS 16: dà l'accento scelto al motore dei colori del sistema (la voce "Personalizzato" di
 * Impostazioni > Sfondi e stile > Colori, e i colori "couiSingleFirst*" letti dalle app di sistema):
 *  1. scrive /data/oplus/uxres/uxcolor/ux_custom_color[_night].xml
 *  2. aggiorna in "theme_customization_overlay_packages" solo il colore di base e l'accento
 * Stesso meccanismo che usava il vecchio modulo Oxydian (SystemColorUtils).
 */
final class OplusColorEngine {
    private static final String DIR = "/data/oplus/uxres/uxcolor";
    private static final String KEY = "theme_customization_overlay_packages";

    private OplusColorEngine() {}

    private static int alpha(int c, float f) {
        return Color.argb(Math.min(255, (int) (Color.alpha(c) * f)), Color.red(c), Color.green(c), Color.blue(c));
    }

    private static int pressed(int c, float f) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * f));
        return Color.HSVToColor(hsv);
    }

    private static String hex(int c) {
        return String.format("#%08X", c & 0xFFFFFFFFL);
    }

    private static String xml(int color, int pressed, int lightNormal, int lightPressed, int textHighlight) {
        String h = hex(color), p = hex(pressed), ln = hex(lightNormal), lp = hex(lightPressed), th = hex(textHighlight);
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n<resources>\n");
        for (String pre : new String[]{"couiSingleFirst", "NXcolorSingleFirst"}) {
            sb.append("<color name=\"").append(pre).append("Normal\">").append(h).append("</color>\n");
            sb.append("<color name=\"").append(pre).append("Pressed\">").append(p).append("</color>\n");
            sb.append("<color name=\"").append(pre).append("LightNormal\">").append(ln).append("</color>\n");
            sb.append("<color name=\"").append(pre).append("LightPressed\">").append(lp).append("</color>\n");
            sb.append("<color name=\"").append(pre).append("TextHighLight\">").append(th).append("</color>\n");
            sb.append("<color name=\"").append(pre).append("BarDisabledColor\">").append(th).append("</color>\n");
        }
        return sb.append("</resources>").toString();
    }

    private static void write(String content, String name) {
        String mod = it.tugaia56.oxydiantheme.utils.ModuleConstants.MODULE_DIR + "/uxcolor";
        String q = "printf '" + content.replace("'", "'\''") + "'";
        // anche una copia nel modulo: all'avvio il modulo la rimette se il sistema l'ha cambiata
        Shell.cmd("mkdir -p " + DIR + " " + mod, q + " > " + DIR + "/" + name, q + " > " + mod + "/" + name).exec();
    }

    /** Colori di sistema spenti: il modulo non deve rimettere piu' quelli vecchi all'avvio. */
    static void clearSaved() {
        Shell.cmd("rm -rf " + it.tugaia56.oxydiantheme.utils.ModuleConstants.MODULE_DIR + "/uxcolor").exec();
    }

    static void apply(int color) {
        try {
            color |= 0xFF000000;
            int pr = pressed(color, 0.3f);
            int th = alpha(color, 0.15f);
            write(xml(color, pr, alpha(color, 0.3f), alpha(pr, 0.3f), th), "ux_custom_color.xml");
            int npr = pressed(color, 0.7f);
            write(xml(color, npr, alpha(color, 0.4f), alpha(npr, 0.3f), th), "ux_custom_color_night.xml");

            String bare = String.format("%08X", color & 0xFFFFFFFFL);
            String cur = String.join("", Shell.cmd("settings get secure " + KEY).exec().getOut()).trim();
            JSONObject o = new JSONObject(cur.isEmpty() || cur.equals("null") ? "{}" : cur);
            o.put("android.theme.customization.system_palette", bare);
            o.put("android.theme.customization.accent_color", bare);
            o.put("_applied_timestamp", System.currentTimeMillis());
            Shell.cmd("settings put secure " + KEY + " '" + o.toString().replace("'", "") + "'").exec();
        } catch (Throwable ignored) {}
    }
}
