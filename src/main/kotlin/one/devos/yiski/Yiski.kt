package one.devos.yiski

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.interactions.components.*
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.*
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object Yiski {
    val logger: Logger = LoggerFactory.getLogger(Yiski::class.java)
    private val version = this::class.java.`package`.implementationVersion ?: "DEV"
    private val config = Config.loadConfig()

    private val timezone: ZoneId = ZoneId.of(config.bot.timezone)

    private val json = Json { this.prettyPrint = true }

    private lateinit var jda: JDA

    // Calculates the next midnight as a getter.
    private val midnight: Date
        get() {
            val timeNow = LocalDate.now()
            val midnight = LocalDateTime.of(timeNow.plusDays(1), LocalTime.of(config.bot.initialResetHour.toInt(), config.bot.initialResetMinute.toInt()))
            return Date.from(midnight.atZone(timezone).toInstant())
        }

    private val dateNow: Date
        get() = Date.from(Instant.now())

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        logger.info("Starting DevOS: Satanic Edition - Yiski v$version")

        jda = light(config.bot.token, enableCoroutines = true) {
            intents += listOf(GatewayIntent.MESSAGE_CONTENT)
            setActivity(Activity.of(Activity.ActivityType.valueOf(config.bot.activity.uppercase()), config.bot.status))
        }

        // Ready Event
        jda.listener<ReadyEvent> {
            // Update the commands on the server every start, just to make sure the command exists
            jda.updateCommands {
                this.addCommands(
                    CommandData.fromData(
                        Command("reset-vent", "Resets the vent channel") {
                            this.isGuildOnly = true
                            this.defaultPermissions = DefaultMemberPermissions.DISABLED
                        }.toData()
                    )
                )
            }.await()

            logger.info("Logged in and ready!")

            // Channel wiper timer
            fixedRateTimer("Channel-Wiper", true, midnight, config.bot.resetInterval.hours.inWholeMilliseconds) {
                launch(Dispatchers.Default) {
                    logger.info("Vent channel wipe initiated at $dateNow")
                    clearVentChannel()
                }
            }

            logger.info("Vent channel is set to wipe on $midnight")
        }

        // Command to manually reset the channel
        jda.onCommand("reset-vent", timeout = 2.minutes) { event ->
            val now = Instant.now()
            event.replyModal(
                Modal("$now:reset_modal", "You are about to manually reset the vent?") {
                    short("$now:reset_modal_confirmation", "Are you absolutely sure?", true, placeholder = "yes")
                }
            ).timeout(1, TimeUnit.MINUTES).await()

            withTimeoutOrNull(1.minutes) {
                jda.listener<ModalInteractionEvent> { modal ->
                    if (modal.modalId != "$now:reset_modal") return@listener
                    val message = modal.deferReply(true).await()

                    if (modal.getValue("$now:reset_modal_confirmation")?.asString?.lowercase() == "yes") {
                        message.editMessage(content = "Vent clearing in progress...").await()
                        logger.info("Vent channel has been manually wiped by ${event.user.asTag} (${event.user.id}) at $dateNow")
                        clearVentChannel()
                        message.editMessage(content = "Vent cleared. \uD83D\uDE38").await()
                    } else {
                        message.editMessage(content = "Vent reset canceled.").await()
                    }
                }
            } ?: event.hook.editMessage(content = "Timed out.", components = emptyList()).await()
        }
    }

    private suspend fun clearVentChannel() {
        val ventChannel = jda.getTextChannelById(config.channels.vent) ?: return logger.error("Vent channel <${config.channels.vent}> doesn't exist.")
        val ventLogChannel = jda.getTextChannelById(config.channels.ventLog) ?: return logger.error("Vent log channel <${config.channels.ventLog}> doesn't exist.")
        val ventAttachmentChannel = jda.getTextChannelById(config.channels.ventAttachments) ?: return logger.error("Vent attachment channel <${config.channels.ventAttachments}> doesn't exist.")
        val history = mutableListOf<Message>()
        val getHistory = ventChannel.getHistoryFromBeginning(100).await()

        history.addAll(getHistory.retrievedHistory)
        history.addAll(getHistory.retrieveFuture(100).await())

        val collectedHistory = history
            .filterNot { it.isPinned }
            .filterNot { it.isWebhookMessage }
            .asReversed()

        // return if the channelHistory is empty, in consequence this will not trigger a log.
        if (collectedHistory.isEmpty()) return

        val serializedHistory = collectedHistory.map {
            SerializedHistory.SerializedMessage(
                messageId = it.idLong,
                authorId = it.author.idLong,
                authorTag = it.author.asTag,
                content = it.contentRaw,
                embeds = it.embeds.count(),
                attachments = it.attachments.count()
            )
        }

        val data = SerializedHistory(
            data = SerializedHistory.SerializedData(
                channelName = ventChannel.name,
                channelId = ventChannel.idLong,
                serverId = ventChannel.guild.idLong,
                messageCount = collectedHistory.count(),
                date = LocalDate.now(timezone).toString()
            ),
            messages = serializedHistory
        )

        val encodedData: String = json.encodeToString(SerializedHistory.serializer(), data)

        val messageAttachments = collectedHistory.filter { it.attachments.size > 0 }.map { message ->
            message to message.attachments
                .filter { it.size <= ventAttachmentChannel.guild.boostTier.maxFileSize }
                .map { it.fileName to it.proxy.download().await() }
        }

        messageAttachments.forEach { (message, attachments) ->
            ventAttachmentChannel.send(
                embeds = listOf(Embed {
                    author {
                        name = "${message.author.asTag} <${message.author.id}>"
                        iconUrl = message.author.effectiveAvatarUrl
                    }
                    title = "Message ID: ${message.id}"
                    if (attachments.isEmpty()) description = "File was either too large to send in this server or was deleted during purge."
                }),
                files = attachments.map { (name, file) -> FileUpload.fromData(file, name) }
            ).await()
        }

        val formattedDate = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())

        ventLogChannel.send(
            content = "Vent log for $formattedDate",
            files = listOf(FileUpload.fromData(encodedData.encodeToByteArray(), "$formattedDate.json"))

        ).await()

        try {
            val messagesOverTwoWeeks = collectedHistory.filter { it.timeCreated.isBefore(OffsetDateTime.now().minusDays(14)) }
            val messages = collectedHistory.filterNot { it.timeCreated.isBefore(OffsetDateTime.now().minusDays(14)) }

            if (messages.size > 1) {
                for (messagesChunk in messages.chunked(200)) {
                    ventChannel.deleteMessages(messagesChunk).await()
                }
            } else {
                ventChannel.deleteMessageById(messages.first().id).await()
            }

            if (messagesOverTwoWeeks.isNotEmpty()) {
                ventLogChannel.send(embeds = listOf(
                    Embed {
                        title = "Admin intervention required"
                        description = "Messages over 2 weeks have been detected in <#${config.channels.vent}>, manual deletion is required."
                    }
                )).queue()
            }
        } catch (e: Exception) {
            logger.error("A fatal error has occurred whilst trying to delete messages", e)
            ventLogChannel.send(embeds = listOf(
                Embed {
                    title = "Developer intervention required"
                    description = "Something has gone horrible wrong, please check the logs"
                }
            )).queue()
        }
    }
}