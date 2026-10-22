package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsdetc.fsdstudio.R

private data class TutorialPage(
    val emoji: String,
    val titleRes: Int,
    val bodyRes: Int,
)

private val TUTORIAL_PAGES = listOf(
    TutorialPage("👋", R.string.tut1_title, R.string.tut1_body),
    TutorialPage("🔍", R.string.tut2_title, R.string.tut2_body),
    TutorialPage("🧍", R.string.tut3_title, R.string.tut3_body),
    TutorialPage("✨", R.string.tut4_title, R.string.tut4_body),
)

/**
 * First-run tutorial: four static pages, shown once (the nav host only
 * composes this while `seen_tutorial` is false). It calls no API and
 * consumes no limits — finishing or skipping just flips the flag.
 * Implemented as a simple state-based page switcher: no pager dependency.
 */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = TUTORIAL_PAGES[pageIndex]
    val isLast = pageIndex == TUTORIAL_PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.tut_skip))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = page.emoji, fontSize = 72.sp)
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(page.bodyRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            TUTORIAL_PAGES.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pageIndex) 12.dp else 9.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pageIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { if (isLast) onDone() else pageIndex++ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (isLast) R.string.tut_start else R.string.tut_next))
        }
        Spacer(Modifier.height(8.dp))
    }
}
