package ru.joutak.jtdemo.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import ru.joutak.jtdemo.JTDemo
import ru.joutak.jtdemo.WarpManager
import java.util.*

/**
 * Менеджер для управления GUI-меню варп-точек
 */
class WarpMenuManager(private val plugin: JTDemo, private val warpManager: WarpManager) : Listener {

    // Размер страницы меню (кратен 9)
    private val PAGE_SIZE = 45 // 5 строк по 9 слотов

    // Маппинг открытых меню к игрокам
    private val openMenus = mutableMapOf<UUID, MenuData>()

    init {
        // Регистрируем слушатель событий
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    /**
     * Открывает меню варп-точек для игрока
     */
    fun openWarpMenu(player: Player, page: Int) {
        // Получаем список варпов
        val warps = if (player.hasPermission("jtdemo.admin")) {
            warpManager.getAllWarps()
        } else {
            warpManager.getAllWarps().filter { warpManager.isWarpEnabled(it) }
        }

        // Вычисляем общее количество страниц
        val totalPages = (warps.size + PAGE_SIZE - 1) / PAGE_SIZE

        // Проверяем валидность номера страницы
        val currentPage = if (page in 0 until totalPages) page else 0

        // Создаем инвентарь для меню
        val inventory = Bukkit.createInventory(
            null,
            PAGE_SIZE + 9, // Основная часть + нижняя панель навигации
            Component.text("Варп-точки (Страница ${currentPage + 1}/$totalPages)")
                .color(NamedTextColor.DARK_PURPLE)
        )

        // Заполняем инвентарь варп-точками для текущей страницы
        val startIndex = currentPage * PAGE_SIZE
        val endIndex = minOf(startIndex + PAGE_SIZE, warps.size)

        for (i in startIndex until endIndex) {
            val warpName = warps[i]
            val item = createWarpItem(warpName, warpManager.isWarpEnabled(warpName))
            inventory.setItem(i - startIndex, item)
        }

        // Добавляем навигационные кнопки в нижней части
        addNavigationButtons(inventory, currentPage, totalPages)

        // Открываем меню и сохраняем информацию о нем
        player.openInventory(inventory)
        openMenus[player.uniqueId] = MenuData(warps, currentPage)
    }

    /**
     * Создает предмет, представляющий варп-точку
     */
    private fun createWarpItem(warpName: String, enabled: Boolean): ItemStack {
        // Выбираем материал в зависимости от статуса варпа
        val material = if (enabled) Material.ENDER_PEARL else Material.BARRIER

        val item = ItemStack(material)
        val meta = item.itemMeta ?: Bukkit.getItemFactory().getItemMeta(material)

        // Устанавливаем имя и описание с использованием Adventure API
        val color = if (enabled) NamedTextColor.GREEN else NamedTextColor.RED
        meta.displayName(Component.text(warpName).color(color))

        val lore = mutableListOf<Component>()
        if (enabled) {
            lore.add(Component.text("Нажмите, чтобы телепортироваться").color(NamedTextColor.GRAY))
        } else {
            lore.add(Component.text("Эта точка отключена").color(NamedTextColor.RED))
            lore.add(Component.text("(Только для администраторов)").color(NamedTextColor.GRAY))
        }

        // Получаем информацию о варпе, если возможно
        val location = warpManager.getWarpLocation(warpName)
        if (location != null) {
            lore.add(Component.text(""))
            lore.add(Component.text("Мир: ").color(NamedTextColor.GRAY)
                .append(Component.text(location.world?.name ?: "Неизвестно").color(NamedTextColor.WHITE)))
            lore.add(Component.text("X: ").color(NamedTextColor.GRAY)
                .append(Component.text(location.x.toInt().toString()).color(NamedTextColor.WHITE)))
            lore.add(Component.text("Y: ").color(NamedTextColor.GRAY)
                .append(Component.text(location.y.toInt().toString()).color(NamedTextColor.WHITE)))
            lore.add(Component.text("Z: ").color(NamedTextColor.GRAY)
                .append(Component.text(location.z.toInt().toString()).color(NamedTextColor.WHITE)))
        }

        meta.lore(lore)
        item.itemMeta = meta

        return item
    }

    /**
     * Добавляет кнопки навигации в нижней части меню
     */
    private fun addNavigationButtons(inventory: Inventory, currentPage: Int, totalPages: Int) {
        // Заполняем нижнюю панель стеклом
        val glassPane = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val glassMeta = glassPane.itemMeta
        glassMeta?.displayName(Component.text(" "))
        glassPane.itemMeta = glassMeta

        for (i in PAGE_SIZE until PAGE_SIZE + 9) {
            inventory.setItem(i, glassPane)
        }

        // Кнопка "Предыдущая страница"
        if (currentPage > 0) {
            val prevButton = ItemStack(Material.ARROW)
            val prevMeta = prevButton.itemMeta
            prevMeta?.displayName(Component.text("Предыдущая страница").color(NamedTextColor.YELLOW))
            prevButton.itemMeta = prevMeta
            inventory.setItem(PAGE_SIZE + 3, prevButton)
        }

        // Кнопка "Следующая страница"
        if (currentPage < totalPages - 1) {
            val nextButton = ItemStack(Material.ARROW)
            val nextMeta = nextButton.itemMeta
            nextMeta?.displayName(Component.text("Следующая страница").color(NamedTextColor.YELLOW))
            nextButton.itemMeta = nextMeta
            inventory.setItem(PAGE_SIZE + 5, nextButton)
        }

        // Кнопка "Закрыть"
        val closeButton = ItemStack(Material.BARRIER)
        val closeMeta = closeButton.itemMeta
        closeMeta?.displayName(Component.text("Закрыть").color(NamedTextColor.RED))
        closeButton.itemMeta = closeMeta
        inventory.setItem(PAGE_SIZE + 4, closeButton)
    }

    /**
     * Обрабатывает клики по инвентарю
     */
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val menuData = openMenus[player.uniqueId] ?: return

        // Проверяем, что клик был в нашем инвентаре
        // Используем современный метод проверки заголовка
        if (!event.view.title().toString().contains("Варп-точки")) return

        // Отменяем стандартное поведение
        event.isCancelled = true

        // Получаем слот, по которому кликнули
        val slot = event.rawSlot

        // Обработка навигационных кнопок
        if (slot >= PAGE_SIZE) {
            when (slot) {
                // Предыдущая страница
                PAGE_SIZE + 3 -> {
                    if (menuData.currentPage > 0) {
                        openWarpMenu(player, menuData.currentPage - 1)
                    }
                    return
                }
                // Закрыть меню
                PAGE_SIZE + 4 -> {
                    player.closeInventory()
                    return
                }
                // Следующая страница
                PAGE_SIZE + 5 -> {
                    val totalPages = (menuData.warps.size + PAGE_SIZE - 1) / PAGE_SIZE
                    if (menuData.currentPage < totalPages - 1) {
                        openWarpMenu(player, menuData.currentPage + 1)
                    }
                    return
                }
            }
            return
        }

        // Вычисляем индекс варпа в общем списке
        val warpIndex = menuData.currentPage * PAGE_SIZE + slot

        // Проверяем, что индекс валидный
        if (warpIndex < 0 || warpIndex >= menuData.warps.size) return

        val warpName = menuData.warps[warpIndex]

        // Проверяем, включен ли варп
        if (!warpManager.isWarpEnabled(warpName) && !player.hasPermission("jtdemo.admin")) {
            player.sendMessage(Component.text("Эта варп-точка отключена.").color(NamedTextColor.RED))
            return
        }

        // Телепортируем игрока
        if (warpManager.teleportPlayerToWarp(player, warpName)) {
            player.sendMessage(Component.text("Вы телепортированы к варп-точке '$warpName'.").color(NamedTextColor.GREEN))
            player.closeInventory()
        } else {
            player.sendMessage(Component.text("Не удалось телепортироваться к варп-точке '$warpName'.").color(NamedTextColor.RED))
        }
    }

    /**
     * Обрабатывает закрытие инвентаря
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return

        // Удаляем информацию о меню при закрытии
        // Используем современный метод проверки заголовка
        if (event.view.title().toString().contains("Варп-точки")) {
            openMenus.remove(player.uniqueId)
        }
    }

    /**
     * Класс для хранения данных о меню
     */
    data class MenuData(
        val warps: List<String>,
        val currentPage: Int
    )
}