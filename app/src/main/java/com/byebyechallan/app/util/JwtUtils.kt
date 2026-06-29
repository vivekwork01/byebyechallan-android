package com.byebyechallan.app.util

import android.util.Base64
import org.json.JSONObject

/**
 * IMPORTANT CONTEXT FOR BACKEND TEAM:
 * AuthResponse from POST /api/v1/auth/login only returns {token, refreshToken, message}.
 * There is no explicit "userId" field, but every other endpoint in the API
 * (profile, document) requires a userId path parameter.
 *
 * This utility decodes the JWT payload client-side to pull out the user id,
 * assuming the backend puts it in a claim called "userId" or "sub" (subject).
 *
 * THIS IS A WORKAROUND, NOT THE IDEAL SOLUTION. Please confirm with backend:
 * 1. What claim name actually holds the user id in the JWT, OR
 * 2. Better: add a "userId" field directly to AuthResponse so the app doesn't
 *    need to parse the token at all. This is the recommended fix.
 *
 * Until then, this code tries a few common claim names and falls back gracefully.
 */
object JwtUtils {

    fun extractUserId(jwtToken: String): Long? {
        return try {
            val parts = jwtToken.split(".")
            if (parts.size < 2) return null

            val payloadJson = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
            )
            val json = JSONObject(payloadJson)

            // Try the most likely claim names in order
            when {
                json.has("userId") -> json.optLong("userId")
                json.has("id") -> json.optLong("id")
                json.has("sub") -> json.optString("sub").toLongOrNull()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
