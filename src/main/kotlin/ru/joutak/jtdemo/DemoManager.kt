package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*

/**
 * Управляет всеми аспектами демо-режима в плагине JTDemo
 */
class DemoManager(val plugin: JTDemo) {

    // Основные коллекции для хранения данных
    private val demoPlayers = mutableSetOf<UUID>()
    private val passwords = mutableMapOf<UUID, String>()
    private val forcedPlayers = mutableSetOf<String>()
    private val pendingTeleports = mutableSetOf<String>()

    // Точка возрождения для демо-режима
    private var demoSpawn: Location? = null

    // Файл данных и его конфигурация
    private val dataFile = File(plugin.dataFolder, "data.yml")
    private var dataConfig = YamlConfiguration.loadConfiguration(dataFile)

    // Интеграция со скорбордом
    private val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
    private var demoTeam: Team? = null

    // Кэш разрешений для оптимизации
    private val permissionCache = mutableMapOf<String, Boolean>()

    /**
     * Инициализация менеджера при запуске плагина
     */
    init {
        // Создаем директорию плагина, если она не существует
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        // Настраиваем команду для демо-режима
        setupDemoTeam()

        // Загружаем данные из файла
        loadData()
    }

    /**
     * Настраивает команду для игроков в демо-режиме
     */
    private fun setupDemoTeam() {
        // Получаем существующую команду или создаем новую
        demoTeam = scoreboard?.getTeam("demoPlayers") ?: scoreboard?.registerNewTeam("demoPlayers")

        // Устанавливаем префикс из конфигурации
        val prefix = plugin.config.getString("settings.demo-prefix") ?: "[DEMO] "
        demoTeam?.setPrefix(prefix) // Используем setPrefix вместо присваивания
    }

    /**
     * Перезагружает плагин (настройки и данные)
     */
    fun reload() {
        // Перезагружаем конфиг плагина
        plugin.reloadConfig()

        // Очищаем кэш разрешений
        permissionCache.clear()

        // Обновляем префикс для команды
        val prefix = plugin.config.getString("settings.demo-prefix") ?: "[DEMO] "
        demoTeam?.setPrefix(prefix)

        // Перезагружаем данные
        dataConfig = YamlConfiguration.loadConfiguration(dataFile)
        loadData()

        // Обновляем префиксы для всех онлайн игроков
        Bukkit.getOnlinePlayers().forEach { player ->
            updatePlayerPrefix(player)
        }
    }

    /**
     * Загружает данные из файла
     */
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

    /**
     * Сохраняет данные в файл
     */
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
     * Добавляет игрока в демо-команду (для префикса)
     */
    private fun addPlayerToTeam(player: Player) {
        if (plugin.config.getBoolean("settings.add-demo-prefix", false)) {
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
     * Обновляет префикс игрока в зависимости от его статуса
     */
    fun updatePlayerPrefix(player: Player) {
        if (demoPlayers.contains(player.uniqueId)) {
            addPlayerToTeam(player)
        } else {
            removePlayerFromTeam(player)
        }
    }

    /**
     * Включает демо-режим для игрока
     */
    fun enableDemoMode(player: Player, password: String): Boolean {
        val uuid = player.uniqueId
        passwords[uuid] = password
        demoPlayers.add(uuid)

        // Добавляем игрока в демо-команду
        addPlayerToTeam(player)

        // Отправляем сообщение о включении демо-режима
        sendDemoStatusMessage(player, true)

        saveData()
        return true
    }

    /**
     * Выключает демо-режим для игрока
     */
    fun disableDemoMode(player: Player, password: String): Boolean {
        val uuid = player.uniqueId
        val storedPassword = passwords[uuid]

        // Проверяем, не является ли игрок принудительным демо-игроком
        if (forcedPlayers.contains(player.name)) {
            // Для принудительного демо-режима проверяем пароль по умолчанию
            val defaultPassword = getDefaultPassword()
            if (password != defaultPassword) {
                return false
            }

            // Временно удаляем из демо-списка
            demoPlayers.remove(uuid)
            removePlayerFromTeam(player)

            // Отправляем сообщение о временном отключении
            sendDemoStatusMessage(player, false, true)

            saveData()
            return true
        }

        // Для обычного демо-режима проверяем пароль
        if (storedPassword == null || storedPassword != password) {
            return false
        }

        // Удаляем игрока из списка и команды
        demoPlayers.remove(uuid)
        removePlayerFromTeam(player)

        // Отправляем сообщение о выключении
        sendDemoStatusMessage(player, false)

        saveData()
        return true
    }

    /**
     * Отправляет сообщение о статусе демо-режима
     */
    private fun sendDemoStatusMessage(player: Player, enabled: Boolean, temporary: Boolean = false) {
        // Сообщение игроку
        val message = when {
            enabled -> getMessage("messages.demo-enabled")
            temporary -> "§aДемо-режим временно отключен. При следующем входе на сервер он будет включен снова."
            else -> getMessage("messages.demo-disabled")
        }

        player.sendMessage(message)

        // Отправляем широковещательное сообщение другим игрокам
        if (plugin.config.getBoolean("settings.show-demo-messages", false)) {
            val broadcastMessage = when {
                enabled -> "§7Игрок §e${player.name}§7 вошел в демо-режим"
                temporary -> "§7Игрок §e${player.name}§7 временно вышел из принудительного демо-режима"
                else -> "§7Игрок §e${player.name}§7 вышел из демо-режима"
            }

            Bukkit.getOnlinePlayers().forEach { p ->
                if (p != player && p.hasPermission("jtdemo.see-messages")) {
                    p.sendMessage(broadcastMessage)
                }
            }
        }
    }

    /**
     * Сбрасывает пароль игрока на пароль по умолчанию
     */
    fun resetPassword(playerName: String): Boolean {
        val targetPlayer = Bukkit.getPlayerExact(playerName)
        val uuid = targetPlayer?.uniqueId ?: Bukkit.getOfflinePlayer(playerName).uniqueId

        if (uuid.toString() == "00000000-0000-0000-0000-000000000000") {
            return false
        }

        // Устанавливаем пароль по умолчанию
        passwords[uuid] = getDefaultPassword()
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
            // Устанавливаем пароль, если его нет
            if (!passwords.containsKey(player.uniqueId)) {
                passwords[player.uniqueId] = getDefaultPassword()
            }

            // Добавляем в список демо-игроков и команду
            demoPlayers.add(player.uniqueId)
            addPlayerToTeam(player)

            // Уведомляем игрока
            player.sendMessage("§cВы были добавлены в принудительный демо-режим администратором.")
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

        // Если игрок онлайн, удаляем его из демо-режима (если нет личного пароля)
        val player = Bukkit.getPlayerExact(playerName)
        if (player != null) {
            // Проверяем, активировал ли игрок демо-режим самостоятельно
            if (!hasPersonalPassword(player.uniqueId)) {
                demoPlayers.remove(player.uniqueId)
                removePlayerFromTeam(player)
            }

            // Уведомляем игрока
            player.sendMessage("§aВы были удалены из принудительного демо-режима администратором.")
        }

        saveData()
        return true
    }

    /**
     * Проверяет, имеет ли игрок персональный пароль (не по умолчанию)
     */
    private fun hasPersonalPassword(uuid: UUID): Boolean {
        val password = passwords[uuid] ?: return false
        return password != getDefaultPassword()
    }

    /**
     * Обновляет статус демо-режима для игрока
     */
    fun updatePlayerDemoStatus(player: Player) {
        val uuid = player.uniqueId

        // Принудительный демо-режим
        if (forcedPlayers.contains(player.name) && !demoPlayers.contains(uuid)) {
            // Добавляем в демо-режим с паролем по умолчанию
            if (!passwords.containsKey(uuid)) {
                passwords[uuid] = getDefaultPassword()
            }

            demoPlayers.add(uuid)
            addPlayerToTeam(player)

            player.sendMessage("§cВы находитесь в принудительном демо-режиме. Используйте /jtdemo off ${getDefaultPassword()} для временного отключения.")
        }
        // Удаление из принудительного режима
        else if (!forcedPlayers.contains(player.name) && demoPlayers.contains(uuid)) {
            // Если нет персонального пароля, удаляем из демо-режима
            if (!hasPersonalPassword(uuid)) {
                demoPlayers.remove(uuid)
                removePlayerFromTeam(player)
                player.sendMessage("§aВы были удалены из принудительного демо-режима администратором.")
            }
        }

        // Обновляем префикс
        updatePlayerPrefix(player)

        // Проверяем ожидающую телепортацию
        if (pendingTeleports.contains(player.name)) {
            teleportPlayerToDemoSpawn(player)
            pendingTeleports.remove(player.name)
            saveData()
        }
    }

    /**
     * Проверяет, находится ли игрок в демо-режиме
     */
    fun isInDemoMode(player: Player): Boolean {
        return demoPlayers.contains(player.uniqueId)
    }

    /**
     * Проверяет, должен ли игрок иметь ограничения демо-режима
     */
    fun shouldHaveDemoRestrictions(player: Player): Boolean {
        return demoPlayers.contains(player.uniqueId)
    }

    /**
     * Устанавливает точку возрождения для демо-режима
     */
    fun setDemoSpawn(location: Location) {
        demoSpawn = location.clone()
        saveData()
    }

    /**
     * Получает точку возрождения для демо-режима
     */
    fun getDemoSpawn(): Location? {
        return demoSpawn
    }

    /**
     * Применяет префикс для игрока при входе на сервер
     */
    fun applyDemoPrefixOnJoin(player: Player) {
        // Проверяем принудительный демо-режим
        if (forcedPlayers.contains(player.name) && !demoPlayers.contains(player.uniqueId)) {
            // Устанавливаем пароль и добавляем в демо-режим
            if (!passwords.containsKey(player.uniqueId)) {
                passwords[player.uniqueId] = getDefaultPassword()
            }

            demoPlayers.add(player.uniqueId)
            player.sendMessage("§cВы находитесь в принудительном демо-режиме. Используйте /jtdemo off ${getDefaultPassword()} для временного отключения.")
        }

        // Обновляем префикс и проверяем телепортацию
        updatePlayerPrefix(player)

        if (pendingTeleports.contains(player.name)) {
            teleportPlayerToDemoSpawn(player)
            pendingTeleports.remove(player.name)
            saveData()
        }
    }

    /**
     * Телепортирует всех игроков в демо-режиме к точке возрождения
     * @return Количество телепортированных игроков
     */
    fun teleportAllDemoPlayers(): Int {
        if (getDemoSpawn() == null) return 0

        var teleportedCount = 0

        // Телепортируем онлайн игроков
        Bukkit.getOnlinePlayers().forEach { player ->
            if (shouldHaveDemoRestrictions(player)) {
                teleportPlayerToDemoSpawn(player)
                teleportedCount++
            }
        }

        // Добавляем оффлайн игроков в список ожидания
        demoPlayers.forEach { uuid ->
            val player = Bukkit.getPlayer(uuid)
            if (player == null || !player.isOnline) {
                val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
                offlinePlayer.name?.let { name ->
                    if (!pendingTeleports.contains(name)) {
                        pendingTeleports.add(name)
                        teleportedCount++
                    }
                }
            }
        }

        saveData()
        return teleportedCount
    }

    /**
     * Телепортирует игрока к точке возрождения демо-режима
     */
    fun teleportPlayerToDemoSpawn(player: Player) {
        val demoSpawn = getDemoSpawn()
        if (demoSpawn != null) {
            player.teleport(demoSpawn)
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
        // Проверяем онлайн-игроков
        val player = Bukkit.getPlayerExact(playerName)

        if (player != null && player.isOnline) {
            if (shouldHaveDemoRestrictions(player)) {
                teleportPlayerToDemoSpawn(player)
                return true
            }
            return false
        }

        // Проверяем оффлайн-игроков
        val isInDemoMode = demoPlayers.any { uuid ->
            val offlinePlayer = Bukkit.getOfflinePlayer(uuid)
            offlinePlayer.name == playerName
        }

        if (isInDemoMode || forcedPlayers.contains(playerName)) {
            if (!pendingTeleports.contains(playerName)) {
                pendingTeleports.add(playerName)
                saveData()
            }
            return true
        }

        return false
    }

    /**
     * Проверяет, разрешено ли действие в старой структуре конфига (для обратной совместимости)
     * @param section Раздел (например, "blocks" или "gameplay")
     * @param key Ключ (например, "allow-doors")
     * @return true если действие разрешено
     */
    fun isAllowed(section: String, key: String): Boolean {
        // Преобразуем старые пути в новые
        val newPath = when {
            // Движение и взаимодействие
            section == "gameplay" && key == "preserve-food-level" -> "movement.preserve-food-level"
            section == "gameplay" && key == "invulnerable" -> "movement.invulnerable"
            section == "gameplay" && key == "allow-player-damage" -> "movement.allow-player-damage"
            section == "gameplay" && key == "allow-mob-damage" -> "movement.allow-mob-damage"

            // Инвентарь и предметы
            section == "gameplay" && key == "allow-inventory" -> "inventory.allow-inventory-use"
            section == "gameplay" && key == "allow-item-pickup" -> "inventory.allow-item-pickup"
            section == "gameplay" && key == "allow-item-drop" -> "inventory.allow-item-drop"
            section == "blocks" && key == "allow-block-breaking" -> "inventory.allow-block-breaking"

            // Интерактивные блоки
            section == "blocks" && key == "allow-doors" -> "interactive-blocks.doors.enabled"
            section == "blocks" && key == "allow-trapdoors" -> "interactive-blocks.trapdoors.enabled"
            section == "blocks" && key == "allow-gates" -> "interactive-blocks.gates.enabled"
            section == "blocks" && key == "allow-buttons" -> "interactive-blocks.buttons.enabled"
            section == "blocks" && key == "allow-pressure-plates" -> "interactive-blocks.pressure-plates.enabled"
            section == "blocks" && key == "allow-chests" -> "interactive-blocks.chests.open"
            section == "blocks" && key == "allow-shulker-boxes" -> "interactive-blocks.shulker-boxes.open"
            section == "blocks" && key == "allow-furnaces" -> "interactive-blocks.furnaces.open"
            section == "blocks" && key == "allow-crafting-tables" -> "interactive-blocks.crafting-tables.enabled"
            section == "blocks" && key == "allow-jukeboxes" -> "interactive-blocks.jukeboxes.enabled"
            section == "blocks" && key == "allow-beehives" -> "interactive-blocks.beehives.enabled"
            section == "blocks" && key == "allow-bookshelves" -> "interactive-blocks.bookshelves.enabled"
            section == "blocks" && key == "allow-crafters" -> "interactive-blocks.crafters.enabled"
            section == "blocks" && key == "allow-lectern" -> "interactive-blocks.lecterns.enabled"

            // Если не нашли соответствия, возвращаем null
            else -> null
        }

        // Если есть новый путь, используем его
        if (newPath != null) {
            return isAllowed(newPath)
        }

        // Для обратной совместимости
        val legacyPath = "restrictions.$section.$key"

        // Проверяем кэш
        if (permissionCache.containsKey(legacyPath)) {
            return permissionCache[legacyPath] ?: false
        }

        val result = plugin.config.getBoolean(legacyPath, false)
        permissionCache[legacyPath] = result
        return result
    }

    /**
     * Проверяет, находится ли игрок в принудительном демо-режиме
     */
    fun isForcedDemoPlayer(player: Player): Boolean {
        return forcedPlayers.contains(player.name)
    }

    /**
     * Получает пароль по умолчанию из конфига
     */
    fun getDefaultPassword(): String {
        return plugin.config.getString("settings.default-password") ?: "12345"
    }

    /**
     * Получает сообщение из конфига по ключу
     */
    private fun getMessage(key: String): String {
        return plugin.config.getString(key) ?: "Сообщение не найдено: $key"
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
            Bukkit.getOfflinePlayer(uuid).name
        }
    }

    fun isAllowed(path: String): Boolean {
        // Проверяем кэш для оптимизации
        if (permissionCache.containsKey(path)) {
            return permissionCache[path] ?: false
        }

        // ИСПРАВЛЕНО: Получаем значение с правильным дефолтным значением
        // Важно использовать true как дефолтное значение для путей,
        // начинающихся с "interactive-blocks", чтобы они работали по умолчанию
        val defaultValue = when {
            path.startsWith("interactive-blocks") -> true
            else -> false
        }

        val result = plugin.config.getBoolean("restrictions.$path", defaultValue)
        permissionCache[path] = result
        return result
    }

    /**
     * Очищает кэш разрешений
     */
    fun clearPermissionCache() {
        permissionCache.clear()
    }
}