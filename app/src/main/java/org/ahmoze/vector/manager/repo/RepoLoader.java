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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.ahmoze.vector.manager.repo;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.ahmoze.vector.manager.App;
import org.ahmoze.vector.manager.R;
import org.ahmoze.vector.manager.repo.model.OnlineModule;
import org.ahmoze.vector.manager.repo.model.Release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RepoLoader {
    private static RepoLoader instance = null;
    private volatile Map<String, OnlineModule> onlineModules = new HashMap<>();
    private volatile Map<String, ModuleVersion> latestVersion = new ConcurrentHashMap<>();

    public static final Map<String, String> LEGACY_PACKAGE_SUCCESSORS = Map.of(
            "com.coderstory.toolkit", "org.lsposed.corepatch",
            "com.seepd.toki", "io.github.meiyongai.toki",
            "cn.wankkoree.xposed.portrait2landscape", "cn.wankkoree.xp.portrait2landscape",
            "cn.wankkoree.xposed.enablewebviewdebugging", "cn.wankkoree.xp.webviewpp"
    );

    @Nullable
    public static String getSuccessorPackage(@Nullable String packageName) {
        return packageName != null ? LEGACY_PACKAGE_SUCCESSORS.get(packageName) : null;
    }

    @Nullable
    public static String getPredecessorPackage(@Nullable String packageName) {
        if (packageName == null) return null;
        for (var entry : LEGACY_PACKAGE_SUCCESSORS.entrySet()) {
            if (packageName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    public String resolveTargetPackageName(@Nullable String packageName) {
        if (packageName == null) return null;
        String successor = LEGACY_PACKAGE_SUCCESSORS.get(packageName);
        if (successor != null && onlineModules.containsKey(successor)) {
            return successor;
        }
        return packageName;
    }

    public static class ModuleVersion {
        public String versionName;
        public long versionCode;
        public boolean hasValidCode;
        public boolean isSuccessorMigration;
        @Nullable
        public String targetPackageName;

        private ModuleVersion(long versionCode, String versionName, boolean hasValidCode) {
            this(versionCode, versionName, hasValidCode, false, null);
        }

        private ModuleVersion(long versionCode, String versionName, boolean hasValidCode, boolean isSuccessorMigration, @Nullable String targetPackageName) {
            this.versionName = versionName != null ? versionName.trim() : "";
            this.versionCode = versionCode;
            this.hasValidCode = hasValidCode;
            this.isSuccessorMigration = isSuccessorMigration;
            this.targetPackageName = targetPackageName;
        }

        public boolean upgradable(long installedCode, String installedName) {
            if (isSuccessorMigration && targetPackageName != null) {
                var util = org.ahmoze.vector.manager.util.ModuleUtil.getInstance();
                return util == null || util.getModule(targetPackageName) == null;
            }
            if (hasValidCode && this.versionCode > 0 && installedCode > 0) {
                if (this.versionCode != installedCode) {
                    return this.versionCode > installedCode;
                }
                if (this.versionName != null && !this.versionName.isEmpty() && installedName != null && !installedName.isEmpty()) {
                    return compareVersionNames(this.versionName, installedName) > 0;
                }
                return false;
            }
            if (this.versionName != null && !this.versionName.isEmpty() && installedName != null && !installedName.isEmpty()) {
                return compareVersionNames(this.versionName, installedName) > 0;
            }
            return false;
        }

        public static int compareVersionNames(String remote, String local) {
            if (remote == null || local == null) return 0;
            String r = extractVersionToken(remote);
            String l = extractVersionToken(local);
            if (r.equals(l)) return 0;
            String[] rParts = r.split("[._\\-+]+");
            String[] lParts = l.split("[._\\-+]+");
            int len = Math.max(rParts.length, lParts.length);
            for (int i = 0; i < len; i++) {
                String rp = i < rParts.length ? rParts[i] : "0";
                String lp = i < lParts.length ? lParts[i] : "0";
                if (rp.equals(lp)) continue;
                boolean rNum = rp.matches("\\d+");
                boolean lNum = lp.matches("\\d+");
                if (rNum && lNum) {
                    try {
                        long rv = Long.parseLong(rp);
                        long lv = Long.parseLong(lp);
                        if (rv != lv) return Long.compare(rv, lv);
                    } catch (NumberFormatException ignored) {
                        int c = rp.compareToIgnoreCase(lp);
                        if (c != 0) return c;
                    }
                } else {
                    int c = rp.compareToIgnoreCase(lp);
                    if (c != 0) return c;
                }
            }
            return 0;
        }

        private static String extractVersionToken(String raw) {
            String s = raw.trim();
            if (s.startsWith("v") || s.startsWith("V")) {
                s = s.substring(1).trim();
            }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(?:[._\\-]\\d+)+[A-Za-z0-9._\\-]*)").matcher(s);
            if (m.find()) {
                return m.group(1);
            }
            return s.replace(' ', '_');
        }
    }

    private final Path repoFile = Paths.get(App.getInstance().getFilesDir().getAbsolutePath(), "repo.json");
    private final Set<RepoListener> listeners = ConcurrentHashMap.newKeySet();
    private volatile boolean repoLoaded = false;
    private volatile boolean isRefreshing = false;
    private volatile long lastRemoteLoadTime = 0L;
    private static final long AUTO_REFRESH_INTERVAL_MS = 15 * 60 * 1000L;
    private static final String originRepoUrl = "https://raw.githubusercontent.com/Ahmoze/Vector-X/gh-pages/";
    private static final String backupRepoUrl = "https://ahmoze.github.io/Vector-X/";
    private static final String lsposedModuleDetailBaseUrl = "https://modules.lsposed.org/";
    private final Resources resources = App.getInstance().getResources();
    private final String[] channels = resources.getStringArray(R.array.update_channel_values);

    public boolean isRepoLoaded() {
        return repoLoaded;
    }

    public static synchronized RepoLoader getInstance() {
        if (instance == null) {
            instance = new RepoLoader();
            App.getExecutorService().submit(() -> instance.loadLocalData(true));
        }
        return instance;
    }

    public static String getRepoUrl() {
        String customUrl = App.getPreferences().getString("custom_repo_url", "");
        if (customUrl != null && !customUrl.trim().isEmpty()) {
            if (!customUrl.endsWith("/")) customUrl += "/";
            return customUrl;
        }
        return originRepoUrl;
    }

    private volatile long lastInstalledRemoteCheckTime = 0L;

    public void loadRemoteDataIfNeeded() {
        if (isRefreshing) return;
        long now = System.currentTimeMillis();
        boolean fileStale = true;
        try {
            if (Files.exists(repoFile)) {
                long fileModified = Files.getLastModifiedTime(repoFile).toMillis();
                fileStale = (now - fileModified) > AUTO_REFRESH_INTERVAL_MS;
            }
        } catch (Exception ignored) {
        }
        if ((now - lastRemoteLoadTime) > AUTO_REFRESH_INTERVAL_MS || fileStale || latestVersion.isEmpty()) {
            App.getExecutorService().submit(this::loadRemoteData);
        } else {
            refreshInstalledModulesIfNeeded();
        }
    }

    public void refreshInstalledModulesIfNeeded() {
        long now = System.currentTimeMillis();
        if ((now - lastInstalledRemoteCheckTime) < AUTO_REFRESH_INTERVAL_MS) return;
        var installedMap = org.ahmoze.vector.manager.util.ModuleUtil.getInstance().getModules();
        if (installedMap == null || installedMap.isEmpty() || !repoLoaded) return;
        lastInstalledRemoteCheckTime = now;
        App.getExecutorService().submit(this::refreshInstalledModulesRemote);
    }

    private static void mergeModulePreservingAssets(@Nullable OnlineModule existing, @NonNull OnlineModule detail) {
        if (existing == null || existing.getReleases() == null || existing.getReleases().isEmpty()) return;
        List<Release> detailRels = detail.getReleases() != null ? new ArrayList<>(detail.getReleases()) : new ArrayList<>();
        for (Release exRel : existing.getReleases()) {
            if (exRel == null) continue;
            Release matched = null;
            for (Release dr : detailRels) {
                if (dr != null && java.util.Objects.equals(dr.getTagName(), exRel.getTagName())) {
                    matched = dr;
                    break;
                }
            }
            if (matched == null) {
                detailRels.add(0, exRel);
            } else if (exRel.getReleaseAssets() != null && !exRel.getReleaseAssets().isEmpty()) {
                List<org.ahmoze.vector.manager.repo.model.ReleaseAsset> assets =
                        matched.getReleaseAssets() != null ? new ArrayList<>(matched.getReleaseAssets()) : new ArrayList<>();
                for (var exAsset : exRel.getReleaseAssets()) {
                    if (exAsset != null && assets.stream().noneMatch(a -> a != null && java.util.Objects.equals(a.getName(), exAsset.getName()))) {
                        assets.add(0, exAsset);
                    }
                }
                matched.setReleaseAssets(assets);
            }
        }
        detail.setReleases(detailRels);
    }

    private void refreshInstalledModulesRemote() {
        var installedMap = org.ahmoze.vector.manager.util.ModuleUtil.getInstance().getModules();
        if (installedMap == null || installedMap.isEmpty()) return;
        Set<String> pkgNames = new java.util.HashSet<>();
        for (var entry : installedMap.entrySet()) {
            String pkg = entry.getKey().first;
            if (pkg != null) {
                pkgNames.add(pkg);
                String successor = LEGACY_PACKAGE_SUCCESSORS.get(pkg);
                if (successor != null) {
                    pkgNames.add(successor);
                }
            }
        }
        if (pkgNames.isEmpty()) return;
        Gson gson = new Gson();
        boolean anyUpdated = false;
        for (String pkg : pkgNames) {
            try (var response = App.getOkHttpClient().newCall(
                    new Request.Builder()
                            .url(String.format(lsposedModuleDetailBaseUrl + "module/%s.json", pkg))
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .header("Cache-Control", "no-cache")
                            .build()
            ).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String bodyStr = response.body().string();
                    OnlineModule detail = gson.fromJson(bodyStr, OnlineModule.class);
                    if (detail != null && detail.getName() != null) {
                        mergeModulePreservingAssets(onlineModules.get(detail.getName()), detail);
                        detail.releasesLoaded = true;
                        onlineModules.put(detail.getName(), detail);
                        anyUpdated = true;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (anyUpdated) {
            var channel = App.getPreferences().getString("update_channel", channels[0]);
            updateLatestVersion(onlineModules.values().toArray(new OnlineModule[0]), channel);
            try {
                String fullJson = gson.toJson(onlineModules.values());
                Files.write(repoFile, fullJson.getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) {
            }
        }
    }

    public synchronized void mergeOnlineModule(OnlineModule module, boolean notifyReleasesLoaded) {
        if (module == null || module.getName() == null) return;
        module.releasesLoaded = true;
        onlineModules.put(module.getName(), module);
        var channel = App.getPreferences().getString("update_channel", channels[0]);
        updateLatestVersion(onlineModules.values().toArray(new OnlineModule[0]), channel);
        if (notifyReleasesLoaded) {
            for (RepoListener listener : listeners) {
                listener.onModuleReleasesLoaded(module);
            }
        }
    }

    private static class ModuleListResponse {
        @com.google.gson.annotations.SerializedName("modules")
        public List<OnlineModule> modules;
        @com.google.gson.annotations.SerializedName("pageCount")
        public int pageCount;
    }

    private List<OnlineModule> parseModulesJson(Gson gson, String bodyString) {
        List<OnlineModule> allModules = new ArrayList<>();
        if (bodyString == null || bodyString.trim().isEmpty()) return allModules;
        String trimmed = bodyString.trim();
        try {
            if (trimmed.startsWith("[")) {
                OnlineModule[] arr = gson.fromJson(trimmed, OnlineModule[].class);
                if (arr != null) {
                    allModules.addAll(Arrays.asList(arr));
                }
            } else {
                ModuleListResponse res = gson.fromJson(trimmed, ModuleListResponse.class);
                if (res != null && res.modules != null) {
                    allModules.addAll(res.modules);
                }
            }
        } catch (Exception e) {
            try {
                ModuleListResponse res = gson.fromJson(trimmed, ModuleListResponse.class);
                if (res != null && res.modules != null) {
                    allModules.addAll(res.modules);
                }
            } catch (Exception ignored) {
            }
        }
        return allModules;
    }

    synchronized public void loadRemoteData() {
        if (isRefreshing) return;
        isRefreshing = true;
        String[] candidateUrls;
        String configuredUrl = getRepoUrl();
        if (configuredUrl.equals(originRepoUrl)) {
            candidateUrls = new String[]{originRepoUrl, backupRepoUrl};
        } else {
            candidateUrls = new String[]{configuredUrl, originRepoUrl, backupRepoUrl};
        }

        Throwable lastError = null;
        Gson gson = new Gson();
        try {
            for (String baseUrl : candidateUrls) {
                try (var response = App.getOkHttpClient().newCall(
                        new Request.Builder()
                                .url(baseUrl + "modules.json")
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .header("Cache-Control", "no-cache")
                                .build()
                ).execute()) {
                    if (response.isSuccessful()) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            String bodyString = body.string();
                            List<OnlineModule> allModules = parseModulesJson(gson, bodyString);
                            if (!allModules.isEmpty()) {
                                String fullJson = gson.toJson(allModules);
                                Files.write(repoFile, fullJson.getBytes(StandardCharsets.UTF_8));
                                lastRemoteLoadTime = System.currentTimeMillis();
                                lastInstalledRemoteCheckTime = 0L;
                                loadLocalData(false);
                                refreshInstalledModulesIfNeeded();
                                return;
                            }
                        }
                    }
                    lastError = new IOException("Failed to fetch modules.json from " + baseUrl + " (HTTP " + response.code() + ")");
                } catch (Throwable t) {
                    lastError = t;
                    Log.w(App.TAG, "load remote data attempt failed for " + baseUrl, t);
                }
            }

            if (lastError != null) {
                Log.e(App.TAG, "load remote data failed on all endpoints", lastError);
                for (RepoListener listener : listeners) {
                    listener.onThrowable(lastError);
                }
            }
            repoLoaded = true;
            for (RepoListener listener : listeners) {
                listener.onRepoLoaded();
            }
        } finally {
            isRefreshing = false;
        }
    }

    synchronized public void loadLocalData(boolean updateRemoteRepo) {
        try {
            if (Files.notExists(repoFile)) {
                loadRemoteData();
                return;
            }
            byte[] encoded = Files.readAllBytes(repoFile);
            String bodyString = new String(encoded, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            List<OnlineModule> parsedList = parseModulesJson(gson, bodyString);
            if (parsedList.isEmpty()) {
                if (updateRemoteRepo) {
                    loadRemoteData();
                }
                return;
            }
            Map<String, OnlineModule> modules = new ConcurrentHashMap<>();
            for (OnlineModule onlineModule : parsedList) {
                if (onlineModule != null && onlineModule.getName() != null) {
                    modules.put(onlineModule.getName(), onlineModule);
                }
            }
            OnlineModule[] repoModules = modules.values().toArray(new OnlineModule[0]);
            onlineModules = modules;
            repoLoaded = true;
            var channel = App.getPreferences().getString("update_channel", channels[0]);
            updateLatestVersion(repoModules, channel);
        } catch (Throwable t) {
            Log.e(App.TAG, Log.getStackTraceString(t));
            for (RepoListener listener : listeners) {
                listener.onThrowable(t);
            }
        } finally {
            repoLoaded = true;
            for (RepoListener listener : listeners) {
                listener.onRepoLoaded();
            }
        }
        if (updateRemoteRepo) {
            loadRemoteData();
        }
    }

    @Nullable
    private static ModuleVersion parseReleaseTag(String rawTag, String fallbackName) {
        String candidate = (rawTag != null && !rawTag.trim().isEmpty()) ? rawTag.trim() : (fallbackName != null ? fallbackName.trim() : "");
        if (candidate.isEmpty()) return null;
        var splits = candidate.split("-", 2);
        long verCode = 0;
        String verName = candidate;
        boolean validCode = false;

        if (splits.length == 2) {
            try {
                verCode = Long.parseLong(splits[0].trim());
                verName = splits[1].trim();
                validCode = verCode > 0;
            } catch (NumberFormatException ignored) {
            }
        }
        if (verName.startsWith("v") || verName.startsWith("V")) {
            verName = verName.substring(1).trim();
        }
        return new ModuleVersion(verCode, verName, validCode);
    }

    @Nullable
    private Release findFirstChannelRelease(OnlineModule module, String channel) {
        List<Release> rels = module.getReleases();
        if (rels == null || rels.isEmpty()) return null;
        for (Release r : rels) {
            if (r == null) continue;
            String name = r.getName() != null ? r.getName().toLowerCase() : "";
            boolean isSnapshot = name.startsWith("snapshot") || name.startsWith("nightly");
            boolean isPrerelease = Boolean.TRUE.equals(r.getIsPrerelease());
            if (channel.equals(channels[0])) {
                if (!isPrerelease && !isSnapshot) return r;
            } else if (channel.equals(channels[1])) {
                if (!isSnapshot) return r;
            } else {
                return r;
            }
        }
        return rels.get(0);
    }

    synchronized private void updateLatestVersion(OnlineModule[] modulesArray, String channel) {
        Map<String, ModuleVersion> versions = new ConcurrentHashMap<>();
        for (var module : modulesArray) {
            if (module == null || module.getName() == null) continue;
            String release = module.getLatestRelease();
            if (channel.equals(channels[1]) && module.getLatestBetaRelease() != null && !module.getLatestBetaRelease().isEmpty()) {
                release = module.getLatestBetaRelease();
            } else if (channel.equals(channels[2])) {
                if (module.getLatestSnapshotRelease() != null && !module.getLatestSnapshotRelease().isEmpty())
                    release = module.getLatestSnapshotRelease();
                else if (module.getLatestBetaRelease() != null && !module.getLatestBetaRelease().isEmpty())
                    release = module.getLatestBetaRelease();
            }

            ModuleVersion parsed = parseReleaseTag(release, null);
            Release firstRel = findFirstChannelRelease(module, channel);
            if (firstRel != null) {
                ModuleVersion fromRel = parseReleaseTag(firstRel.getTagName(), firstRel.getName());
                if (parsed == null) {
                    parsed = fromRel;
                } else if (fromRel != null) {
                    if (!parsed.hasValidCode && fromRel.hasValidCode) {
                        parsed = fromRel;
                    } else if (parsed.hasValidCode && fromRel.hasValidCode && fromRel.versionCode > parsed.versionCode) {
                        parsed = fromRel;
                    }
                }
            }

            if (parsed != null) {
                versions.put(module.getName(), parsed);
            }
        }
        for (var entry : LEGACY_PACKAGE_SUCCESSORS.entrySet()) {
            String legacyPkg = entry.getKey();
            String successorPkg = entry.getValue();
            OnlineModule successorMod = onlineModules.get(successorPkg);
            ModuleVersion successorVer = versions.get(successorPkg);
            if (successorMod != null && successorVer != null) {
                String label = successorMod.getDescription() != null && !successorMod.getDescription().trim().isEmpty()
                        ? successorMod.getDescription().trim() + " " + successorVer.versionName
                        : successorVer.versionName;
                versions.put(legacyPkg, new ModuleVersion(
                        successorVer.versionCode,
                        label,
                        successorVer.hasValidCode,
                        true,
                        successorPkg
                ));
                OnlineModule legacyMod = onlineModules.get(legacyPkg);
                if (legacyMod != null && successorMod.getReleases() != null && !successorMod.getReleases().isEmpty()) {
                    List<Release> merged = new ArrayList<>(successorMod.getReleases());
                    if (legacyMod.getReleases() != null) {
                        for (Release r : legacyMod.getReleases()) {
                            if (r != null && merged.stream().noneMatch(m ->
                                    java.util.Objects.equals(m.getTagName(), r.getTagName()) &&
                                    java.util.Objects.equals(m.getName(), r.getName()))) {
                                merged.add(r);
                            }
                        }
                    }
                    legacyMod.setReleases(merged);
                    if (successorMod.getLatestReleaseTime() != null && !successorMod.getLatestReleaseTime().isEmpty()) {
                        legacyMod.setLatestReleaseTime(successorMod.getLatestReleaseTime());
                    }
                }
            }
        }
        latestVersion = versions;
        repoLoaded = true;
        for (RepoListener listener : listeners) {
            listener.onRepoLoaded();
        }
    }

    public void updateLatestVersion(String channel) {
        if (repoLoaded)
            updateLatestVersion(onlineModules.keySet().parallelStream().map(onlineModules::get).toArray(OnlineModule[]::new), channel);
    }

    @Nullable
    public ModuleVersion getModuleLatestVersion(String packageName) {
        return repoLoaded ? latestVersion.getOrDefault(packageName, null) : null;
    }

    @Nullable
    public List<Release> getReleases(String packageName) {
        var channel = App.getPreferences().getString("update_channel", channels[0]);
        List<Release> releases = new ArrayList<>();
        if (repoLoaded) {
            var module = onlineModules.get(packageName);
            if (module != null) {
                releases = module.getReleases();
                if (!module.releasesLoaded) {
                    if (channel.equals(channels[1]) && !(module.getBetaReleases() != null && module.getBetaReleases().isEmpty())) {
                        releases = module.getBetaReleases();
                    } else if (channel.equals(channels[2]))
                        if (!(module.getSnapshotReleases() != null && module.getSnapshotReleases().isEmpty()))
                            releases = module.getSnapshotReleases();
                        else if (!(module.getBetaReleases() != null && module.getBetaReleases().isEmpty()))
                            releases = module.getBetaReleases();
                }
            }
        }
        return releases;
    }

    @Nullable
    public String getLatestReleaseTime(String packageName, String channel) {
        String releaseTime = null;
        if (repoLoaded) {
            var module = onlineModules.get(packageName);
            if (module != null) {
                releaseTime = module.getLatestReleaseTime();
                if (channel.equals(channels[1]) && module.getLatestBetaReleaseTime() != null) {
                    releaseTime = module.getLatestBetaReleaseTime();
                } else if (channel.equals(channels[2]))
                    if (module.getLatestSnapshotReleaseTime() != null)
                        releaseTime = module.getLatestSnapshotReleaseTime();
                    else if (module.getLatestBetaReleaseTime() != null)
                        releaseTime = module.getLatestBetaReleaseTime();
                if ((releaseTime == null || releaseTime.isEmpty()) && module.getReleases() != null && !module.getReleases().isEmpty()) {
                    Release r = module.getReleases().get(0);
                    releaseTime = r.getPublishedAt() != null ? r.getPublishedAt() : r.getCreatedAt();
                }
            }
        }
        return releaseTime;
    }

    public void loadRemoteReleases(String packageName) {
        App.getOkHttpClient().newCall(
                new Request.Builder()
                        .url(String.format(lsposedModuleDetailBaseUrl + "module/%s.json", packageName))
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(App.TAG, call.request().url() + " " + e.getMessage());
                for (RepoListener listener : listeners) {
                    listener.onThrowable(e);
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) {
                        String bodyString = body.string();
                        Gson gson = new Gson();
                        OnlineModule module = gson.fromJson(bodyString, OnlineModule.class);
                        if (module != null) {
                            mergeModulePreservingAssets(onlineModules.get(packageName), module);
                            module.releasesLoaded = true;
                            onlineModules.put(packageName, module);
                            var channel = App.getPreferences().getString("update_channel", channels[0]);
                            updateLatestVersion(onlineModules.values().toArray(new OnlineModule[0]), channel);
                            for (RepoListener listener : listeners) {
                                listener.onModuleReleasesLoaded(module);
                            }
                            return;
                        }
                    }
                    for (RepoListener listener : listeners) {
                        listener.onThrowable(new IOException("HTTP " + response.code()));
                    }
                } catch (Throwable t) {
                    Log.e(App.TAG, Log.getStackTraceString(t));
                    for (RepoListener listener : listeners) {
                        listener.onThrowable(t);
                    }
                }
            }
        });
    }

    public void addListener(RepoListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RepoListener listener) {
        listeners.remove(listener);
    }

    @Nullable
    public OnlineModule getOnlineModule(String packageName) {
        return repoLoaded && packageName != null ? onlineModules.get(packageName) : null;
    }

    @Nullable
    public Collection<OnlineModule> getOnlineModules() {
        return repoLoaded ? onlineModules.values() : null;
    }

    public interface RepoListener {
        default void onRepoLoaded() {
        }

        default void onModuleReleasesLoaded(OnlineModule module) {
        }

        default void onThrowable(Throwable t) {
            Log.e(App.TAG, "load repo failed", t);
        }
    }
}
