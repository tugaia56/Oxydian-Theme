package it.tugaia56.oxydiantheme.ui;

import android.content.Context;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Categorie di accenti e sfondi (Pastello, Google, Rossi, ...): una riga per categoria in
 * assets/CompileOnDemand/android/{accent,background}_categories.txt, nel formato
 * "Titolo italiano|English title|Cartella1,Cartella2,...". Un accento puo' stare in piu' categorie.
 */
final class ColorCategories {

    static final class Cat {
        final String title;
        final Set<String> names;
        Cat(String title, Set<String> names) { this.title = title; this.names = names; }
    }

    private ColorCategories() {}

    /** @param group "accent" o "background": legge accent_categories.txt / background_categories.txt */
    static List<Cat> load(Context ctx, String group) {
        List<Cat> out = new ArrayList<>();
        boolean it = "it".equals(Locale.getDefault().getLanguage());
        try (InputStream in = ctx.getAssets().open("CompileOnDemand/android/" + group + "_categories.txt")) {
            String all = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : all.split("\r?\n")) {
                String[] p = line.split("[|]");
                if (p.length < 3 || line.startsWith("#")) continue;
                Set<String> names = new LinkedHashSet<>();
                for (String n : p[2].split(",")) if (!n.trim().isEmpty()) names.add(n.trim());
                out.add(new Cat(it ? p[0].trim() : p[1].trim(), names));
            }
        } catch (Exception ignored) {}
        return out;
    }
}
