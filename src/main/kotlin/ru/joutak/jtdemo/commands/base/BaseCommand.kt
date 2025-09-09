package ru.joutak.jtdemo.commands.base

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager

/**
 * Interface for all demo commands
 */
interface BaseCommand {
    /**
     * The command name (used in /jtdemo [name])
     */
    val name: String
    
    /**
     * Permission required to use this command (null if no permission needed)
     */
    val permission: String?
    
    /**
     * Execute the command
     * @param sender The command sender
     * @param args Command arguments (excluding the base command and subcommand)
     * @return true if command executed successfully
     */
    fun execute(sender: CommandSender, args: Array<out String>): Boolean
    
    /**
     * Get the command usage message
     */
    fun getUsage(): String
    
    /**
     * Get the command description for help
     */
    fun getDescription(): String
}