package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppIconCache
import com.example.model.RiskLevel
import com.example.ui.ActiPermUiState
import com.example.ui.theme.AmberSuspicious
import com.example.ui.theme.AmberSuspiciousContainer
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonDangerContainer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.EmeraldSafeContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE -> EmeraldSafe
    RiskLevel.SUSPICIOUS -> AmberSuspicious
    RiskLevel.DANGEROUS -> CrimsonDanger
}

fun riskContainerColor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE -> EmeraldSafeContainer
    RiskLevel.SUSPICIOUS -> AmberSuspiciousContainer
    RiskLevel.DANGEROUS -> CrimsonDangerContainer
}

@Composable
fun AppIconImage(
    packageName: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false
) {
    val context = LocalContext.current
    var bitmap by remember(packageName) { mutableStateOf<ImageBitmap?>(AppIconCache.getCached(packageName)) }

    LaunchedEffect(packageName) {
        if (bitmap == null) {
            bitmap = AppIconCache.loadIcon(context, packageName)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateSurfaceHigh)
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
            .alpha(if (dimmed) 0.5f else 1f),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Android,
                contentDescription = contentDescription,
                tint = TextSecondary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    riskScore: Int,
    modifier: Modifier = Modifier
) {
    val tint = riskColor(riskLevel)
    val bg = riskContainerColor(riskLevel)
    val icon = when (riskLevel) {
        RiskLevel.SAFE -> Icons.Rounded.GppGood
        RiskLevel.SUSPICIOUS -> Icons.Rounded.GppMaybe
        RiskLevel.DANGEROUS -> Icons.Rounded.GppBad
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${riskLevel.label} • $riskScore",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RiskGaugeCanvas(
    score: Int,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 92.dp
) {
    val targetFraction = (score.coerceIn(0, 100)) / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 700),
        label = "riskGauge"
    )
    val activeColor = riskColor(riskLevel)

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(gaugeSize)) {
            val strokePx = 9.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawArc(
                color = SlateBorder,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedFraction.coerceAtLeast(0.03f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = activeColor
            )
            Text(
                text = "/ 100",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SecurityDashboardCard(
    uiState: ActiPermUiState,
    onRiskPillClick: (RiskLevel) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = uiState.totalAppsCount.coerceAtLeast(1)
    val safeFrac = uiState.safeAppsCount.toFloat() / total
    val suspFrac = uiState.suspiciousAppsCount.toFloat() / total
    val dangerFrac = uiState.dangerousAppsCount.toFloat() / total

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("security_dashboard_card"),
        shape = RoundedCornerShape(22.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.12f),
                            radius = size.width * 0.6f
                        )
                    )
                }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (uiState.dangerousAppsCount > 0) {
                                    "${uiState.dangerousAppsCount} aplikasi berisiko tinggi terdeteksi"
                                } else {
                                    "Semua aplikasi terpantau aman"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.dangerousAppsCount > 0) CrimsonDanger else EmeraldSafe,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.detectedDeviceBrandName} • ${uiState.androidVersionLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onExportClick)
                            .testTag("export_txt_button"),
                        shape = RoundedCornerShape(10.dp),
                        color = SlateSurfaceHigh,
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = stringResource(R.string.action_export_full_report),
                                tint = CyberCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_export_txt),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = CyberCyan
                            )
                        }
                    }
                }

                // Dua metrik utama: Total Aplikasi & Total Izin Berbahaya
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardStatTile(
                        title = stringResource(R.string.dashboard_total_apps),
                        value = uiState.totalAppsCount.toString(),
                        subtitle = "${uiState.downloadedAppsCount} Unduhan • ${uiState.systemAppsCount} Bawaan",
                        accentColor = CyberCyan,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_total_apps")
                    )
                    DashboardStatTile(
                        title = stringResource(R.string.dashboard_total_dangerous_perms),
                        value = uiState.totalDangerousPermissionsCount.toString(),
                        subtitle = "Di seluruh aplikasi HP",
                        accentColor = CrimsonDanger,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_total_dangerous_perms")
                    )
                }

                // Segmented Risk Distribution Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    drawRoundRect(
                        color = SlateSurfaceHigh,
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                    var startX = 0f
                    val segments = listOf(
                        dangerFrac to CrimsonDanger,
                        suspFrac to AmberSuspicious,
                        safeFrac to EmeraldSafe
                    )
                    for ((frac, color) in segments) {
                        val w = size.width * frac
                        if (w > 0f) {
                            drawRect(
                                color = color,
                                topLeft = Offset(startX, 0f),
                                size = Size(w, size.height)
                            )
                            startX += w
                        }
                    }
                }

                // Ringkasan 3 kategori skor bahaya (interaktif untuk quick-filter)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiskSummaryPill(
                        label = stringResource(R.string.dashboard_risk_dangerous),
                        count = uiState.dangerousAppsCount,
                        level = RiskLevel.DANGEROUS,
                        selected = uiState.riskFilter == RiskLevel.DANGEROUS,
                        onClick = { onRiskPillClick(RiskLevel.DANGEROUS) },
                        modifier = Modifier.weight(1f)
                    )
                    RiskSummaryPill(
                        label = stringResource(R.string.dashboard_risk_suspicious),
                        count = uiState.suspiciousAppsCount,
                        level = RiskLevel.SUSPICIOUS,
                        selected = uiState.riskFilter == RiskLevel.SUSPICIOUS,
                        onClick = { onRiskPillClick(RiskLevel.SUSPICIOUS) },
                        modifier = Modifier.weight(1f)
                    )
                    RiskSummaryPill(
                        label = stringResource(R.string.dashboard_risk_safe),
                        count = uiState.safeAppsCount,
                        level = RiskLevel.SAFE,
                        selected = uiState.riskFilter == RiskLevel.SAFE,
                        onClick = { onRiskPillClick(RiskLevel.SAFE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardStatTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SlateSurfaceHigh,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RiskSummaryPill(
    label: String,
    count: Int,
    level: RiskLevel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = riskColor(level)
    val bg = if (selected) riskContainerColor(level) else SlateSurfaceHigh
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("risk_pill_${level.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) color else SlateBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) color else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
