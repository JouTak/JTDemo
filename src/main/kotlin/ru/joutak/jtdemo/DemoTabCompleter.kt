package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.ArrayList


class DemoTabCompleter(private val demoManager: DemoManager) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        val completions = ArrayList<String>()

        // Если это не команда jtdemo, возвращаем null
        if (!command.name.equals("jtdemo", ignoreCase = true)) {
            return null
        }

        // Если аргументов нет, предлагаем все доступные подкоманды
        if (args.isEmpty()) {
            return getAvailableSubcommands(sender)
        }

        // Автодополнение для первого аргумента (подкоманды)
        if (args.size == 1) {
            val subcommands = getAvailableSubcommands(sender)
            for (subcommand in subcommands) {
                if (subcommand.startsWith(args[0].lowercase())) {
                    completions.add(subcommand)
                }
            }
            return completions
        }

        // Автодополнение для дополнительных аргументов (в зависимости от подкоманды)
        if (args.size == 2) {
            when (args[0].lowercase()) {
                "reset", "force", "unforce" -> {
                    // Для reset, force и unforce предлагаем имена игроков
                    if (sender.hasPermission("jtdemo.admin")) {
                        val onlinePlayers = Bukkit.getOnlinePlayers()
                        for (player in onlinePlayers) {
                            if (player.name.lowercase().startsWith(args[1].lowercase())) {
                                completions.add(player.name)
                            }
                        }

                        // Для unforce также предлагаем имена из списка принудительных игроков
                        if (args[0].lowercase() == "unforce") {
                            for (name in demoManager.getForcedPlayers()) {
                                if (name.lowercase().startsWith(args[1].lowercase()) &&
                                    !completions.contains(name)) {
                                    completions.add(name)
                                }
                            }
                        }
                    }
                }
//                "off" -> {
//                    // Для off предлагаем пароль по умолчанию, если это игрок в принудительном режиме
//                    if (sender is Player && demoManager.isForcedDemoPlayer(sender)) {
//                        completions.add(demoManager.getDefaultPassword())
//                    }
//                }
                "tp" -> {
                    // Для tp предлагаем "all" и имена игроков в демо-режиме
                    if (sender.hasPermission("jtdemo.admin")) {
                        // Добавляем опцию "all"
                        if ("all".startsWith(args[1].lowercase())) {
                            completions.add("all")
                        }

                        // Предлагаем имена онлайн-игроков в демо-режиме
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (demoManager.isInDemoMode(player) &&
                                player.name.lowercase().startsWith(args[1].lowercase())) {
                                completions.add(player.name)
                            }
                        }

                        // Предлагаем имена из списка демо-игроков
                        for (name in demoManager.getDemoPlayerNames()) {
                            if (name.lowercase().startsWith(args[1].lowercase()) &&
                                !completions.contains(name)) {
                                completions.add(name)
                            }
                        }

                        // Предлагаем имена из списка принудительных демо-игроков
                        for (name in demoManager.getForcedPlayers()) {
                            if (name.lowercase().startsWith(args[1].lowercase()) &&
                                !completions.contains(name)) {
                                completions.add(name)
                            }
                        }
                    }
                }
            }
            return completions
        }

        return completions
    }

    /**
     * Получает список доступных подкоманд для отправителя
     */
    private fun getAvailableSubcommands(sender: CommandSender): List<String> {
        val commands = ArrayList<String>()

        // Базовые команды доступны всем
        commands.add("on")
        commands.add("off")

        // Административные команды доступны только с соответствующими правами
        if (sender.hasPermission("jtdemo.admin")) {
            commands.add("reset")
            commands.add("setspawn")
            commands.add("force")
            commands.add("unforce")
            commands.add("reload")
            commands.add("tp")
            commands.add("list")
        }

        return commands
    }
}