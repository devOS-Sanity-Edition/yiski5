package one.devos.yiski

import kotlinx.serialization.Serializable

@Serializable
    data class YiskiConfig(
        val bot: BotConfig,
        val channels: ChannelConfig,
    ) {
        @Serializable
        data class BotConfig(
            val token: String,
            val activity: String = "LISTENING",
            val status: String = "Screams of the innocent",
            val timezone: String = "America/Los_Angeles",
            val resetInterval: Long = 24,
            val initialResetHour: Long = 0,
            val initialResetMinute: Long = 0
        )

        @Serializable
        data class ChannelConfig(
            val vent: String,
            val ventLog: String,
            val ventAttachments: String
        )
    }