package com.fsdetc.fsdstudio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fsdetc.fsdstudio.FsdStudioApp
import com.fsdetc.fsdstudio.R
import com.fsdetc.fsdstudio.data.Plan

private enum class Billing { WEEKLY, MONTHLY }

/** One feature row on a plan card: a check, a dash, or a short value text. */
private data class FeatureRow(val labelRes: Int, val mark: Mark, val valueRes: Int? = null)

private enum class Mark { CHECK, DASH, VALUE }

private fun planRows(aiValueRes: Int): List<FeatureRow> = listOf(
    FeatureRow(R.string.sub_feat_poses, Mark.CHECK),
    FeatureRow(R.string.sub_feat_hd, Mark.CHECK),
    FeatureRow(R.string.sub_feat_downloads, Mark.CHECK),
    FeatureRow(R.string.sub_feat_backgrounds, Mark.CHECK),
    FeatureRow(R.string.sub_feat_ai, Mark.VALUE, aiValueRes),
    FeatureRow(R.string.sub_feat_noads, Mark.VALUE, R.string.sub_at_launch),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as FsdStudioApp
    val plan by app.container.entitlements.plan.collectAsState(initial = Plan.FREE)

    // ONE hoisted billing-period state drives BOTH cards: price and period
    // label are derived from it in exactly one place, so the Pro and Max
    // cards can never disagree (the old per-card remember wiring could drop
    // the state and leave the cards showing different periods).
    var billingPeriod by rememberSaveable { mutableStateOf(Billing.MONTHLY) }
    val proPriceRes = if (billingPeriod == Billing.WEEKLY) {
        R.string.plan_price_pro_weekly
    } else {
        R.string.plan_price_pro_monthly
    }
    val maxPriceRes = if (billingPeriod == Billing.WEEKLY) {
        R.string.plan_price_max_weekly
    } else {
        R.string.plan_price_max_monthly
    }
    val periodRes = if (billingPeriod == Billing.WEEKLY) {
        R.string.sub_per_week
    } else {
        R.string.sub_per_month
    }
    var dialogPlan by remember { mutableStateOf<Plan?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.pro_headline),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.sub_current_plan,
                    stringResource(planNameRes(plan)),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ------------------------------------------------ billing toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = billingPeriod == Billing.WEEKLY,
                    onClick = { billingPeriod = Billing.WEEKLY },
                    label = { Text(stringResource(R.string.sub_billing_weekly)) },
                )
                FilterChip(
                    selected = billingPeriod == Billing.MONTHLY,
                    onClick = { billingPeriod = Billing.MONTHLY },
                    label = { Text(stringResource(R.string.sub_billing_monthly)) },
                )
            }

            // ------------------------------------------------ plan cards
            PlanCard(
                nameRes = R.string.plan_name_pro,
                priceRes = proPriceRes,
                periodRes = periodRes,
                rows = planRows(R.string.sub_feat_ai_pro_value),
                gradient = false,
                best = false,
                isCurrent = plan == Plan.PRO,
                chooseLabelRes = R.string.sub_choose_pro,
                onChoose = { dialogPlan = Plan.PRO },
            )
            PlanCard(
                nameRes = R.string.plan_name_max,
                priceRes = maxPriceRes,
                periodRes = periodRes,
                rows = planRows(R.string.sub_feat_ai_max_value),
                gradient = true,
                best = true,
                isCurrent = plan == Plan.MAX,
                chooseLabelRes = R.string.sub_choose_max,
                onChoose = { dialogPlan = Plan.MAX },
            )

            // ------------------------------------------------ comparison
            Text(
                text = stringResource(R.string.sub_compare),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            CompareTable()
            Spacer(Modifier.height(8.dp))
        }
    }

    dialogPlan?.let { chosen ->
        AlertDialog(
            onDismissRequest = { dialogPlan = null },
            title = { Text(stringResource(planNameRes(chosen))) },
            text = { Text(stringResource(R.string.sub_purchase_note)) },
            confirmButton = {
                TextButton(onClick = { dialogPlan = null }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

private fun planNameRes(plan: Plan): Int = when (plan) {
    Plan.FREE -> R.string.plan_name_free
    Plan.PRO -> R.string.plan_name_pro
    Plan.MAX -> R.string.plan_name_max
}

@Composable
private fun PlanCard(
    nameRes: Int,
    priceRes: Int,
    periodRes: Int,
    rows: List<FeatureRow>,
    gradient: Boolean,
    best: Boolean,
    isCurrent: Boolean,
    chooseLabelRes: Int,
    onChoose: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header band: solid purple for Pro, purple→blue gradient for Max.
            val headerModifier = if (gradient) {
                Modifier.background(
                    Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.secondary)),
                )
            } else {
                Modifier.background(scheme.primaryContainer)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(headerModifier)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(nameRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                if (best) {
                    InfoChip(text = stringResource(R.string.sub_best_badge))
                }
                if (isCurrent) {
                    InfoChip(text = stringResource(R.string.sub_current_badge))
                }
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(priceRes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = " " + stringResource(periodRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                rows.forEach { row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(row.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        when (row.mark) {
                            Mark.CHECK -> Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = scheme.secondary,
                            )
                            Mark.DASH -> Text(
                                text = "—",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurfaceVariant,
                            )
                            Mark.VALUE -> Text(
                                text = row.valueRes?.let { stringResource(it) } ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.secondary,
                            )
                        }
                    }
                }
                Button(
                    onClick = onChoose,
                    enabled = !isCurrent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Text(
                        stringResource(
                            if (isCurrent) R.string.sub_current_badge else chooseLabelRes,
                        ),
                    )
                }
            }
        }
    }
}

/** Compact Free / Pro / Max comparison. Values are short by design. */
@Composable
private fun CompareTable() {
    val scheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CompareRow(
                label = "",
                values = listOf(
                    stringResource(R.string.plan_name_free),
                    stringResource(R.string.plan_name_pro),
                    stringResource(R.string.plan_name_max),
                ),
                header = true,
            )
            CompareRow(stringResource(R.string.cmp_poses), listOf("18", "60", "60"))
            CompareRow(stringResource(R.string.cmp_hd), listOf("—", "✓", "✓"))
            CompareRow(stringResource(R.string.cmp_downloads), listOf("5", "∞", "∞"))
            CompareRow(stringResource(R.string.cmp_ai), listOf("3", "3", "50"))
        }
    }
}

@Composable
private fun CompareRow(label: String, values: List<String>, header: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            modifier = Modifier.weight(1.6f),
        )
        values.forEach { value ->
            Text(
                text = value,
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = if (header) scheme.onSurface else scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
