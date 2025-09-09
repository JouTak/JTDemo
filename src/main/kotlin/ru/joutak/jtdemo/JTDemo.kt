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
        // Сохраняем конфигурацию по умолчанию, если она не существует
        saveDefaultConfig()

        // Инициализируем менеджеры
        demoManager = DemoManager(this)
        warpManager = WarpManager(this)

        // Регистрируем обработчик событий
        server.pluginManager.registerEvents(DemoListener(demoManager), this)

        // Регистрируем новый обработчик команд и автодополнение
        getCommand("jtdemo")?.let { command ->
            // Используем новый менеджер команд вместо DemoCommands
            val commandManager = CommandManager(demoManager, warpManager)
            command.setExecutor(commandManager)

            // Для TabCompleter оставляем прежнюю реализацию
            // В будущем можно создать соответствующий TabCompleter для новой системы команд
            command.tabCompleter = DemoTabCompleter(demoManager)
        }

        // Настраиваем наблюдение за файлами
        setupFileWatcher()

        logger.info("JTDemo плагин включен!")
    }

    override fun onDisable() {
        // Отменяем задачу проверки файла
        if (watcherTask != -1) {
            Bukkit.getScheduler().cancelTask(watcherTask)
        }

        // Сохраняем все данные
        demoManager.saveData()
        warpManager.saveWarps()
        logger.info("JTDemo плагин выключен!")
    }

    /**
     * Настраивает наблюдение за файлами данных
     */
    private fun setupFileWatcher() {
        val dataFile = File(dataFolder, "data.yml")
        val warpsFile = File(dataFolder, "warps.yml")

        // Если файлы не существуют, создаем их
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }

        if (!warpsFile.exists()) {
            warpsFile.parentFile.mkdirs()
            warpsFile.createNewFile()
        }

        // Сохраняем текущее время модификации
        lastModified = dataFile.lastModified()
        lastWarpsModified = warpsFile.lastModified()

        // Запускаем периодическую проверку файлов каждые 5 секунд
        val checkInterval = 5L // Проверка каждые 5 секунд
        watcherTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, {
            checkDataFileChanges()
            checkWarpsFileChanges()
        }, 20L * checkInterval, 20L * checkInterval)

        logger.info("Настроено наблюдение за файлами данных каждые $checkInterval секунд")
    }

    /**
     * Проверяет, был ли изменен файл данных
     */
    private fun checkDataFileChanges() {
        val dataFile = File(dataFolder, "data.yml")

        // Проверка изменений закомментирована, для применения изменений используйте /jtdemo reload
        /*
        if (dataFile.exists() && dataFile.lastModified() > lastModified) {
            logger.info("Обнаружены изменения в файле data.yml, перезагружаем данные...")
            lastModified = dataFile.lastModified()

            // Перезагружаем данные
            demoManager.reload()

            // Обновляем статус всех онлайн игроков
            Bukkit.getOnlinePlayers().forEach { player ->
                demoManager.updatePlayerDemoStatus(player)
            }
        }
        */

        // Обновляем lastModified без проверки
        if (dataFile.exists()) {
            lastModified = dataFile.lastModified()
        }
    }

    /**
     * Проверяет, был ли изменен файл варпов
     */
    private fun checkWarpsFileChanges() {
        val warpsFile = File(dataFolder, "warps.yml")

        if (warpsFile.exists() && warpsFile.lastModified() > lastWarpsModified) {
            logger.info("Обнаружены изменения в файле warps.yml, перезагружаем данные...")
            lastWarpsModified = warpsFile.lastModified()

            // Перезагружаем варпы
            warpManager.loadWarps()
        }
    }
}