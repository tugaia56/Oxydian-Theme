package it.tugaia56.oxydiantheme.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.BuildConfig;
import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.UpdateChecker;

/** Finestre della pagina Impostazioni: aggiornamenti, informazioni, crediti. */
final class SettingsPage {
    private SettingsPage() {}

    static void openUrl(Activity a, String url) {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) {}
    }

    // ── Aggiornamenti ────────────────────────────────────────────────────────

    static void checkUpdates(Activity a) {
        Toast.makeText(a, R.string.update_checking, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            UpdateChecker.Result r = null;
            boolean failed = false;
            try {
                r = UpdateChecker.check();
            } catch (Throwable t) {
                failed = true;
            }
            final UpdateChecker.Result res = r;
            final boolean err = failed;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (a.isFinishing()) return;
                if (err || res == null) {
                    Dialogs.show(a, new MaterialAlertDialogBuilder(a)
                            .setTitle(R.string.card_update)
                            .setMessage(R.string.update_error)
                            .setPositiveButton(android.R.string.ok, null));
                } else if (!res.newer) {
                    Dialogs.show(a, new MaterialAlertDialogBuilder(a)
                            .setTitle(R.string.card_update)
                            .setMessage(a.getString(R.string.update_uptodate, BuildConfig.VERSION_NAME))
                            .setPositiveButton(android.R.string.ok, null));
                } else {
                    MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(a)
                            .setTitle(a.getString(R.string.update_available, res.version))
                            .setMessage(res.changelog.isEmpty() ? a.getString(R.string.update_no_notes) : res.changelog)
                            .setNegativeButton(R.string.legend_close, null);
                    final String url = res.downloadUrl != null ? res.downloadUrl : UpdateChecker.REPO_URL + "/releases";
                    b.setPositiveButton(R.string.update_download, (d, w) -> openUrl(a, url));
                    Dialogs.show(a, b);
                }
            });
        }).start();
    }

    // ── Informazioni ─────────────────────────────────────────────────────────

    static void about(Activity a) {
        Dialogs.show(a, new MaterialAlertDialogBuilder(a)
                .setTitle("Oxydian Theme " + BuildConfig.VERSION_NAME)
                .setMessage(R.string.about_text)
                .setPositiveButton(R.string.about_github, (d, w) -> openUrl(a, UpdateChecker.REPO_URL))
                .setNeutralButton(R.string.about_oxydian, (d, w) -> openUrl(a, "https://github.com/tugaia56/Oxydian-xposed"))
                .setNegativeButton(R.string.legend_close, null));
    }

    // ── Crediti ──────────────────────────────────────────────────────────────

    private static void row(Activity a, LinearLayout box, String title, String summary, String url) {
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(0, (int) (8 * d), 0, (int) (8 * d));
        TextView t = new TextView(a);
        t.setText(title);
        t.setTextColor(ThemePrefs.accentColor());
        t.setTextSize(16);
        TextView s = new TextView(a);
        s.setText(summary);
        s.setTextColor(a.getColor(R.color.text_dim));
        s.setTextSize(13);
        r.addView(t);
        r.addView(s);
        if (url != null) {
            r.setClickable(true);
            r.setOnClickListener(v -> openUrl(a, url));
        }
        box.addView(r);
    }

    private static void heading(Activity a, LinearLayout box, int res) {
        float d = a.getResources().getDisplayMetrics().density;
        TextView t = new TextView(a);
        t.setText(res);
        t.setTextColor(a.getColor(R.color.text));
        t.setTextSize(14);
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(0, (int) (14 * d), 0, 0);
        box.addView(t);
    }

    static void credits(Activity a) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * a.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        heading(a, box, R.string.credits_thanks);
        row(a, box, "Oxygen Customizer", a.getString(R.string.credits_oc_summary), "https://github.com/DHD2280/Oxygen-Customizer");
        row(a, box, "OOS Theme", a.getString(R.string.credits_oostheme_summary), null);
        row(a, box, "Substratum", a.getString(R.string.credits_substratum_summary), "https://github.com/substratum/substratum");
        row(a, box, "Claude", a.getString(R.string.credits_claude_summary), "https://claude.ai");
        row(a, box, "Oxydian", a.getString(R.string.credits_oxydian_summary), "https://github.com/tugaia56/Oxydian-xposed");
        heading(a, box, R.string.credits_libraries);
        row(a, box, "libsu", "topjohnwu", "https://github.com/topjohnwu/libsu");
        row(a, box, "Material Components", "Google", "https://github.com/material-components/material-components-android");
        row(a, box, "Space Grotesk", "Florian Karsten · SIL Open Font License", "https://github.com/floriankarsten/space-grotesk");
        ScrollView sv = new ScrollView(a);
        sv.addView(box);
        Dialogs.show(a, new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.card_credits)
                .setView(sv)
                .setPositiveButton(R.string.legend_close, null));
    }
}
