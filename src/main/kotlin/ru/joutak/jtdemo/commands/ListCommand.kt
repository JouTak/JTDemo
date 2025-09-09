package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class ListCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "list"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        // Получаем список игроков в демо-режиме
        val demoPlayers = demoManager.getDemoPlayerNames()
        // Получаем список игроков в принудительном демо-режиме
        val forcedPlayers = demoManager.getForcedPlayers()

        // Отправляем информацию отправителю
        sender.sendMessage("§6========== JTDemo Списки игроков ==========")

        if (demoPlayers.isNotEmpty()) {
            sender.sendMessage("§eИгроки в демо-режиме (${demoPlayers.size}):")
            demoPlayers.forEach { player ->
                sender.sendMessage("§7- §f$player")
            }
        } else {
            sender.sendMessage("§eИгроки в демо-режиме: §7нет")
        }

        if (forcedPlayers.isNotEmpty()) {
            sender.sendMessage("§eИгроки в принудительном демо-режиме (${forcedPlayers.size}):")
            forcedPlayers.forEach { player ->
                sender.sendMessage("§7- §f$player")
            }
        } else {
            sender.sendMessage("§eИгроки в принудительном демо-режиме: §7нет")
        }

        sender.sendMessage("§6===========================================")

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo list"
    
    override fun getDescription(): String = "Показать список игроков в демо-режиме и принудительном демо-режиме"
}