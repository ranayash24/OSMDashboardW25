package de.storchp.opentracks.osmplugin

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import de.storchp.opentracks.osmplugin.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    // New function which was cloned
    companion object {
        fun applyInsets(view: View) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<MarginLayoutParams> {
                    bottomMargin = insets.bottom
                    topMargin = insets.top
                }
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        // Calling the function applyInsets() from this file
        applyInsets(binding.root)

        setContentView(binding.getRoot())

        binding.usageInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.osmInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.offlineMaps.movementMethod = LinkMovementMethod.getInstance()
        binding.versionInfo.text = Html.fromHtml(
            getString(
                R.string.version_info,
                BuildConfig.BUILD_TYPE,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            ), Html.FROM_HTML_MODE_COMPACT
        )

        @Suppress("KotlinConstantConditions")
        if (BuildConfig.offline) {
            binding.offlineVersionInfo.visibility = View.VISIBLE
            binding.offlineMapInfo.visibility = View.GONE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

}
