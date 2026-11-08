package pl.art.mg4widget

import android.app.Activity
import android.content.Context
import android.widget.CheckBox

object RiskConsent {
    const val VERSION = 1
    const val REQUIRED = "Otwórz aplikację i zaakceptuj ryzyko korzystania."
    val pl = """
        To niezależna aplikacja testowa, oparta na nieoficjalnym opisie API iSMART. Nie jest produktem MG/SAIC.

        Działanie zależy od usług MG/SAIC. Dostawca może w każdej chwili zmienić API lub odciąć dostęp. Autor nie gwarantuje poprawności ani ciągłości działania. Możliwe są błędne lub opóźnione dane, nieprawidłowe działanie poleceń oraz zablokowanie konta iSMART.

        Używaj dodatkowego konta iSMART z udostępnionym autem. Konto główne może powodować wzajemne wylogowywanie i konflikt z aplikacją producenta.

        APK jest instalowany spoza Google Play. Akceptujesz ryzyko takiej instalacji oraz używania aplikacji i łączenia jej z kontem iSMART. Częste odczyty mogą budzić auto i zwiększać zużycie baterii 12 V.

        Korzystasz na własne ryzyko. W zakresie dozwolonym przez prawo autor wyłącza odpowiedzialność za szkody i inne skutki używania aplikacji. Nie ogranicza to praw, których zgodnie z prawem nie można wyłączyć.
    """.trimIndent()
    val en = """
        This independent test app uses an unofficial description of the iSMART API. It is not an MG/SAIC product.

        Operation depends on MG/SAIC services. The provider may change the API or cut off access at any time. The author does not guarantee correct or continuous operation. Risks include incorrect or delayed data, malfunctioning commands and suspension or blocking of your iSMART account.

        Use an additional iSMART account with vehicle sharing. Using the main account may cause conflicting sessions and mutual sign-outs with the manufacturer's app.

        This APK is installed outside Google Play. You accept the risks of this installation, using the app and connecting it to your iSMART account. Frequent requests may wake the car and increase 12 V battery drain.

        Use is at your own risk. To the extent permitted by law, the author disclaims liability for damage and other consequences of using the app. This does not exclude rights or liabilities that cannot legally be excluded.
    """.trimIndent()
    fun accepted(context: Context) = Store(context).prefs.getInt("riskConsentVersion", 0) == VERSION
    fun requireAccepted(context: Context) { check(accepted(context)) { REQUIRED } }
    fun show(activity: Activity, done: () -> Unit) = with(activity) {
        val page = page()
        heading(page, "Zanim zaczniesz / Before you start")
        val english = !Language.polish
        val body = text(page, if (english) en else pl, 14f)
        toggle(page, "English", english) { body.text = if (it) en else pl }
        val accept = CheckBox(this).apply {
            text = if (Language.polish) "Akceptuję ryzyko" else "I accept the risks"
            setTextColor(Palette.text); minHeight = dp(48)
        }
        page.addView(accept)
        val proceed = button(page, if (Language.polish) "Przejdź do aplikacji" else "Continue", true) {
            if (accept.isChecked && Store(this).prefs.edit().putInt("riskConsentVersion", VERSION)
                    .putLong("riskConsentAt", System.currentTimeMillis()).commit()) {
                RefreshScheduler.start(this)
                done()
            }
        }.apply { isEnabled = false; alpha = 0.4f }
        accept.setOnCheckedChangeListener { _, checked -> proceed.isEnabled = checked; proceed.alpha = if (checked) 1f else 0.4f }
        button(page, "Zamknij / Exit") { finish() }
    }
}
