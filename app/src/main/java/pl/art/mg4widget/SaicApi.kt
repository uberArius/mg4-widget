package pl.art.mg4widget

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit

class ApiFailure(val code: Int, message: String) : Exception(message)
class SaicApi(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).callTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).followRedirects(false).build(),
    private val base: String = "https://gateway-mg-eu.soimt.com/api.app/v1",
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
    private val region: String = "eu",
    private val guard: () -> Unit = {},
    private val progress: () -> Unit = {},
    private val nanoTime: () -> Long = { System.nanoTime() }
) {
    var token: String = ""
    var operationDeadline: Long = Long.MAX_VALUE
    fun login(username: String, password: String, country: String): JSONObject {
        token = ""
        val values = linkedMapOf("grant_type" to "password", "username" to username,
            "password" to SaicCrypto.hash("SHA-1", password), "scope" to "all",
            "deviceId" to "simulator*********************************************${System.currentTimeMillis() / 1000}###com.saicmotor.europecar",
            "deviceType" to "0", "language" to "EN", "loginType" to if (username.contains('@')) "2" else "1")
        if (!username.contains('@')) values["countryCode"] = country
        val result = request("/oauth/token", "POST", values.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" },
            "application/x-www-form-urlencoded", login = true)
        token = result.optString("access_token")
        if (token.isBlank() || result.optLong("expires_in") <= 0) throw ApiFailure(401, "Nie udało się zalogować do iSMART.")
        return result
    }
    fun vehicles() = request("/vehicle/list").optJSONArray("vinList") ?: JSONArray()
    fun status(vin: String, cached: Boolean): JSONObject = request(
        "/vehicle/status${if (cached) "/cache" else ""}?vin=${SaicCrypto.hash("SHA-256", vin)}&vehStatusReqType=2",
        event = !cached)
    fun charging(vin: String) = request("/vehicle/charging/mgmtData?vin=${SaicCrypto.hash("SHA-256", vin)}", event = true)
    fun climate(vin: String, temperature: Int, start: Boolean): JSONObject =
        request("/vehicle/control", "POST", climateBody(vin, temperature, start).toString(), event = true)

    fun control(body: JSONObject) = request("/vehicle/control", "POST", body.toString(), event = true)

    internal fun request(path: String, method: String = "GET", body: String = "", contentType: String = "application/json",
                         event: Boolean = false, login: Boolean = false): JSONObject {
        var eventId = if (event) "0" else null
        val deadline = minOf(operationDeadline, nanoTime() + TimeUnit.SECONDS.toNanos(180))
        repeat(60) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            guard()
            val remaining = deadline - nanoTime()
            if (remaining <= 0) throw ApiFailure(-2, "Brak potwierdzenia w ciągu 180 sekund.")
            val ts = System.currentTimeMillis().toString()
            val encrypted = SaicCrypto.encrypt(path, ts, contentType, body, token)
            val builder = Request.Builder().url(base + path)
                .header("User-Agent", "Europe/2.1.0 (iPad; iOS 18.5; Scale/2.00)")
                .header("Content-Type", "$contentType;charset=utf-8").header("Accept", "application/json")
                .header("REGION", region).header("APP-SEND-DATE", ts).header("APP-CONTENT-ENCRYPTED", "1")
                .header("tenant-id", "459771").header("User-Type", "app").header("APP-LANGUAGE-TYPE", "en")
                .header("APP-VERIFICATION-STRING", encrypted.signature).header("ORIGINAL-CONTENT-TYPE", contentType)
            if (token.isNotBlank()) builder.header("blade-auth", token)
            if (login) builder.header("Authorization", "Basic c3dvcmQ6c3dvcmRfc2VjcmV0")
            eventId?.let { builder.header("event-id", it) }
            builder.method(method, if (method == "GET") null else encrypted.body.toRequestBody(contentType.toMediaType()))
            var retry = false
            client.newCall(builder.build()).apply { timeout().timeout(minOf(remaining, TimeUnit.SECONDS.toNanos(90)), TimeUnit.NANOSECONDS) }.execute().use { response ->
                if (!response.isSuccessful) throw ApiFailure(response.code, "iSMART: błąd HTTP ${response.code}.")
                var text = response.body?.string() ?: throw ApiFailure(-1, "Pusta odpowiedź iSMART.")
                val date = response.header("APP-SEND-DATE")
                val type = response.header("ORIGINAL-CONTENT-TYPE")
                if (date != null && type != null) text = SaicCrypto.decrypt(text, date, type)
                val json = JSONObject(text)
                val code = json.optInt("code", -1)
                if (code in listOf(401, 403, 2, 3, 7)) throw ApiFailure(code, "iSMART: kod $code. Sprawdź sesję lub uprawnienia konta.")
                val nextEvent = response.header("event-id")
                // Reuse the same event-id: never start a second control command when polling its result.
                if (event && ((!json.has("data") && nextEvent != null) || (code != 0 && eventId != "0"))) {
                    val candidate = nextEvent ?: eventId
                    if (candidate.isNullOrBlank() || candidate == "0") throw ApiFailure(-2, "Brak identyfikatora operacji. Nie ponowiono komendy.")
                    eventId = candidate
                    retry = true
                    progress()
                } else {
                    if (code != 0) {
                        val detail = listOf("message", "msg", "error_description").firstNotNullOfOrNull { key ->
                            (json.opt(key) as? String)?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                        }
                        throw ApiFailure(code, "iSMART: kod błędu $code." + (detail?.let { " " + it.take(500) } ?: " Serwer nie podał opisu."))
                    }
                    return json.optJSONObject("data") ?: throw ApiFailure(-1, "Brak danych w odpowiedzi iSMART.")
                }
            }
            if (retry && nanoTime() < deadline) sleep(minOf(3000, TimeUnit.NANOSECONDS.toMillis(deadline - nanoTime()).coerceAtLeast(0))) else throw ApiFailure(-2, "iSMART nie potwierdził wyniku w wyznaczonym czasie.")
        }
        throw ApiFailure(-2, "Przekroczono czas oczekiwania na samochód.")
    }
    companion object {
        private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
        fun seatsBody(vin: String, left: Int, right: Int): JSONObject {
            require(left in 0..3 && right in 0..3)
            return controlBody(vin, "5", listOf(17 to left, 18 to right))
        }
        fun defrostBody(vin: String) = controlBody(vin, "6", listOf(19 to 5, 20 to 8, 22 to 1))
        private fun controlBody(vin: String, type: String, values: List<Pair<Int, Int>>): JSONObject {
            val params = JSONArray()
            (values.map { it.first to byteArrayOf(it.second.toByte()) } + (255 to byteArrayOf(0,0,0,0))).forEach { (id, bytes) ->
                params.put(JSONObject().put("paramId", id).put("paramValue", Base64.getEncoder().encodeToString(bytes)))
            }
            return JSONObject().put("vin", SaicCrypto.hash("SHA-256", vin)).put("rvcReqType", type).put("rvcParams", params)
        }
        fun climateBody(vin: String, temperature: Int, start: Boolean): JSONObject {
            require(temperature in 17..31)
            val params = JSONArray()
            fun param(id: Int, bytes: ByteArray) { params.put(JSONObject().put("paramId", id)
                .put("paramValue", Base64.getEncoder().encodeToString(bytes))) }
            param(19, byteArrayOf(if (start) 2 else 0))
            // MG4 / EH32: index 3 = 17°C. Start leaves AC_ON_OFF unspecified, as upstream does.
            if (start) param(20, byteArrayOf((temperature - 14).toByte())) else param(22, byteArrayOf(0))
            param(255, byteArrayOf(0, 0, 0, 0))
            return JSONObject().put("vin", SaicCrypto.hash("SHA-256", vin)).put("rvcReqType", "6").put("rvcParams", params)
        }
    }
}
