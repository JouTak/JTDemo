package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand
import ru.joutak.jtdemo.commands.base.CommandManager

class HelpCommand(private val demoManager: DemoManager, private val commandManager: CommandManager) : BaseCommand {
    override val name: String = "help"
    override val permission: String? = null
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        sender.sendMessage("§6========== JTDemo Команды ==========")
        
        // Get all commands
        val commands = commandManager.getCommands()
        
        // Show user commands first
        commands.values
            .filter { it.permission == null }
            .filter { it.name != "help" } // Don't show help command in help
            .forEach { cmd ->
                sender.sendMessage("§e/jtdemo ${cmd.name} §f- ${cmd.getDescription()}")
            }
        
        // Show admin commands if user has permission
        if (sender.hasPermission("jtdemo.admin")) {
            commands.values
                .filter { it.permission != null }
                .forEach { cmd ->
                    sender.sendMessage("§e/jtdemo ${cmd.name} §f- ${cmd.getDescription()}")
                }
        }
        
        sender.sendMessage("§6===================================")
        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo help"
    
    override fun getDescription(): String = "Показать справку по командам"
}