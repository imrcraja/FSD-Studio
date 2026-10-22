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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Api
import com.fsdetc.fsdstudio.data.CharacterSource
import com.fsdetc.fsdstudio.data.DetailRequest
import com.fsdetc.fsdstudio.data.PoseCatalog
import com.fsdetc.fsdstudio.data.PoseDef
import com.fsdetc.fsdstudio.data.SkinBitmaps
import com.fsdetc.fsdstudio.viewmodel.MainViewModel
import java.io.File

@Composable
fun PosesScreen(
    mainVm: MainViewModel,
    onOpenDetail: (DetailRequest) -> Unit,
    onOpenPro: () -> Unit,
) {
    val character by mainVm.character.collectAsState()
    val plan by mainVm.plan.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.poses_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))

        val ch = character
        if (ch == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = stringResource(R.string.poses_no_character),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@Column
        }

        when (ch.source) {
            CharacterSource.USERNAME -> {
                val renderId = ch.renderId
                if (renderId == null) {
                    Text(stringResource(R.string.poses_no_character), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    return@Column
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(PoseCatalog.POSES, key = { pose -> pose.id }) { pose ->
                        val locked = !mainVm.entitlements.poseUnlocked(pose, plan)
                        val poseLabel = stringResource(pose.labelRes)
                        PoseCell(
                            pose = pose,
                            poseLabel = poseLabel,
                            renderId = renderId,
                            locked = locked,
                            onClick = {
                                if (locked) {
                                    onOpenPro()
                                } else {
                                    onOpenDetail(
                                        DetailRequest.RemotePose(
                                            renderId = renderId,
                                            poseId = pose.id,
                                            poseLabel = poseLabel,
                                            charName = ch.name,
                                            renderType = pose.renderType,
                                            extraParams = pose.params,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            CharacterSource.UPLOAD -> {
                Text(
                    text = stringResource(R.string.poses_upload_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                val sha = ch.skinHash
                if (sha == null) {
                    return@Column
                }
                val flats = listOf(
                    Triple("front", R.string.flat_front, "Front render"),
                    Triple("back", R.string.flat_back, "Back render"),
                    Triple("face", R.string.flat_face, "Face render"),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(flats) { (kind, labelRes, title) ->
                        val file = SkinBitmaps.flatFile(context, sha, kind)
                        FlatCell(file = file, label = stringResource(labelRes)) {
                            onOpenDetail(
                                DetailRequest.LocalFile(
                                    path = file.absolutePath,
                                    title = title,
                                    subtitle = "Flat render",
                                    sourceName = ch.name,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoseCell(
    pose: PoseDef,
    poseLabel: String,
    renderId: String,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                // Locked poses still LOAD the real render — the user sees a
                // dimmed preview of what is inside; only the tap is locked.
                GridThumb(
                    model = Api.renderUrl(
                        renderId, pose.renderType, 320, extra = pose.params,
                    ),
                    contentDescription = poseLabel,
                    modifier = Modifier.fillMaxSize(),
                )
                if (locked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                    ProChip(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = poseLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FlatCell(file: File, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 8f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = file,
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
