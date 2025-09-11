package ru.joutak.jtdemo.listeners

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEvent
import ru.joutak.jtdemo.DemoManager

/**
 * Обрабатывает события, связанные с блоками
 */
class BlockDemoListener(demoManager: DemoManager) : BaseDemoListener(demoManager) {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-block-breaking")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-block-placing")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!shouldHaveRestrictions(player)) {
            return
        }

        val block = event.clickedBlock ?: return
        val action = event.action

        // Обработка правого клика по блокам
        if (action == Action.RIGHT_CLICK_BLOCK) {
            when (block.type) {
                // Двери
                Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
                Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR,
                Material.MANGROVE_DOOR, Material.CHERRY_DOOR, Material.BAMBOO_DOOR -> {
                    if (!demoManager.isAllowed("interactive-blocks.doors.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Люки
                Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
                Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
                Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR,
                Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.BAMBOO_TRAPDOOR -> {
                    if (!demoManager.isAllowed("interactive-blocks.trapdoors.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Калитки
                Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
                Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
                Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE,
                Material.MANGROVE_FENCE_GATE, Material.CHERRY_FENCE_GATE, Material.BAMBOO_FENCE_GATE -> {
                    if (!demoManager.isAllowed("interactive-blocks.gates.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Кнопки
                Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
                Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
                Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
                Material.POLISHED_BLACKSTONE_BUTTON, Material.MANGROVE_BUTTON,
                Material.CHERRY_BUTTON, Material.BAMBOO_BUTTON -> {
                    if (!demoManager.isAllowed("interactive-blocks.buttons.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Другие интерактивные блоки
                Material.CRAFTING_TABLE -> {
                    if (!demoManager.isAllowed("interactive-blocks.crafting-tables.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }
                Material.JUKEBOX -> {
                    if (!demoManager.isAllowed("interactive-blocks.jukeboxes.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }
                Material.BEEHIVE, Material.BEE_NEST -> {
                    if (!demoManager.isAllowed("interactive-blocks.beehives.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }
                Material.BOOKSHELF, Material.CHISELED_BOOKSHELF -> {
                    if (!demoManager.isAllowed("interactive-blocks.bookshelves.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }
                Material.LECTERN -> {
                    if (!demoManager.isAllowed("interactive-blocks.lecterns.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }
                else -> {
                    // Автокрафтеры (новые блоки в 1.20+)
                    if (block.type.name.contains("CRAFTER")) {
                        if (!demoManager.isAllowed("interactive-blocks.crafters.enabled")) {
                            event.isCancelled = true
                            return
                        }
                    }
                }
            }
        }

        // Обработка нажимных плит
        if (action == Action.PHYSICAL) {
            if (block.type.toString().contains("PRESSURE_PLATE")) {
                if (!demoManager.isAllowed("interactive-blocks.pressure-plates.enabled")) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Блокировка запрещенных взаимодействий
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            val typeName = block.type.toString()
            if (typeName.contains("SIGN") ||
                typeName.contains("MAP") ||
                typeName.contains("FRAME") ||
                typeName.contains("BANNER") ||
                typeName.contains("FLOWER_POT") ||
                typeName.contains("POTTED_") ||
                block.type == Material.ENCHANTING_TABLE ||
                block.type == Material.ANVIL ||
                block.type == Material.BREWING_STAND) {
                event.isCancelled = true
                return
            }
        }

        // Проверка предметов в руке
        val item = event.item
        if (item != null) {
            val type = item.type
            if (type.toString().contains("MAP") ||
                type.toString().contains("SIGN") ||
                type.toString().contains("BANNER") ||
                type.toString().contains("BOOK") ||
                type.toString().contains("BUCKET") ||
                type.toString().contains("MUSIC_DISC") ||
                type.toString().contains("BOTTLE") ||
                type.toString().contains("HONEYCOMB") ||
                type.toString().contains("SHEARS")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            event.isCancelled = true
        }
    }
}