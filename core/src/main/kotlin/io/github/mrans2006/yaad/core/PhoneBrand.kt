package io.github.mrans2006.yaad.core

/**
 * The phone families Yaad has a battery guide for.
 *
 * Phone makers add their own "battery savers" on top of Android, and each one
 * hides the switches in a different place. Phones that share the same system
 * software are grouped together: Realme and OnePlus run ColorOS like Oppo, and
 * Infinix, Tecno and itel are all made by Transsion and share its tools.
 */
enum class PhoneBrand(
    /** The name we show on the guide, e.g. "Xiaomi · Redmi · POCO". */
    val label: String,
    /** The name of the phone's system software, e.g. "MIUI / HyperOS". */
    val system: String,
    /** How the home-screen tip talks about these phones, e.g. "Samsung phones". */
    val phonesName: String,
) {
    SAMSUNG("Samsung", "One UI", "Samsung phones"),
    XIAOMI("Xiaomi · Redmi · POCO", "MIUI / HyperOS", "Xiaomi, Redmi and POCO phones"),
    VIVO("Vivo · iQOO", "Funtouch OS / OriginOS", "Vivo phones"),
    TRANSSION("Infinix · Tecno · itel", "XOS / HiOS", "Infinix and Tecno phones"),
    OPPO("Oppo · Realme · OnePlus", "ColorOS / OxygenOS", "Oppo, Realme and OnePlus phones"),
    GENERIC("Pixel & other phones", "Android", "Many phones");

    companion object {
        /**
         * Which words in the maker's name point to which guide.
         * Matching is on whole words, so "itel" can't accidentally match "satellite".
         */
        private val wordsToBrand: Map<String, PhoneBrand> = mapOf(
            "samsung" to SAMSUNG,
            "xiaomi" to XIAOMI, "redmi" to XIAOMI, "poco" to XIAOMI, "pocophone" to XIAOMI,
            "vivo" to VIVO, "iqoo" to VIVO,
            "infinix" to TRANSSION, "tecno" to TRANSSION, "itel" to TRANSSION, "transsion" to TRANSSION,
            "oppo" to OPPO, "realme" to OPPO, "oneplus" to OPPO,
        )

        /**
         * Works out the phone family from Android's Build.MANUFACTURER and Build.BRAND.
         * Example: ("INFINIX MOBILITY LIMITED", "Infinix") -> TRANSSION.
         * The maker is checked first; the brand helps when the maker is unusual
         * (a POCO phone says MANUFACTURER = "Xiaomi", BRAND = "POCO").
         * Anything we don't know (Google, Motorola, Nokia…) gets the plain Android guide.
         */
        fun detect(manufacturer: String?, brand: String? = null): PhoneBrand =
            fromName(manufacturer) ?: fromName(brand) ?: GENERIC

        private fun fromName(name: String?): PhoneBrand? {
            if (name.isNullOrBlank()) return null
            val words = name.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
            // "OnePlus" can also be written "One Plus".
            if (words.zipWithNext().any { (a, b) -> a == "one" && b == "plus" }) return OPPO
            return words.firstNotNullOfOrNull { wordsToBrand[it] }
        }
    }
}
