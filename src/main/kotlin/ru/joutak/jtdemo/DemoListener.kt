package ru.joutak.jtdemo

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Beehive
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent


class DemoListener(private val demoManager: DemoManager) : Listener {

    // Префикс
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        demoManager.applyDemoPrefixOnJoin(player)
    }

    // Расход голода (выкл)
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity
        if (player is Player && demoManager.isInDemoMode(player)) {
            event.isCancelled = true
            player.foodLevel = 20
        }
    }

    // Атака мобов
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Player && demoManager.isInDemoMode(damager)) {
            event.isCancelled = true
        }
    }

    // Подбор предметов
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onItemPickup(event: EntityPickupItemEvent) {
        val entity = event.entity
        if (entity is Player && demoManager.isInDemoMode(entity)) {
            event.isCancelled = true
        }
    }

    // Инвертарь лок
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked
        if (player is Player && demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Сборщик
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val player = event.player
        if (player is Player && demoManager.isInDemoMode(player)) {
            if (event.inventory.type.name.contains("CRAFTER") ||
                event.view.title.contains("Сборщик") ||
                event.view.title.contains("Crafter")) {
                event.isCancelled = true
            }
        }
    }

    // Разрушение блоков
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Размещение блоков
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Ведра набирать
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBucketFill(event: PlayerBucketFillEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Ведра ставить
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBucketEmpty(event: PlayerBucketEmptyEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Стойки для брони
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            event.isCancelled = true
        }
    }

    // Вращение рамок
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            val entity = event.rightClicked
            if (entity is ItemFrame || entity is Painting ||
                entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME ||
                entity.type == EntityType.PAINTING) {
                event.isCancelled = true
            }
        }
    }

    // Картины, рамки
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        val remover = event.remover
        if (remover is Player && demoManager.isInDemoMode(remover)) {
            event.isCancelled = true
        }
    }

    // Хендлер взаимодействия
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (!demoManager.isInDemoMode(player)) {
            return
        }

        val block = event.clickedBlock ?: return

        // Проверяем блоки
        when (block.type) {
            // Проигрыватель
            Material.JUKEBOX -> {
                event.isCancelled = true
                return
            }

            // Сбор меда
            Material.BEEHIVE, Material.BEE_NEST -> {
                val item = event.item
                if (item != null) {
                    val type = item.type
                    if (type.toString().contains("BOTTLE") ||
                        type.toString().contains("HONEYCOMB") ||
                        type.toString().contains("SHEARS")) {
                        event.isCancelled = true
                        return
                    }
                }

                // Улей
                event.isCancelled = true
                return
            }

            // Книжные полки
            Material.BOOKSHELF, Material.CHISELED_BOOKSHELF -> {
                event.isCancelled = true
                return
            }

            // Сборщик
            Material.CRAFTING_TABLE -> {
                if (block.type.name.contains("CRAFTER")) {
                    event.isCancelled = true
                    return
                }
            }

            // Калитки
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
            Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE -> {
                event.isCancelled = true
                return
            }

            // Разрешаем двери, кнопки, нажимные плиты, порталы
            Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
            Material.CRIMSON_DOOR, Material.WARPED_DOOR, Material.IRON_DOOR,
            Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
            Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
            Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
            Material.POLISHED_BLACKSTONE_BUTTON,
            Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
            Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
            Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
            Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
            Material.WARPED_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
            Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Material.NETHER_PORTAL, Material.END_PORTAL -> {
                return
            }

            else -> {
                // Проверка других блоков
                val typeName = block.type.toString()
                if (typeName.contains("SIGN") ||
                    typeName.contains("MAP") ||
                    typeName.contains("FRAME") ||
                    typeName.contains("BANNER") ||
                    typeName.contains("FLOWER_POT") ||
                    typeName.contains("POTTED_") ||
                    typeName.contains("CRAFTER") ||
                    block.type == Material.CRAFTING_TABLE ||
                    block.type == Material.ENCHANTING_TABLE ||
                    block.type == Material.ANVIL ||
                    block.type == Material.BREWING_STAND) {
                    event.isCancelled = true
                }
            }
        }

        // Предмет в руке
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

    // Картины и рамки
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val entity = event.entity

        if (damager is Player && demoManager.isInDemoMode(damager)) {
            if (entity is Painting || entity is ItemFrame ||
                entity.type == EntityType.PAINTING || entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME) {
                event.isCancelled = true
            }
        }
    }

    // ТП на спавн деморежима
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (demoManager.isInDemoMode(player)) {
            // Очистка предметов
            event.drops.clear()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        if (demoManager.isInDemoMode(player)) {
            // Точка возрождения в мире
            val demoSpawn = demoManager.getDemoSpawn()
            if (demoSpawn != null) {
                event.respawnLocation = demoSpawn
            } else {
                event.respawnLocation = player.world.spawnLocation
            }
        }
    }
}