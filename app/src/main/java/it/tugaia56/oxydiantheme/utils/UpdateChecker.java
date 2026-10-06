package it.tugaia56.oxydiantheme.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import it.tugaia56.oxydiantheme.BuildConfig;

/** Controllo aggiornamenti: legge l'ultima release di GitHub e la confronta con la versione installata. */
public final class UpdateChecker {
    public static final String REPO_URL = "https://github.com/tugaia56/Oxydian-Theme";
    private static final String API_URL = "https://api.github.com/repos/tugaia56/Oxydian-Theme/releases/latest";

    public static final class Result {
        public final boolean newer;
        public final String version, changelog, downloadUrl;

        Result(boolean newer, String version, String changelog, String downloadUrl) {
            this.newer = newer;
            this.version = version;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
        }
    }

    private UpdateChecker() {}

    /** Da chiamare fuori dal thread principale. Lancia un'eccezione se la rete o la risposta non vanno. */
    public static Result check() throws Exception {
        JSONObject release = fetch();
        String tag = release.getString("tag_name");
        String latest = tag.startsWith("v") ? tag.substring(1) : tag;
        String body = release.optString("body", "");
        String url = null;
        JSONArray assets = release.getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.optString("name", "").endsWith(".apk")) {
                url = a.getString("browser_download_url");
                break;
            }
        }
        return new Result(compare(latest, BuildConfig.VERSION_NAME) > 0, latest, body, url);
    }

    private static JSONObject fetch() throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(API_URL).openConnection();
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } finally {
            c.disconnect();
        }
    }

    private static int compare(String a, String b) {
        String[] pa = a.split("\\."), pb = b.split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            int va = i < pa.length ? num(pa[i]) : 0, vb = i < pb.length ? num(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int num(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); } catch (Throwable t) { return 0; }
    }
}
