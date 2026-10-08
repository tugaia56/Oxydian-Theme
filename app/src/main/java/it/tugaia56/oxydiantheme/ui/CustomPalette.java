package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Color;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Accento e sfondo di sistema a scelta libera: si parte da un preset di Dark Shadow Theme (modello) e se ne
 * ricavano tutte le sfumature dal colore scelto, come facevano i preset.
 */
final class CustomPalette {
    private static final String BASE = "CompileOnDemand/android/OPT/";
    // colori del modello: accento "Blue" (0097ff, variante scura 007de2) e sfondo "Blue_Gray_Dark" (1b2029)
    private static final int TEMPLATE_BG = 0xFF1B2029;

    private CustomPalette() {}

    private static String read(Context ctx, String path) {
        try (InputStream in = ctx.getAssets().open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** Contenuto (senza la cornice) delle risorse per un accento a scelta. */
    static String accentBody(Context ctx, int color) {
        String s = read(ctx, BASE + "accent/Blue/res/values/type1a.xml");
        int rgb = color & 0xFFFFFF;
        float[] hsv = new float[3];
        Color.colorToHSV(0xFF000000 | rgb, hsv);
        hsv[2] *= 0.88f;
        int dark = Color.HSVToColor(hsv) & 0xFFFFFF;
        s = s.replaceAll("(?i)0097ff", String.format("%06x", rgb));
        s = s.replaceAll("(?i)007de2", String.format("%06x", dark));
        return inner(s);
    }

    /** Contenuto delle risorse per uno sfondo a scelta (le sfumature seguono il modello). */
    static String backgroundBody(Context ctx, int color) {
        String s = read(ctx, BASE + "background/Blue_Gray_Dark/res/values/type1b.xml");
        float[] hc = new float[3], h0 = new float[3];
        rgbToHsl(color, hc);
        rgbToHsl(TEMPLATE_BG, h0);
        Pattern p = Pattern.compile("(<color name=\"[^\"]+\">)#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})(</color>)");
        Matcher m = p.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String hex = m.group(2);
            int alpha = 0xFF;
            int rgb;
            if (hex.length() == 8) {
                alpha = Integer.parseInt(hex.substring(0, 2), 16);
                rgb = Integer.parseInt(hex.substring(2), 16);
            } else {
                rgb = Integer.parseInt(hex, 16);
            }
            float[] hx = new float[3];
            rgbToHsl(0xFF000000 | rgb, hx);
            boolean neutralExtreme = hx[1] < 0.05f && (hx[2] > 0.85f || hx[2] < 0.03f);
            int res = rgb;
            if (!neutralExtreme && alpha != 0 || (alpha != 0 && !neutralExtreme)) {
                float l = clamp(hx[2] + (hc[2] - h0[2]));
                float sat = hx[1] < 0.02f ? 0f : hc[1];
                res = hslToRgb(hc[0], sat, l) & 0xFFFFFF;
            }
            String nh = hex.length() == 8 ? String.format("%02x%06x", alpha, res) : String.format("%06x", res);
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + "#" + nh + m.group(3)));
        }
        m.appendTail(out);
        return inner(out.toString());
    }

    /** Corpo delle risorse di un preset (accento o sfondo) o del colore a scelta. */
    static String baseBody(Context ctx, String group, String choice, int customColor) {
        if (OptionGroupsDialog.CUSTOM.equals(choice))
            return "accent".equals(group) ? accentBody(ctx, customColor) : backgroundBody(ctx, customColor);
        String file = "accent".equals(group) ? "type1a.xml" : "type1b.xml";
        return inner(read(ctx, BASE + group + "/" + choice + "/res/values/" + file));
    }

    /** Luminosita' (0..1) del colore background_dark in un corpo di risorse, o -1. */
    static float bgDarkLightness(String body) {
        Matcher m = Pattern.compile("name=\"background_dark\">#([0-9a-fA-F]{6,8})<").matcher(body);
        if (!m.find()) return -1f;
        String hex = m.group(1);
        int rgb = Integer.parseInt(hex.substring(hex.length() - 6), 16);
        float[] hsl = new float[3];
        rgbToHsl(0xFF000000 | rgb, hsl);
        return hsl[2];
    }

    /**
     * Ritocca un colore: saturazione in % (100 = invariata), luminosita' in punti % (0 = invariata),
     * nero puro: la luminosita' del colore di fondo l0 diventa 0 e il resto si riallinea.
     */
    static int tweakColor(int rgb, int satPct, int lightPts, boolean pitch, float l0) {
        float[] hx = new float[3];
        rgbToHsl(0xFF000000 | rgb, hx);
        boolean neutralExtreme = hx[1] < 0.05f && (hx[2] > 0.85f || hx[2] < 0.03f);
        if (neutralExtreme) return rgb & 0xFFFFFF;
        float s = clamp(hx[1] * satPct / 100f);
        float l = hx[2];
        if (pitch && l0 > 0f) l = l <= l0 ? 0f : (l - l0) / (1f - l0);
        l = clamp(l + lightPts / 100f);
        return hslToRgb(hx[0], s, l) & 0xFFFFFF;
    }

    /** Applica i ritocchi a tutti i colori espliciti di un corpo di risorse (i riferimenti restano). */
    static String tweakBody(String body, int satPct, int lightPts, boolean pitch, float l0) {
        Pattern p = Pattern.compile("(<color name=\"[^\"]+\">)#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})(</color>)");
        Matcher m = p.matcher(body);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String hex = m.group(2);
            int alpha = 0xFF;
            int rgb;
            if (hex.length() == 8) {
                alpha = Integer.parseInt(hex.substring(0, 2), 16);
                rgb = Integer.parseInt(hex.substring(2), 16);
            } else {
                rgb = Integer.parseInt(hex, 16);
            }
            String nh;
            if (alpha == 0) {
                nh = hex;
            } else {
                int res = tweakColor(rgb, satPct, lightPts, pitch, l0);
                nh = hex.length() == 8 ? String.format("%02x%06x", alpha, res) : String.format("%06x", res);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + "#" + nh + m.group(3)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String inner(String xml) {
        int a = xml.indexOf("<resources>");
        int b = xml.lastIndexOf("</resources>");
        if (a < 0 || b < 0) return "";
        // senza commenti (contengono apostrofi e inutili nel risultato)
        return xml.substring(a + "<resources>".length(), b).replaceAll("(?s)<!--.*?-->", "");
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static void rgbToHsl(int color, float[] out) {
        float r = Color.red(color) / 255f, g = Color.green(color) / 255f, b = Color.blue(color) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f, h = 0f, s = 0f;
        if (max != min) {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) h = (g - b) / d + (g < b ? 6f : 0f);
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
        }
        out[0] = h; out[1] = s; out[2] = l;
    }

    private static float hue2rgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 0.5f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }

    private static int hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1f / 3f);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1f / 3f);
        }
        return Color.rgb(Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
    }
}
