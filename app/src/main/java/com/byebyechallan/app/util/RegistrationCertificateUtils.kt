package com.byebyechallan.app.util

object RegistrationCertificateUtils {

    fun isRegistrationCertificate(docName: String?): Boolean {
        val normalized = docName?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return normalized.contains("registration certificate") ||
            normalized.contains("registration cert") ||
            normalized == "rc"
    }
}
