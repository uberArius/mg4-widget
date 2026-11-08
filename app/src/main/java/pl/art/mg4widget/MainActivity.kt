package pl.art.mg4widget

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

open class MainActivity : Activity() {
    protected open val accountScreen = false
    private lateinit var store: Store
    private var statusView: TextView? = null
    private var dashboard: VehicleDashboard? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var busy = false
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> runOnUiThread { updateStatus() } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); store = Store(this); render()
        store.prefs.registerOnSharedPreferenceChangeListener(listener)
        if (intent.getBooleanExtra("chooseBt",false)) bluetooth()
    }
    override fun onResume() { super.onResume(); if (::store.isInitialized) { if (store.isReady && !busy && !accountScreen) render() else updateStatus() } }
    override fun onDestroy() { store.prefs.unregisterOnSharedPreferenceChangeListener(listener); executor.shutdown(); super.onDestroy() }
    private fun updateStatus() {
        statusView?.text = if (Trial.isExpired(this)) Trial.MESSAGE else store.message
        dashboard?.update()
    }
    private fun render() {
        statusView = null; dashboard = null
        if (accountScreen || !store.isReady) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!RiskConsent.accepted(this)) { RiskConsent.show(this) { render() }; return }
        val page = page()
        text(page, "MG - ${VehicleInfo.title(store.vehicle)}", 28f, Palette.accent)
        button(page, "Język / Language") { Language.choose(this) { render() } }
        heading(page, "Twój samochód pod ręką")
        text(page, "Morski widżet · ustawienia samochodu", 16f, Palette.muted)
        if (store.isReady && !accountScreen) {
            val summary = card(page, compact = true).apply { setPadding(dp(10),dp(10),dp(10),dp(10)) }
            text(summary, if (store.demo) "DANE PRZYKŁADOWE" else VehicleInfo.title(store.vehicle), 15f, Palette.accent)
            dashboard = VehicleDashboard(this, store, summary)
            statusView = text(summary, "", 13f, Palette.muted)
            button(summary, "Odśwież dane samochodu", true) {
                store.message = "Zaplanowano odświeżenie. Oczekiwanie na połączenie…"
                RefreshScheduler.once(this, "manual")
            }
            button(summary, "Ogrzewanie kabiny") { startActivity(Intent(this, ClimateActivity::class.java)) }
            button(page,"Sprawdź wszystkie parametry auta") { startActivity(Intent(this,DiagnosticsActivity::class.java)) }
            heading(page, "Twój widżet")
            button(page,"Dodaj widżet na pulpit") {
                val manager = AppWidgetManager.getInstance(this)
                if (!manager.isRequestPinAppWidgetSupported || !manager.requestPinAppWidget(ComponentName(this,FullWidget::class.java),null,null))
                    Toast.makeText(this, Language.text("Dodaj widżet z menu ekranu głównego."),Toast.LENGTH_LONG).show()
            }
            listOf("tyres" to "Alarm ciśnienia opon", "heat" to "Profil ogrzewania", "updates" to "Aktualizacje i Bluetooth", "appearance" to "Wygląd widżetu").forEach { (section,label) ->
                button(page,label) { startActivity(Intent(this,SettingsActivity::class.java).putExtra("section",section)) }
            }
            text(page,"Region konta: ${store.region.label}. Zmiana regionu wymaga wylogowania. Mini i Standard zastępuje jeden widżet — dodaj go ponownie, jeśli korzystałeś z tych wariantów.",12f,Palette.muted)
            button(page, "Logowanie i dane konta") { startActivity(Intent(this, AccountActivity::class.java)) }
        } else {
            if (accountScreen) button(page, "Wróć do aplikacji") { finish() }
            val savedAccount = store.account()
            val form = card(page)
            text(form, "Połącz konto iSMART", 19f)
            text(form, "Dane logowania pozostają zaszyfrowane na telefonie. Aplikacja łączy się bezpośrednio z serwerem iSMART.", 13f, Palette.muted)
            text(form, "Logowanie na konto główne wiąże się z wylogowaniem z aplikacji MG. Dodaj w aplikacji MG konto dodatkowe i użyj je do widżetu.", 14f, Palette.muted)
            text(form, "Dostępne odczyty i sterowanie zależą od uprawnień udostępnionego auta.", 12f, Palette.muted)
            val regions = android.widget.Spinner(this).apply {
                adapter = android.widget.ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,Region.entries.map { Language.text(it.label) })
                setSelection(store.region.ordinal)
            }
            form.addView(regions)
            val user = input(form, "E-mail lub numer telefonu", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
            val password = input(form, "Hasło iSMART", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply {
                isSaveEnabled = false
                transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            toggle(form, "Pokaż hasło", false) { show ->
                val position = password.selectionStart.coerceAtLeast(0)
                password.transformationMethod = if (show) null else android.text.method.PasswordTransformationMethod.getInstance()
                password.setSelection(position.coerceAtMost(password.length()))
            }
            val phoneBox = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                visibility = android.view.View.GONE
            }
            form.addView(phoneBox)
            text(phoneBox, "Kod kraju numeru telefonu · 48 = Polska", 13f, Palette.muted)
            val country = input(phoneBox, "Np. 48", InputType.TYPE_CLASS_PHONE, "48")
            text(phoneBox, "Numer wpisz bez kodu kraju. Kod nie jest używany przy logowaniu e-mailem.", 12f, Palette.muted)
            user.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val login = s.toString().trim()
                    phoneBox.visibility = if (login.isNotEmpty() && !login.contains('@') && login.first().let { it.isDigit() || it == '+' }) android.view.View.VISIBLE else android.view.View.GONE
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            user.setText(savedAccount?.optString("username") ?: "")
            password.setText(savedAccount?.optString("password") ?: "")
            country.setText(savedAccount?.optString("country")?.takeIf { it.isNotBlank() } ?: "48")
            statusView = text(form, store.message, 13f, Palette.muted)
            button(form, "Połącz z iSMART", true) {
                val u = user.text.toString(); val p = password.text.toString(); val c = country.text.toString()
                val selectedRegion = Region.entries[regions.selectedItemPosition]
                if (!busy) {
                    statusView?.text = Language.text("Logowanie…")
                    task {
                        store.region = selectedRegion
                        val cars = Repository(applicationContext).login(u, p, c)
                        runOnUiThread {
                            if (!isDestroyed) {
                                password.text.clear()
                                if (cars.size == 1) { Repository(applicationContext).selectVehicle(cars[0]); if (accountScreen) finish() else render() }
                                else LocalizedDialog(this).setTitle("Wybierz samochód")
                                    .setItems(cars.map { "${it.optString("modelName")} · ${it.optString("vin").takeLast(6)}" }.toTypedArray()) { _, index ->
                                        Repository(applicationContext).selectVehicle(cars[index]); if (accountScreen) finish() else render()
                                    }.setCancelable(false).show()
                            }
                        }
                    }
                }
            }
            if (savedAccount != null || store.isReady) button(form, "WYCZYŚĆ DANE") {
                task { Repository(applicationContext).logout(); runOnUiThread { if (!isDestroyed) render() } }
            }
            text(form, "Wejście na ten ekran nie usuwa danych. WYCZYŚĆ DANE wylogowuje i usuwa zapisane dane konta oraz samochodu z telefonu.", 12f, Palette.muted)
            if (!accountScreen) button(page, "Zobacz widżety w trybie DEMO") { task { Repository(applicationContext).demo(); runOnUiThread { if (!isDestroyed) render() } } }
            text(page, "To niezależna aplikacja, nie oficjalny produkt MG. Równoległe logowanie w iSMART może zakończyć sesję — wtedy poprosimy o ponowne logowanie.", 12f, Palette.muted)
        }
        text(page, "MG4 Widget · wersja testowa 0.8.0", 12f, Palette.muted)
        button(page, "Informacje o projekcie i licencje") {
            LocalizedDialog(this).setTitle("Źródła rozwiązań · MIT")
                .setMessage(assets.open("THIRD_PARTY_NOTICES.txt").bufferedReader().use { it.readText() })
                .setPositiveButton("Zamknij", null).show()
        }
        updateStatus()
    }
    private fun task(block: () -> Unit) {
        if (busy) return
        busy = true
        executor.execute {
            try { block() } catch (e: Exception) {
                val message = when (e) {
                    is ApiFailure, is IllegalArgumentException, is IllegalStateException -> e.message ?: "Nie udało się wykonać operacji."
                    else -> "Nie udało się połączyć. Sprawdź internet i dane konta."
                }
                runOnUiThread { if (!isDestroyed) { statusView?.text = Language.text(message); Toast.makeText(this, Language.text(message), Toast.LENGTH_LONG).show() } }
            } finally { runOnUiThread { busy = false } }
        }
    }
    private fun bluetooth() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41); return
        }
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) { Toast.makeText(this, Language.text("Włącz Bluetooth i sparuj samochód w ustawieniach telefonu."), Toast.LENGTH_LONG).show(); return }
        val devices = adapter.bondedDevices.toList().sortedBy { it.name ?: it.address }
        if (devices.isEmpty()) { Toast.makeText(this, Language.text("Najpierw sparuj samochód w ustawieniach Bluetooth."), Toast.LENGTH_LONG).show(); return }
        LocalizedDialog(this).setTitle("Bluetooth Twojego MG4")
            .setItems(devices.map { "${it.name ?: "Urządzenie"}\n${it.address}" }.toTypedArray()) { _, index ->
                store.btAddress = devices[index].address; store.prefs.edit().putString("btName",devices[index].name ?: devices[index].address).commit(); render()
            }.setNegativeButton("Zamknij", null).show()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) bluetooth()
    }
}
