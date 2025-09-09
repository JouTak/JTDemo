package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class ForceCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "force"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val playerName = args[0]
        if (demoManager.addForcedPlayer(playerName)) {
            sender.sendMessage("§aИгрок $playerName добавлен в список принудительного демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName уже находится в списке принудительного демо-режима.")
        }

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo force [игрок]"
    
    override fun getDescription(): String = "Добавить игрока в принудительный демо-режим"
}