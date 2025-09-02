package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class JTDemo : JavaPlugin() {

    lateinit var demoManager: DemoManager
    private var lastModified: Long = 0
    private var watcherTask: Int = -1

    override fun onEnable() {
        // Сохраняем конфигурацию по умолчанию, если она не существует
        saveDefaultConfig()

        // Инициализируем менеджер демо-режима
        demoManager = DemoManager(this)

        // Регистрируем обработчик событий
        server.pluginManager.registerEvents(DemoListener(demoManager), this)

        // Регистрируем обработчик команд и автодополнение
        getCommand("jtdemo")?.let { command ->
            val commandExecutor = DemoCommands(demoManager)
            command.setExecutor(commandExecutor)
            command.tabCompleter = DemoTabCompleter(demoManager)
        }

        // Настраиваем наблюдение за файлом данных
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
        logger.info("JTDemo плагин выключен!")
    }

    /**
     * Настраивает наблюдение за файлом данных
     */
    private fun setupFileWatcher() {
        val dataFile = File(dataFolder, "data.yml")

        // Если файл не существует, создаем его
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }

        // Сохраняем текущее время модификации
        lastModified = dataFile.lastModified()

        // Запускаем периодическую проверку файла каждые 5 секунд вместо чтения из конфига
        val checkInterval = 5L // Проверка каждые 5 секунд
        watcherTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, {
            checkDataFileChanges()
        }, 20L * checkInterval, 20L * checkInterval)

        logger.info("Настроено сохранение data.yml каждые $checkInterval секунд")
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
}