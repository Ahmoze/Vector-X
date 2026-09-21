#include <fcntl.h>
#include <jni.h>
#include <sched.h>
#include <stdlib.h>
#include <sys/mount.h>
#include <sys/wait.h>
#include <unistd.h>

#include <string>

#include "logging.h"

// Lightweight RAII wrapper to prevent FD leaks
struct UniqueFd {
    int fd;
    explicit UniqueFd(int fd) : fd(fd) {}
    ~UniqueFd() {
        if (fd >= 0) close(fd);
    }
    operator int() const { return fd; }
};

extern "C" JNIEXPORT void JNICALL Java_org_ahmoze_vector_daemon_env_Dex2OatServer_doMountNative(
    JNIEnv *env, jobject, jboolean enabled, jstring r32, jstring d32, jstring r64, jstring d64,
    jstring w32, jstring w64) {
    const char *r32p = r32 ? env->GetStringUTFChars(r32, nullptr) : nullptr;
    const char *d32p = d32 ? env->GetStringUTFChars(d32, nullptr) : nullptr;
    const char *r64p = r64 ? env->GetStringUTFChars(r64, nullptr) : nullptr;
    const char *d64p = d64 ? env->GetStringUTFChars(d64, nullptr) : nullptr;
    const char *w32p = w32 ? env->GetStringUTFChars(w32, nullptr) : nullptr;
    const char *w64p = w64 ? env->GetStringUTFChars(w64, nullptr) : nullptr;

    pid_t pid = fork();
    if (pid > 0) {  // Parent process
        waitpid(pid, nullptr, 0);

        // Safely release JNI strings in the parent
        if (r32p) env->ReleaseStringUTFChars(r32, r32p);
        if (d32p) env->ReleaseStringUTFChars(d32, d32p);
        if (r64p) env->ReleaseStringUTFChars(r64, r64p);
        if (d64p) env->ReleaseStringUTFChars(d64, d64p);
        if (w32p) env->ReleaseStringUTFChars(w32, w32p);
        if (w64p) env->ReleaseStringUTFChars(w64, w64p);
    } else if (pid == 0) {  // Child process
        UniqueFd ns(open("/proc/1/ns/mnt", O_RDONLY));
        if (ns >= 0) {
            setns(ns, CLONE_NEWNS);
        }

        if (enabled) {
            LOGI("Enable dex2oat wrapper");
            if (r32p && w32p) {
                mount(w32p, r32p, nullptr, MS_BIND, nullptr);
                mount(nullptr, r32p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (d32p && w32p) {
                mount(w32p, d32p, nullptr, MS_BIND, nullptr);
                mount(nullptr, d32p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (r64p && w64p) {
                mount(w64p, r64p, nullptr, MS_BIND, nullptr);
                mount(nullptr, r64p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            if (d64p && w64p) {
                mount(w64p, d64p, nullptr, MS_BIND, nullptr);
                mount(nullptr, d64p, nullptr, MS_BIND | MS_REMOUNT | MS_RDONLY, nullptr);
            }
            execlp("resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
            execlp("/data/adb/magisk/resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
            execlp("/data/adb/ksu/bin/resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
            execlp("/data/adb/ap/bin/resetprop", "resetprop", "--delete", "dalvik.vm.dex2oat-flags", nullptr);
            _exit(0);
        } else {
            LOGI("Disable dex2oat wrapper");
            if (r32p) umount(r32p);
            if (d32p) umount(d32p);
            if (r64p) umount(r64p);
            if (d64p) umount(d64p);
            execlp("resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0",
                   nullptr);
            execlp("/data/adb/magisk/resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0",
                   nullptr);
            execlp("/data/adb/ksu/bin/resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0",
                   nullptr);
            execlp("/data/adb/ap/bin/resetprop", "resetprop", "dalvik.vm.dex2oat-flags", "--inline-max-code-units=0",
                   nullptr);
            _exit(0);
        }
    }
}

static int setsockcreatecon_raw(const char *context) {
    std::string path = "/proc/self/task/" + std::to_string(gettid()) + "/attr/sockcreate";
    UniqueFd fd(open(path.c_str(), O_RDWR | O_CLOEXEC));
    if (fd < 0) return -1;

    int ret;
    if (context) {
        do {
            ret = write(fd, context, strlen(context) + 1);
        } while (ret < 0 && errno == EINTR);
    } else {
        do {
            ret = write(fd, nullptr, 0);  // clear
        } while (ret < 0 && errno == EINTR);
    }
    return ret < 0 ? -1 : 0;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_ahmoze_vector_daemon_env_Dex2OatServer_setSockCreateContext(JNIEnv *env, jclass,
                                                                     jstring contextStr) {
    const char *context = contextStr ? env->GetStringUTFChars(contextStr, nullptr) : nullptr;
    int ret = setsockcreatecon_raw(context);
    if (context) env->ReleaseStringUTFChars(contextStr, context);
    return ret == 0;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_ahmoze_vector_daemon_env_Dex2OatServer_getSockPath(JNIEnv *env, jobject) {
    return env->NewStringUTF("5291374ceda0aef7c5d86cd2a4f6a3ac\0");
}
