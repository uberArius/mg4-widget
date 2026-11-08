package pl.art.mg4widget

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.*
import java.util.concurrent.TimeUnit

class MgApplication : Application() {
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Widgets.updateAll(this)
    }
    override fun onCreate() {
        super.onCreate()
        Language.init(this)
        Trial.observe(this)
        val store = Store(this)
        store.prefs.edit().remove("highestSeen").commit()
        if (store.prefs.getString("message", "") == "Okres testowy się skończył, zainstaluj zaktualizowaną wersję") store.message = "Odśwież dane samochodu"
        if (!store.prefs.contains("btEnabled") && store.btAddress.isNotBlank()) store.setFlag("btEnabled",true)
        if (store.prefs.getBoolean("climatePending", false)) {
            store.message = "Przerwano sprawdzanie klimatyzacji. Stan niepotwierdzony — odśwież dane."
            store.prefs.edit().putBoolean("climatePending", false).commit()
        }
        RefreshScheduler.start(this)
    }
}
object TripPolicy {
    fun shouldRefresh(request: Long, lastDisconnect: Long, connected: Boolean, now: Long) =
        request > 0 && request == lastDisconnect && !connected && now - request in 0..900_000
}
object RefreshScheduler {
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    fun start(context: Context) {
        val store = Store(context)
        val manager = WorkManager.getInstance(context)
        if (!RiskConsent.accepted(context)) { cancel(context); return }
        if (Trial.isExpired(context)) {
            store.message = Trial.MESSAGE; cancel(context); Widgets.updateAll(context); return
        }
        manager.cancelUniqueWork("trial-expiry")
        if (store.interval == 0 || store.demo || !store.isReady) { manager.cancelUniqueWork("hourly"); return }
        val work = PeriodicWorkRequestBuilder<RefreshWorker>(store.interval.toLong(), TimeUnit.MINUTES)
            .setInitialDelay(store.interval.toLong(), TimeUnit.MINUTES).setConstraints(network)
            .setInputData(workDataOf("reason" to "hourly")).addTag("mg4").build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("hourly", ExistingPeriodicWorkPolicy.UPDATE, work)
    }
    fun once(context: Context, reason: String, disconnected: Long = 0) {
        if (!RiskConsent.accepted(context)) { Store(context).message = RiskConsent.REQUIRED; Widgets.updateAll(context); return }
        if (Trial.isExpired(context)) { Store(context).message = Trial.MESSAGE; Widgets.updateAll(context); return }
        if (reason == "bluetooth" && !Store(context).flag("btEnabled")) return
        val builder = OneTimeWorkRequestBuilder<RefreshWorker>().setConstraints(network)
            .setInitialDelay(0, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)
            .setInputData(workDataOf("reason" to reason, "disconnected" to disconnected)).addTag("mg4")
        if (reason == "manual") builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        val work = builder.build()
        WorkManager.getInstance(context).enqueueUniqueWork(if (reason == "bluetooth") "after-trip" else "manual",
            if (reason == "bluetooth") ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, work)
    }
    fun cancel(context: Context) { WorkManager.getInstance(context).cancelAllWorkByTag("mg4") }
}
class RefreshWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = Store(applicationContext)
        if (Trial.isExpired(applicationContext)) { store.message = Trial.MESSAGE; RefreshScheduler.cancel(applicationContext); Widgets.updateAll(applicationContext); return Result.success() }
        if (isStopped || store.prefs.getBoolean("authBlocked", false)) return Result.success()
        val reason = inputData.getString("reason") ?: "hourly"
        val success = Repository(applicationContext, cancelled = { isStopped }).refresh(reason, inputData.getLong("disconnected", 0))
        return if (!success && reason == "bluetooth" && runAttemptCount < 2 && !isStopped &&
            !store.prefs.getBoolean("authBlocked", false)) Result.retry() else Result.success()
    }
}
class RefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (RiskConsent.accepted(context) && !Trial.isExpired(context)) {
            Store(context).message = "Zlecono odświeżenie w tle. Oczekiwanie na sieć i samochód…"
            Widgets.updateAll(context)
        }
        RefreshScheduler.once(context, "manual")
    }
}
class CarBluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        val store = Store(context)
        if (Trial.isExpired(context) || !store.flag("btEnabled") || !store.isReady || store.demo || store.btAddress.isBlank()) return
        @Suppress("DEPRECATION")
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        if (device.address != store.btAddress) return
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                store.prefs.edit().putBoolean("btConnected", true).putLong("btDisconnected", 0).commit()
                WorkManager.getInstance(context).cancelUniqueWork("after-trip")
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val now = System.currentTimeMillis()
                store.prefs.edit().putBoolean("btConnected", false).putLong("btDisconnected", now).commit()
                RefreshScheduler.once(context, "bluetooth", now)
            }
        }
    }
}

class ExpiryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (Trial.isExpired(applicationContext)) {
            Store(applicationContext).message = Trial.MESSAGE
            RefreshScheduler.cancel(applicationContext)
            Widgets.updateAll(applicationContext)
        }
        return Result.success()
    }
}

object HeatScheduler {
    fun enqueue(context: Context, start: Boolean) {
        RiskConsent.requireAccepted(context)
        Trial.requireActive(context)
        val store = Store(context)
        check(!store.demo && store.vehicle != null) { "Sterowanie wymaga konta samochodu." }
        val p = store.profile
        val work = OneTimeWorkRequestBuilder<HeatWorker>().setInputData(workDataOf(
            "start" to start, "created" to System.currentTimeMillis(), "vin" to store.vehicle!!.getString("vin"),
            "cabin" to p.cabin, "temp" to p.temperature, "defrost" to p.defrost,
            "driver" to (p.driver ?: -1), "passenger" to (p.passenger ?: -1), "rhd" to p.rightHandDrive))
            .addTag("mg4").build()
        WorkManager.getInstance(context).enqueueUniqueWork("heat-profile", ExistingWorkPolicy.KEEP, work)
    }
}
class HeatWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = Store(applicationContext)
        // Persist a claim BEFORE any request. A restarted worker must not repeat a possibly executed command.
        if (store.prefs.getString("heatClaim", "") == id.toString()) {
            store.message = "Przerwano operację ogrzewania. Nie ponowiono komend; odśwież stan."
            Widgets.updateAll(applicationContext); return Result.success()
        }
        if (isStopped || System.currentTimeMillis() - inputData.getLong("created", 0) !in 0..300_000 ||
            store.vehicle?.optString("vin") != inputData.getString("vin")) return Result.success()
        if (!store.prefs.edit().putString("heatClaim", id.toString()).commit()) return Result.failure()
        try {
            Trial.requireActive(applicationContext)
            Repository(applicationContext, cancelled = { isStopped }).climate(inputData.getBoolean("start", true), HeatProfile(
                inputData.getBoolean("cabin", true), inputData.getInt("temp",22), inputData.getBoolean("defrost",false),
                inputData.getInt("driver",-1).takeIf { it >= 0 }, inputData.getInt("passenger",-1).takeIf { it >= 0 },
                inputData.getBoolean("rhd",false)))
        } catch (e: Exception) { store.message = e.message ?: "Wynik niepotwierdzony. Odśwież stan." }
        Widgets.updateAll(applicationContext)
        return Result.success()
    }
}
