package ru.joutak.jtdemo.listeners

import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Painting
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import ru.joutak.jtdemo.DemoManager

/**
 * Обрабатывает события, связанные с сущностями
 */
class EntityDemoListener(demoManager: DemoManager) : BaseDemoListener(demoManager) {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val entity = event.entity

        if (damager is Player && shouldHaveRestrictions(damager)) {
            // Защита картин и рамок
            if (entity is Painting || entity is ItemFrame ||
                entity.type == EntityType.PAINTING || entity.type == EntityType.ITEM_FRAME ||
                entity.type == EntityType.GLOW_ITEM_FRAME) {
                event.isCancelled = true
            }
            // Урон игрокам
            else if (entity is Player && !demoManager.isAllowed("movement.allow-player-damage")) {
                event.isCancelled = true
            }
            // Урон мобам
            else if (entity !is Player && !demoManager.isAllowed("movement.allow-mob-damage")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player && shouldHaveRestrictions(entity)) {
            if (demoManager.isAllowed("movement.invulnerable")) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
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
        if (remover is Player && shouldHaveRestrictions(remover)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
            event.isCancelled = true
        }
    }
}