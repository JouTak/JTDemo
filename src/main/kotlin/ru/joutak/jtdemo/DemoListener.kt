package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent

class DemoListener(private val demoManager: DemoManager) : Listener {

    // ===== СОБЫТИЯ ИГРОКА =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        demoManager.applyDemoPrefixOnJoin(player)
        demoManager.updatePlayerDemoStatus(player)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player
        Bukkit.getScheduler().runTask(demoManager.plugin, Runnable {
            demoManager.updatePlayerPrefix(player)
        })
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.from.x != event.to?.x || event.from.y != event.to?.y || event.from.z != event.to?.z) {
            demoManager.updatePlayerPrefix(event.player)
        }
    }

    // ===== СОБЫТИЯ ВЗАИМОДЕЙСТВИЯ С БЛОКАМИ =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        // Проверяем, находится ли игрок в демо-режиме
        if (!demoManager.shouldHaveDemoRestrictions(player)) {
            return
        }

        val block = event.clickedBlock ?: return
        val action = event.action

        // Обработка взаимодействия с блоками только при правом клике (открытие, использование)
        if (action == Action.RIGHT_CLICK_BLOCK) {
            when (block.type) {
                // Двери - проверяем разрешение
                Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
                Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR -> {
                    // ВАЖНОЕ ИЗМЕНЕНИЕ: Отменяем событие только если действие НЕ разрешено
                    if (!demoManager.isAllowed("interactive-blocks.doors.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Люки - проверяем разрешение
                Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
                Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
                Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR -> {
                    if (!demoManager.isAllowed("interactive-blocks.trapdoors.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Калитки - проверяем разрешение
                Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
                Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
                Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE -> {
                    if (!demoManager.isAllowed("interactive-blocks.gates.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Кнопки - проверяем разрешение
                Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
                Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
                Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
                Material.POLISHED_BLACKSTONE_BUTTON -> {
                    if (!demoManager.isAllowed("interactive-blocks.buttons.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Верстаки
                Material.CRAFTING_TABLE -> {
                    if (!demoManager.isAllowed("interactive-blocks.crafting-tables.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Проигрыватели
                Material.JUKEBOX -> {
                    if (!demoManager.isAllowed("interactive-blocks.jukeboxes.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Ульи
                Material.BEEHIVE, Material.BEE_NEST -> {
                    if (!demoManager.isAllowed("interactive-blocks.beehives.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Книжные полки
                Material.BOOKSHELF, Material.CHISELED_BOOKSHELF -> {
                    if (!demoManager.isAllowed("interactive-blocks.bookshelves.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Кафедры
                Material.LECTERN -> {
                    if (!demoManager.isAllowed("interactive-blocks.lecterns.enabled")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Автокрафтеры (если есть)
                else -> {
                    if (block.type.name.contains("CRAFTER")) {
                        if (!demoManager.isAllowed("interactive-blocks.crafters.enabled")) {
                            event.isCancelled = true
                            return
                        }
                    }
                }
            }
        }

        // Обработка нажимных плит при наступании (PHYSICAL)
        if (action == Action.PHYSICAL) {
            if (block.type.toString().contains("PRESSURE_PLATE")) {
                if (!demoManager.isAllowed("interactive-blocks.pressure-plates.enabled")) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Блокировка других взаимодействий, которые не должны быть разрешены
        // Только для левого клика (разрушение) и правого клика с неразрешенными блоками
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            // Специальные блоки, с которыми запрещено взаимодействовать
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

        // Проверка предметов в руке, с которыми запрещено взаимодействовать
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

    // ===== СОБЫТИЯ ИНВЕНТАРЯ =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            val invType = event.inventory.type
            val title = event.view.title().toString()

            // Проверяем тип инвентаря и соответствующие разрешения
            when (invType) {
                InventoryType.CHEST, InventoryType.BARREL, InventoryType.ENDER_CHEST -> {
                    // ИСПРАВЛЕНО: Отменяем событие только если открытие НЕ разрешено
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
                    // Крафтеры и другие нестандартные инвентари
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
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            // Проверка разрешения на использование инвентаря
            if (!demoManager.isAllowed("inventory.allow-inventory-use")) {
                event.isCancelled = true
                return
            }

            // Проверка взятия предметов из контейнеров
            val invType = event.inventory.type

            // Определяем тип контейнера и соответствующий путь в конфигурации
            val containerPath = when (invType) {
                InventoryType.CHEST, InventoryType.BARREL, InventoryType.ENDER_CHEST ->
                    "interactive-blocks.chests.take-items"
                InventoryType.SHULKER_BOX ->
                    "interactive-blocks.shulker-boxes.take-items"
                InventoryType.FURNACE, InventoryType.BLAST_FURNACE, InventoryType.SMOKER ->
                    "interactive-blocks.furnaces.take-items"
                else -> null
            }

            // Если это контейнер и взятие предметов запрещено
            if (containerPath != null && !demoManager.isAllowed(containerPath)) {
                // Блокируем взаимодействия, связанные с перемещением предметов
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            // Проверка разрешения на использование инвентаря
            if (!demoManager.isAllowed("inventory.allow-inventory-use")) {
                event.isCancelled = true
                return
            }

            // Проверка типа инвентаря и разрешения на взятие предметов
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

    // ===== СОБЫТИЯ БЛОКОВ =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-block-breaking")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-block-placing")) {
                event.isCancelled = true
            }
        }
    }

    // ===== СОБЫТИЯ СУЩНОСТЕЙ =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val entity = event.entity

        if (damager is Player && demoManager.shouldHaveDemoRestrictions(damager)) {
            // Защита картин и рамок
            if (entity is Painting || entity is ItemFrame ||
                entity.type == EntityType.PAINTING || entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME) {
                event.isCancelled = true
            }
            // Проверка урона игрокам
            else if (entity is Player && !demoManager.isAllowed("movement.allow-player-damage")) {
                event.isCancelled = true
            }
            // Проверка урона мобам
            else if (entity !is Player && !demoManager.isAllowed("movement.allow-mob-damage")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player && demoManager.shouldHaveDemoRestrictions(entity)) {
            if (demoManager.isAllowed("movement.invulnerable")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            if (demoManager.isAllowed("movement.preserve-food-level")) {
                event.isCancelled = true
                player.foodLevel = 20
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player && demoManager.shouldHaveDemoRestrictions(entity)) {
            if (!demoManager.isAllowed("inventory.allow-item-pickup")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onItemDrop(event: PlayerDropItemEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            if (!demoManager.isAllowed("inventory.allow-item-drop")) {
                event.isCancelled = true
            }
        }
    }

    // ===== ДРУГИЕ СОБЫТИЯ =====

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            val entity = event.rightClicked
            if (entity is ItemFrame || entity is Painting ||
                entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME ||
                entity.type == EntityType.PAINTING) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        if (remover is Player && demoManager.shouldHaveDemoRestrictions(remover)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.drops.clear()
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            val demoSpawn = demoManager.getDemoSpawn()
            if (demoSpawn != null) {
                event.respawnLocation = demoSpawn
                Bukkit.getScheduler().runTask(demoManager.plugin, Runnable {
                    demoManager.updatePlayerPrefix(player)
                })
            } else {
                event.respawnLocation = player.world.spawnLocation
            }
        }
    }
}