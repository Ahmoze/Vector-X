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

import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.ahmoze.vector.manager.App;
import org.ahmoze.vector.manager.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Okio;

public class UpdateUtil {
    private static final String RELEASES_API_URL = "https://api.github.com/repos/Ahmoze/Vector-X/releases/latest";
    private static final String OTA_JSON_URL = "https://raw.githubusercontent.com/Ahmoze/Vector-X/master/zygisk/update.json";
    private static final long REFRESH_INTERVAL_SECONDS = 300L;
    private static final long MIN_VALID_ZIP_BYTES = 100_000L;

    public interface UpdateListener {
        void onUpdateStateChanged();
    }

    public interface ZipDownloadCallback {
        void onReady(@NonNull File zipFile);

        void onError(@NonNull String message);
    }

    private static final class RemoteReleaseInfo {
        final String tagName;
        final String releaseTitle;
        final int versionCode;
        final String releaseNotes;
        final String assetName;
        final String downloadUrl;
        final long assetSize;
        final long updatedAtEpoch;

        RemoteReleaseInfo(String tagName, String releaseTitle, int versionCode, String releaseNotes,
                          String assetName, String downloadUrl, long assetSize, long updatedAtEpoch) {
            this.tagName = tagName;
            this.releaseTitle = releaseTitle;
            this.versionCode = versionCode;
            this.releaseNotes = releaseNotes;
            this.assetName = assetName;
            this.downloadUrl = downloadUrl;
            this.assetSize = assetSize;
            this.updatedAtEpoch = updatedAtEpoch;
        }
    }

    private static final CopyOnWriteArraySet<UpdateListener> listeners = new CopyOnWriteArraySet<>();
    private static final AtomicBoolean checking = new AtomicBoolean(false);
    private static final Object DOWNLOAD_LOCK = new Object();

    public static void addListener(@NonNull UpdateListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(@NonNull UpdateListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (var listener : listeners) {
            try {
                listener.onUpdateStateChanged();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void loadRemoteVersionIfNeeded() {
        var pref = App.getPreferences();
        boolean checked = pref.getBoolean("checked", false);
        long lastCheck = pref.getLong("latest_check", 0L);
        long now = Instant.now().getEpochSecond();
        if (!checked || (now - lastCheck) >= REFRESH_INTERVAL_SECONDS || (needUpdate() && getValidCachedZip() == null)) {
            loadRemoteVersion();
        }
    }

    public static void loadRemoteVersion() {
        if (!checking.compareAndSet(false, true)) {
            return;
        }
        App.getExecutorService().submit(() -> {
            try {
                RemoteReleaseInfo info = fetchAndPersistMetadataSync();
                if (info == null) {
                    var pref = App.getPreferences();
                    if (!pref.getBoolean("checked", false)) {
                        pref.edit().putBoolean("checked", true).apply();
                    }
                    return;
                }
                notifyListeners();

                boolean updateAvailable = isRemoteNewer(info.tagName, info.releaseTitle, info.versionCode);
                if (!updateAvailable) {
                    cleanupStaleCachedZip(info.assetName);
                    return;
                }
                if (!TextUtils.isEmpty(info.downloadUrl) && !TextUtils.isEmpty(info.assetName)) {
                    downloadNewZipSync(info.downloadUrl, info.assetName, info.assetSize, info.updatedAtEpoch, null);
                }
            } catch (Throwable t) {
                Log.e(App.TAG, "loadRemoteVersion: " + t.getMessage(), t);
            } finally {
                checking.set(false);
                notifyListeners();
            }
        });
    }

    public static void ensureUpdateZip(@NonNull ZipDownloadCallback callback) {
        App.getExecutorService().submit(() -> {
            synchronized (DOWNLOAD_LOCK) {
                File existing = getValidCachedZip();
                if (existing != null) {
                    callback.onReady(existing);
                    return;
                }

                var pref = App.getPreferences();
                String downloadUrl = pref.getString("latest_download_url", "");
                String assetName = pref.getString("latest_asset_name", "");
                long expectedSize = pref.getLong("latest_asset_size", -1L);
                long updatedAtEpoch = pref.getLong("zip_time", 0L);

                if (TextUtils.isEmpty(downloadUrl) || TextUtils.isEmpty(assetName)) {
                    RemoteReleaseInfo fresh = fetchAndPersistMetadataSync();
                    if (fresh != null) {
                        downloadUrl = fresh.downloadUrl;
                        assetName = fresh.assetName;
                        expectedSize = fresh.assetSize;
                        updatedAtEpoch = fresh.updatedAtEpoch;
                        notifyListeners();
                    }
                }

                if (TextUtils.isEmpty(downloadUrl) || TextUtils.isEmpty(assetName)) {
                    callback.onError("Release download URL not available");
                    return;
                }

                String[] errorHolder = new String[1];
                File downloaded = downloadNewZipSync(downloadUrl, assetName, expectedSize, updatedAtEpoch, errorHolder);
                if (downloaded != null) {
                    callback.onReady(downloaded);
                    return;
                }

                // Refresh release metadata once in case the previous release URL or size changed.
                RemoteReleaseInfo refreshed = fetchAndPersistMetadataSync();
                if (refreshed != null && !TextUtils.isEmpty(refreshed.downloadUrl) && !TextUtils.isEmpty(refreshed.assetName)) {
                    notifyListeners();
                    errorHolder[0] = null;
                    downloaded = downloadNewZipSync(refreshed.downloadUrl, refreshed.assetName, refreshed.assetSize, refreshed.updatedAtEpoch, errorHolder);
                    if (downloaded != null) {
                        callback.onReady(downloaded);
                        return;
                    }
                }

                callback.onError(errorHolder[0] != null ? errorHolder[0] : "Download failed");
            }
        });
    }

    @Nullable
    public static File getValidCachedZip() {
        var pref = App.getPreferences();
        String path = pref.getString("zip_file", null);
        if (TextUtils.isEmpty(path)) return null;
        File file = new File(path);
        String expectedName = pref.getString("latest_asset_name", null);
        long expectedSize = pref.getLong("latest_asset_size", -1L);
        return isZipFileValid(file, expectedSize, expectedName) ? file : null;
    }

    private static boolean isZipFileValid(@Nullable File file, long expectedSize, @Nullable String expectedName) {
        if (file == null || !file.isFile()) return false;
        if (!TextUtils.isEmpty(expectedName) && !expectedName.equals(file.getName())) return false;
        long len = file.length();
        if (len <= 0) return false;
        if (expectedSize > 0 && len != expectedSize) return false;
        if (expectedSize <= 0 && len < MIN_VALID_ZIP_BYTES) return false;
        byte[] magic = new byte[2];
        try (var fis = new FileInputStream(file)) {
            if (fis.read(magic) < 2) return false;
        } catch (IOException e) {
            return false;
        }
        return magic[0] == 'P' && magic[1] == 'K';
    }

    @Nullable
    private static RemoteReleaseInfo fetchAndPersistMetadataSync() {
        RemoteReleaseInfo info = fetchFromGitHubApiSync();
        if (info == null) {
            info = loadFromOtaJsonFallback();
        }
        if (info != null) {
            persistMetadata(info);
        }
        return info;
    }

    @Nullable
    private static RemoteReleaseInfo fetchFromGitHubApiSync() {
        var request = new Request.Builder()
                .url(RELEASES_API_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) {
                Log.w(App.TAG, "fetchFromGitHubApiSync HTTP " + response.code());
                return null;
            }
            var info = JsonParser.parseReader(body.charStream()).getAsJsonObject();
            String notes = getStringOrDefault(info, "body", "");
            String tagName = getStringOrDefault(info, "tag_name", "");
            String releaseTitle = getStringOrDefault(info, "name", "");
            var assetsArray = info.has("assets") && info.get("assets").isJsonArray()
                    ? info.getAsJsonArray("assets") : null;
            if (assetsArray == null) return null;

            JsonObject targetAsset = null;
            String preferredSuffix = BuildConfig.DEBUG ? "-debug.zip" : "-release.zip";
            for (var assets : assetsArray) {
                var obj = assets.getAsJsonObject();
                var assetName = getStringOrDefault(obj, "name", "").toLowerCase(Locale.ROOT);
                if (assetName.endsWith(preferredSuffix)) {
                    targetAsset = obj;
                    break;
                } else if (targetAsset == null && assetName.endsWith(".zip")) {
                    targetAsset = obj;
                }
            }
            if (targetAsset == null) return null;

            String assetName = getStringOrDefault(targetAsset, "name", "");
            String downloadUrl = getStringOrDefault(targetAsset, "browser_download_url", "");
            long assetSize = targetAsset.has("size") && !targetAsset.get("size").isJsonNull()
                    ? targetAsset.get("size").getAsLong() : -1L;
            String updatedAtStr = getStringOrDefault(targetAsset, "updated_at", "");
            long updatedAtEpoch = 0L;
            if (!TextUtils.isEmpty(updatedAtStr)) {
                try {
                    updatedAtEpoch = Instant.parse(updatedAtStr).getEpochSecond();
                } catch (Exception ignored) {
                }
            }
            int remoteBuild = extractBuildCode(releaseTitle, tagName);
            if (remoteBuild <= 0) {
                remoteBuild = fetchOtaVersionCodeForTagSync(tagName);
            }
            return new RemoteReleaseInfo(tagName, releaseTitle, remoteBuild, notes, assetName, downloadUrl, assetSize, updatedAtEpoch);
        } catch (Throwable t) {
            Log.e(App.TAG, "fetchFromGitHubApiSync: " + t.getMessage(), t);
            return null;
        }
    }

    private static int fetchOtaVersionCodeForTagSync(String tagName) {
        if (TextUtils.isEmpty(tagName)) return 0;
        var request = new Request.Builder()
                .url(OTA_JSON_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) return 0;
            var obj = JsonParser.parseReader(body.charStream()).getAsJsonObject();
            String otaVersion = getStringOrDefault(obj, "version", "");
            if (!tagName.equals(otaVersion)) return 0;
            return obj.has("versionCode") && !obj.get("versionCode").isJsonNull()
                    ? obj.get("versionCode").getAsInt() : 0;
        } catch (Throwable t) {
            Log.w(App.TAG, "fetchOtaVersionCodeForTagSync: " + t.getMessage());
            return 0;
        }
    }

    @Nullable
    private static RemoteReleaseInfo loadFromOtaJsonFallback() {
        var request = new Request.Builder()
                .url(OTA_JSON_URL)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) {
                Log.w(App.TAG, "loadFromOtaJsonFallback HTTP " + response.code());
                return null;
            }
            var obj = JsonParser.parseReader(body.charStream()).getAsJsonObject();
            String version = getStringOrDefault(obj, "version", "");
            int versionCode = obj.has("versionCode") && !obj.get("versionCode").isJsonNull()
                    ? obj.get("versionCode").getAsInt() : 0;
            String zipUrl = getStringOrDefault(obj, "zipUrl", "");
            String changelogUrl = getStringOrDefault(obj, "changelog", "");
            if (TextUtils.isEmpty(version) || TextUtils.isEmpty(zipUrl)) {
                return null;
            }

            int slashIdx = zipUrl.lastIndexOf('/');
            String assetName = slashIdx >= 0 && slashIdx < zipUrl.length() - 1
                    ? zipUrl.substring(slashIdx + 1)
                    : "Vector-" + version + "-Release.zip";

            String releaseNotes = "";
            if (!TextUtils.isEmpty(changelogUrl) && isRemoteNewer(version, "Vector-X " + version, versionCode)) {
                releaseNotes = fetchChangelogSectionSync(changelogUrl, version);
            }
            return new RemoteReleaseInfo(version, "Vector-X " + version, versionCode, releaseNotes, assetName, zipUrl, -1L, 0L);
        } catch (Throwable t) {
            Log.e(App.TAG, "loadFromOtaJsonFallback: " + t.getMessage(), t);
            return null;
        }
    }

    private static String fetchChangelogSectionSync(String changelogUrl, String version) {
        var request = new Request.Builder()
                .url(changelogUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();
        try (Response response = App.getOkHttpClient().newCall(request).execute()) {
            var body = response.body();
            if (!response.isSuccessful() || body == null) return "";
            String content = body.string();
            String marker = "🚀 **What's New in " + version;
            int start = content.indexOf(marker);
            if (start == -1) return "";
            int nextRel = content.indexOf("🚀 **What's New in v", start + marker.length());
            String section = (nextRel != -1 ? content.substring(start, nextRel) : content.substring(start)).trim();
            while (section.endsWith("---")) {
                section = section.substring(0, section.length() - 3).trim();
            }
            return section;
        } catch (Throwable t) {
            Log.w(App.TAG, "fetchChangelogSectionSync: " + t.getMessage());
            return "";
        }
    }

    private static void persistMetadata(RemoteReleaseInfo info) {
        var pref = App.getPreferences();
        String prevAsset = pref.getString("latest_asset_name", "");
        SharedPreferences.Editor editor = pref.edit()
                .putString("latest_version_name", info.tagName)
                .putString("latest_release_title", info.releaseTitle)
                .putInt("latest_version_code", info.versionCode)
                .putString("latest_download_url", info.downloadUrl)
                .putString("latest_asset_name", info.assetName)
                .putLong("latest_asset_size", info.assetSize)
                .putLong("latest_check", Instant.now().getEpochSecond())
                .putBoolean("checked", true);
        if (!TextUtils.isEmpty(info.releaseNotes)) {
            editor.putString("release_notes", info.releaseNotes);
        }
        if (!TextUtils.equals(prevAsset, info.assetName)) {
            editor.putString("zip_file", null);
        }
        editor.apply();
    }

    private static void cleanupStaleCachedZip(@Nullable String currentAssetName) {
        var pref = App.getPreferences();
        String existingPath = pref.getString("zip_file", null);
        if (!TextUtils.isEmpty(existingPath)) {
            File oldFile = new File(existingPath);
            if (oldFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                oldFile.delete();
            }
        }
        if (!TextUtils.isEmpty(currentAssetName)) {
            File namedFile = new File(App.getInstance().getCacheDir(), currentAssetName);
            if (namedFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                namedFile.delete();
            }
        }
        pref.edit().putString("zip_file", null).apply();
    }

    private static boolean isRemoteNewer(String latestVersionName, String latestReleaseTitle,
                                         int latestVersionCode) {
        if (latestVersionName != null && !latestVersionName.isEmpty()) {
            int cmp = compareVersions(latestVersionName, BuildConfig.VERSION_NAME);
            if (cmp > 0) return true;
            if (cmp < 0) return false;
            int effectiveCode = latestVersionCode > 0
                    ? latestVersionCode
                    : extractBuildCode(latestReleaseTitle, latestVersionName);
            return effectiveCode > BuildConfig.VERSION_CODE;
        }
        return latestVersionCode > BuildConfig.VERSION_CODE;
    }

    private static int extractBuildCode(String... sources) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:\\(|\\-)(\\d{4,6})(?:\\)|$)");
        for (String source : sources) {
            if (source == null || source.isEmpty()) continue;
            java.util.regex.Matcher m = pattern.matcher(source);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
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

        var latestVersionName = pref.getString("latest_version_name", "");
        var latestReleaseTitle = pref.getString("latest_release_title", "");
        int latestVersionCode = pref.getInt("latest_version_code", 0);
        return isRemoteNewer(latestVersionName, latestReleaseTitle, latestVersionCode);
    }

    @Nullable
    private static File downloadNewZipSync(String url, String name, long expectedSize,
                                           long updatedAtEpoch, @Nullable String[] errorHolder) {
        synchronized (DOWNLOAD_LOCK) {
            var pref = App.getPreferences();
            var cacheDir = App.getInstance().getCacheDir();
            var targetZip = new File(cacheDir, name);
            var zipTime = pref.getLong("zip_time", 0L);

            if ((updatedAtEpoch <= 0 || updatedAtEpoch == zipTime) && isZipFileValid(targetZip, expectedSize, name)) {
                pref.edit().putString("zip_file", targetZip.getAbsolutePath()).apply();
                return targetZip;
            }

            var partFile = new File(cacheDir, name + ".part");
            OkHttpClient noCacheClient = App.getOkHttpClient().newBuilder().cache(null).build();
            var request = new Request.Builder()
                    .url(url)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build();
            try (Response response = noCacheClient.newCall(request).execute()) {
                var body = response.body();
                if (!response.isSuccessful() || body == null) {
                    String err = "HTTP " + response.code();
                    Log.e(App.TAG, "downloadNewZipSync: " + err);
                    if (errorHolder != null) errorHolder[0] = err;
                    return null;
                }
                if (partFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    partFile.delete();
                }
                try (var source = body.source();
                     var sink = Okio.buffer(Okio.sink(partFile))) {
                    sink.writeAll(source);
                    sink.flush();
                }

                if (!isZipFileValid(partFile, expectedSize, null)) {
                    String err = "Downloaded archive verification failed (size=" + partFile.length() + ", expected=" + expectedSize + ")";
                    Log.e(App.TAG, "downloadNewZipSync: " + err);
                    if (errorHolder != null) errorHolder[0] = err;
                    return null;
                }

                if (targetZip.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    targetZip.delete();
                }
                if (!partFile.renameTo(targetZip)) {
                    String err = "Failed to finalize archive " + targetZip.getName();
                    Log.e(App.TAG, "downloadNewZipSync: " + err);
                    if (errorHolder != null) errorHolder[0] = err;
                    return null;
                }

                pref.edit()
                        .putLong("zip_time", updatedAtEpoch > 0 ? updatedAtEpoch : Instant.now().getEpochSecond())
                        .putString("zip_file", targetZip.getAbsolutePath())
                        .apply();
                return targetZip;
            } catch (IOException e) {
                String err = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Log.e(App.TAG, "downloadNewZipSync: " + err, e);
                if (errorHolder != null) errorHolder[0] = err;
                return null;
            } finally {
                if (partFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    partFile.delete();
                }
            }
        }
    }

    private static String getStringOrDefault(JsonObject obj, String key, String defValue) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return defValue;
        return obj.get(key).getAsString();
    }
}
