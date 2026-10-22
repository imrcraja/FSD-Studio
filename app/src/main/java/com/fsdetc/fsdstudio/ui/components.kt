package com.fsdetc.fsdstudio.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.fsdetc.fsdstudio.R

/** Background choices on the render-detail screen. [hex] is the API `bg` value; null = transparent. */
enum class BgOption(@StringRes val labelRes: Int, val hex: String?, val previewColor: Color?) {
    Transparent(R.string.bg_transparent, null, null),
    Dark(R.string.bg_dark, "101018", Color(0xFF101018)),
    Purple(R.string.bg_purple, "6E4CAD", Color(0xFF6E4CAD)),
    White(R.string.bg_white, "FFFFFF", Color.White),
    ;

    companion object {
        /** Resolve a stored option name (Settings default); Transparent when unknown. */
        fun fromName(name: String?): BgOption =
            entries.firstOrNull { it.name == name } ?: Transparent
    }
}

/** Classic transparency checkerboard. */
@Composable
fun Checkerboard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.drawBehind {
            val s = 14.dp.toPx()
            val cols = (size.width / s).toInt() + 1
            val rows = (size.height / s).toInt() + 1
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val color = if ((r + c) % 2 == 0) Color(0xFFC9C9C9) else Color(0xFF8F8F8F)
                    drawRect(color, topLeft = Offset(c * s, r * s), size = Size(s, s))
                }
            }
        },
        content = content,
    )
}

/**
 * Preview-only branding: "FSD Studio" tiled diagonally (rotated about -30°,
 * white at ~14% alpha, bold) so even a screenshot of a preview carries the
 * brand. This is drawn ONLY as a Compose overlay on previews — downloads
 * always save the raw, clean image bytes, never a composited bitmap.
 */
@Composable
fun WatermarkOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(16.dp))) {
        val paint = android.graphics.Paint().apply {
            color = Color.White.copy(alpha = 0.14f).toArgb()
            textSize = 13.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isFakeBoldText = true
            isAntiAlias = true
        }
        val text = "FSD Studio"
        val stepX = paint.measureText(text) + 56.dp.toPx()
        val stepY = 72.dp.toPx()
        with(drawContext.canvas.nativeCanvas) {
            save()
            translate(size.width / 2f, size.height / 2f)
            rotate(-30f)
            // Cover the full diagonal of the (rotated) frame.
            val limit = maxOf(size.width, size.height)
            var y = -limit
            while (y <= limit) {
                var x = -limit
                while (x <= limit) {
                    drawText(text, x, y, paint)
                    x += stepX
                }
                y += stepY
            }
            restore()
        }
    }
}

/**
 * An image framed on the chosen background: checkerboard for transparent,
 * the matching solid colour otherwise (for remote renders the colour is also
 * baked into the image by the API, this is just the letterbox behind it).
 */
@Composable
fun FramedImage(
    model: Any?,
    bg: BgOption,
    modifier: Modifier = Modifier,
    onSuccess: () -> Unit = {},
    onError: (Throwable?) -> Unit = {},
) {
    Box(modifier.clip(RoundedCornerShape(16.dp))) {
        when {
            bg == BgOption.Transparent -> Checkerboard(Modifier.fillMaxSize()) {}
            bg.previewColor != null -> Box(
                Modifier
                    .fillMaxSize()
                    .background(bg.previewColor),
            )
        }
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            onSuccess = { onSuccess() },
            onError = { state -> onError(state.result.throwable) },
        )
    }
}

/**
 * A grid thumbnail with a loading spinner and a tap-to-retry error placeholder,
 * so a failed render request shows a human state instead of an empty box.
 */
@Composable
fun GridThumb(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    var attempt by remember(model) { mutableIntStateOf(0) }
    key(attempt) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            },
            error = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable { attempt++ },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun BgSelector(selected: BgOption, onSelect: (BgOption) -> Unit) {
    Row {
        BgOption.entries.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = { Text(stringResource(opt.labelRes)) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
fun ProChip(modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = "🔒 " + stringResource(R.string.pro_badge),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}
