package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.jtdemo.commands.base.CommandManager
import java.io.File

class JTDemo : JavaPlugin() {

    lateinit var demoManager: DemoManager
    lateinit var warpManager: WarpManager
    private var lastModified: Long = 0
    private var lastWarpsModified: Long = 0
    private var watcherTask: Int = -1

    override fun onEnable() {
        // Сохраняем конфиг, если его нет
        saveDefaultConfig()

        // Создаем менеджеры
        demoManager = DemoManager(this)
        warpManager = WarpManager(this)

        // Регистрируем обработчик событий
        server.pluginManager.registerEvents(DemoListener(demoManager), this)

        // Регистрируем команды и автодополнение
        getCommand("jtdemo")?.let { command ->
            // Исправлено: передаем this как первый параметр
            val commandManager = CommandManager(this, demoManager, warpManager)
            command.setExecutor(commandManager)
            command.tabCompleter = DemoTabCompleter(demoManager)
        }

        // Запускаем проверку файлов
        setupFileWatcher()

        logger.info("JTDemo плагин включен!")
    }

    override fun onDisable() {
        // Останавливаем проверку файлов
        if (watcherTask != -1) {
            Bukkit.getScheduler().cancelTask(watcherTask)
        }

        // Сохраняем данные
        demoManager.saveData()
        warpManager.saveWarps()
        logger.info("JTDemo плагин выключен!")
    }

    /**
     * Настраивает проверку файлов конфигурации
     */
    private fun setupFileWatcher() {
        val dataFile = File(dataFolder, "data.yml")
        val warpsFile = File(dataFolder, "warps.yml")

        // Создаем файлы, если их нет
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }

        if (!warpsFile.exists()) {
            warpsFile.parentFile.mkdirs()
            warpsFile.createNewFile()
        }

        // Сохраняем время изменения
        lastModified = dataFile.lastModified()
        lastWarpsModified = warpsFile.lastModified()

        // Запускаем проверку каждые 5 секунд
        val checkInterval = 5L
        watcherTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, {
            checkDataFileChanges()
            checkWarpsFileChanges()
        }, 20L * checkInterval, 20L * checkInterval)

        logger.info("Проверка файлов настроена: каждые $checkInterval секунд")
    }

    /**
     * Проверяет изменения в файле данных
     */
    private fun checkDataFileChanges() {
        val dataFile = File(dataFolder, "data.yml")

        // Обновляем lastModified без проверки изменений
        // Для применения изменений используйте /jtdemo reload
        if (dataFile.exists()) {
            lastModified = dataFile.lastModified()
        }
    }

    /**
     * Проверяет изменения в файле варпов
     */
    private fun checkWarpsFileChanges() {
        val warpsFile = File(dataFolder, "warps.yml")

        if (warpsFile.exists() && warpsFile.lastModified() > lastWarpsModified) {
            logger.info("Файл warps.yml изменен, перезагружаем данные...")
            lastWarpsModified = warpsFile.lastModified()
            warpManager.loadWarps()
        }
    }
}