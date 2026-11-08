package pl.art.mg4widget

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VehicleParserTest {
    private val now = 1700000000000L
    private fun status(time: Long, fields: String) = JSONObject("""{"statusTime":$time,"basicVehicleStatus":$fields}""")
    @Test fun `converts units without treating unknown sentinels as measurements`() {
        val result = VehicleParser.parse(status(now / 1000, """{"extendedData1":0,"fuelRangeElec":2780,"interiorTemperature":87,"batteryVoltage":127,"mileage":184320,"frontLeftTyrePressure":65}"""), null, Snapshot(), now)
        assertEquals(0.0, result.fields[Field.SOC]!!.value, 0.0)
        assertEquals(278.0, result.fields[Field.RANGE]!!.value, 0.0)
        assertEquals(12.7, result.fields[Field.AUX]!!.value, 0.001)
        assertEquals(18432.0, result.fields[Field.MILEAGE]!!.value, 0.0)
        assertEquals(2.6, result.fields[Field.TYRE_FL]!!.value, 0.001)
        assertNull(result.fields[Field.INSIDE])
        assertNull(result.fields[Field.LOCKED])
    }
    @Test fun `partial cache cannot advance timestamps of missing fields or overwrite newer readings`() {
        val old = Snapshot(mapOf(Field.SOC to Reading(76.0, now), Field.INSIDE to Reading(19.0, now - 5000)))
        val result = VehicleParser.parse(status(now / 1000 - 60, """{"extendedData1":74}"""), null, old, now + 60_000)
        assertEquals(old.fields, result.fields)
        assertEquals(now + 60_000, result.fetchedAt)
    }
    @Test fun `charging missing time is not labelled freshly measured`() {
        val charge = JSONObject("""{"chrgMgmtData":{"bmsChrgSts":1,"bmsOnBdChrgTrgtSOCDspCmd":5,"chrgngRmnngTime":45,"chrgngRmnngTimeV":0}}""")
        val result = VehicleParser.parse(status(now / 1000, "{}"), charge, Snapshot(), now)
        assertEquals(1.0, result.fields[Field.CHARGING]!!.value, 0.0)
        assertEquals(80.0, result.fields[Field.TARGET]!!.value, 0.0)
        assertEquals(0L, result.fields[Field.CHARGING]!!.measuredAt)
    }
    @Test(expected = IllegalArgumentException::class) fun `invalid measurement date rejected`() {
        VehicleParser.parse(status(2147483647, "{}"), null, Snapshot(), now)
    }
    @Test fun `snapshot survives persistence including unknown timestamp`() {
        val snapshot = Snapshot(mapOf(Field.SOC to Reading(76.0, now), Field.CHARGING to Reading(0.0, 0)), now)
        assertEquals(snapshot, Snapshot.parse(snapshot.json()))
    }
    @Test fun `trip refresh cancelled on reconnect replaced event or delayed execution`() {
        assertTrue(TripPolicy.shouldRefresh(now, now, false, now + 60000))
        assertFalse(TripPolicy.shouldRefresh(now, now, true, now + 60000))
        assertFalse(TripPolicy.shouldRefresh(now, now + 5000, false, now + 60000))
        assertFalse(TripPolicy.shouldRefresh(now, now, false, now + 900001))
        assertFalse(TripPolicy.shouldRefresh(0, 0, false, now))
    }
}
