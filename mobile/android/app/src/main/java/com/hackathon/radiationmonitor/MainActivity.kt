package com.hackathon.radiationmonitor

import android.Manifest
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Expand
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val store by lazy { RadiationStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RadiationTheme(store.darkTheme) { RadiationApp(store) } }
    }

    override fun onStart() {
        super.onStart()
        store.start()
    }

    override fun onStop() {
        store.stop()
        super.onStop()
    }
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    background = Color(0xFF0B1120),
    surface = Color(0xFF171B27),
    surfaceVariant = Color(0xFF111827),
    onBackground = Color(0xFFF5F6FA),
    onSurface = Color(0xFFF5F6FA),
    onSurfaceVariant = Color(0xFF9AA3B2),
    outline = Color(0xFF303645),
    error = Color(0xFFFF453A),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0A84FF),
    background = Color(0xFFF2F3F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFF7F8FA),
    onBackground = Color(0xFF0B1120),
    onSurface = Color(0xFF0B1120),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFDADCE2),
    error = Color(0xFFFF453A),
)

@Composable
private fun RadiationTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

@Composable
private fun RadiationApp(store: RadiationStore) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var stationDetail by remember { mutableStateOf<Station?>(null) }
    var showNotificationPrompt by remember { mutableStateOf(store.shouldShowNotificationPrompt) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        store.setNotifications(granted)
    }
    val askNotificationPermission = {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            store.setNotifications(true)
        }
    }

    store.activeAlert?.let { alert ->
        EmergencyScreen(alert, store::dismissAlert)
        return
    }

    stationDetail?.let { station ->
        BackHandler { stationDetail = null }
        StationDetailScreen(station, store.stationHistory(station.base.id)) { stationDetail = null }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 0.dp,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(tabIcon(item), item.title, Modifier.size(21.dp)) },
                        label = { Text(item.title, fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                AppTab.HOME -> HomeScreen(store, onMap = { tab = AppTab.MAP }, onStation = { stationDetail = it })
                AppTab.MAP -> MapScreen(store.snapshot)
                AppTab.STATISTICS -> StatisticsScreen(store.snapshot, store::refresh)
                AppTab.NOTIFICATIONS -> NotificationsScreen(store.snapshot.alerts)
                AppTab.INSTRUCTIONS -> InstructionsScreen {
                    askNotificationPermission()
                    store.triggerTestAlert()
                }
                AppTab.SETTINGS -> SettingsScreen(store, askNotificationPermission)
            }
        }
    }

    if (showNotificationPrompt) {
        NotificationOnboardingDialog(
            onEnable = {
                showNotificationPrompt = false
                store.completeNotificationOnboarding(true)
                askNotificationPermission()
            },
            onLater = {
                showNotificationPrompt = false
                store.completeNotificationOnboarding(false)
            },
        )
    }
}

@Composable
private fun NotificationOnboardingDialog(onEnable: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text("Включить уведомления?", fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                "Приложение сможет сразу предупредить о критическом уровне радиации, даже если открыт другой экран. Уведомления работают локально и не требуют сервера.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            Button(onClick = onEnable, shape = CircleShape) { Text("Включить", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("Не сейчас") }
        },
    )
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.HOME -> Icons.Outlined.Home
    AppTab.MAP -> Icons.Outlined.Map
    AppTab.STATISTICS -> Icons.Outlined.BarChart
    AppTab.NOTIFICATIONS -> Icons.Outlined.Notifications
    AppTab.INSTRUCTIONS -> Icons.Outlined.Description
    AppTab.SETTINGS -> Icons.Outlined.Settings
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        subtitle?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = MaterialTheme.colorScheme.outline, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { Column(Modifier.padding(18.dp)) { content() } }
}

@Composable
private fun RadiationBadge(status: RadiationStatus, large: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(status.color.copy(alpha = 0.16f), CircleShape).padding(horizontal = if (large) 14.dp else 10.dp, vertical = if (large) 8.dp else 6.dp),
    ) {
        Box(Modifier.size(if (large) 9.dp else 7.dp).background(status.color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(status.label, color = status.color, fontSize = if (large) 14.sp else 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeScreen(store: RadiationStore, onMap: () -> Unit, onStation: (Station) -> Unit) {
    val snapshot = store.snapshot
    val average = snapshot.stations.map { it.level }.average()
    val overall = snapshot.stations.maxByOrNull { it.status.ordinal }?.status ?: RadiationStatus.NORMAL
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .statusBarsPadding().padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                Text("Радиационный мониторинг", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Обстановка по стране", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        item {
            GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Средний уровень", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatLevel(average), fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                        }
                        RadiationBadge(overall, true)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Обновлено: ${dateTime(snapshot.updatedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(onClick = store::refresh, shape = CircleShape, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Text("Обновить", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
                        }
                    }
                }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(240.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(28.dp))) {
                OsmMap(snapshot, interactive = false)
                Button(onClick = onMap, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Outlined.Expand, null, Modifier.size(16.dp))
                    Text("Полная карта", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
        item { Text("Последние точки", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 12.dp)) }
        items(
            snapshot.stations.sortedWith(
                compareByDescending<Station> { it.status.ordinal }.thenByDescending { it.level },
            ).take(5),
            key = { it.base.id },
        ) { station ->
            StationCard(station, Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { onStation(station) }
        }
    }
}

@Composable
private fun StationCard(station: Station, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(station.base.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(station.base.region, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timeAgo(station.updatedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatLevel(station.level), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                RadiationBadge(station.status)
            }
        }
    }
}

@Composable
private fun MapScreen(snapshot: Snapshot) {
    var selected by remember { mutableStateOf<Zone?>(null) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Зоны радиации")
            Row(
                Modifier.padding(start = 20.dp, bottom = 12.dp).background(RadiationStatus.NORMAL.color.copy(alpha = 0.16f), CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).background(RadiationStatus.NORMAL.color, CircleShape))
                Text("Автономная симуляция", color = RadiationStatus.NORMAL.color, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
            OsmMap(snapshot, interactive = true, onZone = { selected = it }, modifier = Modifier.weight(1f))
        }
        selected?.let { zone ->
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { selected = null }) {
                GlassCard(Modifier.align(Alignment.BottomCenter).padding(16.dp), borderColor = zone.status.color.copy(alpha = 0.5f)) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text("Зона «${zone.name}»", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                            Icon(Icons.Outlined.Close, "Закрыть", Modifier.clickable { selected = null })
                        }
                        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateTime(zone.updatedAt), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatLevel(zone.level), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            RadiationBadge(zone.status)
                        }
                        Text(zone.status.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

private fun project(lat: Double, lon: Double, width: Float, height: Float): Offset = Offset(
    (((lon - 43.35) / (46.75 - 43.35)) * width).toFloat(),
    (((41.35 - lat) / (41.35 - 38.75)) * height).toFloat(),
)

@Composable
private fun OfflineMap(snapshot: Snapshot, interactive: Boolean, modifier: Modifier = Modifier, onZone: (Zone) -> Unit = {}) {
    val cardColor = MaterialTheme.colorScheme.surface
    val border = MaterialTheme.colorScheme.outline
    val stationColor = MaterialTheme.colorScheme.onSurface
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))) {
        Canvas(
            Modifier.fillMaxSize().then(if (interactive) Modifier.pointerInput(snapshot.tick) {
                detectTapGestures { point ->
                    snapshot.zones.minByOrNull { zone -> (project(zone.centerLat, zone.centerLon, size.width.toFloat(), size.height.toFloat()) - point).getDistance() }
                        ?.takeIf { (project(it.centerLat, it.centerLon, size.width.toFloat(), size.height.toFloat()) - point).getDistance() < 75f }
                        ?.let(onZone)
                }
            } else Modifier),
        ) {
            val sx = size.width / 320f
            val sy = size.height / 500f
            val outlinePath = Path().apply {
                moveTo(98*sx,28*sy); lineTo(142*sx,48*sy); lineTo(169*sx,91*sy); lineTo(216*sx,109*sy)
                lineTo(238*sx,151*sy); lineTo(225*sx,193*sy); lineTo(267*sx,231*sy); lineTo(251*sx,278*sy)
                lineTo(275*sx,323*sy); lineTo(239*sx,359*sy); lineTo(224*sx,407*sy); lineTo(183*sx,470*sy)
                lineTo(145*sx,451*sy); lineTo(127*sx,399*sy); lineTo(91*sx,373*sy); lineTo(79*sx,329*sy)
                lineTo(48*sx,293*sy); lineTo(66*sx,252*sy); lineTo(43*sx,210*sy); lineTo(68*sx,170*sy)
                lineTo(57*sx,122*sy); lineTo(88*sx,87*sy); close()
            }
            drawPath(outlinePath, cardColor)
            drawPath(outlinePath, border, style = Stroke(width = 4f))
            snapshot.zones.forEach { zone ->
                val center = project(zone.centerLat, zone.centerLon, size.width, size.height)
                val severity = when (zone.status) { RadiationStatus.CRITICAL -> 1f; RadiationStatus.DANGEROUS -> .75f; RadiationStatus.ELEVATED -> .55f; else -> .38f }
                val radius = (24 + severity * 32) * minOf(sx, sy)
                val path = Path()
                repeat(8) { index ->
                    val angle = Math.PI * 2 * index / 8
                    val wobble = .86 + ((sin(snapshot.tick * .8 + index * 2.7 + zone.id.length) + 1) / 2) * .24
                    val p = Offset(center.x + (cos(angle) * radius * wobble).toFloat(), center.y + (sin(angle) * radius * wobble).toFloat())
                    if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, zone.status.color.copy(alpha = .34f))
                drawPath(path, zone.status.color, style = Stroke(width = 3f))
                drawCircle(zone.status.color, radius = 5f, center = center)
            }
            snapshot.stations.forEach { station ->
                drawCircle(stationColor, radius = 3.4f, center = project(station.base.latitude, station.base.longitude, size.width, size.height))
            }
        }
        Text("● станции · цвет — статус зоны", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(horizontal = 9.dp, vertical = 6.dp))
    }
}

@Composable
private fun StatisticsScreen(snapshot: Snapshot, refresh: () -> Unit) {
    val values = snapshot.stations.map { it.level }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
        item { PageHeader("Статистика", "Обновлено: ${dateTime(snapshot.updatedAt)}") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Минимум", values.minOrNull() ?: 0.0, RadiationStatus.NORMAL.color, Icons.Outlined.ArrowCircleDown, Modifier.weight(1f))
                StatPill("Среднее", values.average(), MaterialTheme.colorScheme.primary, Icons.Outlined.Analytics, Modifier.weight(1f))
                StatPill("Максимум", values.maxOrNull() ?: 0.0, RadiationStatus.CRITICAL.color, Icons.Outlined.ArrowCircleUp, Modifier.weight(1f))
            }
        }
        item { GlassCard(Modifier.padding(20.dp)) { Chart("Динамика уровня радиации", snapshot.weekly.map { it.average }, snapshot.weekly.map { it.day }) } }
        item { GlassCard(Modifier.padding(horizontal = 20.dp)) { BarChart("Недельная статистика (среднее)", snapshot.weekly) } }
        item { Text("По дням", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 10.dp)) }
        items(snapshot.weekly) { day ->
            Row(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)).padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(day.day, fontWeight = FontWeight.Bold)
                Text("min %.2f  ·  ср %.2f  ·  max %.2f".format(Locale.US, day.min, day.average, day.max), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { Button(onClick = refresh, modifier = Modifier.padding(20.dp).fillMaxWidth()) { Text("Обновить данные") } }
    }
}

@Composable
private fun StatPill(label: String, value: Double, color: Color, icon: ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text("%.2f".format(Locale.US, value), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 5.dp))
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Chart(title: String, values: List<Double>, labels: List<String>) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        if (values.size < 2) return@Canvas
        val minValue = values.minOrNull() ?: 0.0
        val range = max(0.05, (values.maxOrNull() ?: 1.0) - minValue)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * size.width / (values.size - 1)
            val y = size.height - 24f - (((value - minValue) / range) * (size.height - 40f)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(accent, 5f, Offset(x, y))
        }
        drawPath(path, accent, style = Stroke(4f, cap = StrokeCap.Round))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { labels.forEach { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun BarChart(title: String, stats: List<DailyStat>) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val accent = MaterialTheme.colorScheme.primary
    val maxValue = stats.maxOfOrNull { it.average } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val slot = size.width / stats.size
        stats.forEachIndexed { index, stat ->
            val h = (stat.average / maxValue * (size.height - 12f)).toFloat()
            drawRoundRect(accent.copy(alpha = .8f), topLeft = Offset(index * slot + slot * .2f, size.height - h), size = Size(slot * .6f, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { stats.forEach { Text(it.day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
}

@Composable
private fun NotificationsScreen(alerts: List<RadiationAlert>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
        item { PageHeader("Уведомления", if (alerts.isEmpty()) "Событий пока нет" else "${alerts.size} событий в журнале") }
        if (alerts.isEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Shield, null, Modifier.size(42.dp), tint = RadiationStatus.NORMAL.color)
                Text("Журнал аварийных событий пуст", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            }
        }
        items(alerts, key = { it.id }) { alert ->
            GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), RadiationStatus.CRITICAL.color.copy(alpha = .45f)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(RadiationStatus.CRITICAL.color.copy(alpha = .14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, null, Modifier.size(18.dp), tint = RadiationStatus.CRITICAL.color)
                        }
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(alert.zoneName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Критично", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RadiationStatus.CRITICAL.color)
                        }
                        Text(timeAgo(alert.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Критический уровень радиации в зоне «${alert.zoneName}». Следуйте инструкции по безопасности.",
                        fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun InstructionsScreen(triggerAlert: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        PageHeader("Инструкции")
        GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Общие рекомендации по безопасности", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Text(NORMAL_INSTRUCTIONS, fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            }
        }
        GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), RadiationStatus.CRITICAL.color.copy(alpha = .45f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = RadiationStatus.CRITICAL.color)
                    Text("Тестирование ЧП", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Text("Кнопка локально переводит случайную зону в критическое состояние и открывает экстренную инструкцию. Интернет и сервер не требуются. Через 60 секунд зона восстановится.",
                    fontSize = 13.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                Button(onClick = triggerAlert, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = RadiationStatus.CRITICAL.color)) {
                    Icon(Icons.Default.Warning, null, Modifier.size(18.dp))
                    Text("Тестовое ЧП (критическая зона)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(store: RadiationStore, requestNotification: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        PageHeader("Настройки")
        SettingsCard {
            SettingLabel(Icons.Outlined.Contrast, "Тема оформления")
            Row(Modifier.padding(top = 10.dp)) {
                Segment("Светлая", !store.darkTheme) { store.updateDarkTheme(false) }
                Segment("Тёмная", store.darkTheme) { store.updateDarkTheme(true) }
            }
        }
        SettingsCard {
            Text("Язык интерфейса · скоро", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                Segment("Русский", true) { }
                Segment("Հայերեն", false) { }
                Segment("English", false) { }
            }
        }
        SettingsCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SettingLabel(Icons.Outlined.Notifications, "Локальные уведомления")
                Switch(store.notificationsEnabled, onCheckedChange = {
                    store.setNotifications(it)
                    if (it) requestNotification()
                }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            Helper("Работают без сервера. Разрешение Android запрашивается локально.")
        }
        SettingsCard {
            Text("Режим аварийных событий", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(top = 8.dp)) {
                Segment("Только вручную", !store.automaticMode) { store.setAutomatic(false) }
                Segment("Автосимуляция", store.automaticMode) { store.setAutomatic(true) }
            }
            Helper("В автоматическом режиме событие возникает через 2–5 минут. Одновременно активна только одна зона.")
        }
        SettingsCard {
            Text("Интервал обновления данных", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                listOf(30, 60, 300, 600).forEach { sec -> Segment(if (sec < 60) "$sec сек" else "${sec/60} мин", store.refreshIntervalSec == sec) { store.setRefreshInterval(sec) } }
            }
        }
        Text("Radiation Monitor · v2.2.0 · Kotlin/Compose", textAlign = TextAlign.Center, fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(20.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) = GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { Column { content() } }

@Composable
private fun SettingLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun Segment(label: String, active: Boolean, onClick: () -> Unit) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 8.dp).background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
private fun Helper(text: String) = Text(text, fontSize = 11.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))

@Composable
private fun StationDetailScreen(station: Station, history: List<HistoryPoint>, close: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ArrowBack, "Назад", Modifier.size(34.dp).clickable(onClick = close).padding(5.dp))
            Column(Modifier.padding(start = 8.dp)) {
                Text(station.base.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(station.base.region, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Текущий уровень", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatLevel(station.level), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 4.dp))
                    }
                    RadiationBadge(station.status, true)
                }
                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline)
                DetailRow(Icons.Outlined.LocationOn, "%.4f, %.4f".format(Locale.US, station.base.latitude, station.base.longitude))
                DetailRow(Icons.Outlined.Schedule, "Последнее измерение: ${dateTime(station.updatedAt)}")
            }
        }
        GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Chart("История измерений (24ч)", history.filterIndexed { i, _ -> i % 4 == 0 }.map { it.level }, history.filterIndexed { i, _ -> i % 4 == 0 }.map { time(it.timestamp) })
        }
        val safe = station.status == RadiationStatus.NORMAL || station.status == RadiationStatus.ELEVATED
        val statusColor = if (safe) RadiationStatus.NORMAL.color else RadiationStatus.CRITICAL.color
        GlassCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), statusColor.copy(alpha = .45f)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (safe) Icons.Outlined.Shield else Icons.Default.Warning, null, tint = statusColor)
                    Text(if (safe) "Находиться рядом безопасно" else "Находиться рядом небезопасно", color = statusColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
                }
                Text(recommendation(station.status), fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) = Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
    Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
}

@Composable
private fun EmergencyScreen(alert: RadiationAlert, dismiss: () -> Unit) {
    BackHandler(enabled = true) { }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(alert.id) {
        val player = try { MediaPlayer.create(context, R.raw.alarm)?.apply { isLooping = true; start() } } catch (_: Exception) { null }
        onDispose { try { player?.stop(); player?.release() } catch (_: Exception) { } }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFB00020))) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, Modifier.size(40.dp), tint = Color.White)
            Text("ЭКСТРЕННОЕ ОПОВЕЩЕНИЕ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            Text("Зона «${alert.zoneName}» — критический уровень радиации", color = Color(0xFFFFE4E1), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(EMERGENCY_INSTRUCTIONS, color = Color(0xFF1A1A1A), fontSize = 14.sp, lineHeight = 22.sp)
        }
        Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB00020))) {
            Text("Я ознакомился с инструкцией", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 5.dp))
        }
    }
}

private fun recommendation(status: RadiationStatus) = when (status) {
    RadiationStatus.NORMAL -> "Радиационный фон в норме. Особые меры предосторожности не требуются."
    RadiationStatus.ELEVATED -> "Уровень немного повышен. Следите за обновлениями и сократите длительное пребывание на улице."
    RadiationStatus.DANGEROUS -> "Опасный уровень радиации. Ограничьте пребывание вне помещений."
    RadiationStatus.CRITICAL -> "Критический уровень радиации. Следуйте инструкциям экстренных служб."
}

private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
private val timeFormatter = SimpleDateFormat("HH:mm", Locale("ru"))
private fun dateTime(value: Long) = dateFormatter.format(Date(value))
private fun time(value: Long) = timeFormatter.format(Date(value))
private fun timeAgo(value: Long): String {
    val seconds = max(0, (System.currentTimeMillis() - value) / 1000)
    return when {
        seconds < 60 -> "только что"
        seconds < 3600 -> "${seconds / 60} мин назад"
        seconds < 86400 -> "${seconds / 3600} ч назад"
        else -> "${seconds / 86400} дн назад"
    }
}
