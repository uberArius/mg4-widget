package pl.art.mg4widget

import android.content.Context
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset

enum class Region(val label: String, val code: String) {
    EU("Europa / UK", "eu"), AU("Australia / Nowa Zelandia", "au");
    val endpoint get() = "https://gateway-mg-$code.soimt.com/api.app/v1"
}

// Compatibility hooks for older scheduled workers; releases no longer expire.
object Trial {
    const val MESSAGE = ""
    val expires = Long.MAX_VALUE
    fun expired(now: Long, highestSeen: Long = 0) = false
    fun isExpired(context: Context) = false
    fun observe(context: Context) {}
    fun requireActive(context: Context) {}
}

object VehicleInfo {
    fun title(car: JSONObject?): String {
        if (car == null) return "MG4"
        val name = car.optString("modelName").trim().takeUnless { it == "null" }.orEmpty()
        if (name.isNotBlank()) return name
        return if (car.optString("series").startsWith("EH32", true)) "MG4" else "MG"
    }
    fun config(car: JSONObject?, code: String): String? {
        val items = car?.optJSONArray("vehicleModelConfiguration") ?: return null
        return (0 until items.length()).mapNotNull { items.optJSONObject(it) }
            .firstOrNull { it.optString("itemCode") == code }?.optString("itemValue")
    }
    fun seatMax(car: JSONObject?) = when (config(car, "HeatedSeat")) { "1" -> 3; "2" -> 1; else -> null }
}

data class HeatProfile(val cabin: Boolean = true, val temperature: Int = 22, val defrost: Boolean = false,
    val driver: Int? = null, val passenger: Int? = null, val rightHandDrive: Boolean = false) {
    fun seats(currentLeft: Int?, currentRight: Int?): Pair<Int, Int>? {
        if (driver == null && passenger == null) return null
        val left = (if (rightHandDrive) passenger else driver) ?: currentLeft
        val right = (if (rightHandDrive) driver else passenger) ?: currentRight
        require(left != null && right != null) { "Brak świeżego stanu drugiego fotela. Zaznacz oba fotele albo odśwież dane." }
        return left to right
    }
}

fun Field.isAlert(value: Double?) = when (this) {
    Field.LOCKED -> value == 0.0
    Field.CLIMATE -> value == 2.0 || value == 5.0
    Field.CHARGING -> value == 1.0
    else -> false
}

/** Evaluate all cached readings, independently of visible tiles. Unknown drive state is not OFF. */
fun Snapshot.parkedWarningFields(frontBar: Double = 2.4, rearBar: Double = 2.4): Set<Field> {
    if (fields[Field.ENGINE]?.value != 0.0) return emptySet()
    val tyres = listOf(Field.TYRE_FL, Field.TYRE_FR, Field.TYRE_RL, Field.TYRE_RR)
    val openings = listOf(Field.DOOR_DRIVER, Field.DOOR_PASSENGER, Field.DOOR_RL, Field.DOOR_RR,
        Field.WINDOW_DRIVER, Field.WINDOW_PASSENGER, Field.WINDOW_RL, Field.WINDOW_RR,
        Field.SUNROOF, Field.BONNET, Field.BOOT)
    return (tyres.filter { fields[it]?.value?.let { pressure -> pressure.isFinite() && pressure < if (it == Field.TYRE_FL || it == Field.TYRE_FR) frontBar else rearBar } == true } +
        listOf(Field.LOCKED).filter { fields[it]?.value == 0.0 } +
        openings.filter { fields[it]?.value == 1.0 }).toSet()
}
fun Snapshot.parkedWarning(frontBar: Double = 2.4, rearBar: Double = 2.4): Boolean = parkedWarningFields(frontBar, rearBar).isNotEmpty()
fun Snapshot.tileCausesWarning(field: Field, frontBar: Double = 2.4, rearBar: Double = 2.4): Boolean = Tiles.members(field).any { it in parkedWarningFields(frontBar, rearBar) }
