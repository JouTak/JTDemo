package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*

/**
 * JTDemo - JouTak Demo Mode Plugin
 * Обновлено: 2025-08-29 17:22:41
 * Автор: Kostyamops
 */
class DemoManager(val plugin: JTDemo) {

    private val demoPlayers = mutableSetOf<UUID>()
    private val passwords = mutableMapOf<UUID, String>()
    private var demoSpawn: Location? = null
    private val dataFile = File(plugin.dataFolder, "data.yml")
    private var dataConfig = YamlConfiguration.loadConfiguration(dataFile)
    private val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
    private var demoTeam: Team? = null
    private val forcedPlayers = mutableSetOf<String>()
    private val pendingTeleports = mutableSetOf<String>() // Игроки, ожидающие телепортации при входе
    private val tempDisabledDemo = mutableSetOf<UUID>() // Игроки с временно отключенным демо-режимом

    init {
        // Создаем директорию плагина, если она не существует
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        // Инициализируем команду для демо-режима
        setupDemoTeam()

        // Загружаем данные из конфигурации
        loadData()
    }

    /**
     * Настройка команды для игроков в демо-режиме
     */
    private fun setupDemoTeam() {
        // Получаем существующую команду или создаем новую
        demoTeam = scoreboard?.getTeam("demoPlayers") ?: scoreboard?.registerNewTeam("demoPlayers")

        // Устанавливаем префикс из конфигурации
        val prefix = plugin.getConfig().getString("settings.demo-prefix") ?: "[ДЕМО] "
        demoTeam?.prefix = prefix
    }

    /**
     * Перезагружает плагин (настройки из конфига и данные из data.yml)
     */
    fun reload() {
        // Перезагружаем конфиг
        plugin.reloadConfig()

        // Обновляем префикс для команды
        val prefix = plugin.getConfig().getString("settings.demo-prefix") ?: "[ДЕМО] "
        demoTeam?.prefix = prefix

        // Перезагружаем данные
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        loadData()

        // Обновляем префиксы для всех онлайн игроков
        Bukkit.getOnlinePlayers().forEach { player ->
            updatePlayerPrefix(player)
        }
    }

    fun loadData() {
        // Загружаем игроков в демо-режиме
        val demoPlayersList = dataConfig.getStringList("demo-players")
        demoPlayers.clear()
        demoPlayersList.forEach {
            try {
                val uuid = UUID.fromString(it)
                demoPlayers.add(uuid)

                // Добавляем игрока в демо-команду, если он онлайн
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    addPlayerToTeam(player)
                }
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Некорректный UUID в списке игроков в демо-режиме: $it")
            }
        }

        // Загружаем пароли
        val passwordSection = dataConfig.getConfigurationSection("passwords")
        passwords.clear()
        passwordSection?.getKeys(false)?.forEach { key ->
            try {
                val uuid = UUID.fromString(key)
                val password = passwordSection.getString(key)
                if (password != null) {
                    passwords[uuid] = password
                }
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Некорректный UUID в разделе паролей: $key")
            }
        }

        // Загружаем принудительных игроков
        forcedPlayers.clear()
        forcedPlayers.addAll(dataConfig.getStringList("forced-players"))

        // Загружаем ожидающих телепортации
        pendingTeleports.clear()
        pendingTeleports.addAll(dataConfig.getStringList("pending-teleports"))

        // Загружаем игроков с временно отключенным демо-режимом
        tempDisabledDemo.clear()
        val tempDisabledList = dataConfig.getStringList("temp-disabled-demo")
        tempDisabledList.forEach {
            try {
                val uuid = UUID.fromString(it)
                tempDisabledDemo.add(uuid)
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Некорректный UUID в списке временно отключенного демо-режима: $it")
            }
        }

        // Загружаем точку возрождения в демо-режиме
        if (dataConfig.contains("demo-spawn")) {
            val world = Bukkit.getWorld(dataConfig.getString("demo-spawn.world", "world") ?: "world")
            val x = dataConfig.getDouble("demo-spawn.x", 0.0)
            val y = dataConfig.getDouble("demo-spawn.y", 0.0)
            val z = dataConfig.getDouble("demo-spawn.z", 0.0)
            val yaw = dataConfig.getDouble("demo-spawn.yaw", 0.0).toFloat()
            val pitch = dataConfig.getDouble("demo-spawn.pitch", 0.0).toFloat()

            if (world != null) {
                demoSpawn = Location(world, x, y, z, yaw, pitch)
            }
        }
    }

    fun saveData() {
        // Сохраняем игроков в демо-режиме
        dataConfig.set("demo-players", demoPlayers.map { it.toString() })

        // Сохраняем пароли
        val passwordSection = dataConfig.createSection("passwords")
        passwords.forEach { (uuid, password) ->
            passwordSection.set(uuid.toString(), password)
        }

        // Сохраняем принудительных игроков
        dataConfig.set("forced-players", ArrayList(forcedPlayers))

        // Сохраняем ожидающих телепортации
        dataConfig.set("pending-teleports", ArrayList(pendingTeleports))

        // Сохраняем игроков с временно отключенным демо-режимом
        dataConfig.set("temp-disabled-demo", tempDisabledDemo.map { it.toString() })

        // Сохраняем точку возрождения в демо-режиме
        demoSpawn?.let {
            dataConfig.set("demo-spawn.world", it.world?.name)
            dataConfig.set("demo-spawn.x", it.x)
            dataConfig.set("demo-spawn.y", it.y)
            dataConfig.set("demo-spawn.z", it.z)
            dataConfig.set("demo-spawn.yaw", it.yaw)
            dataConfig.set("demo-spawn.pitch", it.pitch)
        }

        // Сохраняем в файл
        try {
            dataConfig.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("Не удалось сохранить данные демо-режима: ${e.message}")
        }
    }

    /**
     * Добавляет игрока в демо-команду
     */
    private fun addPlayerToTeam(player: Player) {
        if (plugin.getConfig().getBoolean("settings.add-demo-prefix", true)) {
            demoTeam?.addEntry(player.name)
        }
    }

    /**
     * Удаляет игрока из демо-команды
     */
    private fun removePlayerFromTeam(player: Player) {
        demoTeam?.removeEntry(player.name)
    }

    /**
     * Обновляет префикс игрока в зависимости от его статуса в демо-режиме
     * Вызывается при телепортации или изменении положения
     */
    fun updatePlayerPrefix(player: Player) {
        // Проверяем, находится ли игрок в демо-режиме и не отключен ли он временно
        if (isInDemoMode(player) && !isTemporarilyDisabled(player)) {
            addPlayerToTeam(player)
        } else {
            removePlayerFromTeam(player)
        }
    }

    fun enableDemoMode(player: Player, password: String): Boolean {
        val uuid = player.uniqueId
        passwords[uuid] = password
        demoPlayers.add(uuid)

        // Удаляем из списка временно отключенных, если игрок там был
        tempDisabledDemo.remove(uuid)

        // Добавляем игрока в демо-команду
        addPlayerToTeam(player)

        // Отправляем широковещательное сообщение, если это включено в настройках
        if (plugin.getConfig().getBoolean("settings.show-demo-messages", true)) {
            Bukkit.getOnlinePlayers().forEach { p ->
                if (p != player && p.hasPermission("jtdemo.see-messages")) {
                    p.sendMessage("§7Игрок §e${player.name}§7 вошел в демо-режим")
                }
            }
        }

        saveData()
        return true
    }

    fun disableDemoMode(player: Player, password: String): Boolean {
        val uuid = player.uniqueId
        val storedPassword = passwords[uuid]

        // Проверяем, не является ли игрок принудительным демо-игроком
        if (forcedPlayers.contains(player.name)) {
            // Для принудительного демо-режима проверяем, совпадает ли пароль с дефолтным
            val defaultPassword = plugin.getConfig().getString("settings.default-password") ?: "12345"
            if (password != defaultPassword) {
                return false
            }

            // Игрок в принудительном режиме и ввел правильный пароль
            // Добавляем его в список временно отключенных
            tempDisabledDemo.add(uuid)

            // Удаляем игрока из демо-команды
            removePlayerFromTeam(player)

            // Отправляем широковещательное сообщение, если это включено в настройках
            if (plugin.getConfig().getBoolean("settings.show-demo-messages", true)) {
                Bukkit.getOnlinePlayers().forEach { p ->
                    if (p != player && p.hasPermission("jtdemo.see-messages")) {
                        p.sendMessage("§7Игрок §e${player.name}§7 временно вышел из принудительного демо-режима")
                    }
                }
            }

            saveData()
            return true
        }

        // Для обычного демо-режима проверяем пароль
        if (storedPassword == null || storedPassword != password) {
            return false
        }

        // Для обычного демо-режима удаляем игрока из списка
        demoPlayers.remove(uuid)

        // Удаляем игрока из демо-команды
        removePlayerFromTeam(player)

        // Отправляем широковещательное сообщение, если это включено в настройках
        if (plugin.getConfig().getBoolean("settings.show-demo-messages", true)) {
            Bukkit.getOnlinePlayers().forEach { p ->
                if (p != player && p.hasPermission("jtdemo.see-messages")) {
                    p.sendMessage("§7Игрок §e${player.name}§7 вышел из демо-режима")
                }
            }
        }

        saveData()
        return true
    }

    fun resetPassword(playerName: String): Boolean {
        val targetPlayer = Bukkit.getPlayerExact(playerName)
        val uuid = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(playerName).uniqueId

        if (uuid.toString() == "00000000-0000-0000-0000-000000000000") {
            return false
        }

        // Получаем пароль по умолчанию из конфига
        val defaultPassword = plugin.getConfig().getString("settings.default-password") ?: "12345"
        passwords[uuid] = defaultPassword
        saveData()
        return true
    }

    /**
     * Добавляет игрока в список принудительного демо-режима
     */
    fun addForcedPlayer(playerName: String): Boolean {
        if (forcedPlayers.contains(playerName)) {
            return false
        }

        forcedPlayers.add(playerName)

        // Если игрок онлайн, сразу применяем к нему демо-режим
        val player = Bukkit.getPlayerExact(playerName)
        if (player != null) {
            val defaultPassword = plugin.getConfig().getString("settings.default-password") ?: "12345"
            if (!demoPlayers.contains(player.uniqueId)) {
                passwords[player.uniqueId] = defaultPassword
                demoPlayers.add(player.uniqueId)
            }

            // Удаляем из списка временно отключенных
            tempDisabledDemo.remove(player.uniqueId)

            // Добавляем префикс
            addPlayerToTeam(player)
        }

        saveData()
        return true
    }

    /**
     * Удаляет игрока из списка принудительного демо-режима
     */
    fun removeForcedPlayer(playerName: String): Boolean {
        if (!forcedPlayers.contains(playerName)) {
            return false
        }

        forcedPlayers.remove(playerName)

        // Если игрок онлайн, удаляем его из демо-режима (если у него нет личного пароля)
        val player = Bukkit.getPlayerExact(playerName)
        if (player != null && demoPlayers.contains(player.uniqueId)) {
            // Удаляем только если игрок не активировал демо-режим сам
            if (!passwords.containsKey(player.uniqueId)) {
                demoPlayers.remove(player.uniqueId)
            }

            // Удаляем из списка временно отключенных
            tempDisabledDemo.remove(player.uniqueId)

            // Обновляем префикс
            updatePlayerPrefix(player)
        }

        saveData()
        return true
    }

    /**
     * Проверяет, отключен ли демо-режим временно
     */
    fun isTemporarilyDisabled(player: Player): Boolean {
        return tempDisabledDemo.contains(player.uniqueId)
    }

    /**
     * Обновляет статус демо-режима для игрока (вызывается при изменении data.yml)
     */
    fun updatePlayerDemoStatus(player: Player) {
        val wasInDemo = demoPlayers.contains(player.uniqueId)
        val wasForced = forcedPlayers.contains(player.name)
        val wasTemporarilyDisabled = tempDisabledDemo.contains(player.uniqueId)

        // Проверяем, должен ли игрок быть в демо-режиме
        if (forcedPlayers.contains(player.name)) {
            // Игрок должен быть в принудительном демо-режиме
            if (!wasInDemo) {
                // Добавляем игрока в демо-режим
                val defaultPassword = plugin.getConfig().getString("settings.default-password") ?: "12345"
                passwords[player.uniqueId] = defaultPassword
                demoPlayers.add(player.uniqueId)

                // Сбрасываем временное отключение при входе
                tempDisabledDemo.remove(player.uniqueId)

                // Добавляем префикс
                addPlayerToTeam(player)

                // Отправляем сообщение игроку
                player.sendMessage("§cВы были добавлены в принудительный демо-режим администратором.")
            } else if (wasTemporarilyDisabled) {
                // Если игрок входит на сервер и был в списке временно отключенных,
                // восстанавливаем демо-режим
                tempDisabledDemo.remove(player.uniqueId)
                addPlayerToTeam(player)
                player.sendMessage("§cДемо-режим был восстановлен после перезахода.")
            }
        } else if (wasForced && wasInDemo) {
            // Игрок был удален из принудительного демо-режима
            // Проверяем, активировал ли он демо-режим сам или это было принудительно
            if (!passwords.containsKey(player.uniqueId)) {
                // Удаляем игрока из демо-режима
                demoPlayers.remove(player.uniqueId)
                tempDisabledDemo.remove(player.uniqueId)
                removePlayerFromTeam(player)

                // Отправляем сообщение игроку
                player.sendMessage("§aВы были удалены из принудительного демо-режима администратором.")
            }
        }

        // Обновляем префикс
        updatePlayerPrefix(player)

        // Проверяем, есть ли игрок в списке ожидающих телепортации
        if (pendingTeleports.contains(player.name)) {
            teleportPlayerToDemoSpawn(player)
            pendingTeleports.remove(player.name)
            saveData()
        }
    }

    fun isInDemoMode(player: Player): Boolean {
        // Проверяем, не находится ли игрок в списке принудительных демо-игроков
        if (forcedPlayers.contains(player.name)) {
            // Если да, но у него нет пароля, устанавливаем пароль по умолчанию
            if (!passwords.containsKey(player.uniqueId)) {
                val defaultPassword = plugin.getConfig().getString("settings.default-password") ?: "12345"
                passwords[player.uniqueId] = defaultPassword
                demoPlayers.add(player.uniqueId)
                saveData()
            }
            return true
        }

        return demoPlayers.contains(player.uniqueId)
    }

    /**
     * Проверяет, должен ли игрок иметь ограничения демо-режима
     * (учитывает как статус демо-режима, так и временное отключение)
     */
    fun shouldHaveDemoRestrictions(player: Player): Boolean {
        return isInDemoMode(player) && !isTemporarilyDisabled(player)
    }

    fun setDemoSpawn(location: Location) {
        demoSpawn = location.clone()
        saveData()
    }

    fun getDemoSpawn(): Location? {
        return demoSpawn
    }

    /**
     * Применяет префикс для игрока при входе на сервер
     */
    fun applyDemoPrefixOnJoin(player: Player) {
        // Если игрок в принудительном демо-режиме, то при входе отключаем временное отключение
        if (forcedPlayers.contains(player.name)) {
            tempDisabledDemo.remove(player.uniqueId)
        }

        // Проверяем, должен ли игрок иметь ограничения и префикс
        if (shouldHaveDemoRestrictions(player)) {
            addPlayerToTeam(player)
        } else {
            removePlayerFromTeam(player)
        }

        // Проверяем, ожидает ли игрок телепортации
        if (pendingTeleports.contains(player.name)) {
            teleportPlayerToDemoSpawn(player)
            pendingTeleports.remove(player.name)
            saveData()
        }
    }

    /**
     * Телепортирует всех игроков в демо-режиме к точке возрождения демо-режима
     */
    fun teleportAllDemoPlayers(): Int {
        val demoSpawn = getDemoSpawn() ?: return 0

        var teleportedCount = 0

        // Телепортируем всех онлайн игроков в демо-режиме
        Bukkit.getOnlinePlayers().forEach { player ->
            if (shouldHaveDemoRestrictions(player)) {
                teleportPlayerToDemoSpawn(player)
                teleportedCount++
            }
        }

        // Добавляем оффлайн игроков в список ожидающих телепортации
        val offlinePlayers = mutableListOf<String>()

        // Добавляем игроков из обычного демо-режима
        demoPlayers.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline) {
                val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
                offlinePlayer.name?.let { name ->
                    if (!pendingTeleports.contains(name) && !tempDisabledDemo.contains(uuid)) {
                        pendingTeleports.add(name)
                        offlinePlayers.add(name)
                    }
                }
            }
        }

        // Добавляем игроков из принудительного демо-режима
        forcedPlayers.forEach { name ->
            val player = Bukkit.getPlayerExact(name)
            if (player == null || !player.isOnline) {
                if (!pendingTeleports.contains(name)) {
                    pendingTeleports.add(name)
                    offlinePlayers.add(name)
                }
            }
        }

        saveData()
        return teleportedCount + offlinePlayers.size
    }

    /**
     * Телепортирует игрока к точке возрождения демо-режима
     */
    fun teleportPlayerToDemoSpawn(player: Player) {
        val demoSpawn = getDemoSpawn()
        if (demoSpawn != null) {
            player.teleport(demoSpawn)

            // Обновляем префикс после телепортации
            updatePlayerPrefix(player)

            player.sendMessage("§aВы были телепортированы на точку возрождения демо-режима")
        } else {
            player.sendMessage("§cТочка возрождения демо-режима не установлена")
        }
    }

    /**
     * Телепортирует игрока к точке возрождения демо-режима по имени
     * @return true если игрок был телепортирован, false если игрок не найден или не в демо-режиме
     */
    fun teleportPlayerToDemoSpawn(playerName: String): Boolean {
        // Сначала проверяем онлайн-игроков
        val player = Bukkit.getPlayerExact(playerName)

        if (player != null && player.isOnline) {
            // Если игрок онлайн, проверяем, находится ли он в демо-режиме
            if (shouldHaveDemoRestrictions(player)) {
                teleportPlayerToDemoSpawn(player)
                return true
            }
            return false
        }

        // Если игрок оффлайн, проверяем, находится ли он в списке демо-игроков
        // или в списке принудительных демо-игроков
        val isInDemoMode = demoPlayers.any { uuid ->
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
            offlinePlayer.name == playerName && !tempDisabledDemo.contains(uuid)
        }

        val isForced = forcedPlayers.contains(playerName)

        if (isInDemoMode || isForced) {
            // Добавляем игрока в список ожидающих телепортации
            if (!pendingTeleports.contains(playerName)) {
                pendingTeleports.add(playerName)
                saveData()
            }
            return true
        }

        return false
    }

    /**
     * Проверяет, разрешено ли в конфиге определенное действие
     */
    fun isAllowed(section: String, key: String): Boolean {
        return plugin.getConfig().getBoolean("restrictions.$section.$key", false)
    }

    /**
     * Проверяет, является ли игрок в принудительном демо-режиме
     */
    fun isForcedDemoPlayer(player: Player): Boolean {
        return forcedPlayers.contains(player.name)
    }

    /**
     * Получает пароль по умолчанию из конфига
     */
    fun getDefaultPassword(): String {
        return plugin.getConfig().getString("settings.default-password") ?: "12345"
    }

    /**
     * Получает список всех принудительных демо-игроков
     */
    fun getForcedPlayers(): Set<String> {
        return forcedPlayers
    }

    /**
     * Получает список всех игроков в демо-режиме
     */
    fun getDemoPlayerNames(): List<String> {
        return demoPlayers.mapNotNull { uuid ->
            if (!tempDisabledDemo.contains(uuid)) {
                Bukkit.getOfflinePlayer(uuid).name
            } else {
                null
            }
        }
    }
}