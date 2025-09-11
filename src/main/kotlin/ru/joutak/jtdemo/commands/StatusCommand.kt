package ru.joutak.jtdemo.commands

import org.bukkit.command.CommandSender
import ru.joutak.jtdemo.DemoManager
import ru.joutak.jtdemo.commands.base.BaseCommand

class StatusCommand(private val demoManager: DemoManager) : BaseCommand {
    override val name: String = "status"
    override val permission: String? = "jtdemo.admin"

    override fun execute(sender: CommandSender, args: Array<out String>): Boolean {
        sender.sendMessage("§6=== Текущие настройки JTDemo ===")

        // Основные настройки
        sender.sendMessage("§e[Основные настройки]")
        printSetting(sender, "Показывать сообщения", "settings.show-demo-messages")
        printSetting(sender, "Добавлять префикс", "settings.add-demo-prefix")
        printSetting(sender, "Префикс", "settings.demo-prefix", isString = true)
        printSetting(sender, "Пароль по умолчанию", "settings.default-password", isString = true)
        printSetting(sender, "Интервал проверки файлов (сек)", "settings.file-check-interval", isString = true)

        // Перемещение и взаимодействие
        sender.sendMessage("§e[Перемещение]")
        printSetting(sender, "Сохранять уровень сытости", "restrictions.movement.preserve-food-level")
        printSetting(sender, "Неуязвимость", "restrictions.movement.invulnerable")
        printSetting(sender, "Урон игрокам", "restrictions.movement.allow-player-damage")
        printSetting(sender, "Урон мобам", "restrictions.movement.allow-mob-damage")

        // Инвентарь
        sender.sendMessage("§e[Инвентарь]")
        printSetting(sender, "Использование инвентаря", "restrictions.inventory.allow-inventory-use")
        printSetting(sender, "Подбор предметов", "restrictions.inventory.allow-item-pickup")
        printSetting(sender, "Выбрасывание предметов", "restrictions.inventory.allow-item-drop")
        printSetting(sender, "Разрушение блоков", "restrictions.inventory.allow-block-breaking")
        printSetting(sender, "Размещение блоков", "restrictions.inventory.allow-block-placing")

        // Интерактивные блоки
        sender.sendMessage("§e[Интерактивные блоки]")
        printSetting(sender, "Двери", "restrictions.interactive-blocks.doors.enabled")
        printSetting(sender, "Люки", "restrictions.interactive-blocks.trapdoors.enabled")
        printSetting(sender, "Калитки", "restrictions.interactive-blocks.gates.enabled")
        printSetting(sender, "Кнопки", "restrictions.interactive-blocks.buttons.enabled")
        printSetting(sender, "Нажимные плиты", "restrictions.interactive-blocks.pressure-plates.enabled")

        // Контейнеры
        sender.sendMessage("§e[Контейнеры]")
        printSetting(sender, "Открытие сундуков/бочек", "restrictions.interactive-blocks.chests.open")
        printSetting(sender, "Брать предметы из сундуков/бочек", "restrictions.interactive-blocks.chests.take-items")
        printSetting(sender, "Открытие шалкер-боксов", "restrictions.interactive-blocks.shulker-boxes.open")
        printSetting(sender, "Брать предметы из шалкер-боксов", "restrictions.interactive-blocks.shulker-boxes.take-items")
        printSetting(sender, "Открытие печей", "restrictions.interactive-blocks.furnaces.open")
        printSetting(sender, "Брать предметы из печей", "restrictions.interactive-blocks.furnaces.take-items")
        printSetting(sender, "Использование верстаков", "restrictions.interactive-blocks.crafting-tables.enabled")
        printSetting(sender, "Использование автокрафтеров", "restrictions.interactive-blocks.crafters.enabled")

        // Другие блоки
        sender.sendMessage("§e[Другие блоки]")
        printSetting(sender, "Проигрыватели", "restrictions.interactive-blocks.jukeboxes.enabled")
        printSetting(sender, "Ульи", "restrictions.interactive-blocks.beehives.enabled")
        printSetting(sender, "Книжные полки", "restrictions.interactive-blocks.bookshelves.enabled")
        printSetting(sender, "Кафедры", "restrictions.interactive-blocks.lecterns.enabled")

        return true
    }

    /**
     * Печатает значение настройки
     */
    private fun printSetting(sender: CommandSender, name: String, path: String, isString: Boolean = false) {
        val value = if (isString) {
            demoManager.plugin.config.getString(path, "не задано")
        } else {
            val boolValue = demoManager.plugin.config.getBoolean(path, false)
            if (boolValue) "§aВКЛ" else "§cВЫКЛ"
        }
        sender.sendMessage("§7$name: §f$value")
    }

    override fun getUsage(): String = "§e/jtdemo status - Показать текущие настройки плагина"

    override fun getDescription(): String = "Показывает текущие настройки из конфига"
}