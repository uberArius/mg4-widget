package pl.art.mg4widget

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeUnit

class PoliciesTest {
    @Test fun `release does not expire even with a stored future clock`() {
        assertFalse(Trial.expired(Long.MAX_VALUE, Long.MAX_VALUE))
    }
    @Test fun `MG4 family code does not imply XPower`() {
        assertEquals("MG4",VehicleInfo.title(JSONObject().put("series","EH32")))
        assertEquals("MG4 XPOWER",VehicleInfo.title(JSONObject().put("modelName","MG4 XPOWER")))
        assertEquals("MG",VehicleInfo.title(JSONObject().put("series","unknown")))
    }
    @Test fun `seat configuration and steering side are explicit`() {
        fun car(value: String) = JSONObject("""{"vehicleModelConfiguration":[{"itemCode":"HeatedSeat","itemValue":"$value"}]}""")
        assertEquals(3,VehicleInfo.seatMax(car("1")))
        assertEquals(1,VehicleInfo.seatMax(car("2")))
        assertNull(VehicleInfo.seatMax(car("0")))
        assertEquals(3 to 1,HeatProfile(driver=3,passenger=1).seats(null,null))
        assertEquals(1 to 3,HeatProfile(driver=3,passenger=1,rightHandDrive=true).seats(null,null))
        assertEquals(2 to 3,HeatProfile(driver=2).seats(null,3))
        assertEquals(0 to 2,HeatProfile(passenger=2).seats(0,null))
        assertNull(HeatProfile().seats(null,null))
    }
    @Test(expected=IllegalArgumentException::class) fun `excluded seat with no fresh state prevents command`() {
        HeatProfile(driver=1).seats(null,null)
    }
    @Test fun `alert states require actual known values`() {
        assertTrue(Field.LOCKED.isAlert(0.0)); assertFalse(Field.LOCKED.isAlert(1.0))
        assertTrue(Field.CLIMATE.isAlert(2.0)); assertTrue(Field.CLIMATE.isAlert(5.0))
        assertFalse(Field.CLIMATE.isAlert(1.0)); assertFalse(Field.CLIMATE.isAlert(null))
        assertTrue(Field.CHARGING.isAlert(1.0)); assertFalse(Field.CHARGING.isAlert(null))
    }
    @Test fun `control payloads match upstream seat and defrost parameters`() {
        fun params(body: JSONObject): Map<Int, List<Byte>> {
            val a=body.getJSONArray("rvcParams")
            return (0 until a.length()).associate { val p=a.getJSONObject(it); p.getInt("paramId") to Base64.getDecoder().decode(p.getString("paramValue")).toList() }
        }
        val seats=SaicApi.seatsBody("vin",3,0)
        assertEquals("5",seats.getString("rvcReqType"))
        assertEquals(listOf(3.toByte()),params(seats)[17]); assertEquals(listOf(0.toByte()),params(seats)[18])
        val defrost=params(SaicApi.defrostBody("vin"))
        assertEquals(listOf(5.toByte()),defrost[19]); assertEquals(listOf(8.toByte()),defrost[20]); assertEquals(listOf(1.toByte()),defrost[22])
    }
    @Test fun `expiry between polls stops HTTP before next control poll`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"code":0}""").addHeader("event-id","abc"))
            var expired=false
            val api=SaicApi(base=server.url("/api").toString(),sleep={expired=true},guard={check(!expired) { Trial.MESSAGE }})
            try { api.control(SaicApi.defrostBody("vin")); fail() } catch(e: IllegalStateException) { assertEquals(Trial.MESSAGE,e.message) }
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun `AU region sent explicitly and no fallback`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"code":0,"data":{}}"""))
            SaicApi(base=server.url("/api").toString(),region="au").request("/vehicle/list")
            assertEquals("au",server.takeRequest().getHeader("REGION"))
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun `slow event survives 120 seconds and times out at total budget without replay`() {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(MockResponse().setBody("""{"code":0}""").addHeader("event-id","slow")) }
            var clock=0L
            val api=SaicApi(base=server.url("/api").toString(),sleep={clock+=TimeUnit.SECONDS.toNanos(60)},nanoTime={clock})
            try { api.request("/vehicle/control","POST","{}",event=true); fail() } catch(e: ApiFailure) { assertEquals(-2,e.code) }
            assertEquals(3,server.requestCount)
            assertEquals("0",server.takeRequest().getHeader("event-id"))
            repeat(2) { assertEquals("slow",server.takeRequest().getHeader("event-id")) }
        }
    }
    @Test fun `pending response without usable event id must never restart command`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"code":0}""").addHeader("event-id","0"))
            try { SaicApi(base=server.url("/api").toString(),sleep={}).control(SaicApi.defrostBody("vin")); fail() } catch(e: ApiFailure) { assertEquals(-2,e.code) }
            assertEquals(1,server.requestCount)
        }
    }
}
