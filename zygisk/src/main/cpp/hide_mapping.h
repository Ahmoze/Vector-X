#pragma once

namespace vector::native {

/**
 * Parses /proc/self/maps and hides the file-backed memory mappings
 * for the current shared library by replacing them with anonymous memory.
 */
void HideMemoryMap();

} // namespace vector::native
