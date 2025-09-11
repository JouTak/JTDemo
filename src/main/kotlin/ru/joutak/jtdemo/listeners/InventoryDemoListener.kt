package ru.joutak.jtdemo.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import ru.joutak.jtdemo.DemoManager

/**
 * Обрабатывает события, связанные с инвентарем и контейнерами
 */
class InventoryDemoListener(demoManager: DemoManager) : BaseDemoListener(demoManager) {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player
        if (player is Player && shouldHaveRestrictions(player)) {
            val invType = event.inventory.type
            val title = event.view.title().toString()

            // Проверяем разные типы контейнеров
            when (invType) {
                InventoryType.CHEST, InventoryType.BARREL, InventoryType.ENDER_CHEST -> {
                    // ВАЖНО: отменяем только если действие НЕ разрешено
                    if (!demoManager.isAllowed("interactive-blocks.chests.open")) {
                        event.isCancelled = true
                    }
                }
                InventoryType.SHULKER_BOX -> {
                    if (!demoManager.isAllowed("interactive-blocks.shulker-boxes.open")) {
                        event.isCancelled = true
                    }
                }
                InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER -> {
                    if (!demoManager.isAllowed("interactive-blocks.furnaces.open")) {
                        event.isCancelled = true
                    }
                }
                InventoryType.WORKBENCH -> {
                    if (!demoManager.isAllowed("interactive-blocks.crafting-tables.enabled")) {
                        event.isCancelled = true
                    }
                }
                InventoryType.LECTERN -> {
                    if (!demoManager.isAllowed("interactive-blocks.lecterns.enabled")) {
                        event.isCancelled = true
                    }
                }
                else -> {
                    // Крафтеры и другие специальные инвентари
                    if (invType.name.contains("CRAFTER") ||
                        title.contains("Автокрафтер") ||
                        title.contains("Crafter")) {
                        if (!demoManager.isAllowed("interactive-blocks.crafters.enabled")) {
                            event.isCancelled = true
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player is Player && shouldHaveRestrictions(player)) {
            // Основное разрешение на использование инвентаря
            if (!demoManager.isAllowed("inventory.allow-inventory-use")) {
                event.isCancelled = true
                return
            }

            // Проверка взятия предметов из контейнеров
            val invType = event.inventory.type

            // Определяем, какой путь в конфиге использовать
            val containerPath = when (invType) {
                InventoryType.CHEST, InventoryType.BARREL, InventoryType.ENDER_CHEST ->
                    "interactive-blocks.chests.take-items"
                InventoryType.SHULKER_BOX ->
                    "interactive-blocks.shulker-boxes.take-items"
                InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER ->
                    "interactive-blocks.furnaces.take-items"
                else -> null
            }

            // Если путь определен и действие не разрешено
            if (containerPath != null && !demoManager.isAllowed(containerPath)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked
        if (player is Player && shouldHaveRestrictions(player)) {
            // Общее разрешение на использование инвентаря
            if (!demoManager.isAllowed("inventory.allow-inventory-use")) {
                event.isCancelled = true
                return
            }

            // Проверка типа инвентаря
            val invType = event.inventory.type

            val containerPath = when (invType) {
                InventoryType.CHEST, InventoryType.BARREL, InventoryType.ENDER_CHEST ->
                    "interactive-blocks.chests.take-items"
                InventoryType.SHULKER_BOX ->
                    "interactive-blocks.shulker-boxes.take-items"
                InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER ->
                    "interactive-blocks.furnaces.take-items"
                else -> null
            }

            if (containerPath != null && !demoManager.isAllowed(containerPath)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player && shouldHaveRestrictions(entity)) {
            if (!demoManager.isAllowed("inventory.allow-item-pickup")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemDrop(event: PlayerDropItemEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-item-drop")) {
                event.isCancelled = true
            }
        }
    }
}