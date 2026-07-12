#include <iostream>

#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

int main() {
    std::cout << "TEST_OUTPUT: [" << STR(org.ahmoze.vector.manager) << "]" << std::endl;
    return 0;
}
