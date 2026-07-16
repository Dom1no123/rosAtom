package com.hackathon.radiationmonitor

import androidx.compose.ui.graphics.Color

enum class RadiationStatus(val label: String, val color: Color) {
    NORMAL("Норма", Color(0xFF30D158)),
    ELEVATED("Повышено", Color(0xFFFFD60A)),
    DANGEROUS("Опасно", Color(0xFFFF9F0A)),
    CRITICAL("Критично", Color(0xFFFF453A)),
}

fun statusFor(level: Double) = when {
    level < 0.30 -> RadiationStatus.NORMAL
    level < 0.60 -> RadiationStatus.ELEVATED
    level < 1.00 -> RadiationStatus.DANGEROUS
    else -> RadiationStatus.CRITICAL
}

data class StationBase(
    val id: String,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
)

data class Station(
    val base: StationBase,
    val level: Double,
    val updatedAt: Long,
) {
    val status get() = statusFor(level)
}

data class HistoryPoint(val timestamp: Long, val level: Double)

data class Zone(
    val id: String,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val baseline: Double,
    val level: Double,
    val updatedAt: Long,
) {
    val status get() = statusFor(level)
}

data class RadiationAlert(
    val id: String,
    val zoneId: String,
    val zoneName: String,
    val createdAt: Long,
    val expiresAt: Long,
    val acknowledged: Boolean = false,
)

data class DailyStat(val day: String, val average: Double, val min: Double, val max: Double)

data class Snapshot(
    val stations: List<Station>,
    val zones: List<Zone>,
    val histories: Map<String, List<HistoryPoint>>,
    val weekly: List<DailyStat>,
    val alerts: List<RadiationAlert>,
    val updatedAt: Long,
    val tick: Int = 0,
)

enum class AppTab(val title: String) {
    HOME("Главная"),
    MAP("Карта"),
    STATISTICS("Статистика"),
    NOTIFICATIONS("Уведомления"),
    INSTRUCTIONS("Инструкции"),
    SETTINGS("Настройки"),
}

val BASE_STATIONS = listOf(
    StationBase("st-001", "Ереван", "г. Ереван", 40.1792, 44.4991),
    StationBase("st-002", "Гюмри", "Ширакская область", 40.7942, 43.8481),
    StationBase("st-003", "Ванадзор", "Лорийская область", 40.8128, 44.4886),
    StationBase("st-004", "Вагаршапат", "Армавирская область", 40.1611, 44.2911),
    StationBase("st-005", "Раздан", "Котайкская область", 40.4972, 44.7681),
    StationBase("st-006", "Абовян", "Котайкская область", 40.2764, 44.6219),
    StationBase("st-007", "Капан", "Сюникская область", 39.2039, 46.4064),
    StationBase("st-008", "Армавир", "Армавирская область", 40.1511, 44.0428),
    StationBase("st-009", "Арташат", "Араратская область", 39.9506, 44.5486),
    StationBase("st-010", "Иджеван", "Тавушская область", 40.8781, 45.1489),
    StationBase("st-011", "Севан", "Гегаркуникская область", 40.5486, 44.9550),
    StationBase("st-012", "Горис", "Сюникская область", 39.5122, 46.3378),
    StationBase("st-013", "Аштарак", "Арагацотнская область", 40.2989, 44.3628),
    StationBase("st-014", "Чаренцаван", "Котайкская область", 40.5511, 44.6086),
    StationBase("st-015", "Масис", "Араратская область", 39.9633, 44.4353),
    StationBase("st-016", "Дилижан", "Тавушская область", 40.7419, 44.8636),
    StationBase("st-017", "Гавар", "Гегаркуникская область", 40.3597, 45.1275),
    StationBase("st-018", "Мартуни", "Гегаркуникская область", 40.1381, 45.3211),
    StationBase("st-019", "Вайк", "Вайоцдзорская область", 39.6900, 45.4425),
    StationBase("st-020", "Мегри", "Сюникская область", 38.9089, 46.2419),
)

val BASE_ZONES = listOf(
    Zone("zone-north", "Север", 40.86, 44.62, 0.16, 0.16, System.currentTimeMillis()),
    Zone("zone-south", "Юг", 39.28, 46.05, 0.14, 0.14, System.currentTimeMillis()),
    Zone("zone-west", "Запад", 40.20, 43.98, 0.18, 0.18, System.currentTimeMillis()),
    Zone("zone-east", "Восток", 40.28, 45.38, 0.20, 0.20, System.currentTimeMillis()),
    Zone("zone-center", "Центр", 40.22, 44.62, 0.24, 0.24, System.currentTimeMillis()),
)

const val NORMAL_INSTRUCTIONS = """Общие рекомендации по радиационной безопасности

1. Следите за уровнем радиационного фона в вашем регионе через приложение.
2. Уровень «Норма» и «Повышено» не требует особых действий — продолжайте следить за обновлениями.
3. При уровне «Опасно» ограничьте время пребывания на открытом воздухе, особенно для детей, пожилых людей и беременных.
4. Держите под рукой запас питьевой воды, аптечку и заряженный телефон.
5. Никогда не распространяйте непроверенную информацию об инцидентах — ориентируйтесь на официальные источники.
6. При получении экстренного уведомления в этом приложении — следуйте инструкции на экране без промедления."""

const val EMERGENCY_INSTRUCTIONS = """АЛГОРИТМ ДЕЙСТВИЙ В РЕЖИМЕ ЧРЕЗВЫЧАЙНОЙ СИТУАЦИИ (ЧП)

1. КОНТЕКСТ СИТУАЦИИ
Система зафиксировала устойчивое превышение радиационного фона. Влияние погодных условий и технических помех исключено.

Главное правило: сохраняйте спокойствие и действуйте последовательно.

2. МИНИМИЗАЦИЯ РАДИАЦИОННОЙ ДОЗЫ

1. Перейдите в ближайшее капитальное помещение.
2. Закройте окна, форточки, двери и вентиляционные отверстия.
3. Используйте ткань, скотч, плёнку или другие материалы для герметизации щелей.
4. Оставайтесь внутри помещения.

Стены и перекрытия являются основной защитой от гамма-излучения. Чем больше расстояние от внешних стен и окон, тем ниже потенциальная доза.

3. ИНФОРМАЦИОННАЯ БЕЗОПАСНОСТЬ

Не распространяйте непроверенную информацию. Используйте радио, телевидение и официальные каналы МЧС Республики Армения. Ожидайте официальных указаний по эвакуации, приёму йодосодержащих препаратов и дальнейшим действиям.

4. ПОРЯДОК ЭВАКУАЦИИ

Если официально объявлена эвакуация, подготовьте документы, воду, герметично упакованные продукты и лекарства. Отключите электричество, перекройте газ, закройте помещение и следуйте только официальным маршрутам.

5. РЕЖИМ РАБОТЫ СИСТЕМЫ

Эта инструкция доступна автономно и работает без подключения к интернету.

КОНЕЦ ИНСТРУКЦИИ"""
