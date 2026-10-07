package it.tugaia56.oxydiantheme.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import it.tugaia56.oxydiantheme.utils.ModuleSetup;
import it.tugaia56.oxydiantheme.utils.ThemePrefs;
import it.tugaia56.oxydiantheme.utils.overlay.OverlayUtil;
import it.tugaia56.oxydiantheme.utils.overlay.compiler.ThemeCompiler;

/**
 * Cambio dei colori di sistema da Tasker / MacroDroid, senza aprire l'app. Va acceso in
 * Impostazioni > Integrazione Tasker. Azione: it.tugaia56.oxydian.theme.action.APPLY_CONFIG
 * (con il pacchetto it.tugaia56.oxydian.theme). Extra (si mandano solo quelli da cambiare):
 *   accent, background        nome di un preset (es. Violet), "#RRGGBB" oppure "default"
 *   randomColor               true = accento a caso
 *   pitchBlack                true/false
 *   accentSaturation, backgroundSaturation   0..200 (100 = invariata)
 *   backgroundLightness       -10..10 (0 = invariata)
 * Funziona se i colori di sistema sono gia' stati applicati almeno una volta dall'app.
 */
public class TaskerReceiver extends BroadcastReceiver {
    public static final String ACTION = "it.tugaia56.oxydian.theme.action.APPLY_CONFIG";
    private static final String TAG = "OxyTheme";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        if (!ThemePrefs.isTaskerEnabled()) {
            Log.w(TAG, "Tasker: integrazione spenta, richiesta ignorata");
            return;
        }
        final Bundle extras = intent.getExtras() == null ? new Bundle() : new Bundle(intent.getExtras());
        final Context app = context.getApplicationContext();
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                apply(app, extras);
            } catch (Throwable t) {
                Log.e(TAG, "Tasker: errore", t);
            } finally {
                pending.finish();
            }
        }).start();
    }

    private static Integer intExtra(Bundle b, String key) {
        Object o = b.get(key);
        if (o instanceof Number) return ((Number) o).intValue();
        if (o instanceof String) {
            try { return Integer.parseInt(((String) o).trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static Boolean boolExtra(Bundle b, String key) {
        Object o = b.get(key);
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof String) return Boolean.parseBoolean(((String) o).trim());
        return null;
    }

    /** Applica "accent"/"background": preset, colore #RRGGBB o predefinito. */
    private static void setColor(Context ctx, Bundle b, String key, String group) {
        Object o = b.get(key);
        if (!(o instanceof String)) return;
        String v = ((String) o).trim();
        String pkg = SystemColorsDialog.PKG;
        if (v.equalsIgnoreCase("default") || v.isEmpty()) {
            ThemePrefs.setOption(pkg, group, "");
        } else if (v.startsWith("#")) {
            try {
                int c = Color.parseColor(v) | 0xFF000000;
                ThemePrefs.setCustomColor(pkg + "_" + group, c);
                ThemePrefs.setOption(pkg, group, OptionGroupsDialog.CUSTOM);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Tasker: colore non valido " + v);
            }
        } else {
            String m = SystemColorsDialog.matchPreset(ctx, group, v);
            if (m != null) ThemePrefs.setOption(pkg, group, m);
            else Log.w(TAG, "Tasker: preset sconosciuto " + v);
        }
    }

    private static void apply(Context ctx, Bundle b) throws Exception {
        String pkg = SystemColorsDialog.PKG;
        setColor(ctx, b, "accent", "accent");
        setColor(ctx, b, "background", "background");
        if (Boolean.TRUE.equals(boolExtra(b, "randomColor"))) {
            float[] hsv = {new Random().nextInt(360), 0.55f + new Random().nextFloat() * 0.35f, 0.8f + new Random().nextFloat() * 0.2f};
            ThemePrefs.setCustomColor(pkg + "_accent", Color.HSVToColor(hsv) | 0xFF000000);
            ThemePrefs.setOption(pkg, "accent", OptionGroupsDialog.CUSTOM);
        }
        Boolean pitch = boolExtra(b, "pitchBlack");
        if (pitch != null) ThemePrefs.setBgPitch(pitch);
        Integer as = intExtra(b, "accentSaturation");
        if (as != null && as >= 0 && as <= 200) ThemePrefs.setAccSat(as);
        Integer bs = intExtra(b, "backgroundSaturation");
        if (bs != null && bs >= 0 && bs <= 200) ThemePrefs.setBgSat(bs);
        Integer bl = intExtra(b, "backgroundLightness");
        if (bl != null && bl >= -10 && bl <= 10) ThemePrefs.setBgLight(bl);

        SystemColorsDialog.commitFromPrefs(ctx);
        rebuildSystemColors(ctx);
    }

    /** Rifa l'overlay dei colori di sistema (se era gia' installato) e lo aggiorna senza riavvio. */
    private static void rebuildSystemColors(Context ctx) throws Exception {
        String pkg = SystemColorsDialog.PKG;
        String dir = "SYS";
        String name = "DS_" + dir;
        String overlay = ThemeCompiler.namedPackage(name);
        ModuleSetup.ensure();
        Set<String> enabled = MainActivity.enabledOverlays();
        Map<String, String> states = MainActivity.overlayStates();
        boolean active = enabled.contains(overlay);
        boolean known = "[ ]".equals(states.get(overlay));
        if (!active && !known) {
            Log.w(TAG, "Tasker: i colori di sistema non sono mai stati applicati dall'app, niente da aggiornare");
            return;
        }
        List<String> refresh = new ArrayList<>();
        if (active) refresh.add(overlay);
        boolean failed;
        ThemeCompiler.beginBatch();
        try {
            failed = ThemeCompiler.buildNamedInBatch(pkg, dir, name,
                    MainActivity.optionPaths(ctx, pkg), MainActivity.customValuesXml(ctx, pkg, true));
        } finally {
            ThemeCompiler.endBatch(refresh);
        }
        if (failed) {
            Log.e(TAG, "Tasker: compilazione non riuscita");
            return;
        }
        ThemePrefs.setDarkShadowBuilt(pkg, MainActivity.signature(ctx) + MainActivity.optsKey(ctx, pkg, true));
        if (!active) OverlayUtil.enableOverlays(new String[]{overlay});
    }
}
