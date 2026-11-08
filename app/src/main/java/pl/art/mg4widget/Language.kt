package pl.art.mg4widget

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Resources
import android.widget.RemoteViews

/** Local presentation translations, including cached messages from earlier releases. */
object Language {
    private var prefs: SharedPreferences? = null
    private var translations: Map<String, String> = emptyMap()
    private var pattern: Regex? = null
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("mg4", Context.MODE_PRIVATE)
        translations = context.assets.open("translations-en.tsv").bufferedReader().useLines { lines ->
            lines.filter { it.contains('|') }.associate { it.substringBefore('|') to it.substringAfter('|') }
        }
        pattern = translations.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }.toRegex()
    }
    fun resolve(choice: String, system: String) = if (choice == "pl" || (choice == "auto" && system == "pl")) "pl" else "en"
    val code get() = if (prefs == null) "pl" else resolve(prefs!!.getString("language", "auto")!!, Resources.getSystem().configuration.locales[0].language)
    val polish get() = code == "pl"
    val locale get() = java.util.Locale.forLanguageTag(code)
    fun text(value: CharSequence?): String = if (polish) value?.toString().orEmpty() else
        pattern?.replace(value?.toString().orEmpty()) { translations[it.value] ?: it.value } ?: value?.toString().orEmpty()
    fun choose(context: Context, changed: () -> Unit) {
        val keys = listOf("auto", "pl", "en")
        android.app.AlertDialog.Builder(context).setTitle("Język / Language")
            .setSingleChoiceItems(arrayOf("Automatycznie / Automatic", "Polski", "English"), keys.indexOf(prefs?.getString("language", "auto"))) { dialog, index ->
                prefs!!.edit().putString("language", keys[index]).commit()
                Widgets.updateAll(context); dialog.dismiss(); changed()
            }.setNegativeButton(if (polish) "Zamknij" else "Close", null).show()
    }
}
fun RemoteViews.setLocalizedText(id: Int, text: CharSequence?) = setTextViewText(id, Language.text(text))
class LocalizedDialog(context: Context) : android.app.AlertDialog.Builder(context) {
    override fun setTitle(title: CharSequence?): LocalizedDialog { super.setTitle(Language.text(title)); return this }
    override fun setMessage(message: CharSequence?): LocalizedDialog { super.setMessage(Language.text(message)); return this }
    override fun setPositiveButton(text: CharSequence?, listener: DialogInterface.OnClickListener?): LocalizedDialog { super.setPositiveButton(Language.text(text), listener); return this }
    override fun setNegativeButton(text: CharSequence?, listener: DialogInterface.OnClickListener?): LocalizedDialog { super.setNegativeButton(Language.text(text), listener); return this }
    override fun setItems(items: Array<out CharSequence>?, listener: DialogInterface.OnClickListener?): LocalizedDialog { super.setItems(items?.map { Language.text(it) }?.toTypedArray(), listener); return this }
    override fun setSingleChoiceItems(items: Array<out CharSequence>?, checkedItem: Int, listener: DialogInterface.OnClickListener?): LocalizedDialog { super.setSingleChoiceItems(items?.map { Language.text(it) }?.toTypedArray(), checkedItem, listener); return this }
}

class LanguageReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action == android.content.Intent.ACTION_LOCALE_CHANGED) Widgets.updateAll(context)
    }
}
