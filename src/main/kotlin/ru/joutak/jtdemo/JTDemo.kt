package ru.joutak.jtdemo

import org.bukkit.plugin.java.JavaPlugin


class JTDemo : JavaPlugin() {

    lateinit var demoManager: DemoManager

    override fun onEnable() {
        saveDefaultConfig()
        demoManager = DemoManager(this)
        server.pluginManager.registerEvents(DemoListener(demoManager), this)
        getCommand("jtdemo")?.setExecutor(DemoCommands(demoManager))
        logger.info("JTDemo плагин включен!")
    }

    override fun onDisable() {
        demoManager.saveData()
        logger.info("JTDemo плагин выключен!")
    }
}