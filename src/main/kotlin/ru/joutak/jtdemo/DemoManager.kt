package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import java.io.File
import java.util.*


class DemoManager(val plugin: JTDemo) {

    private val demoPlayers = mutableSetOf<UUID>()
    private val passwords = mutableMapOf<UUID, String>()
    private var demoSpawn: Location? = null
    private val dataFile = File(plugin.dataFolder, "data.yml")
    private val dataConfig = YamlConfiguration.loadConfiguration(dataFile)
    private val scoreboard = Bukkit.getScoreboardManager()?.mainScoreboard
    private var demoTeam: Team? = null

    init {
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }
        setupDemoTeam()
        loadData()
    }

    /**
     * Настройка команды
     */
    private fun setupDemoTeam() {
        demoTeam = scoreboard?.getTeam("demoPlayers") ?: scoreboard?.registerNewTeam("demoPlayers")

        // Префикс
        val prefix = plugin.getConfig().getString("settings.demo-prefix") ?: "[DEMO] "
        demoTeam?.prefix = prefix
    }

    fun loadData() {
        val demoPlayersList = dataConfig.getStringList("demo-players")
        demoPlayers.clear()
        demoPlayersList.forEach {
            try {
                val uuid = UUID.fromString(it)
                demoPlayers.add(uuid)

                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    addPlayerToTeam(player)
                }
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Некорректный UUID в списке игроков в демо-режиме: $it")
            }
        }

        // Пароли
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

        // Спавн
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
        // Игроки
        dataConfig.set("demo-players", demoPlayers.map { it.toString() })

        // Пароли
        val passwordSection = dataConfig.createSection("passwords")
        passwords.forEach { (uuid, password) ->
            passwordSection.set(uuid.toString(), password)
        }

        // Спавн
        demoSpawn?.let {
            dataConfig.set("demo-spawn.world", it.world?.name)
            dataConfig.set("demo-spawn.x", it.x)
            dataConfig.set("demo-spawn.y", it.y)
            dataConfig.set("demo-spawn.z", it.z)
            dataConfig.set("demo-spawn.yaw", it.yaw)
            dataConfig.set("demo-spawn.pitch", it.pitch)
        }

        try {
            dataConfig.save(dataFile)
        } catch (e: Exception) {
            plugin.logger.severe("Не удалось сохранить данные демо-режима: ${e.message}")
        }
    }

    /**
     * Добавление игрока из демо-команды
     */
    private fun addPlayerToTeam(player: Player) {
        if (plugin.getConfig().getBoolean("settings.add-demo-prefix", true)) {
            demoTeam?.addEntry(player.name)
        }
    }

    /**
     * Удаление игрока из демо-команды
     */
    private fun removePlayerFromTeam(player: Player) {
        demoTeam?.removeEntry(player.name)
    }

    fun enableDemoMode(player: Player, password: String): Boolean {
        val uuid = player.uniqueId
        passwords[uuid] = password
        demoPlayers.add(uuid)

        addPlayerToTeam(player)

        // Уведомление в чат
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

        if (storedPassword == null || storedPassword != password) {
            return false
        }

        demoPlayers.remove(uuid)

        removePlayerFromTeam(player)

        // Уведомление в чат
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

        passwords[uuid] = "12345"
        saveData()
        return true
    }

    fun isInDemoMode(player: Player): Boolean {
        return demoPlayers.contains(player.uniqueId)
    }

    fun setDemoSpawn(location: Location) {
        demoSpawn = location.clone()
        saveData()
    }

    fun getDemoSpawn(): Location? {
        return demoSpawn
    }

    /**
     * Префикс
     */
    fun applyDemoPrefixOnJoin(player: Player) {
        if (isInDemoMode(player)) {
            addPlayerToTeam(player)
        }
    }
}