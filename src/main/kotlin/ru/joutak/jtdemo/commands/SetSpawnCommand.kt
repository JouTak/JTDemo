package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class SetSpawnCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "setspawn"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        demoManager.setDemoSpawn(sender.location)
        val message = demoManager.plugin.getConfig().getString("settings.spawn-set-message")
            ?: "§aТочка возрождения для демо-режима установлена на вашей текущей позиции."
        sender.sendMessage(message)

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo setspawn"
    
    override fun getDescription(): String = "Установить точку возрождения для игроков в демо-режиме"
}