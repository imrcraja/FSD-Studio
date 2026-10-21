package com.fsdetc.fsdstudio.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurface
import com.fsdetc.fsdstudio.ui.theme.FsdPrimary
import com.fsdetc.fsdstudio.ui.theme.FsdSecondaryAccent
import com.fsdetc.fsdstudio.ui.theme.FsdSurfaceVariant

/** Background choices on the render-detail screen. [hex] is the API `bg` value; null = transparent. */
enum class BgOption(@StringRes val labelRes: Int, val hex: String?, val previewColor: Color?) {
    Transparent(R.string.bg_transparent, null, null),
    Dark(R.string.bg_dark, "101018", Color(0xFF101018)),
    Purple(R.string.bg_purple, "6E4CAD", Color(0xFF6E4CAD)),
    White(R.string.bg_white, "FFFFFF", Color.White),
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
    Surface(modifier, color = FsdPrimary, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = "🔒 " + stringResource(R.string.pro_badge),
            color = FsdOnSurface,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = FsdSurfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            color = FsdSecondaryAccent,
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
        color = FsdOnSurface,
        modifier = modifier,
    )
}
