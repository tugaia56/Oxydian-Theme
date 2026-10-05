package it.tugaia56.oxydiantheme.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/** Scelta di un colore libero: tavolozza + codice esadecimale. */
final class ColorPickDialog {
    private static final int[] PALETTE = {
            0xFF908DFF, 0xFF7268FC, 0xFF6200EA, 0xFF7C4DFF, 0xFFD401E9, 0xFF78038C,
            0xFFC51162, 0xFFE70CA5, 0xFF880E4F, 0xFFF39DCC, 0xFFB37AA3, 0xFFFF404C,
            0xFFEF5350, 0xFFFF0000, 0xFFD50000, 0xFFCC0000, 0xFFA92C2C, 0xFFFF2837,
            0xFFFF7514, 0xFFE0610E, 0xFFF86734, 0xFFFBA723, 0xFFFFC107, 0xFFFFD600,
            0xFF9DD200, 0xFF80FF00, 0xFF1CFF12, 0xFF3DDC84, 0xFF557A52, 0xFF7DB695,
            0xFF00897B, 0xFF00695C, 0xFF26A69A, 0xFF0097A7, 0xFF0097FF, 0xFF4285F4,
            0xFF5E97F6, 0xFFA1B6ED, 0xFF2962FF, 0xFF2E61F5, 0xFF304FFE, 0xFF607D8B,
            0xFF78909C, 0xFF90A4AE, 0xFFBDBDBD, 0xFF9E9E9E, 0xFFFFFFFF, 0xFF000000,
    };

    interface Listener { void onPicked(int color); }

    private ColorPickDialog() {}

    private static GradientDrawable circle(int fill, boolean selected) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        g.setStroke(selected ? 7 : 2, selected ? Color.WHITE : 0x55FFFFFF);
        return g;
    }

    static void show(Context ctx, int initial, int titleRes, Listener listener) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        final int[] chosen = {initial | 0xFF000000};
        final EditText hex = new EditText(ctx);
        GridLayout grid = new GridLayout(ctx);
        grid.setColumnCount(6);
        int cell = (int) (44 * d), m = (int) (4 * d);
        for (int c : PALETTE) {
            View v = new View(ctx);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(m, m, m, m);
            v.setLayoutParams(lp);
            v.setBackground(circle(c, c == chosen[0]));
            v.setOnClickListener(x -> {
                chosen[0] = c;
                hex.setText(String.format("%06X", c & 0xFFFFFF));
                for (int i = 0; i < grid.getChildCount(); i++) grid.getChildAt(i).setBackground(circle(PALETTE[i], PALETTE[i] == c));
            });
            grid.addView(v);
        }
        box.addView(grid);
        TextView label = new TextView(ctx);
        label.setText(R.string.accent_custom);
        label.setTextColor(ctx.getColor(R.color.text_dim));
        label.setPadding(0, pad, 0, 0);
        box.addView(label);
        hex.setText(String.format("%06X", chosen[0] & 0xFFFFFF));
        hex.setSingleLine();
        hex.setTextColor(ctx.getColor(R.color.text));
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        hex.setHint("RRGGBB");
        box.addView(hex, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ScrollView sv = new ScrollView(ctx);
        sv.addView(box);
        Dialogs.show(ctx, new MaterialAlertDialogBuilder(ctx)
                .setTitle(titleRes)
                .setView(sv)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dlg, w) -> {
                    int color = chosen[0];
                    String t = hex.getText().toString().trim();
                    if (t.length() == 6) {
                        try { color = 0xFF000000 | Integer.parseInt(t, 16); } catch (NumberFormatException ignored) {}
                    }
                    listener.onPicked(color);
                }));
    }
}
