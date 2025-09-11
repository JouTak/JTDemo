package ru.joutak.jtdemo.listeners

import org.bukkit.entity.Player
import org.bukkit.event.Listener
import ru.joutak.jtdemo.DemoManager

/**
 * Базовый класс для всех слушателей демо-режима
 */
abstract class BaseDemoListener(protected val demoManager: DemoManager) : Listener {
    /**
     * Проверяет, должны ли применяться ограничения демо-режима к игроку
     */
    protected fun shouldHaveRestrictions(player: Player): Boolean {
        return demoManager.shouldHaveDemoRestrictions(player)
    }
}