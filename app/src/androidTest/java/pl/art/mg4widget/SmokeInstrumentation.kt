package pl.art.mg4widget

import android.app.Instrumentation
import android.os.Bundle
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Emulator-only checks; never logs in with real credentials or sends vehicle commands. */
class SmokeInstrumentation : Instrumentation() {
    private var preview=false
    private var alertPreview=false
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); alertPreview=arguments?.getString("preview")=="alert"; preview=arguments?.getString("preview")=="v03" || alertPreview; start() }
    override fun onStart() {
        val context=targetContext
        val store=Store(context)
        val backup=store.prefs.all.toMap()
        val results=mutableListOf<String>()
        var failure: Throwable?=null
        try {
            store.prefs.edit().putString("language","pl").commit()
            check(store.account()==null) { "Run these tests only on a demo emulator without an account." }
            store.prefs.edit().remove("riskConsentVersion").commit()
            try { Repository(context).login("synthetic-test@example.invalid","synthetic-password","48"); error("Consent gate failed") }
            catch(e: IllegalStateException) { check(e.message==RiskConsent.REQUIRED) }
            RefreshScheduler.start(context)
            check(WorkManager.getInstance(context).getWorkInfosForUniqueWork("hourly").get(10,TimeUnit.SECONDS).none { !it.state.isFinished })
            store.prefs.edit().putInt("riskConsentVersion",RiskConsent.VERSION).commit()
            results += "Consent required before login HTTP and scheduling"
            store.demo=false
            store.vehicle=org.json.JSONObject().put("vin","synthetic-test-vin")
            store.interval=15
            RefreshScheduler.start(context)
            val scheduled=WorkManager.getInstance(context).getWorkInfosForUniqueWork("hourly").get(10,TimeUnit.SECONDS)
            check(scheduled.any { !it.state.isFinished }); results += "15 min: periodic work scheduled with initial delay"
            store.interval=0; store.setFlag("btEnabled",false)
            RefreshScheduler.start(context)
            val jobs=WorkManager.getInstance(context).getWorkInfosForUniqueWork("hourly").get(10,TimeUnit.SECONDS)
            check(jobs.none { !it.state.isFinished }); results += "0 + BT OFF: no periodic work"
            store.selectedFields=emptyList(); check(store.selectedFields.isEmpty()); Widgets.updateAll(context)
            store.selectedFields=Field.entries.toList(); check(store.selectedFields==Tiles.selectable); Widgets.updateAll(context)
            store.setFlag("icons",false); store.setFlag("showRefresh",false); store.setFlag("showHeat",false); Widgets.updateAll(context)
            results += "0/all fields and hidden controls accepted by widget host"
            store.prefs.edit().putString("selectedFields","INSIDE,OUTSIDE,TYRE_FL,TYRE_RR").commit()
            check(store.selectedFields==listOf(Field.TEMPERATURES,Field.TYRES))
            store.demo=true
            val report=Repository(context).diagnostics()
            check(report.getBoolean("demo")); check(report.getJSONArray("groups").length()==6)
            results += "Legacy fields merge; demo diagnostics builds a full report without HTTP"
            store.prefs.edit().remove("frontTyreThreshold").remove("rearTyreThreshold").commit()
            check(store.frontTyreBar == 2.4 && store.rearTyreBar == 2.4)
            store.frontTyreThreshold = 22; store.rearTyreThreshold = 27
            check(Store(context).frontTyreBar == 2.2 && Store(context).rearTyreBar == 2.7)
            results += "Axle threshold defaults and persistence verified"
            val credentials = org.json.JSONObject().put("username", "synthetic-test@example.invalid").put("password", "synthetic-password").put("country", "48")
            store.saveAccount(credentials)
            fun descendants(view: android.view.View): List<android.view.View> = listOf(view) +
                if (view is android.view.ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
            val account = startActivitySync(android.content.Intent(context, AccountActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync()
            runOnMainSync {
                val views = descendants(account.window.decorView)
                val inputs = views.filterIsInstance<android.widget.EditText>()
                check(inputs.any { it.text.toString() == "synthetic-test@example.invalid" })
                check(inputs.any { it.text.toString() == "synthetic-password" && it.transformationMethod is android.text.method.PasswordTransformationMethod })
                check(store.account()?.optString("username") == "synthetic-test@example.invalid")
                views.filterIsInstance<android.widget.Button>().first { it.text.toString() == "WYCZYŚĆ DANE" }.performClick()
            }
            val until = android.os.SystemClock.elapsedRealtime() + 5000
            while (store.account() != null && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(50)
            waitForIdleSync()
            check(store.account() == null && store.vehicle == null)
            runOnMainSync { account.finish() }
            results += "Account screen retains credentials masked; explicit clear removes account data"
            check(Language.resolve("auto","pl") == "pl")
            listOf("en","de","fr","ja").forEach { check(Language.resolve("auto",it) == "en") }
            check(Language.resolve("pl","en") == "pl")
            check(Language.resolve("en","pl") == "en")
            store.prefs.edit().putString("language","en").commit()
            check(Language.text("Bateria / Cel") == "Battery / Target")
            check(Language.text("Odświeżanie co 30 minut") == "Refresh every 30 minutes")
            check(Language.text("Nie ładuje") == "Not charging")
            runOnMainSync {
                val tile = Widgets.tile(context,store,Field.SOC,58).apply(context,null)
                check(descendants(tile).filterIsInstance<android.widget.TextView>().any { it.text.toString() == "Battery / Target" })
                val dialog = LocalizedDialog(context).setTitle("Profil ogrzewania").setPositiveButton("Zapisz",null)
                // Builder construction verifies localized overload compatibility without a window.
            }
            store.prefs.edit().putString("language","pl").commit()
            check(Language.text("Bateria / Cel") == "Bateria / Cel")
            results += "Automatic PL/EN, manual override and widget translation verified"
            store.prefs.edit().putLong("highestSeen",Long.MAX_VALUE).commit()
            check(!Trial.isExpired(context))
            Trial.requireActive(context)
            results += "No release expiry, even with an old future clock marker"

        } catch(t: Throwable) { failure=t }
        finally {
            val editor=store.prefs.edit().clear()
            backup.forEach { (key,value) -> when(value) {
                is String -> editor.putString(key,value)
                is Boolean -> editor.putBoolean(key,value)
                is Int -> editor.putInt(key,value)
                is Long -> editor.putLong(key,value)
                is Float -> editor.putFloat(key,value)
            } }
            editor.commit(); RefreshScheduler.start(context); Widgets.updateAll(context)
        }
        if(preview && failure==null) {
            Repository(context).demo()
            store.selectedFields=listOf(Field.SOC,Field.RANGE,Field.TEMPERATURES,Field.SCHEDULE,Field.TYRES,Field.DOORS,Field.WINDOWS,Field.SEATS,Field.LOCKED,Field.CLIMATE)
            store.setFlag("icons",true); store.setFlag("showRefresh",true); store.setFlag("showHeat",true)
            if (alertPreview) {
                val now=System.currentTimeMillis()
                store.snapshot=store.snapshot.copy(fields=store.snapshot.fields + mapOf(Field.ENGINE to Reading(0.0,now),Field.TYRE_FL to Reading(2.3,now)))
                store.selectedFields=listOf(Field.SOC,Field.RANGE)
                check(store.snapshot.parkedWarning())
                results += "Parked warning with tyre tile hidden"
            }
            Widgets.updateAll(context)
        }
        val output=Bundle().apply { putString("stream",results.joinToString("\n") + (failure?.let { "\nFAILED: $it" } ?: "\nPASS")) }
        finish(if(failure==null) -1 else 0,output)
    }
}
