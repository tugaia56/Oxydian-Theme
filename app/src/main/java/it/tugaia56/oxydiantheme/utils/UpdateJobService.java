package it.tugaia56.oxydiantheme.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;

import it.tugaia56.oxydiantheme.R;
import it.tugaia56.oxydiantheme.ui.MainActivity;

/** Controllo periodico in background: se c'e' una versione piu' nuova su GitHub, manda una notifica. */
public class UpdateJobService extends JobService {
    private static final int NOTIF_ID = 501;

    @Override
    public boolean onStartJob(JobParameters params) {
        new Thread(() -> {
            try {
                UpdateChecker.Result r = UpdateChecker.check();
                if (r.newer && r.downloadUrl != null) notifyNewVersion(this, r.version);
            } catch (Throwable ignored) {
                // niente rete o risposta strana: il prossimo giro riprova da solo
            }
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private static void notifyNewVersion(Context ctx, String version) {
        try {
            UpdateScheduler.ensureChannel(ctx);
            Intent open = new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(ctx, UpdateScheduler.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .setContentTitle(ctx.getString(R.string.update_notif_title))
                    .setContentText(ctx.getString(R.string.update_notif_body, version))
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .build();
            ctx.getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
        } catch (Throwable ignored) {
            // permesso notifiche non concesso: il controllo avviene comunque, l'avviso si vede aprendo l'app
        }
    }
}
