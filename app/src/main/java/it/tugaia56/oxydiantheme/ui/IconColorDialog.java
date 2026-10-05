package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.ThemeProps;

/**
 * Colore delle icone Wi-Fi e segnale mobile. Non e' un overlay: il sistema OnePlus le ricolora da
 * solo, quindi la scelta viene pubblicata come proprieta' di sistema e la applica Oxydian (hook).
 */
final class IconColorDialog {
    /** nome mostrato, colore (0 = segue l'accento scelto) */
    private static final Object[][] COLORS = {
            {"Accento", 0},
            {"Bianco", 0xFFFFFFFF},
            {"Nero", 0xFF000000},
            {"Rosso", 0xFFFF3B30},
            {"Arancione", 0xFFFF9500},
            {"Giallo", 0xFFFFD60A},
            {"Verde", 0xFF34C759},
            {"Azzurro", 0xFF00B8D4},
            {"Blu", 0xFF2962FF},
            {"Viola", 0xFFAA00FF},
            {"Rosa", 0xFFFF4081},
    };

    interface Listener { void onSaved(); }

    private IconColorDialog() {}

    private static int colorOf(String name) {
        for (Object[] c : COLORS) {
            if (c[0].equals(name)) {
                int v = (Integer) c[1];
                return v == 0 ? ThemePrefs.accentColor() : v;
            }
        }
        return 0;
    }

    static String summary(Context ctx) {
        String n = ThemePrefs.getIconColor();
        return n.isEmpty() ? ctx.getString(R.string.options_default) : n;
    }

    /** Pubblica la scelta attuale come proprieta' di sistema (all'avvio dell'app e dopo ogni cambio). */
    static void republish() {
        String n = ThemePrefs.getIconColor();
        ThemeProps.publishIconColor(n.isEmpty() ? null : colorOf(n));
    }

    private static GradientDrawable circle(int fill, boolean selected, int accent) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(selected ? 7 : 2, selected ? Color.WHITE : (fill == 0xFF2A2A30 ? accent : 0x55FFFFFF));
        return g;
    }

    static void show(Context ctx, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        final String[] sel = {ThemePrefs.getIconColor()};
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(6);
        int cell = (int) (44 * d), m = (int) (4 * d);
        TextView name = new TextView(ctx);
        name.setTextColor(ctx.getColor(R.color.text_dim));
        name.setTextSize(13);
        name.setPadding(0, pad / 2, 0, 0);

        final int count = COLORS.length + 1;
        final int[] fills = new int[count];
        final String[] names = new String[count];
        fills[0] = 0xFF2A2A30;
        names[0] = "";
        for (int i = 0; i < COLORS.length; i++) {
            names[i + 1] = (String) COLORS[i][0];
            fills[i + 1] = colorOf(names[i + 1]);
        }
        Runnable refresh = () -> {
            for (int i = 0; i < count; i++) {
                grid.getChildAt(i).setBackground(circle(fills[i], names[i].equals(sel[0]), ThemePrefs.accentColor()));
            }
            name.setText(sel[0].isEmpty() ? ctx.getString(R.string.options_default) : sel[0]);
        };
        for (int i = 0; i < count; i++) {
            View v = new View(ctx);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(m, m, m, m);
            v.setLayoutParams(lp);
            final String n = names[i];
            v.setOnClickListener(x -> { sel[0] = n; refresh.run(); });
            grid.addView(v);
        }
        box.addView(grid);
        box.addView(name);
        refresh.run();

        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.card_sigcolor)
                .setView(sv)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    ThemePrefs.setIconColor(sel[0]);
                    republish();
                    listener.onSaved();
                }));
    }
}
