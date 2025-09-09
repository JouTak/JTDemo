package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class OffCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "off"
    override val permission: String? = null
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        // Проверяем, находится ли игрок в демо-режиме
        if (!demoManager.isInDemoMode(sender)) {
            sender.sendMessage("§cВы не находитесь в демо-режиме.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val password = args[0]
        if (demoManager.disableDemoMode(sender, password)) {
            // Если игрок в принудительном режиме, отправляем особое сообщение
            if (demoManager.isForcedDemoPlayer(sender)) {
                sender.sendMessage("§aДемо-режим временно отключен. При следующем входе на сервер он будет включен снова.")
            } else {
                val message = demoManager.plugin.getConfig().getString("settings.demo-disabled-message")
                    ?: "§aДемо-режим выключен."
                sender.sendMessage(message)
            }
        } else {
            val message = demoManager.plugin.getConfig().getString("settings.wrong-password-message")
                ?: "§cНеверный пароль или демо-режим не активирован."
            sender.sendMessage(message)
        }

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo off [пароль]"
    
    override fun getDescription(): String = "Выключить демо-режим"
}