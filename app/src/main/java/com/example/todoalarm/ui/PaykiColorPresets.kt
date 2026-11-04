package com.example.todoalarm.ui

internal object PaykiColorPresets {
    // Stored sRGB channels; shared with the Windows color picker without remapping older colors.
    val Rgb12 = listOf(
        Triple(194, 75, 143), Triple(240, 91, 85), Triple(248, 159, 69), Triple(251, 203, 69), Triple(81, 186, 67), Triple(23, 207, 181),
        Triple(76, 136, 255), Triple(80, 195, 240), Triple(95, 104, 233), Triple(140, 86, 236), Triple(214, 97, 214), Triple(117, 117, 117)
    )
    val Rainbow12 = Rgb12.map { (r, g, b) -> "#%02X%02X%02X".format(java.util.Locale.ROOT, r, g, b) }

    const val DefaultEvent = "#DF58A5"
}
