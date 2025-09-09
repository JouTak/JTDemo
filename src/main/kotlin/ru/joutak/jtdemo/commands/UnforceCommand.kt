package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class UnforceCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "unforce"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val playerName = args[0]
        if (demoManager.removeForcedPlayer(playerName)) {
            sender.sendMessage("§aИгрок $playerName удален из списка принудительного демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName не найден в списке принудительного демо-режима.")
        }

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo unforce [игрок]"
    
    override fun getDescription(): String = "Удалить игрока из принудительного демо-режима"
}