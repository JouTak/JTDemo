package ru.joutak.jtdemo

import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.IOException

/**
 * Менеджер для управления варп-точками
 */
class WarpManager(private val plugin: JTDemo) {

    private val warpsFile = File(plugin.dataFolder, "warps.yml")
    private var warpsConfig = YamlConfiguration()
    private var lastModified: Long = 0

    init {
        // Загружаем конфигурацию варпов
        loadWarps()
    }

    /**
     * Загружает точки из файла warps.yml
     */
    fun loadWarps() {
        if (!warpsFile.exists()) {
            plugin.dataFolder.mkdirs()
            warpsFile.createNewFile()
        }

        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile)
        lastModified = warpsFile.lastModified()
    }

    /**
     * Сохраняет точки в файл warps.yml
     */
    fun saveWarps() {
        try {
            warpsConfig.save(warpsFile)
            lastModified = warpsFile.lastModified()
        } catch (e: IOException) {
            plugin.logger.severe("Не удалось сохранить варпы в ${warpsFile.path}")
            e.printStackTrace()
        }
    }

    /**
     * Проверяет обновление файла warps.yml
     * @return true если файл был обновлен и загружен
     */
    fun checkWarpsFile(): Boolean {
        if (warpsFile.exists() && warpsFile.lastModified() > lastModified) {
            plugin.logger.info("Обнаружены изменения в файле warps.yml, перезагружаем данные...")
            loadWarps()
            return true
        }
        return false
    }

    /**
     * Добавляет новую точку
     * @param name Название точки
     * @param location Координаты точки
     * @param enabled Статус точки (включена/выключена)
     * @return true если точка успешно добавлена
     */
    fun addWarp(name: String, location: Location, enabled: Boolean): Boolean {
        // Создаем секцию для точки
        val warpSection = warpsConfig.createSection(name)

        // Сохраняем координаты и мир
        warpSection.set("world", location.world?.name)
        warpSection.set("x", location.x)
        warpSection.set("y", location.y)
        warpSection.set("z", location.z)
        warpSection.set("yaw", location.yaw)
        warpSection.set("pitch", location.pitch)
        warpSection.set("enabled", enabled)

        saveWarps()
        return true
    }

    /**
     * Удаляет точку
     * @param name Название точки
     * @return true если точка успешно удалена
     */
    fun deleteWarp(name: String): Boolean {
        if (warpsConfig.contains(name)) {
            warpsConfig.set(name, null)
            saveWarps()
            return true
        }
        return false
    }

    /**
     * Изменяет статус точки (включена/выключена)
     * @param name Название точки
     * @param enabled Новый статус
     * @return true если статус успешно изменен
     */
    fun setWarpEnabled(name: String, enabled: Boolean): Boolean {
        if (warpsConfig.contains(name)) {
            warpsConfig.set("$name.enabled", enabled)
            saveWarps()
            return true
        }
        return false
    }

    /**
     * Проверяет, существует ли точка
     * @param name Название точки
     * @return true если точка существует
     */
    fun warpExists(name: String): Boolean {
        return warpsConfig.contains(name)
    }

    /**
     * Получает локацию точки
     * @param name Название точки
     * @return Location или null, если точка не существует
     */
    fun getWarpLocation(name: String): Location? {
        if (!warpExists(name)) return null

        val world = plugin.server.getWorld(warpsConfig.getString("$name.world") ?: return null)
        val x = warpsConfig.getDouble("$name.x")
        val y = warpsConfig.getDouble("$name.y")
        val z = warpsConfig.getDouble("$name.z")
        val yaw = warpsConfig.getDouble("$name.yaw").toFloat()
        val pitch = warpsConfig.getDouble("$name.pitch").toFloat()

        return Location(world, x, y, z, yaw, pitch)
    }

    /**
     * Проверяет, включена ли точка
     * @param name Название точки
     * @return true если точка включена, false если выключена или не существует
     */
    fun isWarpEnabled(name: String): Boolean {
        return warpsConfig.getBoolean("$name.enabled", false)
    }

    /**
     * Получает список всех точек
     * @return List<String> список названий точек
     */
    fun getAllWarps(): List<String> {
        return warpsConfig.getKeys(false).toList()
    }

    /**
     * Телепортирует игрока на точку
     * @param player Игрок
     * @param warpName Название точки
     * @return true если телепортация прошла успешно
     */
    fun teleportPlayerToWarp(player: Player, warpName: String): Boolean {
        if (!warpExists(warpName)) return false
        if (!isWarpEnabled(warpName)) return false

        val location = getWarpLocation(warpName) ?: return false
        return player.teleport(location)
    }

    fun addOrUpdateWarp(name: String, location: Location, enabled: Boolean): Boolean {
        // Создаем секцию для точки или обновляем существующую
        val warpSection = if (warpsConfig.contains(name)) {
            warpsConfig.getConfigurationSection(name) ?: warpsConfig.createSection(name)
        } else {
            warpsConfig.createSection(name)
        }

        // Сохраняем координаты и мир
        warpSection.set("world", location.world?.name)
        warpSection.set("x", location.x)
        warpSection.set("y", location.y)
        warpSection.set("z", location.z)
        warpSection.set("yaw", location.yaw)
        warpSection.set("pitch", location.pitch)
        warpSection.set("enabled", enabled)

        saveWarps()
        return true
    }
}