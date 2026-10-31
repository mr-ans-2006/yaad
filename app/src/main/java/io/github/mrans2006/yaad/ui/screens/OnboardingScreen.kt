package io.github.mrans2006.yaad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mrans2006.yaad.ui.components.BrutalBox
import io.github.mrans2006.yaad.ui.components.BrutalButton
import io.github.mrans2006.yaad.ui.theme.Palette
import io.github.mrans2006.yaad.ui.theme.Yaad
import io.github.mrans2006.yaad.ui.theme.YaadType

/** Which page of the welcome flow we're on. */
sealed interface OnboardingStep {
    data object Welcome : OnboardingStep
    data class Import(val found: Int?) : OnboardingStep // null = still counting
    data object NoPermission : OnboardingStep
}

data class OnboardingActions(
    val onStart: () -> Unit = {},
    val onImportAll: () -> Unit = {},
    val onOnlyNew: () -> Unit = {},
)

@Composable
fun OnboardingScreen(step: OnboardingStep, actions: OnboardingActions) {
    Column(
        Modifier.fillMaxSize().background(Yaad.colors.background).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        BrutalBox(fill = Palette.Yellow, shadow = 5.dp) {
            Text("YAAD", style = YaadType.hero.copy(fontSize = 54.sp, lineHeight = 58.sp), color = Palette.Ink,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("یاد · memory", style = YaadType.body, color = Yaad.colors.subtext)
        Spacer(Modifier.height(22.dp))

        when (step) {
            OnboardingStep.Welcome -> Welcome(actions.onStart)
            is OnboardingStep.Import -> ImportChoice(step.found, actions)
            OnboardingStep.NoPermission -> NoPermission(actions.onOnlyNew)
        }
    }
}

@Composable
private fun Welcome(onStart: () -> Unit) {
    Text("Never lose a screenshot again.", style = YaadType.heading, color = Yaad.colors.text)
    Spacer(Modifier.height(20.dp))
    Feature(Icons.Rounded.Screenshot, Palette.Yellow, "Auto-pick", "Every new screenshot is saved on its own. You can also share any image to Yaad.")
    Feature(Icons.Rounded.TextFields, Palette.Pink, "Reads the text", "Pulls out the key line and tags it: Code, Tools, Places, Study, Hacks…")
    Feature(Icons.Rounded.AutoAwesome, Palette.Purple, "Search by meaning", "Type \u201Ca tool for data recovery\u201D and find the one that said \u201Crestore deleted files\u201D.")
    Feature(Icons.Rounded.Lock, Palette.Green, "Stays on your phone", "No account. No server. Yaad doesn't even have internet permission. Open source, so anyone can check.")
    Spacer(Modifier.height(10.dp))
    BrutalButton("Let's go", onStart, fill = Palette.Yellow, modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp))
    Spacer(Modifier.height(10.dp))
    Text("Next, Android will ask to let Yaad see your photos. Yaad only looks inside Screenshots folders.",
        style = YaadType.small, color = Yaad.colors.subtext)
}

@Composable
private fun Feature(icon: ImageVector, color: Color, title: String, body: String) {
    BrutalBox(Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            BrutalBox(fill = color, shadow = 0.dp, radius = 8.dp) {
                Icon(icon, null, tint = Palette.Ink, modifier = Modifier.padding(8.dp).size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = YaadType.title, color = Yaad.colors.text)
                Spacer(Modifier.height(2.dp))
                Text(body, style = YaadType.body, color = Yaad.colors.subtext)
            }
        }
    }
}

@Composable
private fun ImportChoice(found: Int?, actions: OnboardingActions) {
    Text(
        when (found) { null -> "Looking…"; 0 -> "No screenshots yet"; else -> "Found $found screenshots" },
        style = YaadType.heading, color = Yaad.colors.text,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        if (found == 0) "That's fine. Take one and it will appear in Yaad."
        else "Should Yaad read the ones already on your phone? It works in the background, roughly a minute per hundred. You can use the app meanwhile.",
        style = YaadType.body, color = Yaad.colors.subtext,
    )
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (found != 0) {
            BrutalButton(if (found == null) "Import all" else "Import all $found", actions.onImportAll, fill = Palette.Yellow,
                modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp), enabled = found != null)
        }
        BrutalButton(if (found == 0) "Continue" else "Only new ones from now", actions.onOnlyNew,
            modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp))
    }
}

@Composable
private fun NoPermission(onContinue: () -> Unit) {
    Text("No problem", style = YaadType.heading, color = Yaad.colors.text)
    Spacer(Modifier.height(10.dp))
    Text("Without photo access Yaad can't pick screenshots up on its own, but sharing an image to Yaad still works. You can allow access later from the home screen.",
        style = YaadType.body, color = Yaad.colors.subtext)
    Spacer(Modifier.height(24.dp))
    BrutalButton("Continue", onContinue, fill = Palette.Yellow, modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp))
}
