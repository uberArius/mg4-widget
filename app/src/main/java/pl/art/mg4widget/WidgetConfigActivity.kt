package pl.art.mg4widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle

class WidgetConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val page = page()
        heading(page, "Twój morski widżet")
        text(page, "Zawartość i wygląd wszystkich instancji zmienisz w aplikacji. Zmniejsz szerokość, aby uzyskać jedną kolumnę. Kafelki można przewijać.")
        button(page, "Wybierz kafelki") { startActivity(Intent(this, SettingsActivity::class.java).putExtra("section", "tiles")) }
        button(page, "Dodaj / zapisz widżet", true) {
            Widgets.update(this, id)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)); finish()
        }
    }
}
