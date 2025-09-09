package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class ResetCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "reset"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        val playerName = args[0]
        if (demoManager.resetPassword(playerName)) {
            val defaultPassword = demoManager.getDefaultPassword()
            val message = demoManager.plugin.getConfig()
                .getString("settings.password-reset-message")
                ?.replace("%player%", playerName)
                ?.replace("%default-password%", defaultPassword)
                ?: "§aПароль для $playerName был сброшен на '$defaultPassword'."
            sender.sendMessage(message)
        } else {
            sender.sendMessage("§cИгрок не найден.")
        }

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo reset [игрок]"
    
    override fun getDescription(): String = "Сбросить пароль игрока на пароль по умолчанию"
}