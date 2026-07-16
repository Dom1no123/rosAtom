package com.hackathon.radiationmonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.random.Random

class RadiationStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("radiation-monitor", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())

    var snapshot by mutableStateOf(createInitialSnapshot())
        private set
    var activeAlert by mutableStateOf<RadiationAlert?>(null)
        private set
    var darkTheme by mutableStateOf(preferences.getBoolean("dark-theme", true))
        private set
    var notificationsEnabled by mutableStateOf(preferences.getBoolean("notifications", true))
        private set
    var automaticMode by mutableStateOf(preferences.getBoolean("automatic", false))
        private set
    var refreshIntervalSec by mutableStateOf(preferences.getInt("refresh-interval", 60))
        private set
    val shouldShowNotificationPrompt: Boolean
        get() = !preferences.getBoolean("notification-prompt-seen", false)

    private var nextAutomaticAlert = scheduleAutomatic()
    private var lastStationRefresh = System.currentTimeMillis()

    init {
        restoreAlerts()
        createNotificationChannel()
    }

    private val ticker = object : Runnable {
        override fun run() {
            advance()
            handler.postDelayed(this, 5_000)
        }
    }

    fun start() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    fun stop() = handler.removeCallbacks(ticker)

    fun refresh() {
        val now = System.currentTimeMillis()
        val runningAlert = snapshot.alerts.firstOrNull { it.expiresAt > now }
        val incidentZone = runningAlert?.let { alert -> snapshot.zones.firstOrNull { it.id == alert.zoneId } }
        val normalStations = snapshot.stations.map { station ->
            station.copy(level = safeLevel(min(station.level, 0.29)), updatedAt = now)
        }
        val stations = if (incidentZone != null) applyIncidentToStations(normalStations, incidentZone, now) else normalStations
        val histories = snapshot.histories.toMutableMap()
        stations.forEach { station ->
            histories[station.base.id] = (histories[station.base.id].orEmpty() + HistoryPoint(now, station.level)).takeLast(24)
        }
        snapshot = snapshot.copy(stations = stations, histories = histories, updatedAt = now, tick = snapshot.tick + 1)
        lastStationRefresh = now
    }

    private fun advance() {
        val now = System.currentTimeMillis()
        if (now - lastStationRefresh >= refreshIntervalSec * 1_000L) refresh()
        val runningAlert = snapshot.alerts.firstOrNull { it.expiresAt > now }
        if (runningAlert == null && snapshot.stations.any { it.level >= 0.30 }) {
            val stations = snapshot.stations.map { it.copy(level = safeLevel(min(it.level, 0.29)), updatedAt = now) }
            val histories = appendHistory(snapshot.histories, stations, now)
            snapshot = snapshot.copy(stations = stations, histories = histories, updatedAt = now)
            lastStationRefresh = now
        }
        var zones = snapshot.zones.map { zone ->
            val alertIsActive = snapshot.alerts.any { it.zoneId == zone.id && it.expiresAt > now }
            if (alertIsActive) zone else zone.copy(level = safeLevel(min(zone.level, 0.28)), updatedAt = now)
        }
        val hasRunningIncident = snapshot.alerts.any { it.expiresAt > now }
        if (automaticMode && activeAlert == null && !hasRunningIncident && now >= nextAutomaticAlert) {
            val alert = createAlert(zones.random())
            zones = snapshot.zones
            activeAlert = alert
            nextAutomaticAlert = scheduleAutomatic(now + 60_000)
        }
        snapshot = snapshot.copy(zones = zones, updatedAt = now, tick = snapshot.tick + 1)
    }

    fun triggerTestAlert(zoneId: String? = null) {
        val now = System.currentTimeMillis()
        val running = snapshot.alerts.firstOrNull { it.expiresAt > now }
        if (running != null) {
            if (!running.acknowledged) activeAlert = running
            return
        }
        val zone = snapshot.zones.firstOrNull { it.id == zoneId } ?: snapshot.zones.random()
        activeAlert = createAlert(zone)
    }

    private fun createAlert(zone: Zone): RadiationAlert {
        val now = System.currentTimeMillis()
        val alert = RadiationAlert(
            id = "zone-alert-${zone.id}-$now",
            zoneId = zone.id,
            zoneName = zone.name,
            createdAt = now,
            expiresAt = now + 60_000,
        )
        val stations = applyIncidentToStations(snapshot.stations, zone, now)
        val histories = appendHistory(snapshot.histories, stations, now)
        snapshot = snapshot.copy(
            zones = snapshot.zones.map { if (it.id == zone.id) it.copy(level = 1.5, updatedAt = now) else it },
            stations = stations,
            histories = histories,
            alerts = (listOf(alert) + snapshot.alerts).take(50),
            updatedAt = now,
            tick = snapshot.tick + 1,
        )
        saveAlerts()
        if (notificationsEnabled) showNotification(alert)
        return alert
    }

    private fun applyIncidentToStations(stations: List<Station>, zone: Zone, now: Long): List<Station> =
        stations.map { station ->
            val averageLatitude = Math.toRadians((station.base.latitude + zone.centerLat) / 2.0)
            val latitudeKm = (station.base.latitude - zone.centerLat) * 111.0
            val longitudeKm = (station.base.longitude - zone.centerLon) * 111.0 * cos(averageLatitude)
            val distanceKm = sqrt(latitudeKm * latitudeKm + longitudeKm * longitudeKm)
            val incidentLevel = when {
                distanceKm <= 50.0 -> 1.25
                distanceKm <= 100.0 -> 0.78
                distanceKm <= 160.0 -> 0.42
                else -> station.level
            }
            station.copy(level = max(station.level, incidentLevel), updatedAt = now)
        }

    private fun appendHistory(
        source: Map<String, List<HistoryPoint>>,
        stations: List<Station>,
        now: Long,
    ): Map<String, List<HistoryPoint>> {
        val histories = source.toMutableMap()
        stations.forEach { station ->
            histories[station.base.id] = (histories[station.base.id].orEmpty() + HistoryPoint(now, station.level)).takeLast(24)
        }
        return histories
    }

    fun dismissAlert() {
        val current = activeAlert ?: return
        activeAlert = null
        snapshot = snapshot.copy(alerts = snapshot.alerts.map { if (it.id == current.id) it.copy(acknowledged = true) else it })
        saveAlerts()
    }

    fun updateDarkTheme(value: Boolean) {
        darkTheme = value
        preferences.edit().putBoolean("dark-theme", value).apply()
    }

    fun setNotifications(value: Boolean) {
        notificationsEnabled = value
        preferences.edit().putBoolean("notifications", value).apply()
    }

    fun completeNotificationOnboarding(enabled: Boolean) {
        setNotifications(enabled)
        preferences.edit().putBoolean("notification-prompt-seen", true).apply()
    }

    fun setAutomatic(value: Boolean) {
        automaticMode = value
        nextAutomaticAlert = scheduleAutomatic()
        preferences.edit().putBoolean("automatic", value).apply()
    }

    fun setRefreshInterval(seconds: Int) {
        refreshIntervalSec = seconds
        preferences.edit().putInt("refresh-interval", seconds).apply()
    }

    fun stationHistory(id: String) = snapshot.histories[id].orEmpty()

    private fun createInitialSnapshot(): Snapshot {
        val now = System.currentTimeMillis()
        val stations = BASE_STATIONS.mapIndexed { index, base ->
            Station(base, safeLevel(0.11 + (index % 6) * 0.025), now)
        }
        val histories = stations.associate { station ->
            var level = station.level
            station.base.id to List(24) { index ->
                level = safeLevel(level)
                HistoryPoint(now - (23 - index) * 60L * 60_000L, level)
            }
        }
        val weekly = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").map { day ->
            val average = 0.13 + Random.nextDouble() * 0.14
            DailyStat(day, average, max(0.05, average - Random.nextDouble() * 0.05), average + Random.nextDouble() * 0.12)
        }
        return Snapshot(stations, BASE_ZONES.map { it.copy(updatedAt = now) }, histories, weekly, emptyList(), now)
    }

    private fun safeLevel(current: Double): Double = (current + (Random.nextDouble() - 0.5) * 0.06).coerceIn(0.06, 0.29)

    private fun scheduleAutomatic(from: Long = System.currentTimeMillis()) = from + Random.nextLong(120_000, 300_001)

    private fun saveAlerts() {
        val encoded = snapshot.alerts.joinToString("\n") {
            listOf(it.id, it.zoneId, it.zoneName, it.createdAt, it.expiresAt, it.acknowledged).joinToString("\t")
        }
        preferences.edit().putString("alerts", encoded).apply()
    }

    private fun restoreAlerts() {
        val restored = preferences.getString("alerts", null).orEmpty().lineSequence().mapNotNull { line ->
            val values = line.split('\t')
            if (values.size != 6) return@mapNotNull null
            RadiationAlert(values[0], values[1], values[2], values[3].toLongOrNull() ?: return@mapNotNull null,
                values[4].toLongOrNull() ?: return@mapNotNull null, values[5].toBoolean())
        }.toList()
        snapshot = snapshot.copy(alerts = restored)
        activeAlert = restored.firstOrNull { !it.acknowledged && it.expiresAt > System.currentTimeMillis() }
        activeAlert?.let { alert ->
            snapshot.zones.firstOrNull { it.id == alert.zoneId }?.let { zone ->
                snapshot = snapshot.copy(stations = applyIncidentToStations(snapshot.stations, zone, System.currentTimeMillis()))
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("radiation-alerts", "Радиационные оповещения", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    @Suppress("DEPRECATION")
    private fun showNotification(alert: RadiationAlert) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, "radiation-alerts")
        } else android.app.Notification.Builder(context)
        builder
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Критический уровень радиации")
            .setContentText("Зона «${alert.zoneName}». Откройте инструкцию по безопасности.")
            .setStyle(android.app.Notification.BigTextStyle().bigText("Критический уровень радиации в зоне «${alert.zoneName}». Следуйте инструкции по безопасности."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(android.app.Notification.PRIORITY_MAX)
        try { manager.notify(alert.id.hashCode(), builder.build()) } catch (_: SecurityException) { }
    }
}

fun formatLevel(level: Double) = String.format(Locale.US, "%.2f мкЗв/ч", level)
