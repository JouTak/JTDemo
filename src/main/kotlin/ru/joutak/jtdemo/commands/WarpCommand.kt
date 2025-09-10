package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.WarpManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class WarpCommand(private val demoManager: DemoManager, private val warpManager: WarpManager) : BaseCommand {
    override val name: String = "warp"
    override val permission: String? = null // Null для базовой проверки, детальная проверка внутри execute

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val action = args[0].lowercase()

        // Обычные игроки могут использовать только tp
        if (action != "tp" && !sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cТолько администраторы могут использовать эту команду.")
            return true
        }

        when (action) {
            "set" -> return handleSetWarp(sender, args)
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

    private fun handleSetWarp(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo warp set [имя_точки] (x y z)")
            return true
        }

        val warpName = args[1]

        // Определяем координаты
        val location = if (args.size >= 5) {
            try {
                val x = args[2].toDouble()
                val y = args[3].toDouble()
                val z = args[4].toDouble()

                // Используем текущий мир игрока
                sender.location.clone().apply {
                    this.x = x
                    this.y = y
                    this.z = z
                }
            } catch (e: NumberFormatException) {
                sender.sendMessage("§cНеверный формат координат. Используйте числа.")
                return true
            }
        } else {
            // Используем текущие координаты игрока
            sender.location
        }

        // Обновляем или добавляем точку
        if (warpManager.warpExists(warpName)) {
            // Обновляем существующую точку
            warpManager.addWarp(warpName, location, warpManager.isWarpEnabled(warpName))
            sender.sendMessage("§aТочка '$warpName' успешно обновлена.")
        } else {
            // Добавляем новую точку
            warpManager.addWarp(warpName, location, true)
            sender.sendMessage("§aТочка '$warpName' успешно добавлена.")
        }

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

    override fun getUsage(): String {
        return if (permission != null && permission.equals("jtdemo.admin")) {
            """
            §eИспользование: 
            §e/jtdemo warp set [имя_точки] (x y z) - Добавить или обновить точку
            §e/jtdemo warp delete [имя_точки] - Удалить точку
            §e/jtdemo warp on [имя_точки] - Включить точку
            §e/jtdemo warp off [имя_точки] - Выключить точку
            §e/jtdemo warp list - Показать список точек
            §e/jtdemo warp tp [имя_точки] - Телепортироваться к точке
            """.trimIndent()
        } else {
            "§e/jtdemo warp tp [имя_точки] - Телепортироваться к варп-точке"
        }
    }

    override fun getDescription(): String = "Управление варп-точками"
}