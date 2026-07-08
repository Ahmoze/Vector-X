/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 LSPosed Contributors
 */

package org.ahmoze.vector.manager.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.ahmoze.vector.manager.App;
import org.ahmoze.vector.manager.BuildConfig;
import org.ahmoze.vector.manager.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okio.Okio;

public class UpdateUtil {
    public static void loadRemoteVersion() {
        var request = new Request.Builder()
                .url("https://api.github.com/repos/Ahmoze/Vector-X/releases/latest")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();
        var callback = new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) return;
                var body = response.body();
                if (body == null) return;
                try {
                    var info = JsonParser.parseReader(body.charStream()).getAsJsonObject();
                    var notes = info.get("body").getAsString();
                    var tagName = info.has("tag_name") ? info.get("tag_name").getAsString() : "";
                    var assetsArray = info.getAsJsonArray("assets");
                    for (var assets : assetsArray) {
                        checkAssets(assets.getAsJsonObject(), notes, tagName);
                    }
                } catch (Throwable t) {
                    Log.e(App.TAG, t.getMessage(), t);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(App.TAG, "loadRemoteVersion: " + e.getMessage());
                var pref = App.getPreferences();
                if (pref.getBoolean("checked", false)) return;
                pref.edit().putBoolean("checked", true).apply();
            }
        };
        App.getOkHttpClient().newCall(request).enqueue(callback);
    }

    private static void checkAssets(JsonObject assets, String releaseNotes, String tagName) {
        var pref = App.getPreferences();
        var name = assets.get("name").getAsString();
        
        pref.edit()
                .putString("latest_version_name", tagName)
                .putLong("latest_check", Instant.now().getEpochSecond())
                .putString("release_notes", releaseNotes)
                .putString("zip_file", null)
                .putBoolean("checked", true)
                .apply();
                
        var updatedAt = Instant.parse(assets.get("updated_at").getAsString());
        var downloadUrl = assets.get("browser_download_url").getAsString();
        var zipTime = pref.getLong("zip_time", 0);
        if (!updatedAt.equals(Instant.ofEpochSecond(zipTime))) {
            var zip = downloadNewZipSync(downloadUrl, name);
            var size = assets.get("size").getAsLong();
            if (zip != null && zip.length() == size) {
                pref.edit()
                        .putLong("zip_time", updatedAt.getEpochSecond())
                        .putString("zip_file", zip.getAbsolutePath())
                        .apply();
            }
        }
    }

    private static int compareVersions(String v1, String v2) {
        if (v1 == null) v1 = "";
        if (v2 == null) v2 = "";
        v1 = v1.replace("v", "").replaceAll("[^0-9.]", "");
        v2 = v2.replace("v", "").replaceAll("[^0-9.]", "");
        if (v1.isEmpty() || v2.isEmpty()) return 0;
        
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int n1 = i < parts1.length && !parts1[i].isEmpty() ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length && !parts2[i].isEmpty() ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return Integer.compare(n1, n2);
        }
        return 0;
    }

    public static boolean needUpdate() {
        var pref = App.getPreferences();
        if (!pref.getBoolean("checked", false)) return false;
        var now = Instant.now();
        var buildTime = Instant.ofEpochSecond(BuildConfig.BUILD_TIME);
        var check = pref.getLong("latest_check", 0);
        if (check > 0) {
            var checkTime = Instant.ofEpochSecond(check);
            if (checkTime.atOffset(ZoneOffset.UTC).plusDays(30).toInstant().isBefore(now))
                return true;
            
            var latestVersionName = pref.getString("latest_version_name", "");
            if (latestVersionName != null && !latestVersionName.isEmpty()) {
                return compareVersions(latestVersionName, BuildConfig.VERSION_NAME) > 0;
            }
            return false;
        }
        return buildTime.atOffset(ZoneOffset.UTC).plusDays(30).toInstant().isBefore(now);
    }

    @Nullable
    private static File downloadNewZipSync(String url, String name) {
        var request = new Request.Builder().url(url).build();
        var zip = new File(App.getInstance().getCacheDir(), name);
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) return null;
            try (var source = body.source();
                 var sink = Okio.buffer(Okio.sink(zip))) {
                sink.writeAll(source);
            }
        } catch (IOException e) {
            Log.e(App.TAG, "downloadNewZipSync: " + e.getMessage());
            return null;
        }
        return zip;
    }
}
