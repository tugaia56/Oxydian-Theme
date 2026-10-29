package it.tugaia56.oxydiantheme.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;

/** Finestre di dialogo con sfondo scuro e bordo del colore accento. */
final class Dialogs {
    private Dialogs() {}

    static void show(Context ctx, MaterialAlertDialogBuilder b) {
        AlertDialog d = b.create();
        style(ctx, d);
        d.show();
        int acc = ThemePrefs.accentColor();
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            android.widget.Button btn = d.getButton(which);
            if (btn != null) btn.setTextColor(acc);
        }
    }

    /** Finestra di lavoro in corso: non si chiude da sola ne' con un tocco fuori, mostra l'avanzamento e tiene lo schermo acceso. */
    static final class Busy {
        private final AlertDialog dialog;
        private final android.widget.TextView progress;
        private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());

        Busy(AlertDialog d, android.widget.TextView p) { dialog = d; progress = p; }

        void update(String text) { ui.post(() -> progress.setText(text)); }

        void dismissNow() {
            try { dialog.dismiss(); } catch (Throwable ignored) {}
        }

        void dismiss() {
            ui.post(() -> {
                try { dialog.dismiss(); } catch (Throwable ignored) {}
            });
        }
    }

    static Busy showBusy(Context ctx, int title, int message) {
        float d = ctx.getResources().getDisplayMetrics().density;
        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (22 * d);
        box.setPadding(pad, pad / 2, pad, 0);
        android.widget.TextView msg = new android.widget.TextView(ctx);
        msg.setText(message);
        msg.setTextColor(ctx.getColor(R.color.text));
        msg.setTextSize(15);
        box.addView(msg);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ThemePrefs.accentColor()));
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (18 * d);
        bar.setLayoutParams(lp);
        box.addView(bar);
        android.widget.TextView prog = new android.widget.TextView(ctx);
        prog.setTextColor(ctx.getColor(R.color.text_dim));
        prog.setTextSize(13);
        prog.setPadding(0, (int) (10 * d), 0, (int) (6 * d));
        box.addView(prog);
        AlertDialog dlg = new MaterialAlertDialogBuilder(ctx).setTitle(title).setView(box).setCancelable(false).create();
        style(ctx, dlg);
        if (dlg.getWindow() != null) dlg.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        dlg.show();
        return new Busy(dlg, prog);
    }

    static void style(Context ctx, Dialog d) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        // con uno sfondo con trasparenza il colore "card" diventa quasi trasparente: le finestre restano opache e leggibili
        int fill = ctx.getColor(R.color.card);
        if (android.graphics.Color.alpha(fill) < 0xFF) fill = 0xFF232A36;
        bg.setColor(fill);
        bg.setCornerRadius(18 * density);
        bg.setStroke((int) (2 * density), ThemePrefs.accentColor());
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(bg);
    }
}
