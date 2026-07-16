package com.hackathon.radiationmonitor

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

private const val TILE_SIZE = 256f
private const val CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1_000
private const val OSM_USER_AGENT = "RadiationMonitor/2.2 (com.hackathon.radiationmonitor)"
private val MAP_LOADING_COLOR = Color(0xFFE7EBF0)
private const val HEX_LATITUDE_RADIUS = 0.040
private const val HEX_LONGITUDE_RADIUS = 0.052

private val ARMENIA_OUTLINE = listOf(
    41.0988144 to 43.4471395, 40.8362290 to 43.6877510, 40.7620736 to 43.7331945,
    40.7279267 to 43.7474990, 40.7088664 to 43.7421084, 40.6858344 to 43.7444339,
    40.6334738 to 43.7183357, 40.5375072 to 43.6398182, 40.4871711 to 43.5569447,
    40.4089184 to 43.6137604, 40.2573388 to 43.6828082, 40.1591773 to 43.6806249,
    40.1149040 to 43.6568476, 40.0981609 to 43.6807799, 40.0852573 to 43.7063532,
    40.0813594 to 43.7449503, 40.0798949 to 43.7707693, 40.0661277 to 43.8009982,
    40.0530751 to 43.8240712, 40.0436667 to 43.8553754, 40.0342892 to 43.8758060,
    40.0212111 to 43.8974385, 40.0221083 to 43.9286198, 40.0186943 to 43.9525675,
    40.0219245 to 43.9722399, 40.0311227 to 43.9921611, 40.0262065 to 44.0177308,
    40.0285944 to 44.0369723, 40.0324019 to 44.0709003, 40.0315664 to 44.1065084,
    40.0300573 to 44.1287423, 40.0308564 to 44.1541493, 40.0300460 to 44.1817165,
    40.0391005 to 44.2058189, 40.0427931 to 44.2251913, 40.0476835 to 44.2567184,
    40.0433631 to 44.2946238, 40.0311229 to 44.3272806, 40.0200176 to 44.3574314,
    40.0031012 to 44.3981724, 39.9864223 to 44.4361350, 39.9724789 to 44.4641738,
    39.9561416 to 44.4916187, 39.9305645 to 44.5129255, 39.9138932 to 44.5393812,
    39.8826139 to 44.5526316, 39.8637375 to 44.5843715, 39.8297254 to 44.5917388,
    39.8226405 to 44.6192843, 39.8055707 to 44.6353424, 39.7922888 to 44.6698676,
    39.7863972 to 44.6936722, 39.7693906 to 44.7108603, 39.7461279 to 44.7368377,
    39.7143565 to 44.7574428, 39.5872958 to 45.2243830, 39.5752065 to 45.7820774,
    39.2005637 to 45.9894778, 38.8819884 to 46.2579588, 38.8893288 to 46.5336549,
    39.1835632 to 46.4325592, 39.1826365 to 46.5532320, 39.3471184 to 46.4455915,
    39.3785637 to 46.4028733, 39.4767427 to 46.5354104, 39.6008648 to 46.2495074,
    40.0029654 to 45.8430846, 40.3664315 to 45.6655823, 40.5987051 to 45.4052168,
    40.8880608 to 45.5898884, 40.8992149 to 45.5574435, 40.9995138 to 45.4190089,
    41.0001319 to 45.3476411, 41.0259061 to 45.2211759, 41.0500028 to 45.1151552,
    41.0972623 to 45.0714648, 41.1138360 to 45.1475098, 41.1662893 to 45.1914371,
    41.2235327 to 45.0478303, 41.2562399 to 44.9581791, 41.2786609 to 44.8472511,
    41.2298169 to 44.8423081, 41.2035443 to 44.7263829, 41.2024520 to 44.5983490,
    41.1879170 to 44.4584840, 41.1880650 to 44.1137270, 41.1888972 to 44.0285510,
    41.1392939 to 43.7949534,
)

private fun worldX(longitude: Double, zoom: Int): Double {
    val scale = 2.0.pow(zoom) * TILE_SIZE
    return (longitude + 180.0) / 360.0 * scale
}

private fun worldY(latitude: Double, zoom: Int): Double {
    val safeLatitude = latitude.coerceIn(-85.0511, 85.0511)
    val radians = Math.toRadians(safeLatitude)
    val scale = 2.0.pow(zoom) * TILE_SIZE
    return (1.0 - ln(tan(radians) + 1.0 / kotlin.math.cos(radians)) / PI) / 2.0 * scale
}

private fun longitudeAt(worldX: Double, zoom: Int): Double = worldX / (2.0.pow(zoom) * TILE_SIZE) * 360.0 - 180.0

private fun latitudeAt(worldY: Double, zoom: Int): Double {
    val scale = 2.0.pow(zoom) * TILE_SIZE
    return Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * worldY / scale))))
}

private fun insideArmenia(latitude: Double, longitude: Double): Boolean {
    var inside = false
    var previous = ARMENIA_OUTLINE.last()
    for (current in ARMENIA_OUTLINE) {
        val intersects = (current.first > latitude) != (previous.first > latitude) &&
            longitude < (previous.second - current.second) * (latitude - current.first) /
            (previous.first - current.first) + current.second
        if (intersects) inside = !inside
        previous = current
    }
    return inside
}

private fun interpolatedLevel(latitude: Double, longitude: Double, zones: List<Zone>): Double {
    var weightedLevel = 0.0
    var totalWeight = 0.0
    zones.forEach { zone ->
        val dx = (longitude - zone.centerLon) * cos(Math.toRadians(latitude))
        val dy = latitude - zone.centerLat
        val distanceSquared = dx * dx + dy * dy
        val weight = exp(-distanceSquared / (2.0 * 0.48 * 0.48)).coerceAtLeast(0.0001)
        weightedLevel += zone.level * weight
        totalWeight += weight
    }
    return if (totalWeight == 0.0) 0.12 else weightedLevel / totalWeight
}

private fun radiationGradient(level: Double): Color = when {
    level < 0.30 -> lerp(Color(0xFF30D158), Color(0xFFFFD60A), ((level - 0.06) / 0.24).toFloat().coerceIn(0f, 1f))
    level < 0.60 -> lerp(Color(0xFFFFD60A), Color(0xFFFF9F0A), ((level - 0.30) / 0.30).toFloat())
    level < 1.00 -> lerp(Color(0xFFFF9F0A), Color(0xFFFF453A), ((level - 0.60) / 0.40).toFloat())
    else -> Color(0xFFFF453A)
}

private data class RadiationHex(val latitude: Double, val longitude: Double)
private data class ColoredRadiationHex(val latitude: Double, val longitude: Double, val color: Color)

private fun buildArmeniaHexGrid(): List<RadiationHex> {
    val cells = mutableListOf<RadiationHex>()
    val latitudeStep = HEX_LATITUDE_RADIUS * 1.5
    val longitudeStep = sqrt(3.0) * HEX_LONGITUDE_RADIUS
    var row = 0
    var latitude = 38.78
    while (latitude <= 41.34) {
        var longitude = 43.35 + if (row % 2 == 0) 0.0 else longitudeStep / 2.0
        while (longitude <= 46.65) {
            if (insideArmenia(latitude, longitude)) cells += RadiationHex(latitude, longitude)
            longitude += longitudeStep
        }
        latitude += latitudeStep
        row++
    }
    return cells
}

private val ARMENIA_HEX_GRID by lazy(::buildArmeniaHexGrid)

@Composable
fun OsmMap(
    snapshot: Snapshot,
    interactive: Boolean,
    modifier: Modifier = Modifier,
    onZone: (Zone) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val zoomState = remember { mutableFloatStateOf(7f) }
    val centerLatitude = remember { mutableDoubleStateOf(40.05) }
    val centerLongitude = remember { mutableDoubleStateOf(44.90) }
    val coloredHexes = remember(snapshot.zones) {
        ARMENIA_HEX_GRID.map { cell ->
            val level = interpolatedLevel(cell.latitude, cell.longitude, snapshot.zones)
            ColoredRadiationHex(
                latitude = cell.latitude,
                longitude = cell.longitude,
                color = radiationGradient(level).copy(alpha = if (level >= 1.0) 0.78f else 0.60f),
            )
        }
    }
    val gestures = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, gestureZoom, _ ->
                val currentZoom = zoomState.floatValue
                val currentTileZoom = floor(currentZoom).toInt().coerceIn(6, 11)
                val currentScale = 2.0.pow((currentZoom - currentTileZoom).toDouble())
                val panX = pan.x / density.density / currentScale
                val panY = pan.y / density.density / currentScale
                centerLongitude.doubleValue = longitudeAt(
                    worldX(centerLongitude.doubleValue, currentTileZoom) - panX,
                    currentTileZoom,
                ).coerceIn(42.8, 47.2)
                centerLatitude.doubleValue = latitudeAt(
                    worldY(centerLatitude.doubleValue, currentTileZoom) - panY,
                    currentTileZoom,
                ).coerceIn(38.1, 41.9)
                zoomState.floatValue = (
                    currentZoom + (ln(gestureZoom.toDouble()) / ln(2.0)).toFloat()
                ).coerceIn(6.5f, 11f)
            }
        }
    } else Modifier
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(MAP_LOADING_COLOR)
            .then(gestures),
    ) {
        val width = maxWidth.value
        val height = maxHeight.value
        val zoom = zoomState.floatValue
        val tileZoom = floor(zoom).toInt().coerceIn(6, 11)
        val renderScale = 2.0.pow((zoom - tileZoom).toDouble()).toFloat()
        val centerX = worldX(centerLongitude.doubleValue, tileZoom)
        val centerY = worldY(centerLatitude.doubleValue, tileZoom)
        val left = centerX - width / (2.0 * renderScale)
        val top = centerY - height / (2.0 * renderScale)
        val firstX = floor(left / TILE_SIZE).toInt()
        val lastX = floor((left + width / renderScale) / TILE_SIZE).toInt()
        val firstY = floor(top / TILE_SIZE).toInt()
        val lastY = floor((top + height / renderScale) / TILE_SIZE).toInt()

        for (tileY in firstY..lastY) {
            for (tileX in firstX..lastX) {
                OsmTile(
                    zoom = tileZoom,
                    x = tileX,
                    y = tileY,
                    modifier = Modifier
                        .offset(
                            x = ((tileX * TILE_SIZE - left) * renderScale).toFloat().dp,
                            y = ((tileY * TILE_SIZE - top) * renderScale).toFloat().dp,
                        )
                        .size((TILE_SIZE * renderScale).dp),
                )
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            val borderColor = Color.White.copy(alpha = 0.28f)
            coloredHexes.forEach { cell ->
                val centerX = with(density) {
                    ((worldX(cell.longitude, tileZoom) - left) * renderScale).toFloat().dp.toPx()
                }
                val centerY = with(density) {
                    ((worldY(cell.latitude, tileZoom) - top) * renderScale).toFloat().dp.toPx()
                }
                if (centerX < -80f || centerX > size.width + 80f || centerY < -80f || centerY > size.height + 80f) {
                    return@forEach
                }
                val radiusX = with(density) {
                    ((worldX(cell.longitude + HEX_LONGITUDE_RADIUS, tileZoom) - worldX(cell.longitude, tileZoom)) * renderScale)
                        .toFloat().dp.toPx()
                }
                val radiusY = with(density) {
                    ((worldY(cell.latitude - HEX_LATITUDE_RADIUS, tileZoom) - worldY(cell.latitude, tileZoom)) * renderScale)
                        .toFloat().dp.toPx()
                }
                val radius = minOf(radiusX, radiusY) * 0.91f
                val hexagon = Path().apply {
                    repeat(6) { index ->
                        val angle = Math.toRadians(60.0 * index - 30.0)
                        val x = centerX + (cos(angle) * radius).toFloat()
                        val y = centerY + (sin(angle) * radius).toFloat()
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(hexagon, cell.color)
                drawPath(hexagon, borderColor, style = Stroke(width = 0.55f))
            }

            val countryBorder = Path().apply {
                ARMENIA_OUTLINE.forEachIndexed { index, point ->
                    val x = with(density) { ((worldX(point.second, tileZoom) - left) * renderScale).toFloat().dp.toPx() }
                    val y = with(density) { ((worldY(point.first, tileZoom) - top) * renderScale).toFloat().dp.toPx() }
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(countryBorder, Color.White.copy(alpha = 0.92f), style = Stroke(width = with(density) { 1.7.dp.toPx() }))
            drawPath(countryBorder, Color(0xFF253247).copy(alpha = 0.7f), style = Stroke(width = with(density) { 0.7.dp.toPx() }))
        }

        snapshot.zones.forEach { zone ->
            val x = ((worldX(zone.centerLon, tileZoom) - left) * renderScale).toFloat()
            val y = ((worldY(zone.centerLat, tileZoom) - top) * renderScale).toFloat()
            val zoneModifier = Modifier
                .offset((x - 16).dp, (y - 16).dp)
                .size(32.dp)
            Box(
                modifier = if (interactive) zoneModifier.clickable { onZone(zone) } else zoneModifier,
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(9.dp).background(Color.White.copy(alpha = 0.9f), CircleShape).padding(2.dp).background(zone.status.color, CircleShape))
            }
        }

        snapshot.stations.forEach { station ->
            val x = ((worldX(station.base.longitude, tileZoom) - left) * renderScale).toFloat()
            val y = ((worldY(station.base.latitude, tileZoom) - top) * renderScale).toFloat()
            Box(
                Modifier
                    .offset((x - 2.5f).dp, (y - 2.5f).dp)
                    .size(5.dp)
                    .background(station.status.color, CircleShape),
            )
        }

        if (interactive) {
            Column(Modifier.align(Alignment.CenterEnd).padding(10.dp)) {
                MapZoomButton("+") { zoomState.floatValue = (zoomState.floatValue + 1f).coerceAtMost(11f) }
                Spacer(Modifier.size(6.dp))
                MapZoomButton("−") { zoomState.floatValue = (zoomState.floatValue - 1f).coerceAtLeast(6.5f) }
            }
        }

        Text(
            "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), RoundedCornerShape(5.dp))
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright")))
                }
                .padding(horizontal = 6.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun MapZoomButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun OsmTile(zoom: Int, x: Int, y: Int, modifier: Modifier) {
    val context = LocalContext.current
    val tileCount = 1 shl zoom
    if (y !in 0 until tileCount) return
    val wrappedX = ((x % tileCount) + tileCount) % tileCount
    val bitmap by produceState<ImageBitmap?>(null, zoom, wrappedX, y) {
        value = loadTile(context.filesDir, zoom, wrappedX, y)
    }
    val image = bitmap
    if (image == null) {
        Box(modifier.background(MAP_LOADING_COLOR))
    } else {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier,
        )
    }
}

private suspend fun loadTile(cacheRoot: File, zoom: Int, x: Int, y: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    val tileFile = File(cacheRoot, "osm-tiles/$zoom/$x/$y.png")
    val cached = tileFile.takeIf { it.isFile && System.currentTimeMillis() - it.lastModified() < CACHE_MAX_AGE_MS }
    if (cached != null) {
        return@withContext BitmapFactory.decodeFile(cached.absolutePath)?.asImageBitmap()
    }

    try {
        tileFile.parentFile?.mkdirs()
        val connection = URL("https://tile.openstreetmap.org/$zoom/$x/$y.png").openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 12_000
        connection.setRequestProperty("User-Agent", OSM_USER_AGENT)
        connection.setRequestProperty("Accept", "image/png")
        connection.instanceFollowRedirects = true
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return@withContext cached?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
        val temporary = File(tileFile.parentFile, "${tileFile.name}.download")
        connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
        connection.disconnect()
        if (!temporary.renameTo(tileFile)) {
            temporary.copyTo(tileFile, overwrite = true)
            temporary.delete()
        }
        BitmapFactory.decodeFile(tileFile.absolutePath)?.asImageBitmap()
    } catch (_: Exception) {
        if (tileFile.isFile) BitmapFactory.decodeFile(tileFile.absolutePath)?.asImageBitmap() else null
    }
}
