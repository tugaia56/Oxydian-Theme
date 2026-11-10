package it.tugaia56.oxydiantheme.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import java.util.Collections;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ModuleConstants;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil;
import it.tugaia56.oxydiantheme.utils.overlay.compiler.ThemeCompiler;

/**
 * Card di Impostazioni su Android puro (crDroid, ecc.): piene (di serie), trasparenti (colore delle card con trasparenza,
 * lo fa AospTheme) oppure con bordo (overlay DS_SSB che ridisegna le card con sfondo e accento del Theme).
 */
final class SettingsCards {
    static final String FULL = "", GLASS = "glass", BORDER = "border", GLASS_BORDER = "glassborder";
    static final String OVERLAY_NAME = "DS_SSTB";
    private static final String ASSET_DIR = "SSB", ASSET_DIR_GLASS = "SSBG";

    private SettingsCards() {}

    static String label(Context ctx, String mode) {
        if (GLASS.equals(mode)) return ctx.getString(R.string.scards_glass);
        if (BORDER.equals(mode)) return ctx.getString(R.string.scards_border);
        if (GLASS_BORDER.equals(mode)) return ctx.getString(R.string.scards_glassborder);
        return ctx.getString(R.string.scards_full);
    }

    /** Nome completo dell'overlay, da tenere nell'elenco di quelli da accendere ad ogni avvio quando e' in uso. */
    static String overlayPackage() {
        return ThemeCompiler.namedPackage(OVERLAY_NAME);
    }

    /** Stili con l'overlay del bordo (DS_SSTB). */
    static boolean usesBorder(String mode) { return BORDER.equals(mode) || GLASS_BORDER.equals(mode); }

    static void show(Activity act, Runnable onDone) {
        final String[] modes = {FULL, GLASS, BORDER, GLASS_BORDER};
        final String cur = ThemePrefs.getSettingsCards();
        final int checked = GLASS.equals(cur) ? 1 : BORDER.equals(cur) ? 2 : GLASS_BORDER.equals(cur) ? 3 : 0;
        final CharSequence[] names = {act.getString(R.string.scards_full), act.getString(R.string.scards_glass), act.getString(R.string.scards_border), act.getString(R.string.scards_glassborder)};
        final float dp = act.getResources().getDisplayMetrics().density;
        final int accent = ThemePrefs.accentColor();
        Integer bgc = SystemColorsDialog.currentBg(act);
        final int bg = 0xFF000000 | (bgc != null ? bgc : 0x1B2029);

        final android.widget.LinearLayout list = new android.widget.LinearLayout(act);
        list.setOrientation(android.widget.LinearLayout.VERTICAL);
        list.setPadding((int) (12 * dp), (int) (8 * dp), (int) (12 * dp), (int) (4 * dp));
        final AlertDialog[] ref = new AlertDialog[1];
        for (int i = 0; i < 4; i++) {
            final int which = i;
            android.widget.LinearLayout row = new android.widget.LinearLayout(act);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding((int) (8 * dp), (int) (8 * dp), (int) (8 * dp), (int) (8 * dp));
            android.widget.RadioButton rb = new android.widget.RadioButton(act);
            rb.setChecked(which == checked);
            rb.setClickable(false);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
            row.addView(rb);
            android.widget.TextView tv = new android.widget.TextView(act);
            tv.setText(names[which]);
            tv.setTextSize(16);
            tv.setTextColor(0xFFFFFFFF);
            row.addView(tv, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(preview(act, which, bg, accent, dp));
            row.setOnClickListener(v -> {
                if (ref[0] != null) ref[0].dismiss();
                ThemePrefs.setSettingsCards(modes[which]);
                if (onDone != null) onDone.run();
                final Context app = act.getApplicationContext();
                Toast.makeText(act, R.string.working, Toast.LENGTH_SHORT).show();
                new Thread(() -> {
                    boolean needsReboot = apply(app);
                    if (needsReboot) act.runOnUiThread(() -> Toast.makeText(act, R.string.scards_reboot, Toast.LENGTH_LONG).show());
                }).start();
            });
            list.addView(row);
        }
        MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(act)
                .setTitle(R.string.card_scards)
                .setView(list)
                .setNegativeButton(android.R.string.cancel, null);
        AlertDialog d = b.create();
        Dialogs.style(act, d);
        ref[0] = d;
        d.show();
        android.widget.Button neg = d.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (neg != null) neg.setTextColor(accent);
    }

    /** Anteprima di uno stile: una card disegnata come nelle Impostazioni, sopra uno sfondo a sfumatura (cosi' si vede la trasparenza). */
    private static android.view.View preview(Context ctx, int mode, int bg, int accent, float dp) {
        android.widget.FrameLayout box = new android.widget.FrameLayout(ctx);
        GradientDrawable back = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF5B6BC8, 0xFF1B2A55, 0xFF8A5BB8});
        back.setCornerRadius(14 * dp);
        box.setBackground(back);
        boolean glass = mode == 1 || mode == 3;
        boolean border = mode == 2 || mode == 3;
        int solid = mode == 3 ? bg : mixWhite(bg, mode == 2 ? 0.10f : 0.15f);
        int fill = glass ? ((solid & 0xFFFFFF) | (0xB3 << 24)) : solid;
        GradientDrawable card = new GradientDrawable();
        card.setColor(fill);
        card.setCornerRadius(11 * dp);
        if (border) card.setStroke((int) Math.max(1, 1.5f * dp), accent);
        android.view.View c = new android.view.View(ctx);
        c.setBackground(card);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams((int) (92 * dp), (int) (34 * dp));
        lp.gravity = android.view.Gravity.CENTER;
        box.addView(c, lp);
        // due righe finte di testo
        android.widget.LinearLayout lines = new android.widget.LinearLayout(ctx);
        lines.setOrientation(android.widget.LinearLayout.VERTICAL);
        lines.setGravity(android.view.Gravity.CENTER_VERTICAL);
        lines.addView(line(ctx, 0xFFFFFFFF, 50, dp));
        lines.addView(line(ctx, 0x99FFFFFF, 34, dp));
        android.widget.FrameLayout.LayoutParams lp2 = new android.widget.FrameLayout.LayoutParams((int) (92 * dp), (int) (34 * dp));
        lp2.gravity = android.view.Gravity.CENTER;
        lines.setPadding((int) (10 * dp), 0, 0, 0);
        box.addView(lines, lp2);
        box.setLayoutParams(new android.widget.LinearLayout.LayoutParams((int) (116 * dp), (int) (52 * dp)));
        return box;
    }

    private static android.view.View line(Context ctx, int color, int widthDp, float dp) {
        android.view.View v = new android.view.View(ctx);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(2 * dp);
        v.setBackground(g);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams((int) (widthDp * dp), (int) (4 * dp));
        lp.topMargin = (int) (2 * dp);
        lp.bottomMargin = (int) (2 * dp);
        v.setLayoutParams(lp);
        return v;
    }

    private static int mixWhite(int c, float t) {
        int r = (int) (((c >> 16) & 0xFF) * (1 - t) + 255 * t);
        int g = (int) (((c >> 8) & 0xFF) * (1 - t) + 255 * t);
        int bl = (int) ((c & 0xFF) * (1 - t) + 255 * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /**
     * Mette in pratica la scelta (fuori dal thread principale): costruisce/accende o spegne l'overlay del bordo e rifa' i
     * colori di sistema (le card trasparenti sono un colore). Vero se serve riavviare (un overlay nuovo si registra solo all'avvio).
     */
    static boolean apply(Context app) {
        String mode = ThemePrefs.getSettingsCards();
        String pkg = overlayPackage();
        String list = ModuleConstants.MODULE_DIR + "/enabled.list";
        boolean needsReboot = false;
        if (usesBorder(mode)) {
            try {
                ThemeCompiler.beginBatch();
                boolean registeredBefore = registered(pkg);
                boolean failed = ThemeCompiler.buildNamedInBatch("com.android.settings", GLASS_BORDER.equals(mode) ? ASSET_DIR_GLASS : ASSET_DIR, OVERLAY_NAME);
                if (!failed) {
                    Shell.cmd("grep -qx '" + pkg + "' " + list + " 2>/dev/null || echo '" + pkg + "' >> " + list).exec();
                    if (registeredBefore) {
                        OverlayUtil.enableOverlays(pkg);
                        ThemeCompiler.endBatch(Collections.singletonList(pkg));
                    } else {
                        needsReboot = true;
                    }
                }
            } catch (Throwable t) {
                android.util.Log.e("OxyTheme", "settings cards border failed", t);
            }
        } else if (registered(pkg)) {
            OverlayUtil.disableOverlays(pkg);
            Shell.cmd("sed -i '/^" + pkg + "$/d' " + list + " 2>/dev/null").exec();
        }
        // le card trasparenti sono un colore di sistema: si rifanno i colori
        AospTheme.apply(app, ThemePrefs.isDarkShadowEnabled(SystemColorsDialog.PKG));
        return needsReboot;
    }

    private static boolean registered(String pkg) {
        return !Shell.cmd("cmd overlay list --user 0 2>/dev/null | grep -F '" + pkg + "'").exec().getOut().isEmpty();
    }
}
