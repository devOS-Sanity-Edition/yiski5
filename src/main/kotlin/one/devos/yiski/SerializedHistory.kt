package one.devos.yiski

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SerializedHistory(
    val data: SerializedData,
    val messages: List<SerializedMessage>
) {
    @Serializable
    data class SerializedData(
        @SerialName("name")
        val channelName: String,
        @SerialName("id")
        val channelId: Long,
        @SerialName("server-id")
        val serverId: Long,
        @SerialName("messages")
        val messageCount: Int,
        val date: String
    )

    @Serializable
    data class SerializedMessage(
        @SerialName("message-id")
        val messageId: Long,
        @SerialName("author-id")
        val authorId: Long,
        @SerialName("author-readable")
        val authorTag: String,
        val content: String,
        val embeds: Int,
        val attachments: Int,
    )
}