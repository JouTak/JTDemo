package ru.joutak.jtdemo.listeners

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import ru.joutak.jtdemo.DemoManager

/**
 * Обрабатывает события, связанные с игроками
 */
class PlayerDemoListener(demoManager: DemoManager) : BaseDemoListener(demoManager) {

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

    @EventHandler(priority = EventPriority.NORMAL)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity
        if (player is Player && shouldHaveRestrictions(player)) {
            if (demoManager.isAllowed("movement.preserve-food-level")) {
                event.isCancelled = true
                player.foodLevel = 20
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        if (shouldHaveRestrictions(player)) {
            event.drops.clear()
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        if (shouldHaveRestrictions(player)) {
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