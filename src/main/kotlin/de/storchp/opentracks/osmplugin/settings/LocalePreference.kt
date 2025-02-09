package de.storchp.opentracks.osmplugin.settings

import android.app.LocaleConfig
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import de.storchp.opentracks.osmplugin.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

private val TAG = LocalePreference::class.java.getSimpleName()

class LocalePreference : ListPreference {

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(context)
    }

    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    @Suppress("unused")
    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        isPersistent = false

        val systemDefaultLocale =
            LocaleItem("", context.getString(R.string.settings_locale_system_default))

        val currentLocale = LocaleItem(
            Locale.getDefault().toLanguageTag(),
            Locale.getDefault().displayName
        )

        val supportedLocales = AppCompatDelegate.getApplicationLocales()


        val localeItemsSorted = buildList {
            for (i in 0 until supportedLocales.size()) {
                val current = supportedLocales.get(i)
                if (current.toLanguageTag() != currentLocale.languageTag) {
                    add(LocaleItem(current.toLanguageTag(), current.displayName))
                }
            }
        }.sortedBy { it.displayName }

        val localeItemList = buildList {
            add(systemDefaultLocale)
            add(currentLocale)
            addAll(localeItemsSorted)
        }

        entries = localeItemList.map { it.displayName }.toTypedArray()
        entryValues = localeItemList.map { it.languageTag }.toTypedArray()

        if (AppCompatDelegate.getApplicationLocales() == LocaleListCompat.getEmptyLocaleList()) {
            setValue(systemDefaultLocale.languageTag)
        } else {
            setValue(currentLocale.languageTag)
        }
    }

    override fun setOnPreferenceChangeListener(onPreferenceChangeListener: OnPreferenceChangeListener?) {
        super.onPreferenceChangeListener = onPreferenceChangeListener
    }

    override fun callChangeListener(newValue: Any): Boolean {
        var newLocale = LocaleListCompat.getEmptyLocaleList()
        if (newValue != "") {
            newLocale = LocaleListCompat.forLanguageTags(newValue as String)
        }
        AppCompatDelegate.setApplicationLocales(newLocale)
        return super.callChangeListener(newValue)
    }

    data class LocaleItem(val languageTag: String, val displayName: String)

    // TODO Get this functionality from any Androidx compat library: should be in LocaleListCompat or LocaleManagerCompat
    // 2024-12-12: on Android 14-: LocaleManagerCompat.getApplicationLocales(getContext()) returned "[]"
    // See: https://stackoverflow.com/questions/78116375/per-app-language-preferences-get-list-of-apps-available-language-programmatic
    private fun getLocaleListCompat(): LocaleListCompat {
        return AppCompatDelegate.getApplicationLocales()
    }
}
