package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.WarpManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class WarpCommand(private val demoManager: DemoManager, private val warpManager: WarpManager) : BaseCommand {
    override val name: String = "warp"
    override val permission: String = "jtdemo.admin"  // Только для админов

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val action = args[0].lowercase()

        when (action) {
            "add" -> return handleAddWarp(sender, args)
            "delete", "del" -> return handleDeleteWarp(sender, args)
            "on" -> return handleEnableWarp(sender, args)
            "off" -> return handleDisableWarp(sender, args)
            "list" -> return handleListWarps(sender)
            "tp" -> return handleTeleportToWarp(sender, args)
            else -> {
                sender.sendMessage(getUsage())
                return true
            }
        }
    }

    private fun handleAddWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp add [имя_точки] (x y z мир)")
            return true
        }

        val warpName = args[1]

        // Проверяем, существует ли уже такая точка
        if (warpManager.warpExists(warpName)) {
            sender.sendMessage("§cТочка с именем '$warpName' уже существует.")
            return true
        }

        // Определяем координаты
        val location = if (args.size >= 5) {
            try {
                val x = args[2].toDouble()
                val y = args[3].toDouble()
                val z = args[4].toDouble()

                val world = if (args.size >= 6) {
                    sender.server.getWorld(args[5]) ?: sender.server.worlds[0]
                } else {
                    sender.server.worlds[0]
                }

                org.bukkit.Location(world, x, y, z)
            } catch (e: NumberFormatException) {
                sender.sendMessage("§cНеверный формат координат. Используйте числа.")
                return true
            }
        } else {
            // Используем текущие координаты игрока
            sender.location
        }

        // Добавляем точку
        warpManager.addWarp(warpName, location, true)
        sender.sendMessage("§aТочка '$warpName' успешно добавлена.")

        return true
    }

    private fun handleDeleteWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp delete [имя_точки]")
            return true
        }

        val warpName = args[1]

        // Проверяем, существует ли точка
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage("§cТочка с именем '$warpName' не существует.")
            return true
        }

        // Удаляем точку
        warpManager.deleteWarp(warpName)
        sender.sendMessage("§aТочка '$warpName' успешно удалена.")

        return true
    }

    private fun handleEnableWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp on [имя_точки]")
            return true
        }

        val warpName = args[1]

        // Проверяем, существует ли точка
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage("§cТочка с именем '$warpName' не существует.")
            return true
        }

        // Включаем точку
        warpManager.setWarpEnabled(warpName, true)
        sender.sendMessage("§aТочка '$warpName' успешно включена.")

        return true
    }

    private fun handleDisableWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp off [имя_точки]")
            return true
        }

        val warpName = args[1]

        // Проверяем, существует ли точка
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage("§cТочка с именем '$warpName' не существует.")
            return true
        }

        // Выключаем точку
        warpManager.setWarpEnabled(warpName, false)
        sender.sendMessage("§aТочка '$warpName' успешно выключена.")

        return true
    }

    private fun handleListWarps(sender: CommandSender): Boolean {
        val warps = warpManager.getAllWarps()

        if (warps.isEmpty()) {
            sender.sendMessage("§cСписок точек пуст.")
            return true
        }

        sender.sendMessage("§6========== JTDemo Warps ==========")
        warps.forEach { warpName ->
            val status = if (warpManager.isWarpEnabled(warpName)) "§a✓" else "§c✗"
            sender.sendMessage("$status §f$warpName")
        }
        sender.sendMessage("§6==================================")

        return true
    }

    private fun handleTeleportToWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp tp [имя_точки]")
            return true
        }

        val warpName = args[1]

        // Проверяем, существует ли точка
        if (!warpManager.warpExists(warpName)) {
            sender.sendMessage("§cТочка с именем '$warpName' не существует.")
            return true
        }

        // Проверяем, включена ли точка
        if (!warpManager.isWarpEnabled(warpName)) {
            sender.sendMessage("§cТочка '$warpName' отключена.")
            return true
        }

        // Телепортируем игрока
        if (warpManager.teleportPlayerToWarp(sender, warpName)) {
            sender.sendMessage("§aВы телепортированы к точке '$warpName'.")
        } else {
            sender.sendMessage("§cНе удалось телепортироваться к точке '$warpName'.")
        }

        return true
    }

    override fun getUsage(): String = """
        §cИспользование: 
        §c/jtdemo warp add [имя_точки] (x y z мир) - Добавить новую точку
        §c/jtdemo warp delete [имя_точки] - Удалить точку
        §c/jtdemo warp on [имя_точки] - Включить точку
        §c/jtdemo warp off [имя_точки] - Выключить точку
        §c/jtdemo warp list - Показать список точек
        §c/jtdemo warp tp [имя_точки] - Телепортироваться к точке
    """.trimIndent()

    override fun getDescription(): String = "Управление варп-точками"
}