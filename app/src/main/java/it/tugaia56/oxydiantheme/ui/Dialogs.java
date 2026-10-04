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
    }

    static void style(Context ctx, Dialog d) {
        float density = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ctx.getColor(R.color.card));
        bg.setCornerRadius(18 * density);
        bg.setStroke((int) (2 * density), ThemePrefs.accentColor());
        if (d.getWindow() != null) d.getWindow().setBackgroundDrawable(bg);
    }
}
