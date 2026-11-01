package io.github.mrans2006.yaad.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.mrans2006.yaad.ui.theme.LocalReduceMotion

/*
 * Shared-element plumbing. MainActivity wraps the NavHost in a SharedTransitionLayout and every
 * screen in the AnimatedContent scope Navigation gives it; screens read both from these locals.
 * In tests and previews they are null, and [sharedShot] simply does nothing.
 */
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Which list a thumbnail lives in ("search", "folder:tag:Code"…). It is part of the shared-element name,
 * so a screenshot shown in two lists never "flies" between them when you switch tabs: it only links
 * the list you tapped in to the detail screen.
 */
val LocalShotOrigin = compositionLocalOf { "list" }

/** How long opening / closing a screenshot takes. Calm, not slow. */
const val OPEN_MS = 380

private val shotBounds = BoundsTransform { _, _ -> tween(OPEN_MS, easing = FastOutSlowInEasing) }

/**
 * Marks this image as "screenshot [id]". The same mark on the grid thumbnail and on the detail image
 * makes the thumbnail grow into the detail image when you open it, and shrink back when you go back.
 */
@Composable
fun Modifier.sharedShot(id: Long, origin: String = LocalShotOrigin.current): Modifier {
    val shared = LocalSharedScope.current
    val nav = LocalNavScope.current
    if (shared == null || nav == null || LocalReduceMotion.current) return this
    return with(shared) {
        this@sharedShot.sharedElement(
            rememberSharedContentState("shot-$id@$origin"),
            nav,
            boundsTransform = shotBounds,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(12.dp)),
        )
    }
}

private val tabs = setOf("search", "folders")
private fun isShot(route: String?) = route?.startsWith("shot/") == true

/**
 * Screen changes: a gentle fade plus a slight slide (1/14 of the width).
 * Opening a screenshot only cross-fades, because the shared image does the moving.
 * Switching tabs only cross-fades.
 */
fun enterFor(from: String?, to: String?, forward: Boolean): EnterTransition = when {
    isShot(to) || isShot(from) -> fadeIn(tween(OPEN_MS / 2 + 60))
    from in tabs && to in tabs -> fadeIn(tween(220, delayMillis = 60))
    forward -> fadeIn(tween(260, easing = LinearOutSlowInEasing)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 14 }
    else -> fadeIn(tween(260, easing = LinearOutSlowInEasing)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 14 }
}

fun exitFor(from: String?, to: String?, forward: Boolean): ExitTransition = when {
    isShot(to) || isShot(from) -> fadeOut(tween(OPEN_MS / 2))
    from in tabs && to in tabs -> fadeOut(tween(160))
    forward -> fadeOut(tween(200)) + slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 14 }
    else -> fadeOut(tween(200)) + slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 14 }
}
