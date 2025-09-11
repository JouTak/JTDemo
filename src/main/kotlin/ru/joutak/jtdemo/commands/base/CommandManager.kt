package ru.joutak.jtdemo.commands.base

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.JTDemo
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.WarpManager
import ru.joutak.jtdemo.commands.*

/**
 * Manages all demo commands
 */
class CommandManager(
    private val plugin: JTDemo,
    private val demoManager: DemoManager,
    private val warpManager: WarpManager
) : CommandExecutor {
    private val commands = mutableMapOf<String, BaseCommand>()

    init {
        registerCommand(OnCommand(demoManager))
        registerCommand(OffCommand(demoManager))
        registerCommand(ResetCommand(demoManager))
        registerCommand(SetSpawnCommand(demoManager))
        registerCommand(ForceCommand(demoManager))
        registerCommand(UnforceCommand(demoManager))
        registerCommand(ReloadCommand(demoManager))
        registerCommand(TpCommand(demoManager))
        registerCommand(ListCommand(demoManager))
        registerCommand(WarpCommand(demoManager, warpManager))
        registerCommand(HelpCommand(demoManager, this))
        registerCommand(WarpsCommand(plugin, demoManager, warpManager))
        registerCommand(StatusCommand(demoManager))
    }

    /**
     * Register a command
     */
    private fun registerCommand(command: BaseCommand) {
        commands[command.name.lowercase()] = command
    }

    /**
     * Get all registered commands
     */
    fun getCommands(): Map<String, BaseCommand> = commands

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("jtdemo", ignoreCase = true)) {
            if (args.isEmpty()) {
                return commands["help"]?.execute(sender, emptyArray()) ?: false
            }

            val subCommand = args[0].lowercase()
            val subArgs = args.copyOfRange(1, args.size)

            return if (commands.containsKey(subCommand)) {
                val cmd = commands[subCommand]!!

                val permission = cmd.permission
                if (permission != null && !sender.hasPermission(permission)) {
                    sender.sendMessage("§cУ вас нет прав для использования этой команды.")
                    return true
                }

                cmd.execute(sender, subArgs)
            } else {
                commands["help"]?.execute(sender, emptyArray()) ?: false
            }
        }
        return false
    }
}