package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class TpCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "tp"
    override val permission: String = "jtdemo.admin"
    
    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        // Проверяем, установлена ли точка возрождения
        if (demoManager.getDemoSpawn() == null) {
            sender.sendMessage("§cТочка возрождения демо-режима не установлена. Используйте /jtdemo setspawn")
            return true
        }

        // Если аргументов недостаточно, показываем подсказку
        if (args.isEmpty()) {
            sender.sendMessage(getUsage())
            return true
        }

        // Если указано 'all', телепортируем всех игроков в демо-режиме
        if (args[0].equals("all", ignoreCase = true)) {
            val count = demoManager.teleportAllDemoPlayers()

            if (count > 0) {
                sender.sendMessage("§aВсе игроки в демо-режиме ($count) были телепортированы к точке возрождения демо-режима.")
            } else {
                sender.sendMessage("§cНет игроков в демо-режиме для телепортации.")
            }
            return true
        }

        // Иначе пытаемся телепортировать конкретного игрока
        val playerName = args[0]
        val success = demoManager.teleportPlayerToDemoSpawn(playerName)

        if (success) {
            sender.sendMessage("§aИгрок $playerName был телепортирован к точке возрождения демо-режима.")
        } else {
            sender.sendMessage("§cИгрок $playerName не найден или не находится в демо-режиме.")
        }

        return true
    }
    
    override fun getUsage(): String = "§cИспользование: /jtdemo tp [all|игрок]"
    
    override fun getDescription(): String = "Телепортировать всех или конкретного игрока в демо-режиме на точку возрождения"
}