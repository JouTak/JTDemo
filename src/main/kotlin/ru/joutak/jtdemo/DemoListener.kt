package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Beehive
import org.bukkit.block.Lectern
import org.bukkit.block.ShulkerBox
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
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
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
import org.bukkit.inventory.ItemStack


class DemoListener(private val demoManager: DemoManager) : Listener {

    // Обработка входа игрока для применения префикса
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        demoManager.applyDemoPrefixOnJoin(player)

        // Также обновляем статус игрока
        demoManager.updatePlayerDemoStatus(player)
    }

    // Обработка телепортации игрока для проверки префикса
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        val player = event.player

        // Запускаем проверку префикса в следующем тике (после телепортации)
        Bukkit.getScheduler().runTask(demoManager.plugin, Runnable {
            demoManager.updatePlayerPrefix(player)
        })
    }

    // Обработка движения игрока (для проверки префикса)
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Проверяем только изменение координат XYZ, а не поворот головы
        if (event.from.x != event.to?.x || event.from.y != event.to?.y || event.from.z != event.to?.z) {
            val player = event.player
            demoManager.updatePlayerPrefix(player)
        }
    }

    // Предотвращение расхода голода
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            if (demoManager.isAllowed("gameplay", "preserve-food-level")) {
                event.isCancelled = true
                player.foodLevel = 20
            }
        }
    }

    // Предотвращение атаки игроков или сущностей
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Player && demoManager.shouldHaveDemoRestrictions(damager)) {
            // Проверяем, можно ли наносить урон мобам или игрокам
            if (event.entity is Player) {
                if (!demoManager.isAllowed("gameplay", "allow-player-damage")) {
                    event.isCancelled = true
                }
            } else {
                if (!demoManager.isAllowed("gameplay", "allow-mob-damage")) {
                    event.isCancelled = true
                }
            }
        }
    }

    // Предотвращение подбора предметов
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player && demoManager.shouldHaveDemoRestrictions(entity)) {
            if (!demoManager.isAllowed("gameplay", "allow-item-pickup")) {
                event.isCancelled = true
            }
        }
    }

    // Предотвращение броска предметов
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemDrop(event: PlayerDropItemEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            if (!demoManager.isAllowed("gameplay", "allow-item-drop")) {
                event.isCancelled = true
            }
        }
    }

    // Блокирование открытия некоторых инвентарей
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player
        if (player is Player && demoManager.shouldHaveDemoRestrictions(player)) {
            // Проверяем тип инвентаря
            val invName = event.inventory.type.name
            val title = event.view.title

            // Проверяем кафедру
            if (event.inventory.type == InventoryType.LECTERN) {
                if (!demoManager.isAllowed("blocks", "allow-lectern")) {
                    event.isCancelled = true
                }
                return
            }

            if (invName.contains("CRAFTER") || title.contains("Автокрафтер") || title.contains("Crafter")) {
                if (!demoManager.isAllowed("blocks", "allow-crafters")) {
                    event.isCancelled = true
                }
            } else if (invName.contains("SHULKER")) {
                if (!demoManager.isAllowed("blocks", "allow-shulker-boxes")) {
                    event.isCancelled = true
                }
            } else if (invName.contains("CHEST")) {
                if (!demoManager.isAllowed("blocks", "allow-chests")) {
                    event.isCancelled = true
                }
            } else if (invName.contains("FURNACE")) {
                if (!demoManager.isAllowed("blocks", "allow-furnaces")) {
                    event.isCancelled = true
                }
            }
        }
    }

    // Предотвращение разрушения блоков
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            if (!demoManager.isAllowed("blocks", "allow-block-breaking")) {
                event.isCancelled = true
            }
        }
    }

    // Предотвращение размещения блоков
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    // Предотвращение набора жидкостей ведрами
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    // Предотвращение размещения жидкостей ведрами
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    // Предотвращение манипуляций со стойками для брони
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            event.isCancelled = true
        }
    }

    // Предотвращение взаимодействия с картинами и рамками (вращение, взятие предметов)
    @EventHandler(priority = EventPriority.HIGHEST)
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

    // Предотвращение разрушения висящих сущностей (картин, рамок)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        if (remover is Player && demoManager.shouldHaveDemoRestrictions(remover)) {
            event.isCancelled = true
        }
    }

    // Обработка взаимодействия игрока (таблички, карты, рамки, двери, кнопки и т.д.)
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!demoManager.shouldHaveDemoRestrictions(player)) {
            return
        }

        val block = event.clickedBlock ?: return
        val itemInHand = event.item

        // Особая обработка для кафедры (lectern)
        if (block.type == Material.LECTERN) {
            // Полностью блокируем взаимодействие с кафедрами, если они не разрешены
            if (!demoManager.isAllowed("blocks", "allow-lectern")) {
                event.isCancelled = true
                return
            }
            // Если разрешено, то не трогаем дальше
            return
        }

        // Проверяем блоки, с которыми нельзя взаимодействовать
        when (block.type) {
            // Проигрыватель дисков
            Material.JUKEBOX -> {
                if (!demoManager.isAllowed("blocks", "allow-jukeboxes")) {
                    event.isCancelled = true
                    return
                }
            }

            // Ульи и пчелиные домики
            Material.BEEHIVE, Material.BEE_NEST -> {
                if (!demoManager.isAllowed("blocks", "allow-beehives")) {
                    event.isCancelled = true
                    return
                }
            }

            // Книжные полки
            Material.BOOKSHELF, Material.CHISELED_BOOKSHELF -> {
                if (!demoManager.isAllowed("blocks", "allow-bookshelves")) {
                    event.isCancelled = true
                    return
                }
            }

            // Верстаки и автокрафтеры
            Material.CRAFTING_TABLE -> {
                // Проверяем тип блока, чтобы различать обычный верстак и автокрафтер
                if (block.type.name.contains("CRAFTER")) {
                    if (!demoManager.isAllowed("blocks", "allow-crafters")) {
                        event.isCancelled = true
                        return
                    }
                } else {
                    if (!demoManager.isAllowed("blocks", "allow-crafting-tables")) {
                        event.isCancelled = true
                        return
                    }
                }
            }

            // Калитки
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE -> {
                if (!demoManager.isAllowed("blocks", "allow-gates")) {
                    event.isCancelled = true
                    return
                }
            }

            // Двери
            Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR -> {
                if (!demoManager.isAllowed("blocks", "allow-doors")) {
                    event.isCancelled = true
                    return
                }
            }

            // Люки
            Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
            Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.IRON_TRAPDOOR -> {
                if (!demoManager.isAllowed("blocks", "allow-trapdoors")) {
                    event.isCancelled = true
                    return
                }
            }

            // Кнопки
            Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
            Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
            Material.POLISHED_BLACKSTONE_BUTTON -> {
                if (!demoManager.isAllowed("blocks", "allow-buttons")) {
                    event.isCancelled = true
                    return
                }
            }

            // Нажимные плиты
            Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
            Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
            Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
            Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
            Material.WARPED_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE -> {
                if (!demoManager.isAllowed("blocks", "allow-pressure-plates")) {
                    event.isCancelled = true
                    return
                }
            }

            // Порталы всегда разрешены
            Material.NETHER_PORTAL, Material.END_PORTAL -> {
                return
            }

            else -> {
                // Для всех других блоков проверяем, являются ли они картами, табличками, рамками и т.д.
                val typeName = block.type.toString()
                if (typeName.contains("SIGN") ||
                    typeName.contains("MAP") ||
                    typeName.contains("FRAME") ||
                    typeName.contains("BANNER") ||
                    typeName.contains("FLOWER_POT") ||
                    typeName.contains("POTTED_") ||
                    typeName.contains("CRAFTER") ||
                    block.type == Material.ENCHANTING_TABLE ||
                    block.type == Material.ANVIL ||
                    block.type == Material.BREWING_STAND) {
                    event.isCancelled = true
                }
            }
        }

        // Также проверяем предмет в руке
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

    // Предотвращение разрушения картин и рамок для предметов
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val entity = event.entity

        if (damager is Player && demoManager.shouldHaveDemoRestrictions(damager)) {
            // Картины и рамки защищены всегда
            if (entity is Painting || entity is ItemFrame ||
                entity.type == EntityType.PAINTING || entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME) {
                event.isCancelled = true
            }
            // Если сущность - игрок, и повреждение игроков запрещено
            else if (entity is Player && !demoManager.isAllowed("gameplay", "allow-player-damage")) {
                event.isCancelled = true
            }
            // Если сущность - моб, и повреждение мобов запрещено
            else if (entity !is Player && !demoManager.isAllowed("gameplay", "allow-mob-damage")) {
                event.isCancelled = true
            }
        }
    }

    // Обработка смерти игрока и возрождение в точке спавна демо-режима
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            // Очищаем выпадающие предметы у игрока в демо-режиме
            event.drops.clear()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        if (demoManager.shouldHaveDemoRestrictions(player)) {
            // Получаем точку возрождения в демо-режиме
            val demoSpawn = demoManager.getDemoSpawn()
            if (demoSpawn != null) {
                event.respawnLocation = demoSpawn

                // Проверяем префикс в следующем тике (после возрождения)
                Bukkit.getScheduler().runTask(demoManager.plugin, Runnable {
                    demoManager.updatePlayerPrefix(player)
                })
            } else {
                // Используем точку возрождения мира по умолчанию вместо 0,0,0
                event.respawnLocation = player.world.spawnLocation
            }
        }
    }
}