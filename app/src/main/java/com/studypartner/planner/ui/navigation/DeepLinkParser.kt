package com.studypartner.planner.ui.navigation

import java.net.URI

object DeepLinkParser {
    /**
     * Parses a deep link URI string into an AppRoute destination.
     * Expected URIs:
     * - studypartner://join/{code} -> JoinGroup(code)
     * - studypartner://event/{id}  -> EventDetail(id)
     * - studypartner://task/{id}   -> TaskDetail(id)
     */
    fun parse(uriString: String?): AppRoute? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = URI.create(uriString)
            if (uri.scheme != "studypartner") return null

            val host = uri.host
            val path = uri.path?.removePrefix("/") ?: ""
            val segments = path.split("/").filter { it.isNotBlank() }

            when {
                host == "join" || (host == null && segments.firstOrNull() == "join") -> {
                    val code = if (host == "join") {
                        segments.firstOrNull() ?: extractFromSchemeSpecific(uri, "join")
                    } else segments.getOrNull(1)
                    if (!code.isNullOrBlank()) JoinGroup(code) else null
                }
                host == "event" || (host == null && segments.firstOrNull() == "event") -> {
                    val id = if (host == "event") {
                        segments.firstOrNull() ?: extractFromSchemeSpecific(uri, "event")
                    } else segments.getOrNull(1)
                    if (!id.isNullOrBlank()) EventDetail(id) else null
                }
                host == "task" || (host == null && segments.firstOrNull() == "task") -> {
                    val id = if (host == "task") {
                        segments.firstOrNull() ?: extractFromSchemeSpecific(uri, "task")
                    } else segments.getOrNull(1)
                    if (!id.isNullOrBlank()) TaskDetail(id) else null
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractFromSchemeSpecific(uri: URI, keyword: String): String? {
        val ssp = uri.rawSchemeSpecificPart
        val prefix = "//$keyword/"
        return if (ssp.startsWith(prefix)) {
            ssp.removePrefix(prefix).trim('/')
        } else null
    }
}
