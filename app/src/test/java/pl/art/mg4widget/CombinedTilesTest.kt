package pl.art.mg4widget

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CombinedTilesTest {
    private fun parse(raw: String,previous: Snapshot=Snapshot()): Snapshot = VehicleParser.parse(
        JSONObject("""{"statusTime":1700000000,"basicVehicleStatus":{}}"""),JSONObject().put("chrgMgmtData",JSONObject(raw)),previous,1700000000000)
    @Test fun `schedule supports midnight overnight disabled and target SOC`() {
        val on=parse("""{"bmsReserCtrlDspCmd":1,"bmsReserStHourDspCmd":22,"bmsReserStMintueDspCmd":30,"bmsReserSpHourDspCmd":0,"bmsReserSpMintueDspCmd":0}""")
        assertEquals("Włączony\n22:30 → 00:00",on.scheduleText())
        assertEquals(0L,on.fields[Field.SCHEDULE_START]!!.measuredAt)
        val soc=parse("""{"bmsReserCtrlDspCmd":3,"bmsReserStHourDspCmd":22,"bmsReserStMintueDspCmd":0}""")
        assertEquals("Do celu SOC\n22:00 → cel SOC",soc.scheduleText())
        assertEquals("Wyłączony",parse("""{"bmsReserCtrlDspCmd":2}""").value(Field.SCHEDULE_MODE))
    }
    @Test fun `partial schedule cannot reuse previous times and invalid zero mode is unknown`() {
        val old=Snapshot(mapOf(Field.SCHEDULE_START to Reading(123.0,0),Field.SCHEDULE_MODE to Reading(1.0,0)))
        val data=parse("""{"bmsReserCtrlDspCmd":0,"bmsReserStHourDspCmd":255,"bmsReserStMintueDspCmd":60}""",old)
        assertNull(data.fields[Field.SCHEDULE_START]); assertNull(data.fields[Field.SCHEDULE_MODE])
        assertEquals("—\n— → —",data.scheduleText())
    }
    @Test fun `selection migration merges groups once preserving order and empty selection`() {
        assertEquals(listOf(Field.SOC,Field.TEMPERATURES,Field.TYRES),Tiles.normalize(listOf(Field.SOC,Field.INSIDE,Field.OUTSIDE,Field.TYRE_FL,Field.TYRE_RR)))
        assertEquals(emptyList<Field>(),Tiles.normalize(emptyList()))
        assertEquals(4,Tiles.members(Field.DOORS).size)
        assertFalse(Tiles.members(Field.WINDOWS).contains(Field.SUNROOF))
    }
    @Test fun `physical door and window positions follow steering side`() {
        assertEquals(Field.DOOR_DRIVER,Tiles.members(Field.DOORS,false)[0])
        assertEquals(Field.DOOR_PASSENGER,Tiles.members(Field.DOORS,true)[0])
        assertEquals(Field.WINDOW_PASSENGER,Tiles.members(Field.WINDOWS,true)[0])
        assertEquals(listOf(Field.SEAT_LEFT,Field.SEAT_RIGHT),Tiles.members(Field.SEATS,true))
    }
    @Test fun `group timestamp is conservative and missing cell stays unknown`() {
        val data=Snapshot(mapOf(Field.TYRE_FL to Reading(2.6,1700000000000),Field.TYRE_FR to Reading(2.7,1700000010000)))
        assertTrue(Tiles.time(data,Field.TYRES).startsWith("Niepełne · Najstarszy"))
        assertEquals("—",Tiles.cell(data,Field.TYRE_RL))
        assertEquals("2,60",Tiles.cell(data,Field.TYRE_FL))
    }
    @Test fun `diagnostics distinguish zero false missing field and missing response`() {
        val catalog=JSONObject("""{"status":["basic.a","basic.b","basic.c"],"charging":["mode"]}""")
        val report=DiagnosticReport.build(catalog,mapOf("status" to JSONObject("""{"basic":{"a":0,"b":false}}""")),mapOf("charging" to "timeout"),Snapshot(),1,2,false)
        val groups=report.getJSONArray("groups")
        val all=(0 until groups.length()).map { groups.getJSONObject(it) }
        val status=all.first { it.getString("name")=="Stan pojazdu i GPS" }.getJSONArray("items")
        assertEquals("0",status.getJSONObject(0).getString("value"))
        assertEquals("false",status.getJSONObject(1).getString("value"))
        assertEquals("Nie otrzymano wartości w odpowiedzi",status.getJSONObject(2).getString("value"))
        val charge=all.first { it.getString("name")=="Bateria i zarządzanie ładowaniem" }.getJSONArray("items")
        assertTrue(charge.getJSONObject(0).getString("value").startsWith("Nie otrzymano odpowiedzi"))
    }
    @Test fun `charging merges old selections and displays two readings without status`() {
        assertEquals(listOf(Field.SOC, Field.CHARGING, Field.RANGE), Tiles.normalize(listOf(Field.SOC, Field.POWER, Field.RANGE, Field.REMAINING, Field.CHARGING)))
        assertEquals(listOf(Field.CHARGING), Tiles.normalize(listOf(Field.REMAINING)))
        assertFalse(Tiles.selectable.contains(Field.POWER))
        assertFalse(Tiles.selectable.contains(Field.REMAINING))
        val data = Snapshot(mapOf(Field.CHARGING to Reading(1.0, 1000), Field.POWER to Reading(7.2, 1000), Field.REMAINING to Reading(80.0, 1000)))
        assertEquals("MOC: 7,2 kW\nKONIEC: 80 min", Tiles.chargingText(data))
        assertTrue(Field.CHARGING.isAlert(data.fields[Field.CHARGING]?.value))
        assertFalse(Field.CHARGING.isAlert(0.0))
        assertEquals("Trwa ładowanie", Tiles.chargingTitle(data))
        assertEquals("Nie ładuje", Tiles.chargingTitle(Snapshot(mapOf(Field.CHARGING to Reading(0.0,1000)))))
        assertEquals("Stan nieznany", Tiles.chargingTitle(Snapshot()))
        assertEquals("Trwa", data.value(Field.CHARGING)) // Raw status remains available to diagnostics.
        assertEquals("MOC: —\nKONIEC: -", Tiles.chargingText(Snapshot()))
        assertTrue(Tiles.time(Snapshot(mapOf(Field.CHARGING to Reading(1.0,1000))),Field.CHARGING).startsWith("Niepełne"))
    }
    @Test fun `inactive charging hides cached remaining time and resumes with active state`() {
        val oldTime = Reading(92.0, 1000)
        val inactive = Snapshot(mapOf(Field.CHARGING to Reading(0.0, 2000), Field.REMAINING to oldTime))
        assertEquals("MOC: —\nKONIEC: -", Tiles.chargingText(inactive))
        assertEquals(oldTime, inactive.fields[Field.REMAINING])
        val active = inactive.copy(fields = inactive.fields + (Field.CHARGING to Reading(1.0, 3000)))
        assertEquals("MOC: —\nKONIEC: 92 min", Tiles.chargingText(active))
        assertEquals("MOC: —\nKONIEC: -", Tiles.chargingText(Snapshot(mapOf(Field.CHARGING to Reading(0.0,2000)))))
    }
    @Test fun `battery target combines selection and keeps unknown values visible`() {
        assertEquals(listOf(Field.RANGE, Field.SOC), Tiles.normalize(listOf(Field.RANGE, Field.TARGET, Field.SOC)))
        assertEquals(listOf(Field.SOC), Tiles.normalize(listOf(Field.TARGET)))
        assertFalse(Tiles.selectable.contains(Field.TARGET))
        val data = Snapshot(mapOf(Field.SOC to Reading(56.0, 1000), Field.TARGET to Reading(80.0, 0)))
        assertEquals("56 / 80 (%)", Tiles.batteryText(data))
        assertEquals("56 %", data.value(Field.SOC))
        assertEquals("— / — (%)", Tiles.batteryText(Snapshot()))
        assertEquals("56 / — (%)", Tiles.batteryText(Snapshot(mapOf(Field.SOC to Reading(56.0,1000)))))
    }
    @Test fun `current missing or invalid remaining time never inherits cached value`() {
        val old = Snapshot(mapOf(Field.CHARGING to Reading(1.0,0), Field.REMAINING to Reading(92.0,0)))
        val valid = parse("""{"bmsChrgSts":1,"chrgngRmnngTimeV":0,"chrgngRmnngTime":80}""",old)
        assertEquals(80.0, valid.fields[Field.REMAINING]!!.value,0.0)
        listOf(
            """{"bmsChrgSts":1}""",
            """{"bmsChrgSts":1,"chrgngRmnngTimeV":1,"chrgngRmnngTime":80}""",
            """{"bmsChrgSts":1,"chrgngRmnngTimeV":0}""",
            """{"bmsChrgSts":1,"chrgngRmnngTimeV":0,"chrgngRmnngTime":65535}"""
        ).forEach { raw ->
            val current = parse(raw,old)
            assertNull(current.fields[Field.REMAINING])
            assertEquals("MOC: —\nKONIEC: -", Tiles.chargingText(current))
        }
        val unavailable = VehicleParser.parse(JSONObject("""{"statusTime":1700000000,"basicVehicleStatus":{}}"""),null,old,1700000000000)
        assertNull(unavailable.fields[Field.REMAINING])
    }
}
