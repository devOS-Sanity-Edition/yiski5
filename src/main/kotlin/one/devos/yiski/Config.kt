package one.devos.yiski

import com.akuleshov7.ktoml.file.TomlFileReader
import kotlinx.serialization.serializer
import kotlin.system.exitProcess

object Config {
    private val configPath: String = System.getProperty("yiski_config", "config.toml")

    fun loadConfig(): YiskiConfig {
        Yiski.logger.info("Loading config from $configPath...")
        return try {
            TomlFileReader.decodeFromFile(serializer(), configPath)
        } catch (e: Exception) {
            Yiski.logger.error("Failed to load config", e)
            exitProcess(1)
        }
    }
}