package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.ui.theme.FsdBackground
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurface
import com.fsdetc.fsdstudio.ui.theme.FsdOnSurfaceMuted
import com.fsdetc.fsdstudio.ui.theme.FsdSecondaryAccent

private data class Benefit(
    val textRes: Int,
    val chipRes: Int? = null,
)

private val BENEFITS = listOf(
    Benefit(R.string.pro_all_poses),
    Benefit(R.string.pro_hd),
    Benefit(R.string.pro_ai_50),
    Benefit(R.string.pro_multi, R.string.coming_soon),
    Benefit(R.string.pro_backgrounds),
    Benefit(R.string.pro_pfp, R.string.coming_soon),
    Benefit(R.string.pro_noads, R.string.pro_at_launch),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pro_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FsdBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.pro_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = FsdOnSurface,
            )
            Text(
                text = stringResource(R.string.pro_price),
                style = MaterialTheme.typography.titleLarge,
                color = FsdSecondaryAccent,
            )
            Spacer(Modifier.height(4.dp))
            BENEFITS.forEach { benefit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = FsdSecondaryAccent,
                    )
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(
                        text = stringResource(benefit.textRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = FsdOnSurface,
                    )
                    benefit.chipRes?.let { chip ->
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        InfoChip(stringResource(chip))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { /* Purchases open at launch; nothing to do in this build. */ },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.pro_at_launch))
            }
            Text(
                text = stringResource(R.string.pro_note),
                style = MaterialTheme.typography.bodySmall,
                color = FsdOnSurfaceMuted,
            )
        }
    }
}
