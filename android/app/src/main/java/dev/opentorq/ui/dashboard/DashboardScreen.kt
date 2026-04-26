package dev.opentorq.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.opentorq.ble.ConnectionState
import dev.opentorq.ble.Telemetry
import dev.opentorq.ui.components.RpmGauge
import dev.opentorq.ui.components.SpeedometerGauge

private val BgColor = Color(0xFF0A0A0A)
private val SurfaceColor = Color(0xFF161616)
private val LabelColor = Color(0xFF666666)
private val ValueColor = Color.White
private val GreenColor = Color(0xFF4CAF50)
private val OrangeColor = Color(0xFFFF9800)
private val RedColor = Color(0xFFF44336)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val state by vm.connectionState.collectAsState()
    val telemetry by vm.telemetry.collectAsState()
    val bluetoothError by vm.bluetoothError.collectAsState()

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text("OpenTorq", color = ValueColor, fontWeight = FontWeight.Bold)
                },
                actions = {
                    ConnectionChip(state = state, onStart = vm::startService, onStop = vm::stopService)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bluetoothError != null) {
                ErrorBanner(message = bluetoothError!!)
            }

            if (state == ConnectionState.Disconnected && telemetry == null) {
                DisconnectedPrompt()
            } else {
                val t = telemetry ?: Telemetry()

                // Side stand warning banner
                if (t.sideStand) {
                    SideStandWarning()
                }

                // Speedometer — full width, prominent
                SpeedometerGauge(
                    speed = t.speedKmh,
                    maxSpeed = 180,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )

                // Turn signal + RPM row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TurnSignalIndicator(direction = "◀", active = t.turnSignal == 1 || t.turnSignal == 3)
                    RpmGauge(
                        rpm = t.rpm,
                        maxRpm = 9000,
                        modifier = Modifier.size(180.dp),
                    )
                    TurnSignalIndicator(direction = "▶", active = t.turnSignal == 2 || t.turnSignal == 3)
                }

                // Stat row 1: Gear, Fuel, DTE
                StatRow {
                    StatCard(
                        label = "GEAR",
                        value = if (t.gear == 0) "N" else "${t.gear}",
                        valueColor = if (t.gear == 0) OrangeColor else ValueColor,
                    )
                    StatCard(
                        label = "FUEL",
                        value = "${t.fuelPercent}%",
                        valueColor = when {
                            t.fuelPercent < 15 -> RedColor
                            t.fuelPercent < 30 -> OrangeColor
                            else -> GreenColor
                        },
                    )
                    StatCard(
                        label = "DTE",
                        value = "${t.distanceToEmpty}",
                        unit = "km",
                        valueColor = when {
                            t.distanceToEmpty < 20 -> RedColor
                            t.distanceToEmpty < 50 -> OrangeColor
                            else -> ValueColor
                        },
                    )
                }

                // Stat row 2: Engine Temp, Throttle, Ride Mode
                StatRow {
                    StatCard(
                        label = "ENGINE",
                        value = "${t.engineTempC}°C",
                        valueColor = when {
                            t.engineTempC > 110 -> RedColor
                            t.engineTempC > 95  -> OrangeColor
                            else -> ValueColor
                        },
                    )
                    StatCard(label = "THROTTLE", value = "${t.throttlePercent}%")
                    StatCard(
                        label = "MODE",
                        value = rideModeLabel(t.rideMode),
                        valueColor = rideModeColor(t.rideMode),
                    )
                }

                // Stat row 3: Odometer
                StatRow {
                    StatCard(label = "ODO", value = "${"%.1f".format(t.odometer)}", unit = "km")
                    StatCard(label = "MILEAGE", value = if (t.instantMileage > 0) "${t.instantMileage}" else "—", unit = if (t.instantMileage > 0) "km/L" else null)
                    StatCard(label = "   ", value = "   ") // placeholder to keep grid even
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ConnectionChip(
    state: ConnectionState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val (label, color) = when (state) {
        ConnectionState.Connected    -> "Connected" to GreenColor
        ConnectionState.Scanning     -> "Scanning…" to OrangeColor
        ConnectionState.Connecting   -> "Connecting…" to OrangeColor
        ConnectionState.Disconnected -> "Connect" to LabelColor
    }

    Button(
        onClick = if (state == ConnectionState.Disconnected) onStart else onStop,
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = "  $label",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A1010))
            .padding(12.dp),
    ) {
        Text(text = message, color = RedColor, fontSize = 13.sp)
    }
}

@Composable
private fun DisconnectedPrompt() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🏍️", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Tap Connect to find your bike", color = LabelColor, fontSize = 15.sp)
        }
    }
}

@Composable
private fun StatRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun RowScope.StatCard(
    label: String,
    value: String,
    unit: String? = null,
    valueColor: Color = ValueColor,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceColor)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = LabelColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        if (unit != null) {
            Text(text = unit, color = LabelColor, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TurnSignalIndicator(direction: String, active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "blink",
    )
    Text(
        text = direction,
        fontSize = 28.sp,
        color = OrangeColor,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .alpha(if (active) alpha else 0.1f),
    )
}

@Composable
private fun SideStandWarning() {
    val infiniteTransition = rememberInfiniteTransition(label = "sidestand")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "sidestand",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3A1A00))
            .alpha(alpha)
            .padding(12.dp),
    ) {
        Text(
            text = "⚠ Side stand is down",
            color = OrangeColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun rideModeLabel(mode: Int) = when (mode) {
    0 -> "RAIN"
    1 -> "URBAN"
    2 -> "SPORT"
    3 -> "TRACK"
    else -> "—"
}

private fun rideModeColor(mode: Int) = when (mode) {
    0 -> Color(0xFF2196F3)  // blue — rain
    1 -> Color(0xFF4CAF50)  // green — urban
    2 -> Color(0xFFFF9800)  // orange — sport
    3 -> Color(0xFFF44336)  // red — track
    else -> LabelColor
}
