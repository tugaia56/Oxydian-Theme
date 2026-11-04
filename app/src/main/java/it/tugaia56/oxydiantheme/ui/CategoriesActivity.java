package it.tugaia56.oxydiantheme.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/**
 * Pagina "Accenti per categoria" / "Sfondi per categoria": una card per categoria (due per riga). La scelta torna alla
 * pagina principale (extra "which" e "index"), che apre l'elenco con i colori di quella categoria.
 */
public class CategoriesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int which = getIntent().getIntExtra("which", 0);
        final List<ColorCategories.Cat> cats = ColorCategories.load(this, which == 0 ? "accent" : "background");
        float d = getResources().getDisplayMetrics().density;
        int accent = ThemePrefs.accentColor();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.bg));
        root.setFitsSystemWindows(true);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding((int) (8 * d), 0, 0, 0);
        ImageButton back = new ImageButton(this);
        back.setImageResource(R.drawable.ic_back);
        back.setImageTintList(android.content.res.ColorStateList.valueOf(accent));
        back.setBackground(null);
        back.setContentDescription(getString(R.string.btn_back));
        back.setOnClickListener(v -> finish());
        head.addView(back, new LinearLayout.LayoutParams((int) (48 * d), (int) (48 * d)));
        TextView title = new TextView(this);
        title.setText(which == 0 ? R.string.cat_accents_title : R.string.cat_backgrounds_title);
        title.setTextColor(getColor(R.color.text));
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding((int) (12 * d), (int) (12 * d), (int) (12 * d), (int) (12 * d));
        head.addView(title);
        root.addView(head);

        String cur = ThemePrefs.getOption(SystemColorsDialog.PKG, which == 0 ? "accent" : "background");
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding((int) (8 * d), (int) (4 * d), (int) (8 * d), (int) (24 * d));
        LinearLayout row = null;
        for (int i = 0; i < cats.size(); i++) {
            final int idx = i;
            ColorCategories.Cat c = cats.get(i);
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                list.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (16 * d);
            card.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            int m = (int) (4 * d);
            lp.setMargins(m, m, m, m);
            card.setLayoutParams(lp);
            GradientDrawable g = new GradientDrawable();
            g.setColor(getColor(R.color.card));
            g.setCornerRadius(14 * d);
            g.setStroke((int) (1.5f * d), accent);
            card.setBackground(g);
            card.setClickable(true);
            card.setFocusable(true);
            TextView t = new TextView(this);
            t.setText(c.title);
            t.setTextColor(getColor(R.color.text));
            t.setTextSize(17);
            t.setTypeface(null, Typeface.BOLD);
            TextView sub = new TextView(this);
            sub.setTextColor(getColor(R.color.text_dim));
            sub.setTextSize(14);
            sub.setPadding(0, (int) (4 * d), 0, 0);
            sub.setText(c.names.contains(cur) ? prettyName(cur) : getString(R.string.cat_count, c.names.size()));
            card.addView(t);
            card.addView(sub);
            card.setOnClickListener(v -> {
                setResult(RESULT_OK, new Intent().putExtra("which", which).putExtra("index", idx));
                finish();
            });
            row.addView(card);
            if (i == cats.size() - 1 && i % 2 == 0) {
                View sp = new View(this);
                sp.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
                row.addView(sp);
            }
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private static String prettyName(String n) {
        StringBuilder sb = new StringBuilder();
        for (String p : n.split("_")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT)).append(p.substring(1));
        }
        return sb.toString();
    }
}
