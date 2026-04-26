package dev.opentorq.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Arc spans from 150° to 390° (240° sweep), bottom-centred like a real speedometer
private const val START_ANGLE = 150f
private const val SWEEP_ANGLE = 240f

@Composable
fun SpeedometerGauge(
    speed: Int,
    maxSpeed: Int = 180,
    modifier: Modifier = Modifier,
) {
    val animatedSpeed = remember { Animatable(0f) }
    LaunchedEffect(speed) {
        animatedSpeed.animateTo(speed.toFloat(), tween(300))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            drawArcGauge(
                value = animatedSpeed.value,
                max = maxSpeed.toFloat(),
                trackColor = Color(0xFF222222),
                sweepBrush = speedBrush(),
                strokeWidth = size.width * 0.08f,
                tickCount = 9,
                tickLabels = (0..maxSpeed step (maxSpeed / 6)).map { it.toString() },
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${speed}",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "km/h",
                fontSize = 14.sp,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun RpmGauge(
    rpm: Int,
    maxRpm: Int = 9000,
    modifier: Modifier = Modifier,
) {
    val animatedRpm = remember { Animatable(0f) }
    LaunchedEffect(rpm) {
        animatedRpm.animateTo(rpm.toFloat(), tween(300))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            drawArcGauge(
                value = animatedRpm.value,
                max = maxRpm.toFloat(),
                trackColor = Color(0xFF222222),
                sweepBrush = rpmBrush(),
                strokeWidth = size.width * 0.07f,
                tickCount = 9,
                tickLabels = listOf("0", "1k", "2k", "3k", "4k", "5k", "6k", "7k", "8k"),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${rpm}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = rpmColor(rpm, maxRpm),
            )
            Text(
                text = "rpm",
                fontSize = 11.sp,
                color = Color(0xFF888888),
            )
        }
    }
}

private fun DrawScope.drawArcGauge(
    value: Float,
    max: Float,
    trackColor: Color,
    sweepBrush: Brush,
    strokeWidth: Float,
    tickCount: Int,
    tickLabels: List<String>,
) {
    val inset = strokeWidth / 2f + strokeWidth * 0.1f
    val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
    val topLeft = Offset(inset, inset)

    // Track (background arc)
    drawArc(
        color = trackColor,
        startAngle = START_ANGLE,
        sweepAngle = SWEEP_ANGLE,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
    )

    // Value arc
    val valueSweep = (value / max).coerceIn(0f, 1f) * SWEEP_ANGLE
    if (valueSweep > 0f) {
        drawArc(
            brush = sweepBrush,
            startAngle = START_ANGLE,
            sweepAngle = valueSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }

    // Tick marks
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = (size.width / 2f) - inset - strokeWidth * 0.1f
    val tickOuter = radius + strokeWidth * 0.3f
    val tickInner = radius - strokeWidth * 0.5f

    for (i in 0..tickCount) {
        val angle = Math.toRadians((START_ANGLE + (i.toFloat() / tickCount) * SWEEP_ANGLE).toDouble())
        val outerX = cx + tickOuter * cos(angle).toFloat()
        val outerY = cy + tickOuter * sin(angle).toFloat()
        val innerX = cx + tickInner * cos(angle).toFloat()
        val innerY = cy + tickInner * sin(angle).toFloat()
        drawLine(
            color = Color(0xFF555555),
            start = Offset(outerX, outerY),
            end = Offset(innerX, innerY),
            strokeWidth = 2.dp.toPx(),
        )
    }

    // Needle dot at center
    drawCircle(
        color = Color(0xFF333333),
        radius = strokeWidth * 0.6f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = strokeWidth * 0.25f,
        center = Offset(cx, cy),
    )
}

private fun speedBrush() = Brush.sweepGradient(
    0.0f to Color(0xFF4CAF50),   // green
    0.4f to Color(0xFF8BC34A),   // light green
    0.6f to Color(0xFFFFC107),   // amber
    0.8f to Color(0xFFFF5722),   // deep orange
    1.0f to Color(0xFFF44336),   // red
)

private fun rpmBrush() = Brush.sweepGradient(
    0.0f to Color(0xFF2196F3),   // blue
    0.5f to Color(0xFF4CAF50),   // green
    0.75f to Color(0xFFFF9800),  // orange
    1.0f to Color(0xFFF44336),   // red
)

@Composable
private fun rpmColor(rpm: Int, max: Int): Color {
    val ratio = rpm.toFloat() / max
    return when {
        ratio > 0.85f -> Color(0xFFF44336)
        ratio > 0.7f  -> Color(0xFFFF9800)
        else          -> Color(0xFF4CAF50)
    }
}
