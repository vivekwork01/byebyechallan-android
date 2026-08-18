package com.byebyechallan.app.data.model

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val RECIPIENT_SEPARATOR = ","

data class NotificationRecipientState(
    val emailEnabled: Boolean = false,
    val emailRecipients: List<String> = emptyList(),
    val whatsAppEnabled: Boolean = false,
    val whatsAppRecipients: List<String> = emptyList(),
    val smsEnabled: Boolean = false,
    val smsRecipients: List<String> = emptyList()
) {
    fun toRecipientsMap(): Map<NotificationChannel, String> {
        val map = mutableMapOf<NotificationChannel, String>()
        if (emailEnabled) {
            map[NotificationChannel.EMAIL] = joinRecipients(emailRecipients)
        }
        if (whatsAppEnabled) {
            map[NotificationChannel.WHATSAPP] = joinRecipients(whatsAppRecipients)
        }
        if (smsEnabled) {
            map[NotificationChannel.SMS] = joinRecipients(smsRecipients)
        }
        return map
    }

    fun isChannelEnabled(channel: NotificationChannel): Boolean = when (channel) {
        NotificationChannel.EMAIL -> emailEnabled
        NotificationChannel.WHATSAPP -> whatsAppEnabled
        NotificationChannel.SMS -> smsEnabled
    }

    companion object {
        fun fromMap(
            recipients: Map<NotificationChannel, String>?,
            defaultEmail: String = "",
            defaultMobile: String = ""
        ): NotificationRecipientState {
            if (recipients.isNullOrEmpty()) {
                return defaultsFromUser(defaultEmail, defaultMobile)
            }
            val emailValues = parseRecipients(recipients[NotificationChannel.EMAIL])
            val whatsAppValues = parseRecipients(recipients[NotificationChannel.WHATSAPP])
            val smsValues = parseRecipients(recipients[NotificationChannel.SMS])
            return NotificationRecipientState(
                emailEnabled = recipients.containsKey(NotificationChannel.EMAIL),
                emailRecipients = emailValues.ifEmpty {
                    if (recipients.containsKey(NotificationChannel.EMAIL)) listOf("") else emptyList()
                },
                whatsAppEnabled = recipients.containsKey(NotificationChannel.WHATSAPP),
                whatsAppRecipients = whatsAppValues.ifEmpty {
                    if (recipients.containsKey(NotificationChannel.WHATSAPP)) listOf("") else emptyList()
                },
                smsEnabled = recipients.containsKey(NotificationChannel.SMS),
                smsRecipients = smsValues.ifEmpty {
                    if (recipients.containsKey(NotificationChannel.SMS)) listOf("") else emptyList()
                }
            )
        }

        fun fromLegacyStringMap(
            recipients: Map<String, String>?,
            defaultEmail: String = "",
            defaultMobile: String = ""
        ): NotificationRecipientState {
            if (recipients.isNullOrEmpty()) {
                return defaultsFromUser(defaultEmail, defaultMobile)
            }
            val normalized = recipients.mapKeys { (key, _) -> legacyKeyToChannel(key) }
                .filterKeys { it != null }
                .mapKeys { (key, _) -> key!! }
            return fromMap(normalized, defaultEmail, defaultMobile)
        }

        fun defaultsFromUser(email: String?, mobile: String?): NotificationRecipientState {
            val userEmail = email.orEmpty()
            val userMobile = mobile.orEmpty()
            return NotificationRecipientState(
                emailEnabled = userEmail.isNotBlank(),
                emailRecipients = if (userEmail.isNotBlank()) listOf(userEmail) else emptyList(),
                whatsAppEnabled = userMobile.isNotBlank(),
                whatsAppRecipients = if (userMobile.isNotBlank()) listOf(userMobile) else emptyList(),
                smsEnabled = userMobile.isNotBlank(),
                smsRecipients = if (userMobile.isNotBlank()) listOf(userMobile) else emptyList()
            )
        }

        fun isChannelEnabledOnProfile(
            recipients: Map<NotificationChannel, String>?,
            channel: NotificationChannel
        ): Boolean = recipients?.containsKey(channel) == true

        /** Channel is usable on document upload only when profile has at least one recipient. */
        fun isChannelAvailableOnProfile(
            recipients: Map<NotificationChannel, String>?,
            channel: NotificationChannel
        ): Boolean {
            val value = recipients?.get(channel)
            if (value.isNullOrBlank()) return false
            return parseRecipients(value).isNotEmpty()
        }

        fun normalizeStringMap(map: Map<String, String>): Map<NotificationChannel, String> =
            map.mapKeys { (key, _) -> legacyKeyToChannel(key) }
                .filterKeys { it != null }
                .mapKeys { (key, _) -> key!! }

        fun parseRecipientsJsonString(json: String): Map<NotificationChannel, String>? {
            return try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val raw: Map<String, String> = Gson().fromJson(json, type)
                normalizeStringMap(raw)
            } catch (_: Exception) {
                null
            }
        }

        private fun joinRecipients(recipients: List<String>): String =
            recipients.map { it.trim() }.filter { it.isNotBlank() }.joinToString(RECIPIENT_SEPARATOR)

        private fun parseRecipients(value: String?): List<String> {
            if (value.isNullOrBlank()) return emptyList()
            return value.split(RECIPIENT_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
        }

        private fun legacyKeyToChannel(key: String): NotificationChannel? = when (key) {
            "EMAIL", "email" -> NotificationChannel.EMAIL
            "WHATSAPP", "whatsApp", "whatsapp" -> NotificationChannel.WHATSAPP
            "SMS", "sms" -> NotificationChannel.SMS
            else -> null
        }
    }
}
