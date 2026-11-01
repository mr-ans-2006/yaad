package io.github.mrans2006.yaad

import android.graphics.BitmapFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.mrans2006.yaad.data.Shot
import io.github.mrans2006.yaad.ui.HomeState
import io.github.mrans2006.yaad.ui.YaadNavHost
import io.github.mrans2006.yaad.ui.components.LocalPreviewImages
import io.github.mrans2006.yaad.ui.screens.DetailActions
import io.github.mrans2006.yaad.ui.screens.SearchActions
import io.github.mrans2006.yaad.ui.theme.ThemeMode
import io.github.mrans2006.yaad.ui.theme.YaadTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Not a normal test: it drives the REAL navigation (YaadNavHost: shared-element open/back, fades,
 * tab switching) with a paused clock and saves one PNG every 16 ms (60 fps) to app/build/motion/.
 * Those frames are stitched into a video. It shows the animation timing and shapes, but it is a
 * desktop render, not a phone: it says nothing about real-device smoothness.
 *
 * Skipped unless you ask for it:  ./gradlew :app:testDebugUnitTest --tests '*MotionRecordingTest*' -Pyaad.motion=true
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", application = android.app.Application::class)
class MotionRecordingTest {
    @get:Rule val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val hour = 3_600_000L
    private var frame = 0

    private fun sample(id: Long, name: String, keyLine: String, tags: String, ago: Long) =
        Shot(id = id, uri = "sample://$name", source = Shot.SOURCE_AUTO, takenAt = now - ago, status = Shot.STATUS_DONE,
            keyLine = keyLine, tags = tags, text = keyLine, width = 1080, height = 2340,
            links = if (name == "recuva") "https://recuva.com" else "")

    private val shots = listOf(
        sample(1, "recuva", "Recuva — Restore deleted files", "Tools", 2 * hour),
        sample(2, "llama", "ggerganov / llama.cpp — LLM inference in C/C++", "Code", 5 * hour),
        sample(3, "kolachi", "Kolachi — Seafood restaurant · Rs 3,000-4,000", "Places", 26 * hour),
        sample(4, "spoon", "Put a wooden spoon across a pot of boiling water and it won't boil over.", "Hacks", 50 * hour),
        sample(5, "dp", "Lecture 7: Dynamic Programming", "Study", 3 * 24 * hour),
        sample(6, "buds", "Xiaomi Redmi Buds 5 Wireless Earbuds", "Shopping", 4 * 24 * hour),
        sample(8, "chat", "bro send me the DSA notes for tomorrow's exam", "Chats,Study", 9 * 24 * hour),
        sample(9, "testdisk", "TestDisk Step By Step", "Tools", 12 * 24 * hour),
    )

    private val images by lazy {
        shots.map { it.uri.removePrefix("sample://") }.distinct().associate { name ->
            val bytes = javaClass.getResourceAsStream("/samples/$name.png")!!.readBytes()
            "sample://$name" to BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
        }
    }

    private fun snap() {
        compose.onRoot().captureRoboImage("build/motion/frame_%04d.png".format(frame++))
    }

    /** Let [ms] of animation time pass, one 60 fps frame at a time, saving each frame. */
    private fun play(ms: Int) {
        repeat(ms / 16) { compose.mainClock.advanceTimeBy(16); snap() }
    }

    @Test fun recordOpenBackAndTabs() {
        if (System.getProperty("yaad.motion") != "true") return
        val state = HomeState(shots = shots, loaded = true, query = "a tool for data recovery", results = listOf(shots[7], shots[0]))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalPreviewImages provides images) {
                YaadTheme(ThemeMode.LIGHT) {
                    YaadNavHost(state = state, search = SearchActions(), onAdd = {}, detailActions = { back -> DetailActions(onBack = back) })
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        play(300)

        // 1. Open a search result: the thumbnail grows into the detail image.
        compose.onNodeWithText("TestDisk Step By Step").performClick()
        play(620)
        play(400)
        // 2. Back: it shrinks back into its card.
        compose.onNodeWithContentDescription("Back").performClick()
        play(620)
        play(300)

        // 3. Switch to the Folders tab (cross-fade), open a folder (fade + slight slide).
        compose.onNodeWithText("Folders").performClick()
        play(450)
        play(300)
        compose.onNodeWithText("Tools").performClick()
        play(500)
        play(300)
        // 4. Open from the folder grid, then back.
        compose.onNodeWithText("Recuva — Restore deleted files").performClick()
        play(620)
        play(400)
        compose.onNodeWithContentDescription("Back").performClick()
        play(620)
        play(250)
        // 5. Back to the Search tab: it kept its results.
        compose.onNodeWithText("Search").performClick()
        play(450)
        play(300)
    }
}
