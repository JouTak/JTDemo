package ru.joutak.jtdemo.commands

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.JTDemo
import ru.joutak.jtdemo.WarpManager
import ru.joutak.jtdemo.commands.base.BaseCommand
import ru.joutak.jtdemo.gui.WarpMenuManager
import java.util.*

class WarpsCommand(
    private val plugin: JTDemo,
    private val demoManager: DemoManager,
    private val warpManager: WarpManager
) : BaseCommand {

    private val menuManager: WarpMenuManager = WarpMenuManager(plugin, warpManager)

    override val name: String = "warps"
    override val permission: String? = null // Доступно всем

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        // Получаем список варпов
        val warps = warpManager.getAllWarps()

        if (warps.isEmpty()) {
            sender.sendMessage("§cСписок варп-точек пуст.")
            return true
        }

        // Фильтруем только активные варпы для обычных игроков
        val filteredWarps = if (sender.hasPermission("jtdemo.admin")) {
            warps
        } else {
            warps.filter { warpManager.isWarpEnabled(it) }
        }

        if (filteredWarps.isEmpty()) {
            sender.sendMessage("§cНет доступных варп-точек.")
            return true
        }

        // Открываем GUI-меню с варп-точками
        menuManager.openWarpMenu(sender, 0)
        return true
    }

    override fun getUsage(): String = "§e/jtdemo warps - Открыть меню варп-точек"

    override fun getDescription(): String = "Открыть меню доступных варп-точек"
}