package com.saveflow.downloader.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Set to false to hide the dashed "Native ad space" box once your real ad is wired in. */
const val SHOW_AD_PLACEHOLDER = true

/**
 * Reserved spot for a native ad. Pass your ad view as [content] later; until then a dashed
 * placeholder marks where it will appear.
 */
@Composable
fun NativeAdSlot(modifier: Modifier = Modifier, content: (@Composable () -> Unit)? = null) {
    if (content != null) {
        Box(modifier.fillMaxWidth()) { content() }
        return
    }
    if (!SHOW_AD_PLACEHOLDER) return
    val color = MaterialTheme.colorScheme.outline
    Box(
        modifier
            .fillMaxWidth()
            .height(220.dp)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Native ad space", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Shown while a link is being resolved. The percentage is an ESTIMATE that eases toward ~92%:
 * the server reports no real progress for this step. The dialog closes when the result arrives.
 */
@Composable
fun AnalyzingDialog(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    adContent: (@Composable () -> Unit)? = null
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(0f, 0.92f, animationSpec = tween(30_000, easing = FastOutSlowInEasing)) { value, _ -> progress = value }
    }
    val status = when {
        progress < 0.25f -> "Checking the link…"
        progress < 0.60f -> "Contacting the server…"
        progress < 0.85f -> "Fetching the media…"
        else -> "Almost ready…"
    }

    val transition = rememberInfiniteTransition(label = "analyzing")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "phase"
    )
    val bob by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob"
    )

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Preparing your download", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Spacer(Modifier.height(20.dp))
                Icon(
                    Icons.Rounded.ArrowDownward, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp).offset(y = (bob * 6).dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(3) { i ->
                        val on = phase.toInt() == i
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (on) 0.18f else 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow, null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = if (on) 1f else 0.35f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("${(progress * 100).toInt()}%", fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                TextButton(onClick = onCancel, modifier = Modifier.padding(top = 4.dp)) { Text("Cancel") }

                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                NativeAdSlot(content = adContent)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Only the link is sent to the server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
