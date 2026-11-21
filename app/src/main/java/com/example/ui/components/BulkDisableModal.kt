package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.BulkDisableQueueState
import com.example.model.BulkItemStatus
import com.example.ui.theme.AmberSuspicious
import com.example.ui.theme.AmberSuspiciousContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BulkSelectionBottomBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onStartBulkDisable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bulk_selection_bar"),
        shape = RoundedCornerShape(18.dp),
        color = SlateSurfaceHigh,
        border = BorderStroke(1.5.dp, AmberSuspicious.copy(alpha = 0.7f)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$selectedCount aplikasi dipilih",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Buka detail sistem satu per satu",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            TextButton(
                onClick = onClearSelection,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("bulk_clear_btn")
            ) {
                Text(
                    text = stringResource(R.string.bulk_clear_selection),
                    color = TextSecondary
                )
            }

            Button(
                onClick = onStartBulkDisable,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("bulk_start_disable_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberSuspicious,
                    contentColor = ObsidianBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Block,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stringResource(R.string.bulk_disable_action)} ($selectedCount)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun BulkDisableQueueDialog(
    queue: BulkDisableQueueState,
    autoAdvanceActive: Boolean,
    onOpenNow: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("bulk_queue_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = SlateSurface,
            border = BorderStroke(1.dp, SlateBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bulk_queue_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        val total = queue.entries.size
                        val currentPos = (queue.currentIndex + 1).coerceAtMost(total)
                        Text(
                            text = if (queue.isFinished) {
                                "Selesai memproses $total aplikasi"
                            } else {
                                "Aplikasi $currentPos dari $total • ${queue.completedCount} diproses"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberCyan
                        )
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup antrean",
                            tint = TextSecondary
                        )
                    }
                }

                val progress = if (queue.entries.isEmpty()) 0f else {
                    queue.completedCount.toFloat() / queue.entries.size
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (queue.isFinished) EmeraldSafe else AmberSuspicious,
                    trackColor = SlateSurfaceHigh
                )

                Text(
                    text = if (autoAdvanceActive) {
                        "Membuka Pengaturan Detail Aplikasi berikutnya otomatis…"
                    } else {
                        stringResource(R.string.bulk_queue_instruction)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (autoAdvanceActive) AmberSuspicious else TextSecondary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(queue.entries) { index, item ->
                        val isCurrent = !queue.isFinished && index == queue.currentIndex
                        val statusColor = when (item.status) {
                            BulkItemStatus.DISABLED -> EmeraldSafe
                            BulkItemStatus.ACTIVE -> AmberSuspicious
                            BulkItemStatus.STILL_ACTIVE -> TextSecondary
                            BulkItemStatus.SKIPPED -> TextMuted
                            BulkItemStatus.PENDING -> TextSecondary
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) AmberSuspiciousContainer.copy(alpha = 0.55f) else SlateSurfaceHigh,
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) AmberSuspicious.copy(alpha = 0.6f) else SlateBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppIconImage(
                                    packageName = item.packageName,
                                    contentDescription = item.appName,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.appName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.status.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor
                                    )
                                }
                                Icon(
                                    imageVector = when (item.status) {
                                        BulkItemStatus.DISABLED -> Icons.Rounded.CheckCircle
                                        BulkItemStatus.ACTIVE -> Icons.Rounded.OpenInNew
                                        else -> Icons.Rounded.HourglassTop
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (queue.isFinished) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("bulk_done_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSafe,
                            contentColor = ObsidianBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Selesai",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("bulk_cancel_btn"),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bulk_queue_cancel),
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("bulk_skip_btn"),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bulk_queue_skip),
                                color = TextPrimary
                            )
                        }
                        Button(
                            onClick = onOpenNow,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("bulk_open_now_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberSuspicious,
                                contentColor = ObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bulk_queue_open_now),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
