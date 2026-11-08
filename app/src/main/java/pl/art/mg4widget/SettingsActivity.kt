package pl.art.mg4widget

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.NumberPicker

class SettingsActivity : Activity() {
    private lateinit var store: Store
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); store = Store(this); render() }
    private fun changed() { Widgets.updateAll(this) }
    private fun flag(parent: LinearLayout, key: String, label: String, default: Boolean = false, action: () -> Unit = {}) {
        toggle(parent, label, store.flag(key, default)) { store.setFlag(key, it); changed(); action() }
    }
    private fun render() {
        val page = page()
        when (intent.getStringExtra("section")) {
            "tiles" -> {
                startActivity(Intent(this, MainActivity::class.java)); finish(); return
            }
            "tyres" -> {
                heading(page, "Alarm ciśnienia opon")
                text(page, "Alarm przy wyłączonym napędzie, gdy ciśnienie jest niższe od progu dla danej osi. Domyślnie 2,4 bar z przodu i z tyłu. Zmiana od razu przelicza ostatnie odczyty, bez łączenia z autem.", 14f, Palette.muted)
                val box = card(page)
                fun pressure(value: Int) = java.lang.String.format(Language.locale, "%.1f bar", value / 10.0)
                fun choose(front: Boolean) {
                    val picker = NumberPicker(this).apply {
                        minValue = 10; maxValue = 40; wrapSelectorWheel = false
                        setFormatter { pressure(it) }
                        value = if (front) store.frontTyreThreshold else store.rearTyreThreshold
                    }
                    LocalizedDialog(this).setTitle(if (front) "Próg — opony przednie" else "Próg — opony tylne")
                        .setView(picker).setPositiveButton("Zapisz") { _, _ ->
                            picker.clearFocus()
                            if (front) store.frontTyreThreshold = picker.value else store.rearTyreThreshold = picker.value
                            changed(); render()
                        }.setNegativeButton("Anuluj", null).show()
                }
                button(box, "Przód: ${pressure(store.frontTyreThreshold)}") { choose(true) }
                button(box, "Tył: ${pressure(store.rearTyreThreshold)}") { choose(false) }
            }
            "heat" -> {
                heading(page, "Profil ogrzewania")
                text(page, "Przycisk widżetu otwiera ten zapisany profil do uruchomienia. Wyłączony przełącznik pomija funkcję; poziom 0 wysyła wyłączenie fotela.", 14f, Palette.muted)
                val box = card(page)
                flag(box, "heatCabin", "Temperatura kabiny", true)
                button(box, "Temperatura: ${store.temperature} °C") { number("Temperatura docelowa",17,31,store.temperature) { store.temperature = it; render() } }
                flag(box, "defrost", "Nawiew / odszranianie szyby")
                text(box, "Tryb szyby ma pierwszeństwo przed temperaturą: profil API używa stałego indeksu 8 (MG4: 22°C) i nawiewu 5.", 12f, Palette.muted)
                flag(box, "rhd", "Kierownica po prawej (UK / Australia)")
                val max = VehicleInfo.seatMax(store.vehicle)
                text(box, when (max) { 3 -> "Auto zgłasza fotele z poziomami 0–3."; 1 -> "Auto zgłasza fotele OFF / ON."; else -> "Auto nie zgłasza rozpoznanego wyposażenia foteli. Nie wysyłamy prób automatycznie." }, 13f, Palette.muted)
                if (max == null) flag(box, "seatTest", "Zezwól na mój test foteli (0 / 1)")
                listOf("driver" to "Fotel kierowcy", "passenger" to "Fotel pasażera").forEach { (key, label) ->
                    flag(box, key + "Heat", label)
                    val level = store.prefs.getInt(key + "Level",1)
                    button(box, "$label: $level") { number(label,0,max ?: 1,level.coerceIn(0,max ?: 1)) { store.prefs.edit().putInt(key + "Level",it).commit(); render() } }
                }
                text(page, "Pominięty fotel zachowa świeżo odczytany stan. Gdy nie można go ustalić, komenda foteli nie zostanie wysłana. Sterowanie w tej wersji obejmuje serię MG4 / EH32.", 13f, Palette.muted)
            }
            "updates" -> {
                heading(page, "Aktualizacje i Bluetooth")
                val restricted = getSystemService(android.app.ActivityManager::class.java).isBackgroundRestricted
                val batteryExempt = getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
                text(page, "Praca w tle: ${if (restricted) "ograniczona przez Androida" else "brak zgłoszonej blokady Androida"}. Optymalizacja baterii: ${if (batteryExempt) "wyłączona dla aplikacji" else "aktywna"}.", 13f, Palette.muted)
                text(page, "Odświeżanie działa przez systemowe zadania w tle; Android może je opóźnić. Przy opóźnieniach ustaw Bateria → Bez ograniczeń. Na Samsungu usuń aplikację z uśpionych/głęboko uśpionych i dodaj do Nigdy nieusypianych. Nie możemy automatycznie zmienić ani sprawdzić wszystkich ustawień One UI. Dla BT zezwól na Urządzenia w pobliżu. Po wymuszonym zatrzymaniu otwórz aplikację ponownie.", 13f, Palette.muted)
                button(page, "Ustawienia systemowe aplikacji") {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
                }

                val box = card(page)
                button(box, if (store.interval == 0) "Cykliczne odświeżanie: wyłączone" else "Odświeżanie co ${store.interval} minut") {
                    val values = listOf(0) + (15..60).toList()
                    LocalizedDialog(this).setTitle("Interwał odświeżania")
                        .setSingleChoiceItems(values.map { if (it == 0) "0 · wyłączone" else "$it minut" }.toTypedArray(), values.indexOf(store.interval)) { dialog, index ->
                            store.interval = values[index]; RefreshScheduler.start(this); dialog.dismiss(); render()
                        }.setNegativeButton("Anuluj",null).show()
                }
                flag(box,"cacheOnly","Cyklicznie tylko dane z serwera (oszczędnie)")
                text(box,"Aktywne odczyty mogą budzić samochód. Częste odświeżanie może zwiększać pobór energii z akumulatora 12 V. Dane z pamięci serwera mogą być starsze. Android może opóźnić pracę w tle.",13f,Palette.muted)
                flag(box,"btEnabled","Aktualizuj bezpośrednio po rozłączeniu BT") {
                    if (!store.flag("btEnabled")) androidx.work.WorkManager.getInstance(this).cancelUniqueWork("after-trip")
                }
                text(box, if (store.btAddress.isBlank()) "Nie wybrano urządzenia Bluetooth." else "Wybrane BT: ${store.prefs.getString("btName", store.btAddress)}",13f,Palette.muted)
                button(box,"Wybierz Bluetooth samochodu") { startActivity(Intent(this,MainActivity::class.java).putExtra("chooseBt",true)); finish() }
                text(page,"0 + wyłączone BT = wyłącznie odświeżanie na żądanie. Po uruchomieniu ogrzewania nadal sprawdzamy jego wynik. Po rozłączeniu BT najwyżej dwie dodatkowe próby, jeśli dane są stare; ponowne połączenie je anuluje.",14f,Palette.muted)
            }
            else -> {
                heading(page,"Wygląd widżetu")
                val box = card(page)
                flag(box,"icons","Ikony w ramkach",true)
                flag(box,"showRefresh","Przycisk Odśwież",true)
                flag(box,"showHeat","Przycisk Ogrzewanie",true)
                flag(box,"miles","Odległości w milach")
                text(page,"Poniżej około 280 dp: jedna kolumna. Powyżej: dwie. Przytrzymaj widżet na pulpicie, aby zmienić rozmiar. Dłuższą listę przewijaj palcem. Ustawienia dotyczą wszystkich instancji widżetu.",14f,Palette.muted)
            }
        }
        button(page,"Gotowe") { finish() }
    }
    private fun order() {
        startActivity(Intent(this, TileOrderActivity::class.java))
    }
    private fun number(title: String, min: Int, max: Int, current: Int, save: (Int) -> Unit) {
        val picker = NumberPicker(this).apply { minValue = min; maxValue = max; value = current; wrapSelectorWheel = false }
        LocalizedDialog(this).setTitle(title).setView(picker).setPositiveButton("Zapisz") { _, _ -> save(picker.value) }.setNegativeButton("Anuluj",null).show()
    }
}
