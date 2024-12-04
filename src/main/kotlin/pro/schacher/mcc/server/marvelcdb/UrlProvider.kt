package pro.schacher.mcc.server.marvelcdb

import java.util.Locale

class UrlProvider {

    private companion object {
        const val SERVICE_URL_DEFAULT = "https://marvelcdb.com"
        const val SERVICE_URL_GERMAN = "https://de.marvelcdb.com"
    }

    fun getUrl(locale: Locale? = null): String = when (locale) {
        Locale.GERMAN -> SERVICE_URL_GERMAN
        else -> SERVICE_URL_DEFAULT
    }
}