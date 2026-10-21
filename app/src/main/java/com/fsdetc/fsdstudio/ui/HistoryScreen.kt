package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.data.db.HistoryItem
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurface
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurfaceMuted
import com.fsdetc.fsdstudio.ui.theme.FsdSurface
import com.fsdetc.fsdstudio.ui.theme.FsdSurfaceVariant
import com.fsdetc.fsdstudio.viewmodel.HistoryViewModel
import java.io.File

@Composable
fun HistoryScreen(
    historyVm: HistoryViewModel,
    onOpenDetail: (DetailRequest) -> Unit,
) {
    val items by historyVm.items.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineSmall,
            color = FsdOnSurface,
        )
        Spacer(Modifier.height(12.dp))

        if (items.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = FsdSurface)) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FsdOnSurfaceMuted,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item ->
                HistoryCell(item) {
                    onOpenDetail(
                        DetailRequest.LocalFile(
                            path = item.filePath,
                            title = item.title,
                            subtitle = item.subtitle,
                            sourceName = item.sourceName,
                            historyId = item.id,
                            deletableHistory = true,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCell(item: HistoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = FsdSurface),
    ) {
        Column(Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FsdSurfaceVariant),
            ) {
                AsyncImage(
                    model = File(item.filePath),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = FsdOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = FsdOnSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
