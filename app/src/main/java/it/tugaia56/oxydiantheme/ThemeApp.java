package it.tugaia56.oxydiantheme;

import android.app.Application;
import android.content.Context;

import com.topjohnwu.superuser.Shell;

public class ThemeApp extends Application {
    private static Context appContext;

    static {
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(20));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }
}
