package pl.art.mg4widget

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.work.*
import org.json.JSONArray
import org.json.JSONObject

object DiagnosticReport {
    fun lookup(root: JSONObject?,path: String): Any? {
        var current: Any?=root
        path.split('.').forEach { key -> current=(current as? JSONObject)?.opt(key) }
        return current?.takeUnless { it==JSONObject.NULL }
    }
    fun build(catalog: JSONObject,responses: Map<String,JSONObject>,errors: Map<String,String>,fresh: Snapshot?,started: Long,finished: Long,demo: Boolean): JSONObject {
        val groups=JSONArray()
        val readings=JSONArray()
        Field.entries.filter { it !in Tiles.groups }.forEach { field ->
            readings.put(JSONObject().put("name",field.label).put("value",if(fresh?.fields?.containsKey(field)==true) fresh.value(field) else "Nie otrzymano wartości w tym odczycie"))
        }
        groups.put(JSONObject().put("name","Odczyty użytkowe").put("items",readings))
        val names=mapOf("vehicle" to "Samochód i wyposażenie","status" to "Stan pojazdu i GPS","charging" to "Bateria i zarządzanie ładowaniem","chargeStatus" to "Szczegóły ładowania i podróży","batterySchedule" to "Harmonogram ogrzewania baterii")
        catalog.keys().forEach { key ->
            val fields=catalog.getJSONArray(key); val items=JSONArray()
            repeat(fields.length()) { i ->
                val path=fields.getString(i); val value=lookup(responses[key],path)
                items.put(JSONObject().put("name",path).put("value",value?.toString() ?: if(errors.containsKey(key)) "Nie otrzymano odpowiedzi: ${errors[key]}" else "Nie otrzymano wartości w odpowiedzi"))
            }
            groups.put(JSONObject().put("name",names[key] ?: key).put("items",items))
        }
        return JSONObject().put("started",started).put("finished",finished).put("demo",demo).put("groups",groups)
    }
}
class DiagnosticsWorker(context: Context,params: WorkerParameters): Worker(context,params) {
    override fun doWork(): Result {
        val store=Store(applicationContext)
        try {
            if(isStopped) return Result.success()
            if(!store.demo) Trial.requireActive(applicationContext)
            store.prefs.edit().putBoolean("diagnosticsRunning",true).remove("diagnostics").commit()
            Repository(applicationContext,cancelled={isStopped}).diagnostics()
        } catch(e: Exception) {
            store.message=e.message ?: "Nie udało się odczytać parametrów."
            val catalog=JSONObject(applicationContext.assets.open("diagnostic-catalog.json").bufferedReader().use { it.readText() })
            val errors=catalog.keys().asSequence().associateWith { store.message }
            val report=DiagnosticReport.build(catalog,emptyMap(),errors,null,System.currentTimeMillis(),System.currentTimeMillis(),store.demo)
            store.prefs.edit().putString("diagnostics",report.toString()).commit()
        }
        finally { store.prefs.edit().putBoolean("diagnosticsRunning",false).commit() }
        return Result.success()
    }
}
class DiagnosticsActivity: Activity() {
    private lateinit var store: Store
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private val listener=SharedPreferences.OnSharedPreferenceChangeListener { _,key ->
        runOnUiThread { if(key=="diagnostics") showReport(); status.text=store.message }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); store=Store(this)
        val page=page()
        heading(page,"Wszystkie parametry auta")
        text(page,"Sprawdzimy odczyty stanu, ładowania, harmonogramu ogrzewania baterii i wyposażenia. Odczyt może budzić auto i potrwać kilka minut. Nie zmieniamy ustawień samochodu.",13f,Palette.muted)
        text(page,"Najpierw wartości użytkowe, dalej pełna lista pól tych odpowiedzi. Pola techniczne pokazują surowe wartości i kody. Brak danych nie oznacza zera ani braku wyposażenia. Wiadomości konta i dane logowania nie są częścią tego odczytu.",12f,Palette.muted)
        button(page,"Odczytaj wszystkie parametry",true) {
            val work=OneTimeWorkRequestBuilder<DiagnosticsWorker>().addTag("mg4").build()
            WorkManager.getInstance(this).enqueueUniqueWork("diagnostics",ExistingWorkPolicy.KEEP,work)
            status.text=Language.text("Zlecono sprawdzanie parametrów…")
        }
        status=text(page,store.message,13f,Palette.accent)
        results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; page.addView(results)
        store.prefs.registerOnSharedPreferenceChangeListener(listener); showReport()
    }
    private fun showReport() {
        results.removeAllViews()
        val report=store.prefs.getString("diagnostics",null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return
        text(results,"Odczyt: ${readingTime(report.optLong("started"))}"+(if(report.optBoolean("demo")) " · DEMONSTRACJA, bez sieci" else ""),12f,Palette.muted)
        val groups=report.getJSONArray("groups")
        repeat(groups.length()) { i ->
            val group=groups.getJSONObject(i); heading(results,group.getString("name"))
            val items=group.getJSONArray("items")
            repeat(items.length()) { j ->
                val item=items.getJSONObject(j); val box=card(results,compact=true)
                text(box,item.getString("name"),13f,Palette.accent)
                text(box,item.getString("value"),13f).setTextIsSelectable(true)
            }
        }
    }
    override fun onDestroy() { store.prefs.unregisterOnSharedPreferenceChangeListener(listener); super.onDestroy() }
}
