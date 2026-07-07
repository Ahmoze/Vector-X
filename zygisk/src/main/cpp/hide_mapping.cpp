#include "hide_mapping.h"
#include <common/logging.h>
#include <sys/mman.h>
#include <dlfcn.h>
#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <vector>
#include <malloc.h>
#include <unistd.h>

namespace vector::native {

struct RemapData {
    void* start;
    size_t size;
    int prot;
    void* backup;
    void* (*sys_mmap)(void*, size_t, int, int, int, off_t);
    void* (*sys_memcpy)(void*, const void*, size_t);
    int (*sys_mprotect)(void*, size_t, int);
};

#pragma clang optimize off
#pragma GCC optimize("O0")
__attribute__((noinline, no_stack_protector))
static void do_remap_stub(RemapData* data) {
    data->sys_mmap(data->start, data->size, 7 /*PROT_READ|WRITE|EXEC*/, 0x32, -1, 0);
    data->sys_memcpy(data->start, data->backup, data->size);
    data->sys_mprotect(data->start, data->size, data->prot);
}
static void do_remap_stub_end() {}
#pragma GCC optimize("O3")
#pragma clang optimize on

struct MemoryRegion {
    uintptr_t start;
    uintptr_t end;
    int prot;
};

void HideMemoryMap() {
    Dl_info info;
    if (dladdr((void*)HideMemoryMap, &info) == 0) {
        LOGE("HideMemoryMap: dladdr failed");
        return;
    }
    
    if (!info.dli_fname) {
        LOGE("HideMemoryMap: dli_fname is null");
        return;
    }
    
    FILE *fp = fopen("/proc/self/maps", "r");
    if (!fp) {
        LOGE("HideMemoryMap: failed to open /proc/self/maps");
        return;
    }
    
    std::vector<MemoryRegion> regions;
    char line[512];
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, info.dli_fname)) {
            uintptr_t start, end;
            char perms[5];
            if (sscanf(line, "%" PRIxPTR "-%" PRIxPTR " %4s", &start, &end, perms) == 3) {
                int prot = 0;
                if (perms[0] == 'r') prot |= PROT_READ;
                if (perms[1] == 'w') prot |= PROT_WRITE;
                if (perms[2] == 'x') prot |= PROT_EXEC;
                regions.push_back({start, end, prot});
            }
        }
    }
    fclose(fp);
    
    if (regions.empty()) {
        LOGW("HideMemoryMap: No memory regions found for %s", info.dli_fname);
        return;
    }
    
    LOGI("HideMemoryMap: Hiding %zu memory regions for %s", regions.size(), info.dli_fname);
    
    size_t page_size = sysconf(_SC_PAGESIZE);
    void* stub_mem = mmap(nullptr, page_size, PROT_READ | PROT_WRITE | PROT_EXEC, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (stub_mem == MAP_FAILED) {
        LOGE("HideMemoryMap: Failed to allocate stub memory");
        return;
    }
    
    // Copy the stub. Ensure thumb bit is cleared for memcpy, and restored for execution.
    uintptr_t stub_addr = (uintptr_t)do_remap_stub & ~1UL;
    memcpy(stub_mem, (void*)stub_addr, 1024);
    auto execute_stub = (void(*)(RemapData*))((uintptr_t)stub_mem | ((uintptr_t)do_remap_stub & 1UL));
    
    for (const auto& reg : regions) {
        size_t size = reg.end - reg.start;
        void* backup = malloc(size);
        if (!backup) {
            LOGE("HideMemoryMap: malloc failed for backup");
            continue;
        }
        memcpy(backup, (void*)reg.start, size);
        
        RemapData data;
        data.start = (void*)reg.start;
        data.size = size;
        data.prot = reg.prot;
        data.backup = backup;
        data.sys_mmap = (void*(*)(void*, size_t, int, int, int, off_t))mmap;
        data.sys_memcpy = (void*(*)(void*, const void*, size_t))memcpy;
        data.sys_mprotect = (int(*)(void*, size_t, int))mprotect;
        
        execute_stub(&data);
        
        free(backup);
    }
    
    munmap(stub_mem, page_size);
    LOGI("HideMemoryMap: Successfully anonymized library memory.");
}

} // namespace vector::native
