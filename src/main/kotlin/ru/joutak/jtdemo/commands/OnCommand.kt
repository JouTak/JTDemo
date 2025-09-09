package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class OnCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "on"
    override val permission: String? = null
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        // Проверяем, не находится ли игрок уже в демо-режиме
        if (demoManager.isInDemoMode(sender)) {
            sender.sendMessage("§cВы уже находитесь в демо-режиме.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val password = args[0]
        demoManager.enableDemoMode(sender, password)
        val message = demoManager.plugin.getConfig().getString("settings.demo-enabled-message")
            ?: "§aДемо-режим включен. Используйте /jtdemo off [пароль] для выключения."
        sender.sendMessage(message)

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo on [пароль]"
    
    override fun getDescription(): String = "Включить демо-режим"
}