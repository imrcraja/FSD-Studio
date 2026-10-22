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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
                    itemsIndexed(Api.POSES, key = { _, pose -> pose.id }) { index, pose ->
                        val locked = index >= mainVm.entitlements.maxPoseIndex(plan)
                        PoseCell(
                            pose = pose,
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
                                            poseLabel = pose.label,
                                            charName = ch.name,
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
    pose: Api.Pose,
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
                if (locked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ProChip(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    )
                } else {
                    GridThumb(
                        model = Api.renderUrl(renderId, "body", 320, pose = pose.id),
                        contentDescription = pose.label,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = pose.label,
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
