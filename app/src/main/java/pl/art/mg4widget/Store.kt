package pl.art.mg4widget

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class Store(context: Context) {
    val prefs = context.getSharedPreferences("mg4", Context.MODE_PRIVATE)
    var snapshot: Snapshot
        get() = Snapshot.parse(prefs.getString("snapshot", null))
        set(value) { prefs.edit().putString("snapshot", value.json()).commit() }
    var message: String
        get() = Language.text(prefs.getString("message", "Zaloguj się lub włącz podgląd demonstracyjny.")!!)
        set(value) { prefs.edit().putString("message", value).commit() }
    var demo: Boolean
        get() = prefs.getBoolean("demo", false)
        set(v) { prefs.edit().putBoolean("demo", v).commit() }
    var vehicle: JSONObject?
        get() = prefs.getString("vehicle", null)?.let { JSONObject(it) }
        set(v) { prefs.edit().putString("vehicle", v?.toString()).commit() }
    var btAddress: String
        get() = prefs.getString("bt", "")!!
        set(v) { prefs.edit().putString("bt", v).putBoolean("btConnected", false).putLong("btDisconnected", 0).commit() }
    var temperature: Int
        get() = prefs.getInt("temperature", 22).coerceIn(17, 31)
        set(v) { prefs.edit().putInt("temperature", v.coerceIn(17, 31)).commit() }
    var frontTyreThreshold: Int
        get() = prefs.getInt("frontTyreThreshold", 24).coerceIn(10, 40)
        set(v) { prefs.edit().putInt("frontTyreThreshold", v.coerceIn(10, 40)).commit() }
    var rearTyreThreshold: Int
        get() = prefs.getInt("rearTyreThreshold", 24).coerceIn(10, 40)
        set(v) { prefs.edit().putInt("rearTyreThreshold", v.coerceIn(10, 40)).commit() }
    val frontTyreBar get() = frontTyreThreshold / 10.0
    val rearTyreBar get() = rearTyreThreshold / 10.0
    val isReady get() = demo || vehicle != null
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("mg4-account", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("mg4-account", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun account(): JSONObject? {
        val raw = prefs.getString("account", null) ?: return null
        val parts = raw.split(':')
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])))
        return JSONObject(cipher.doFinal(Base64.getDecoder().decode(parts[1])).toString(Charsets.UTF_8))
    }
    fun saveAccount(account: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encode = Base64.getEncoder()
        val data = encode.encodeToString(cipher.iv) + ":" + encode.encodeToString(cipher.doFinal(account.toString().toByteArray(Charsets.UTF_8)))
        check(prefs.edit().putString("account", data).commit())
    }
    fun clearAccount() {
        prefs.edit().remove("account").remove("vehicle").remove("snapshot").remove("demo")
            .remove("diagnostics").remove("diagnosticsRunning").remove("btConnected").remove("btDisconnected").remove("authBlocked").remove("climatePending").commit()
    }
    var interval: Int
        get() = prefs.getInt("interval", 60).let { if (it == 0) 0 else it.coerceIn(15, 60) }
        set(v) { require(v == 0 || v in 15..60); prefs.edit().putInt("interval", v).commit() }
    fun flag(key: String, default: Boolean = false) = prefs.getBoolean(key, default)
    fun setFlag(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).commit() }
    var region: Region
        get() = runCatching { Region.valueOf(prefs.getString("region", "EU")!!) }.getOrDefault(Region.EU)
        set(v) { prefs.edit().putString("region", v.name).commit() }
    var selectedFields: List<Field>
        get() {
            val raw = prefs.getString("selectedFields", null)
                ?: prefs.all.entries.firstOrNull { it.key.startsWith("fields_") && ((it.value as? String)?.split(',')?.size ?: 0) > 4 }?.value as? String
            return Tiles.normalize(raw?.split(',')?.mapNotNull { runCatching { Field.valueOf(it) }.getOrNull() }?.distinct()
                ?: listOf(Field.SOC, Field.RANGE, Field.INSIDE, Field.OUTSIDE, Field.LOCKED, Field.CLIMATE,
                    Field.MILEAGE, Field.AUX, Field.CHARGING, Field.TARGET))
        }
        set(v) { prefs.edit().putString("selectedFields", Tiles.normalize(v).joinToString(",") { it.name }).commit() }
    fun fields(widgetId: Int, kind: String) = selectedFields
    fun saveFields(id: Int, fields: List<Field>) { selectedFields = fields }
    val profile get() = HeatProfile(flag("heatCabin", true), temperature, flag("defrost"),
        if (flag("driverHeat")) prefs.getInt("driverLevel", 1) else null,
        if (flag("passengerHeat")) prefs.getInt("passengerLevel", 1) else null, flag("rhd"))
    companion object { fun limit(kind: String) = Field.entries.size }
}
