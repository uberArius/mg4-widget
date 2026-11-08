package pl.art.mg4widget

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Field(val label: String, val unit: String = "") {
    SOC("Bateria / Cel", "%"), RANGE("Zasięg", "km"), INSIDE("W kabinie", "°C"), OUTSIDE("Na zewnątrz", "°C"),
    LOCKED("Zamki"), CLIMATE("Klimatyzacja"), MILEAGE("Przebieg", "km"), AUX("Akumulator 12 V", "V"),
    CHARGING("Ładowanie"), REMAINING("Do końca", "min"), POWER("Moc baterii", "kW"), TARGET("Cel ładowania", "%"),
    TYRE_FL("Opona lewa przód", "bar"), TYRE_FR("Opona prawa przód", "bar"),
    TYRE_RL("Opona lewa tył", "bar"), TYRE_RR("Opona prawa tył", "bar"),
    DOOR_DRIVER("Drzwi kierowcy"), DOOR_PASSENGER("Drzwi pasażera"), DOOR_RL("Drzwi lewe tył"), DOOR_RR("Drzwi prawe tył"),
    BONNET("Maska"), BOOT("Bagażnik"), WINDOW_DRIVER("Szyba kierowcy"), WINDOW_PASSENGER("Szyba pasażera"),
    WINDOW_RL("Szyba lewa tył"), WINDOW_RR("Szyba prawa tył"), SUNROOF("Szyberdach"),
    LOW_BEAM("Światła mijania"), HIGH_BEAM("Światła drogowe"), SIDE_LIGHT("Światła pozycyjne"),
    REAR_HEAT("Ogrzewanie tylnej szyby"), SEAT_LEFT("Fotel lewy · grzanie"), SEAT_RIGHT("Fotel prawy · grzanie"),
    HANDBRAKE("Hamulec postojowy"), ENGINE("Napęd aktywny"), JOURNEY("Bieżąca podróż", "km"),
    FUEL("Paliwo", "%"), FUEL_RANGE("Zasięg paliwowy", "km"),
    TEMPERATURES("Temperatury"), TYRES("Opony · bar"), DOORS("Drzwi"), WINDOWS("Szyby"), SEATS("Fotele"),
    SCHEDULE("Plan ładowania"), SCHEDULE_MODE("Tryb harmonogramu"),
    SCHEDULE_START("Start harmonogramu"), SCHEDULE_END("Koniec harmonogramu");
    val opening get() = this in listOf(DOOR_DRIVER, DOOR_PASSENGER, DOOR_RL, DOOR_RR, BONNET, BOOT,
        WINDOW_DRIVER, WINDOW_PASSENGER, WINDOW_RL, WINDOW_RR, SUNROOF)
    val binary get() = this in listOf(LOW_BEAM, HIGH_BEAM, SIDE_LIGHT, REAR_HEAT, HANDBRAKE, ENGINE)
}
data class Reading(val value: Double, val measuredAt: Long) {
    fun json() = JSONObject().put("value", value).put("time", measuredAt)
}
data class Snapshot(val fields: Map<Field, Reading> = emptyMap(), val fetchedAt: Long = 0, val demo: Boolean = false) {
    fun json(): String = JSONObject().put("fetchedAt", fetchedAt).put("demo", demo).put("fields", JSONObject().apply {
        fields.forEach { (field, reading) -> put(field.name, reading.json()) }
    }).toString()
    fun value(field: Field, miles: Boolean = false): String {
        if (field == Field.SCHEDULE) return scheduleText()
        if (field in Tiles.groups && field != Field.CHARGING && field != Field.SOC) return Tiles.members(field).joinToString(" / ") { value(it,miles) }
        val v = fields[field]?.value ?: return "—"
        if (field.unit == "bar") return String.format(Language.locale, "%.2f bar", v)
        if (field.opening) return if (v == 1.0) "Otwarte" else "Zamknięte"
        if (field.binary) return if (v == 1.0) "Włączone" else "Wyłączone"
        if (field.unit == "km" && miles) return String.format(Locale.US, "%.1f mi", v / 1.609344)
        return when (field) {
            Field.LOCKED -> if (v == 1.0) "Zamknięte" else "Otwarte"
            Field.CLIMATE -> when (v.toInt()) { 0 -> "Wyłączona"; 1 -> "Nawiew"; 2 -> "Włączona"; 5 -> "Odszranianie"; else -> "Nieznany" }
            Field.SCHEDULE_MODE -> when(v.toInt()) { 1 -> "Włączony"; 2 -> "Wyłączony"; 3 -> "Do celu SOC"; else -> "Nieznany" }
            Field.SCHEDULE_START, Field.SCHEDULE_END -> "%02d:%02d".format(v.toInt()/60,v.toInt()%60)
            Field.CHARGING -> if (v == 1.0) "Trwa" else "Nie ładuje"
            else -> (if (v == v.toLong().toDouble()) v.toLong().toString() else String.format(Language.locale, "%.1f", v)) + " ${field.unit}"
        }
    }
    fun scheduleText(): String {
        val mode = fields[Field.SCHEDULE_MODE]?.value?.toInt()
        val end = if (mode == 3) "cel SOC" else value(Field.SCHEDULE_END)
        return "${value(Field.SCHEDULE_MODE)}\n${value(Field.SCHEDULE_START)} → $end"
    }
    companion object {
        fun parse(raw: String?): Snapshot {
            if (raw.isNullOrBlank()) return Snapshot()
            return runCatching {
                val j = JSONObject(raw); val values = j.getJSONObject("fields")
                Snapshot(Field.entries.mapNotNull { f -> values.optJSONObject(f.name)?.let {
                    f to Reading(it.getDouble("value"), it.getLong("time")) } }.toMap(), j.optLong("fetchedAt"), j.optBoolean("demo"))
            }.getOrDefault(Snapshot())
        }
        fun demo(): Snapshot {
            val now = System.currentTimeMillis()
            return Snapshot(mapOf(Field.SOC to 76.0, Field.RANGE to 278.0, Field.INSIDE to 19.0, Field.OUTSIDE to 8.0,
                Field.LOCKED to 1.0, Field.CLIMATE to 0.0, Field.MILEAGE to 18432.0, Field.AUX to 12.7,
                Field.CHARGING to 0.0, Field.TARGET to 80.0,
                Field.TYRE_FL to 2.6, Field.TYRE_FR to 2.6, Field.TYRE_RL to 2.7, Field.TYRE_RR to 2.7,
                Field.DOOR_DRIVER to 0.0, Field.DOOR_PASSENGER to 0.0, Field.DOOR_RL to 0.0, Field.DOOR_RR to 0.0,
                Field.WINDOW_DRIVER to 0.0, Field.WINDOW_PASSENGER to 0.0, Field.WINDOW_RL to 0.0, Field.WINDOW_RR to 0.0,
                Field.SEAT_LEFT to 1.0, Field.SEAT_RIGHT to 0.0, Field.SCHEDULE_MODE to 1.0,
                Field.SCHEDULE_START to 22.0*60, Field.SCHEDULE_END to 6.0*60).mapValues { Reading(it.value, now) }, now, true)
        }
    }
}
object VehicleParser {
    fun parse(status: JSONObject, charging: JSONObject?, previous: Snapshot, now: Long): Snapshot {
        val seconds = status.optLong("statusTime")
        require(seconds > 0 && seconds != 2147483647L && seconds <= (now + 300_000) / 1000) { "Nieprawidłowy czas danych samochodu." }
        val time = seconds * 1000
        val basic = status.optJSONObject("basicVehicleStatus") ?: throw IllegalArgumentException("Brak stanu samochodu.")
        val result = previous.fields.toMutableMap()
        fun add(field: Field, value: Double?, stamp: Long = time) {
            if (value != null && value.isFinite() && stamp >= (result[field]?.measuredAt ?: 0)) result[field] = Reading(value, stamp)
        }
        fun num(key: String, min: Double, max: Double): Double? = basic.number(key)?.takeIf { it >= min && it < max }
        add(Field.SOC, num("extendedData1", 0.0, 101.0))
        add(Field.RANGE, num("fuelRangeElec", 1.0, 20460.0)?.div(10))
        add(Field.INSIDE, num("interiorTemperature", -127.0, 127.0)?.takeUnless { it == 87.0 })
        add(Field.OUTSIDE, num("exteriorTemperature", -127.0, 127.0)?.takeUnless { it == 87.0 })
        add(Field.MILEAGE, num("mileage", 1.0, 2147483647.0)?.div(10))
        add(Field.AUX, num("batteryVoltage", 1.0, 65535.0)?.div(10))
        add(Field.LOCKED, num("lockStatus", 0.0, 2.0))
        add(Field.CLIMATE, num("remoteClimateStatus", 0.0, 6.0)?.takeIf { it.toInt() in listOf(0, 1, 2, 5) })
        listOf(Field.TYRE_FL to "frontLeftTyrePressure", Field.TYRE_FR to "frontRightTyrePressure",
            Field.TYRE_RL to "rearLeftTyrePressure", Field.TYRE_RR to "rearRightTyrePressure").forEach { (f, k) -> add(f, num(k, 1.0, 255.0)?.times(0.04)) }
        val binaryFields = mapOf(Field.DOOR_DRIVER to "driverDoor", Field.DOOR_PASSENGER to "passengerDoor",
            Field.DOOR_RL to "rearLeftDoor", Field.DOOR_RR to "rearRightDoor", Field.BONNET to "bonnetStatus", Field.BOOT to "bootStatus",
            Field.WINDOW_DRIVER to "driverWindow", Field.WINDOW_PASSENGER to "passengerWindow", Field.WINDOW_RL to "rearLeftWindow",
            Field.WINDOW_RR to "rearRightWindow", Field.SUNROOF to "sunroofStatus", Field.LOW_BEAM to "dippedBeamStatus",
            Field.HIGH_BEAM to "mainBeamStatus", Field.SIDE_LIGHT to "sideLightStatus", Field.REAR_HEAT to "rmtHtdRrWndSt",
            Field.HANDBRAKE to "handBrake", Field.ENGINE to "engineStatus")
        binaryFields.forEach { (f, key) -> add(f, num(key, 0.0, 2.0)) }
        add(Field.SEAT_LEFT, num("frontLeftSeatHeatLevel", 0.0, 4.0))
        add(Field.SEAT_RIGHT, num("frontRightSeatHeatLevel", 0.0, 4.0))
        add(Field.JOURNEY, num("currentJourneyDistance", 0.0, 2147483647.0)?.div(10))
        add(Field.FUEL, num("fuelLevelPrc", 0.0, 101.0))
        add(Field.FUEL_RANGE, num("fuelRange", 0.0, 65535.0)?.div(10))
        // mgmtData has no vehicle measurement timestamp. Keep its readings explicitly undated (0).
        // Remaining time is valid only for the current response; never reuse an old countdown.
        result.remove(Field.REMAINING)
        charging?.optJSONObject("chrgMgmtData")?.let { c ->
            fun charge(f: Field, v: Double?) { if (v != null && v.isFinite()) result[f] = Reading(v, 0) }
            // A partial schedule must not mix new mode with old start/end settings.
            listOf(Field.SCHEDULE_MODE,Field.SCHEDULE_START,Field.SCHEDULE_END).forEach { result.remove(it) }
            charge(Field.SCHEDULE_MODE,c.number("bmsReserCtrlDspCmd")?.takeIf { it in listOf(1.0,2.0,3.0) })
            fun minuteOfDay(h: String, m: String): Double? {
                val hour=c.number(h)?.takeIf { it in 0.0..23.0 && it % 1 == 0.0 } ?: return null
                val minute=c.number(m)?.takeIf { it in 0.0..59.0 && it % 1 == 0.0 } ?: return null
                return hour*60+minute
            }
            charge(Field.SCHEDULE_START,minuteOfDay("bmsReserStHourDspCmd","bmsReserStMintueDspCmd"))
            charge(Field.SCHEDULE_END,minuteOfDay("bmsReserSpHourDspCmd","bmsReserSpMintueDspCmd"))
            c.number("bmsChrgSts")?.takeIf { it in 0.0..13.0 }?.let { charge(Field.CHARGING, if (it.toInt() in listOf(1, 3, 10, 12)) 1.0 else 0.0) }
            if (c.number("chrgngRmnngTimeV") == 0.0) charge(Field.REMAINING, c.number("chrgngRmnngTime")?.takeIf { it >= 0 && it < 65535 })
            charge(Field.TARGET, c.number("bmsOnBdChrgTrgtSOCDspCmd")?.takeIf { it in 1.0..7.0 }?.let { 30 + it * 10 })
            val current = c.number("bmsPackCrnt")?.takeIf { it in 0.0..<65535.0 }
            val voltage = c.number("bmsPackVol")?.takeIf { it in 0.0..<65535.0 }
            if (c.number("bmsPackCrntV") == 0.0 && current != null && voltage != null) charge(Field.POWER, (current * 0.05 - 1000) * voltage * 0.25 / 1000)
        }
        return Snapshot(result, now)
    }
}
fun JSONObject.number(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
fun readingTime(time: Long): String = if (time <= 0) "czas pomiaru nieznany" else
    DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))
