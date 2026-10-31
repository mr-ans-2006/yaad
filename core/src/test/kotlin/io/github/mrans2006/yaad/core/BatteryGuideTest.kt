package io.github.mrans2006.yaad.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryGuideTest {

    // ---- Brand detection: real Build.MANUFACTURER / Build.BRAND values ----

    @Test fun detectsEachMaker() {
        val cases = mapOf(
            "samsung" to PhoneBrand.SAMSUNG,
            "vivo" to PhoneBrand.VIVO,
            "INFINIX MOBILITY LIMITED" to PhoneBrand.TRANSSION,
            "TECNO MOBILE LIMITED" to PhoneBrand.TRANSSION,
            "Xiaomi" to PhoneBrand.XIAOMI,
            "Redmi" to PhoneBrand.XIAOMI,
            "POCO" to PhoneBrand.XIAOMI,
            "OPPO" to PhoneBrand.OPPO,
            "realme" to PhoneBrand.OPPO,
            "OnePlus" to PhoneBrand.OPPO,
            "Google" to PhoneBrand.GENERIC,
        )
        for ((maker, expected) in cases) assertEquals("for \"$maker\"", expected, PhoneBrand.detect(maker))
    }

    @Test fun moreRealValues() {
        assertEquals(PhoneBrand.TRANSSION, PhoneBrand.detect("ITEL MOBILE LIMITED"))
        assertEquals(PhoneBrand.TRANSSION, PhoneBrand.detect("Infinix"))
        assertEquals(PhoneBrand.VIVO, PhoneBrand.detect("iQOO"))
        assertEquals(PhoneBrand.OPPO, PhoneBrand.detect("One Plus"))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("motorola"))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("HMD Global"))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("robolectric"))
    }

    @Test fun brandHelpsWhenMakerIsUnknown() {
        // The maker wins when we know it; the brand is the backup.
        assertEquals(PhoneBrand.XIAOMI, PhoneBrand.detect("Xiaomi", "POCO"))
        assertEquals(PhoneBrand.XIAOMI, PhoneBrand.detect("unknown", "Redmi"))
        assertEquals(PhoneBrand.TRANSSION, PhoneBrand.detect("", "TECNO"))
    }

    @Test fun emptyOrOddValuesFallBackToGeneric() {
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect(null, null))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("   "))
        // Whole-word matching: these contain "itel"/"vivo"/"poco" inside other words.
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("Satellite Phones Ltd"))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("Vivobook"))
        assertEquals(PhoneBrand.GENERIC, PhoneBrand.detect("Pocomaker"))
    }

    // ---- The guides themselves ----

    @Test fun everyBrandHasAGuide() {
        for (b in PhoneBrand.entries) {
            val g = BatteryGuides.forBrand(b)
            assertEquals(b, g.brand)
            assertTrue("$b needs steps", g.steps.isNotEmpty())
            assertTrue("$b steps need text", g.steps.all { it.title.isNotBlank() && it.body.isNotBlank() })
        }
        assertEquals(PhoneBrand.entries.size, BatteryGuides.all.map { it.brand }.toSet().size)
    }

    @Test fun myPhoneComesFirstThenTheRest() {
        val list = BatteryGuides.ordered(PhoneBrand.TRANSSION)
        assertEquals(PhoneBrand.TRANSSION, list.first().brand)
        assertEquals(PhoneBrand.entries.toSet(), list.map { it.brand }.toSet())
        assertEquals(PhoneBrand.entries.size, list.size)
    }

    @Test fun everyButtonEndsWithScreensThatAlwaysExist() {
        val safeNets = setOf(SettingsTarget.APP_INFO, SettingsTarget.BATTERY_OPTIMISATION_LIST)
        for (g in BatteryGuides.all) for (s in g.steps) for (b in s.buttons) {
            assertTrue("${g.brand} '${b.label}' is empty", b.targets.isNotEmpty())
            assertTrue("${g.brand} '${b.label}' must end with standard Android screens", b.targets.takeLast(2).toSet() == safeNets)
            for (t in b.targets) {
                assertTrue("target must be a component or an action: $t", t.isComponent || t.action != null)
                if (t.isComponent) {
                    assertTrue("bad package ${t.packageName}", t.packageName!!.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+")))
                    assertTrue("bad class ${t.className}", t.className!!.contains('.'))
                }
            }
        }
    }

    @Test fun keyStepsArePresent() {
        fun text(b: PhoneBrand) = BatteryGuides.forBrand(b).steps.joinToString(" ") { it.title + " " + it.body }
        assertTrue(text(PhoneBrand.SAMSUNG).contains("Sleeping apps"))
        assertTrue(text(PhoneBrand.SAMSUNG).contains("Unrestricted"))
        assertTrue(text(PhoneBrand.XIAOMI).contains("Autostart"))
        assertTrue(text(PhoneBrand.XIAOMI).contains("No restrictions"))
        assertTrue(text(PhoneBrand.VIVO).contains("Autostart"))
        assertTrue(text(PhoneBrand.VIVO).contains("Background power"))
        assertTrue(text(PhoneBrand.TRANSSION).contains("Auto-start management"))
        assertTrue(text(PhoneBrand.TRANSSION).contains("Launch manager"))
        assertTrue(text(PhoneBrand.OPPO).contains("Auto launch"))
    }
}
