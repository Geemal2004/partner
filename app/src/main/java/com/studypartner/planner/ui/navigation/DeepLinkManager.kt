package com.studypartner.planner.ui.navigation

import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeepLinkManager {

    companion object {
        const val KEY_PENDING_DEEP_LINK_URI = "pending_deep_link_uri"
    }

    private val _pendingRoute = MutableStateFlow<AppRoute?>(null)
    val pendingRoute: StateFlow<AppRoute?> = _pendingRoute.asStateFlow()

    var rawPendingUri: String? = null
        private set

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_VIEW) {
            val dataString = intent.dataString ?: intent.data?.toString()
            if (dataString != null) {
                val parsed = DeepLinkParser.parse(dataString)
                if (parsed != null) {
                    rawPendingUri = dataString
                    _pendingRoute.value = parsed
                }
            }
        }
    }

    fun consumePendingRoute(): AppRoute? {
        val current = _pendingRoute.value
        if (current != null) {
            _pendingRoute.value = null
            rawPendingUri = null
        }
        return current
    }

    fun saveInstanceState(outState: Bundle) {
        if (_pendingRoute.value != null && rawPendingUri != null) {
            outState.putString(KEY_PENDING_DEEP_LINK_URI, rawPendingUri)
        }
    }

    fun restoreInstanceState(savedInstanceState: Bundle?) {
        val savedUri = savedInstanceState?.getString(KEY_PENDING_DEEP_LINK_URI)
        if (savedUri != null) {
            rawPendingUri = savedUri
            _pendingRoute.value = DeepLinkParser.parse(savedUri)
        }
    }
}
