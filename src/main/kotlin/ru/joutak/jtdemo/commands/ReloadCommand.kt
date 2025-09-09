package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class ReloadCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "reload"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        demoManager.reload()

        val message = demoManager.plugin.getConfig().getString("settings.reload-message")
            ?: "§aПлагин JTDemo был успешно перезагружен."
        sender.sendMessage(message)

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo reload"
    
    override fun getDescription(): String = "Перезагрузить плагин и обновить настройки"
}