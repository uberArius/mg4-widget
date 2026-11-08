package pl.art.mg4widget

import android.content.Context
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.withLock

class Repository(private val context: Context, private val cancelled: () -> Boolean = { false }) {
    private val store = Store(context)
    private var readStage: String? = null
    private var stageStarted = 0L
    private fun readingStage(label: String) {
        readStage = label
        stageStarted = System.nanoTime()
        store.message = "$label · oczekiwanie na odpowiedź (do 3 minut)…"
        Widgets.updateAll(context)
    }
    fun login(username: String, password: String, country: String): List<JSONObject> = lock.withLock {
        require(username.isNotBlank() && password.isNotEmpty()) { "Wpisz login i hasło." }
        val api = newApi()
        fun stage(label: String, operation: () -> JSONObject): JSONObject = try { operation() } catch (e: ApiFailure) {
            var detail = e.message.orEmpty()
            listOf(password, SaicCrypto.hash("SHA-1", password), username.trim(), api.token).filter { it.isNotEmpty() }
                .sortedByDescending { it.length }.forEach { detail = detail.replace(it, "[ukryte]") }
            throw ApiFailure(e.code, "$label: $detail")
        }
        val session = stage("Logowanie") { api.login(username.trim(), password, country.trim().removePrefix("+")) }
        val array = stage("Pobieranie samochodów (logowanie powiodło się)") { JSONObject().put("cars", api.vehicles()) }.getJSONArray("cars")
        val cars = (0 until array.length()).map { array.getJSONObject(it) }.filter { it.optString("vin").isNotBlank() }
        require(cars.isNotEmpty()) { "Na koncie nie znaleziono samochodu." }
        RefreshScheduler.cancel(context)
        store.clearAccount()
        store.saveAccount(JSONObject().put("username", username.trim()).put("password", password).put("country", country)
            .put("token", api.token).put("expires", System.currentTimeMillis() + session.getLong("expires_in") * 1000))
        store.message = "Wybierz samochód."
        cars
    }
    fun selectVehicle(car: JSONObject) = lock.withLock {
        store.vehicle = car
        store.snapshot = Snapshot()
        store.message = "Połączono z kontem. Oczekiwanie na pierwszy odczyt."
        RefreshScheduler.start(context)

        Widgets.updateAll(context)
    }
    fun demo() = lock.withLock {
        RefreshScheduler.cancel(context)
        store.clearAccount()
        store.demo = true
        store.snapshot = Snapshot.demo()
        store.message = "DEMO · przykładowe dane, bez połączenia z autem"
        Widgets.updateAll(context)
    }
    fun logout() = lock.withLock {
        RefreshScheduler.cancel(context)
        store.clearAccount()
        store.message = "Wylogowano. Dane konta i samochodu zostały usunięte."
        Widgets.updateAll(context)
    }
    private fun newApi() = SaicApi(base = store.region.endpoint, region = store.region.code,
        guard = { RiskConsent.requireAccepted(context); check(!cancelled()) { "Operację anulowano." }; Trial.requireActive(context) }, progress = {
            store.message = readStage?.let {
                val seconds = java.util.concurrent.TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - stageStarted)
                "$it · samochód przetwarza żądanie (${seconds}s / maks. 180s)…"
            } ?: "Samochód przetwarza żądanie…"
            Widgets.updateAll(context)
        })
    private fun api(): SaicApi {
        Trial.requireActive(context)
        check(!store.prefs.getBoolean("authBlocked", false)) { "Sesja iSMART wygasła lub została zastąpiona. Zaloguj się ponownie." }
        val account = store.account() ?: error("Zaloguj się do iSMART.")
        val api = newApi()
        if (account.optLong("expires") > System.currentTimeMillis() + 60_000) api.token = account.getString("token")
        else {
            val response = api.login(account.getString("username"), account.getString("password"), account.optString("country", "48"))
            account.put("token", api.token).put("expires", System.currentTimeMillis() + response.getLong("expires_in") * 1000)
            store.saveAccount(account)
        }
        return api
    }
    fun refresh(reason: String, disconnected: Long = 0): Boolean = lock.withLock {
        if (!store.isReady) return true
        if (reason == "hourly" && store.interval == 0) return true
        if (reason == "bluetooth" && !store.flag("btEnabled")) return true
        if (reason == "bluetooth" && !TripPolicy.shouldRefresh(disconnected,
                store.prefs.getLong("btDisconnected", 0), store.prefs.getBoolean("btConnected", false), System.currentTimeMillis())) return true
        if (store.demo) { store.snapshot = Snapshot.demo(); Widgets.updateAll(context); return true }
        try {
            store.message = "Pobieranie danych…"
            Widgets.updateAll(context)
            val api = api()
            val vin = store.vehicle!!.getString("vin")
            val cached = reason == "hourly" && store.flag("cacheOnly")
            readingStage(if (cached) "Stan zapisany na serwerze" else "Etap 1/2: stan samochodu")
            val status = api.status(vin, cached)
            // Publish the first response before waiting for the independent charging request.
            store.snapshot = VehicleParser.parse(status, null, store.snapshot, System.currentTimeMillis())
            Widgets.updateAll(context)
            var charge: JSONObject? = null
            var partial = false
            if (!cached) {
                readingStage("Etap 2/2: dane ładowania; stan auta już pobrany")
                try { charge = api.charging(vin) }
                catch (e: Exception) {
                    if (e is ApiFailure && e.code in listOf(401, 403, 2, 3, 7)) throw e
                    partial = true
                }
            }
            store.snapshot = VehicleParser.parse(status, charge, store.snapshot, System.currentTimeMillis())
            val freshAfterTrip = reason != "bluetooth" || status.optLong("statusTime") * 1000 >= disconnected / 1000 * 1000
            store.message = when {
                !freshAfterTrip -> "Auto zwróciło dane sprzed rozłączenia BT."
                partial -> "Stan pobrany; dane ładowania niedostępne."
                cached -> "Odczyt z serwera · bez budzenia auta"
                else -> "Odczyt samochodu zakończony"
            }
            freshAfterTrip
        } catch (e: Exception) {
            markFailure(e)
            if (e is ApiFailure && e.code == -2 && readStage != null)
                store.message = "${readStage}: nie otrzymano danych w limicie 3 minut. Zachowano wcześniejsze odczyty."
            false
        } finally { readStage = null; Widgets.updateAll(context) }
    }
    fun diagnostics(): org.json.JSONObject = lock.withLock {
        val catalog=JSONObject(context.assets.open("diagnostic-catalog.json").bufferedReader().use { it.readText() })
        val responses=mutableMapOf<String,JSONObject>()
        val errors=mutableMapOf<String,String>()
        val started=System.currentTimeMillis()
        if(store.demo) {
            errors.putAll(listOf("vehicle","status","charging","chargeStatus","batterySchedule").associateWith { "Tryb demonstracyjny: nie wysłano zapytania." })
        } else {
            val api=api()
            api.operationDeadline=System.nanoTime()+java.util.concurrent.TimeUnit.MINUTES.toNanos(8)
            val vin=store.vehicle?.getString("vin") ?: error("Wybierz samochód.")
            val hash=SaicCrypto.hash("SHA-256",vin)
            val reads=listOf<Pair<String,()->JSONObject>>(
                "vehicle" to { val a=api.vehicles(); (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.optString("vin")==vin } ?: error("Samochód nie występuje w odpowiedzi listy.") },
                "status" to { api.status(vin,false) },
                "charging" to { api.charging(vin) },
                "chargeStatus" to { api.request("/vehicle/charging/status?vin=$hash",event=true) },
                "batterySchedule" to { api.request("/charging/batteryHeating?vin=$hash") })
            var stopReason: String?=null
            reads.forEachIndexed { index,(key,read) ->
                if(stopReason!=null) errors[key]="Nie odczytano: $stopReason"
                else try {
                    check(!cancelled()) { "Operację anulowano." }
                    Trial.requireActive(context)
                    store.message="Sprawdzanie parametrów (${index+1}/${reads.size})…"
                    responses[key]=read()
                } catch(e: Exception) {
                    errors[key]=e.message ?: "Nie otrzymano odpowiedzi."
                    if(e is ApiFailure && e.code in listOf(401,403,2,3,7)) { markFailure(e); stopReason=store.message }
                    if(Trial.isExpired(context) || cancelled()) stopReason=if(Trial.isExpired(context)) Trial.MESSAGE else "anulowano"
                }
            }
        }
        // Diagnostic values come ONLY from this scan, never from saved snapshot fallbacks.
        val fresh=if(store.demo) Snapshot.demo() else (responses["status"] ?: if(responses.containsKey("charging")) JSONObject().put("statusTime",System.currentTimeMillis()/1000).put("basicVehicleStatus",JSONObject()) else null)?.let { runCatching { VehicleParser.parse(it,responses["charging"],Snapshot(),System.currentTimeMillis()) }.getOrNull() }
        val report=DiagnosticReport.build(catalog,responses,errors,fresh,started,System.currentTimeMillis(),store.demo)
        store.prefs.edit().putString("diagnostics",report.toString()).commit()
        store.message=if(Trial.isExpired(context)) Trial.MESSAGE else "Sprawdzanie parametrów zakończone. Wyniki w aplikacji."
        Widgets.updateAll(context)
        report
    }
    fun climate(start: Boolean, profile: HeatProfile) {
        check(climateInFlight.compareAndSet(false, true)) { "Poprzednie polecenie jest jeszcze przetwarzane." }
        try { lock.withLock {
            check(!store.demo) { "Sterowanie jest wyłączone w trybie DEMO." }
            val car = store.vehicle ?: error("Wybierz samochód.")
            check(car.optString("series").startsWith("EH32", true)) { "Sterowanie wymaga potwierdzonej serii MG4 (EH32)." }
            val api = api()
            api.operationDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(8)
            val vin = car.getString("vin")
            val expected = mutableMapOf<Field, Double>()
            val notes = mutableListOf<String>()
            val commandAt = System.currentTimeMillis()
            store.message = "Wysyłanie profilu ogrzewania…"
            store.prefs.edit().putBoolean("climatePending", true).commit()
            Widgets.updateAll(context)
            try {
                if (!start || profile.cabin || profile.defrost) {
                    expected[Field.CLIMATE] = if (!start) 0.0 else if (profile.defrost) 5.0 else 2.0
                    val body = if (start && profile.defrost) SaicApi.defrostBody(vin) else SaicApi.climateBody(vin, profile.temperature, start)
                    try {
                        val result = api.control(body)
                        if (result.optInt("failureType", 0) != 0) notes += "Klimatyzacja: samochód odmówił wykonania."
                    } catch (e: Exception) {
                        Trial.requireActive(context)
                        if (e is ApiFailure && e.code in listOf(401,403,2,3,7)) throw e
                        notes += "Klimatyzacja: brak potwierdzenia przyjęcia; nie ponowiono komendy."
                    }
                }
                if (start && (profile.driver != null || profile.passenger != null)) {
                    try {
                        val max = VehicleInfo.seatMax(car)
                        check(max != null || store.flag("seatTest")) { "Wyposażenie foteli nieustalone; najpierw włącz świadomy test w profilu." }
                        val limit = max ?: 1
                        require(listOfNotNull(profile.driver, profile.passenger).all { it in 0..limit }) { "Niedozwolony poziom ogrzewania fotela." }
                        if (profile.driver == null || profile.passenger == null) {
                            store.snapshot = VehicleParser.parse(api.status(vin, false), null, store.snapshot, System.currentTimeMillis())
                        }
                        fun current(field: Field) = store.snapshot.fields[field]?.takeIf {
                            it.measuredAt >= commandAt && System.currentTimeMillis() - it.measuredAt in 0..120_000
                        }?.value?.toInt()
                        val pair = profile.seats(current(Field.SEAT_LEFT), current(Field.SEAT_RIGHT))!!
                        expected[Field.SEAT_LEFT] = pair.first.toDouble(); expected[Field.SEAT_RIGHT] = pair.second.toDouble()
                        val result = api.control(SaicApi.seatsBody(vin, pair.first, pair.second))
                        if (result.optInt("failureType", 0) != 0) notes += "Fotele: samochód odmówił wykonania."
                    } catch (e: Exception) {
                        Trial.requireActive(context)
                        if (e is ApiFailure && e.code in listOf(401,403,2,3,7)) throw e
                        notes += "Fotele: ${e.message ?: "brak potwierdzenia"}. Nie ponowiono komendy."
                    }
                }
                if (expected.isEmpty()) { store.message = notes.joinToString(" ").ifBlank { "Profil nie zawiera żadnej funkcji." }; return@withLock }
                val confirmed = mutableSetOf<Field>()
                repeat(3) { attempt ->
                    store.message = "Potwierdzanie stanu samochodu (${attempt + 1}/3)…"
                    Widgets.updateAll(context)
                    if (attempt > 0) Thread.sleep(10_000)
                    try {
                        val status = api.status(vin, false)
                        store.snapshot = VehicleParser.parse(status, null, store.snapshot, System.currentTimeMillis())
                        expected.forEach { (field, value) ->
                            val r = store.snapshot.fields[field]
                            if (r != null && r.measuredAt >= commandAt && System.currentTimeMillis() - r.measuredAt in 0..120_000 && r.value == value) confirmed += field
                        }
                    } catch (e: Exception) {
                        Trial.requireActive(context)
                        if (e is ApiFailure && e.code in listOf(401,403,2,3,7)) throw e
                    }
                    if (confirmed.containsAll(expected.keys)) {
                        store.message = "Potwierdzono: ${confirmed.joinToString { it.label }}. " + notes.joinToString(" ")
                        return@withLock
                    }
                }
                store.message = "Potwierdzono: ${confirmed.joinToString { it.label }.ifBlank { "brak" }}. " +
                    "Niepotwierdzone: ${(expected.keys - confirmed).joinToString { it.label }}. " + notes.joinToString(" ")
            } catch (e: Exception) {
                markFailure(e)
                if (!Trial.isExpired(context)) store.message = "Wynik sterowania niepotwierdzony. ${store.message} Nie ponowiono polecenia."
            } finally {
                store.prefs.edit().putBoolean("climatePending", false).commit()
                Widgets.updateAll(context)
            }
        } } finally { climateInFlight.set(false) }
    }
    private fun markFailure(e: Exception) {
        if (e is ApiFailure && e.code in listOf(401, 403, 2, 3, 7)) {
            store.prefs.edit().putBoolean("authBlocked", true).commit()
            store.message = "Sesja iSMART utracona. Zaloguj się ponownie w aplikacji."
        } else store.message = when (e) {
            is ApiFailure -> e.message ?: "Błąd iSMART."
            is java.io.IOException -> "Brak połączenia z iSMART. Zachowano poprzednie dane."
            is IllegalArgumentException, is IllegalStateException -> e.message ?: "Nieprawidłowe dane."
            else -> "Nie udało się odczytać danych. Sprawdź połączenie lub zaloguj się ponownie."
        }
    }
    companion object { private val lock = ReentrantLock(); private val climateInFlight = AtomicBoolean(false) }
}
