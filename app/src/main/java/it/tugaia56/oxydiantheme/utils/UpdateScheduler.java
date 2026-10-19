package it.tugaia56.oxydiantheme.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

import java.util.concurrent.TimeUnit;

import it.tugaia56.oxydiantheme.R;

/** Pianifica (o annulla) il controllo periodico degli aggiornamenti, in base all'impostazione "Aggiornamento automatico". */
public final class UpdateScheduler {
    public static final String CHANNEL_ID = "updates";
    private static final int JOB_ID = 4701;

    private UpdateScheduler() {}

    public static void reschedule(Context ctx) {
        try {
            JobScheduler js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) return;
            if (!ThemePrefs.isAutoUpdate()) {
                js.cancel(JOB_ID);
                return;
            }
            // gia' pianificato: non si tocca (altrimenti ogni apertura rimanda il controllo)
            for (JobInfo j : js.getAllPendingJobs()) if (j.getId() == JOB_ID) return;
            js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(ctx, UpdateJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
                    .setPersisted(true)
                    .build());
        } catch (Throwable ignored) {}
    }

    public static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
        nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                ctx.getString(R.string.update_channel), NotificationManager.IMPORTANCE_DEFAULT));
    }
}
