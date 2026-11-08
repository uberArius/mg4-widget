package pl.art.mg4widget

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

class ClimateActivity : Activity() {
    private lateinit var store: Store
    private var status: TextView? = null
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "message") runOnUiThread { status?.text = store.message }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); store = Store(this); store.prefs.registerOnSharedPreferenceChangeListener(listener)
    }
    override fun onResume() {
        super.onResume()
        if (!RiskConsent.accepted(this)) { startActivity(Intent(this, MainActivity::class.java)); finish(); return }
        val page = page(); val profile = store.profile
        heading(page,"Ogrzewanie kabiny")
        val box = card(page)
        text(box, if (profile.defrost) "Szyba · odszranianie (22°C)" else if (profile.cabin) "Kabina · ${profile.temperature}°C" else "Kabina · bez zmian",20f)
        text(box,"Fotel kierowcy: ${profile.driver?.toString() ?: "bez zmian"}\nFotel pasażera: ${profile.passenger?.toString() ?: "bez zmian"}",15f,Palette.muted)
        button(box,"Zmień profil") { startActivity(Intent(this,SettingsActivity::class.java).putExtra("section","heat")) }
        text(page,"Wynik sprawdzimy odczytem auta. Odpowiedź może trwać kilka minut. Możesz zamknąć ten ekran; operacja będzie kontynuowana w tle.",14f,Palette.muted)
        status = text(page,if (Trial.isExpired(this)) Trial.MESSAGE else store.message,14f,Palette.accent)
        fun submit(start: Boolean) {
            try { HeatScheduler.enqueue(this,start); status?.text = Language.text("Zlecono operację. Oczekiwanie na samochód…") }
            catch (e: Exception) { Toast.makeText(this, Language.text(e.message),Toast.LENGTH_LONG).show() }
        }
        button(page,"Uruchom zapisany profil",true) { submit(true) }.isEnabled = !store.demo && store.vehicle != null && !Trial.isExpired(this)
        button(page,"Zatrzymaj klimatyzację") { submit(false) }.isEnabled = !store.demo && store.vehicle != null && !Trial.isExpired(this)
        text(page,"Zatrzymanie klimatyzacji nie zmienia ustawień foteli. Aby je wyłączyć, wybierz w profilu poziom 0.",12f,Palette.muted)
        button(page,"Wróć") { finish() }
    }
    override fun onDestroy() { store.prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
}
