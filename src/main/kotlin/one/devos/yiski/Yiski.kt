package one.devos.yiski

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.awaitButton
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.*
import java.util.Date
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
        }

        // Command to manually reset the channel
        jda.onCommand("reset-vent", timeout = 2.minutes) { event ->
            val confirm = danger("${Instant.now()}:reset", "Confirm reset")

            event.reply_(
                "Are you sure you want to **reset** the vent?",
                components = confirm.into(),
                ephemeral = true
            ).queue()

            withTimeoutOrNull(1.minutes) {
                val pressed = event.user.awaitButton(confirm)
                pressed.deferEdit().queue()

                logger.info("Vent channel has been manually wiped by ${event.user.asTag} (${event.user.id}) at $dateNow")
                clearVentChannel()
                event.hook.editMessage(content = "Vent cleared.", components = emptyList()).await()
            } ?: event.hook.editMessage(content = "Timed out.", components = emptyList()).await()
        }

        // Channel wiper timer
        fixedRateTimer("Channel-Wiper", true, midnight, config.bot.resetInterval.hours.inWholeMilliseconds) {
            launch(Dispatchers.Default) {
                logger.info("Vent channel wipe initiated at $dateNow")
                clearVentChannel()
            }
        }

        logger.info("Vent channel is set to wipe on $midnight")
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

        val attachments = collectedHistory.filter { it.attachments.size > 0 }.map { message ->
            "[${message.id}] ${message.author.asTag} <${message.author.id}>" to message.attachments.map { "${it.fileName}.${it.fileExtension}" to it.proxy.download().await() }
        }

        attachments.forEach { (author, attachments) ->
            ventAttachmentChannel.send(
                content = author,
                files = attachments.map { (name, file) ->
                    FileUpload.fromData(file, name)
                }
            ).await()
        }

        val formattedDate = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Date())

        ventLogChannel.send(
            content = "Vent log for $formattedDate",
            files = listOf(FileUpload.fromData(encodedData.encodeToByteArray(), "$formattedDate.json"))

        ).await()

        ventChannel.deleteMessages(collectedHistory).await()
    }
}