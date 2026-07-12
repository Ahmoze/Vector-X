#include <iostream>

constexpr jint HashPackageName(const char* str) {
    uint64_t hash = 0;
    while (*str) {
        hash = (hash * 31 + static_cast<uint64_t>(*str)) % 100000000ULL;
        str++;
    }
    return static_cast<jint>(hash);
}

#ifndef STR_HELPER
#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)
#endif

int main() {
    std::cout << HashPackageName(STR(org.ahmoze.vector.manager)) << std::endl;
    std::cout << HashPackageName("org.ahmoze.vector.manager") << std::endl;
    return 0;
}
