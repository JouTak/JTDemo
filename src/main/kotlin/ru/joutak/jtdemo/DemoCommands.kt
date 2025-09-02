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
                "force" -> {
                    return handleDemoForce(sender, args)
                }
                "unforce" -> {
                    return handleDemoUnforce(sender, args)
                }
                "reload" -> {
                    return handleDemoReload(sender)
                }
                "tp" -> {
                    return handleDemoTeleport(sender, args)
                }
                "list" -> {
                    return handleDemoList(sender)
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

        // Проверяем, не находится ли игрок уже в демо-режиме
        if (demoManager.isInDemoMode(sender)) {
            sender.sendMessage("§cВы уже находитесь в демо-режиме.")
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

        // Проверяем, находится ли игрок в демо-режиме
        if (!demoManager.isInDemoMode(sender)) {
            sender.sendMessage("§cВы не находитесь в демо-режиме.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo off [пароль]")
            return true
        }

        val password = args[1]
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

    private fun handleDemoForce(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo force [игрок]")
            return true
        }

        val playerName = args[1]
        if (demoManager.addForcedPlayer(playerName)) {
            sender.sendMessage("§aИгрок $playerName добавлен в список принудительного демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName уже находится в списке принудительного демо-режима.")
        }

        return true
    }

    private fun handleDemoUnforce(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo unforce [игрок]")
            return true
        }

        val playerName = args[1]
        if (demoManager.removeForcedPlayer(playerName)) {
            sender.sendMessage("§aИгрок $playerName удален из списка принудительного демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName не найден в списке принудительного демо-режима.")
        }

        return true
    }

    private fun handleDemoReload(sender: CommandSender): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        demoManager.reload()

        val message = demoManager.plugin.getConfig().getString("settings.reload-message")
            ?: "§aПлагин JTDemo был успешно перезагружен."
        sender.sendMessage(message)

        return true
    }

    private fun handleDemoTeleport(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        // Проверяем, установлена ли точка возрождения
        if (demoManager.getDemoSpawn() == null) {
            sender.sendMessage("§cТочка возрождения демо-режима не установлена. Используйте /jtdemo setspawn")
            return true
        }

        // Если аргументов недостаточно, показываем подсказку
        if (args.size < 2) {
            sender.sendMessage("§cИспользование: /jtdemo tp [all|player_name]")
            return true
        }

        // Если указано 'all', телепортируем всех игроков в демо-режиме
        if (args[1].equals("all", ignoreCase = true)) {
            val count = demoManager.teleportAllDemoPlayers()

            if (count > 0) {
                sender.sendMessage("§aВсе игроки в демо-режиме ($count) были телепортированы к точке возрождения демо-режима.")
            } else {
                sender.sendMessage("§cНет игроков в демо-режиме для телепортации.")
            }
            return true
        }

        // Иначе пытаемся телепортировать конкретного игрока
        val playerName = args[1]
        val success = demoManager.teleportPlayerToDemoSpawn(playerName)

        if (success) {
            sender.sendMessage("§aИгрок $playerName был телепортирован к точке возрождения демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName не найден или не находится в демо-режиме.")
        }

        return true
    }

    /**
     * Обработчик команды list, показывающей списки игроков в демо-режиме
     */
    private fun handleDemoList(sender: CommandSender): Boolean {
        if (!sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.")
            return true
        }

        // Получаем список игроков в демо-режиме
        val demoPlayers = demoManager.getDemoPlayerNames()
        // Получаем список игроков в принудительном демо-режиме
        val forcedPlayers = demoManager.getForcedPlayers()

        // Отправляем информацию отправителю
        sender.sendMessage("§6========== JTDemo Списки игроков ==========")

        if (demoPlayers.isNotEmpty()) {
            sender.sendMessage("§eИгроки в демо-режиме (${demoPlayers.size}):")
            demoPlayers.forEach { player ->
                sender.sendMessage("§7- §f$player")
            }
        } else {
            sender.sendMessage("§eИгроки в демо-режиме: §7нет")
        }

        if (forcedPlayers.isNotEmpty()) {
            sender.sendMessage("§eИгроки в принудительном демо-режиме (${forcedPlayers.size}):")
            forcedPlayers.forEach { player ->
                sender.sendMessage("§7- §f$player")
            }
        } else {
            sender.sendMessage("§eИгроки в принудительном демо-режиме: §7нет")
        }

        sender.sendMessage("§6===========================================")

        return true
    }

    private fun sendHelpMessage(sender: CommandSender) {
        sender.sendMessage("§6========== JTDemo Команды ==========")
        sender.sendMessage("§e/jtdemo on [пароль] §f- Включить демо-режим")
        sender.sendMessage("§e/jtdemo off [пароль] §f- Выключить демо-режим")

        if (sender.hasPermission("jtdemo.admin")) {
            sender.sendMessage("§e/jtdemo reset [игрок] §f- Сбросить пароль игрока на пароль по умолчанию")
            sender.sendMessage("§e/jtdemo setspawn §f- Установить точку возрождения для игроков в демо-режиме")
            sender.sendMessage("§e/jtdemo force [игрок] §f- Добавить игрока в принудительный демо-режим")
            sender.sendMessage("§e/jtdemo unforce [игрок] §f- Удалить игрока из принудительного демо-режима")
            sender.sendMessage("§e/jtdemo tp [all|игрок] §f- Телепортировать всех или конкретного игрока в демо-режиме на точку возрождения")
            sender.sendMessage("§e/jtdemo list §f- Показать список игроков в демо-режиме и принудительном демо-режиме")
            sender.sendMessage("§e/jtdemo reload §f- Перезагрузить плагин и обновить настройки")
        }

        sender.sendMessage("§6===================================")
    }
}