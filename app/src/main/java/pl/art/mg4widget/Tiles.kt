package pl.art.mg4widget

object Tiles {
    val groups=listOf(Field.SOC,Field.CHARGING,Field.TEMPERATURES,Field.TYRES,Field.DOORS,Field.WINDOWS,Field.SEATS,Field.SCHEDULE)
    fun members(field: Field,rhd: Boolean=false): List<Field> = when(field) {
        Field.SOC -> listOf(Field.SOC, Field.TARGET)
        Field.CHARGING -> listOf(Field.CHARGING, Field.POWER, Field.REMAINING)
        Field.TEMPERATURES -> listOf(Field.INSIDE,Field.OUTSIDE)
        Field.TYRES -> listOf(Field.TYRE_FL,Field.TYRE_FR,Field.TYRE_RL,Field.TYRE_RR)
        Field.DOORS -> (if(rhd) listOf(Field.DOOR_PASSENGER,Field.DOOR_DRIVER) else listOf(Field.DOOR_DRIVER,Field.DOOR_PASSENGER)) + listOf(Field.DOOR_RL,Field.DOOR_RR)
        Field.WINDOWS -> (if(rhd) listOf(Field.WINDOW_PASSENGER,Field.WINDOW_DRIVER) else listOf(Field.WINDOW_DRIVER,Field.WINDOW_PASSENGER)) + listOf(Field.WINDOW_RL,Field.WINDOW_RR)
        Field.SEATS -> listOf(Field.SEAT_LEFT,Field.SEAT_RIGHT)
        Field.SCHEDULE -> listOf(Field.SCHEDULE_MODE,Field.SCHEDULE_START,Field.SCHEDULE_END)
        else -> listOf(field)
    }
    fun normalize(fields: List<Field>) = fields.map { f -> groups.firstOrNull { f in members(it) } ?: f }.distinct()
    fun toggled(selected: List<Field>, field: Field) = if (field in selected) selected - field else listOf(field) + selected
    fun moved(selected: List<Field>, field: Field, target: Field): List<Field> {
        if (field !in selected || field == target) return selected
        val destination = selected.indexOf(target).takeIf { it >= 0 } ?: selected.lastIndex
        return selected.toMutableList().apply { remove(field); add(destination.coerceAtMost(size), field) }
    }
    val selectable get() = normalize(Field.entries.toList())
    fun time(data: Snapshot, field: Field): String {
        val members=members(field).let { if(field==Field.SCHEDULE && data.fields[Field.SCHEDULE_MODE]?.value==3.0) it-Field.SCHEDULE_END else it }
        val values=members.map { data.fields[it] }
        if(values.all { it==null }) return "brak odczytu"
        val prefix=if(values.any { it==null }) "Niepełne · " else ""
        if(values.filterNotNull().any { it.measuredAt==0L }) return prefix+"czas nieznany"
        val oldest=values.filterNotNull().minOf { it.measuredAt }
        val mixed=values.filterNotNull().map { it.measuredAt }.distinct().size>1
        return prefix+(if(mixed) "Najstarszy · " else if(System.currentTimeMillis()-oldest>3_600_000) "Starsze · " else "")+readingTime(oldest)
    }
    fun batteryText(data: Snapshot): String {
        fun percent(field: Field) = data.value(field).removeSuffix(" %")
        return "${percent(Field.SOC)} / ${percent(Field.TARGET)} (%)"
    }
    fun chargingTitle(data: Snapshot) = when (data.fields[Field.CHARGING]?.value) {
        1.0 -> "Trwa ładowanie"
        0.0 -> "Nie ładuje"
        else -> "Stan nieznany"
    }
    fun chargingText(data: Snapshot): String {
        val remaining = if (data.fields[Field.CHARGING]?.value == 0.0 || data.fields[Field.REMAINING] == null) "-" else data.value(Field.REMAINING)
        return "MOC: ${data.value(Field.POWER)}\nKONIEC: $remaining"
    }
    fun cell(data: Snapshot, field: Field): String {
        val v=data.fields[field]?.value ?: return "—"
        return if(field.opening) { if(v==1.0) "Otwarte" else "Zamk." }
        else if(field.unit=="bar") java.lang.String.format(Language.locale,"%.2f",v)
        else data.value(field).trim()
    }
}
