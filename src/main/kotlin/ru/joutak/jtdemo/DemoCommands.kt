package ru.joutak.jtdemo

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class DemoCommands(private val demoManager: DemoManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("jtdemo", ignoreCase = true)) {
            if (args.isEmpty()) {
                sendHelpMessage(sender)
                return true
            }

            when (args[0].lowercase()) {
                "on" -> {
                    return handleDemoOn(sender, args)
                }
                "off" -> {
                    return handleDemoOff(sender, args)
                }
                "reset" -> {
                    return handleDemoReset(sender, args)
                }
                "setspawn" -> {
                    return handleDemoSetSpawn(sender)
                }
                else -> {
                    sendHelpMessage(sender)
                    return true
                }
            }
        }
        return false
    }

    private fun handleDemoOn(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo on [пароль]")
            return true
        }

        val password = args[1]
        demoManager.enableDemoMode(sender, password)
        val message = demoManager.plugin.getConfig().getString("settings.demo-enabled-message")
            ?: "§aДемо-режим включен. Используйте /jtdemo off [пароль] для выключения."
        sender.sendMessage(message)

        return true
    }

    private fun handleDemoOff(sender: CommandSender, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cЭта команда может быть использована только игроками.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo off [пароль]")
            return true
        }

        val password = args[1]
        if (demoManager.disableDemoMode(sender, password)) {
            val message = demoManager.plugin.getConfig().getString("settings.demo-disabled-message")
                ?: "§aДемо-режим выключен."
            sender.sendMessage(message)
        } else {
            val message = demoManager.plugin.getConfig().getString("settings.wrong-password-message")
                ?: "§cНеверный пароль или демо-режим не активирован."
            sender.sendMessage(message)
        }

        return true
    }

    private fun handleDemoReset(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo reset [игрок]")
            return true
        }

        val playerName = args[1]
        if (demoManager.resetPassword(playerName)) {
            val message = demoManager.plugin.getConfig().getString("settings.password-reset-message")?.replace("%player%", playerName)
                ?: "§aПароль для $playerName был сброшен на '12345'."
            sender.sendMessage(message)
        } else {
            sender.sendMessage("§cИгрок не найден.")
        }

        return true
    }

    private fun handleDemoSetSpawn(sender: CommandSender): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

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

    private fun sendHelpMessage(sender: CommandSender) {
        sender.sendMessage("§6========== JTDemo Команды ==========")
        sender.sendMessage("§e/jtdemo on [пароль] §f- Включить демо-режим")
        sender.sendMessage("§e/jtdemo off [пароль] §f- Выключить демо-режим")

        if (sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§e/jtdemo reset [игрок] §f- Сбросить пароль игрока на '12345'")
            sender.sendMessage("§e/jtdemo setspawn §f- Установить точку возрождения для игроков в демо-режиме")
        }

        sender.sendMessage("§6===================================")
    }
}