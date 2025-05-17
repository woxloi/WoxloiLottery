package red.man10.woxloiLottery

import com.comphenix.protocol.ProtocolManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.command.CommandSender
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.commands.WoxloiLotteryCommand
import red.man10.woxloiLottery.commands.logic.EditCommand

class WoxloiLottery : JavaPlugin(), Listener {
    companion object {
        val version = "2025/1/11"
        var commandSender: CommandSender? = null
        val prefix = "§f[§5§lWoxloi§e§lLottery§f]"
        lateinit var plugin: JavaPlugin
        lateinit var protocolManager: ProtocolManager
        lateinit var commandRouter: WoxloiLotteryCommand
        lateinit var economy: Economy
    }

    override fun onEnable() {
        plugin = this

        val econ = server.servicesManager.getRegistration(Economy::class.java)?.provider
        if (econ == null) {
            logger.severe("Vault経済APIが見つかりません！")
            server.pluginManager.disablePlugin(this)
            return
        }
        economy = econ

        // WoxloiLotteryCommandのインスタンス化時に「this」を渡して初期化
        commandRouter = WoxloiLotteryCommand(this)

        // コマンド設定
        getCommand("wlottery")!!.setExecutor(commandRouter)
        getCommand("wlottery")!!.tabCompleter = commandRouter
        getCommand("wlot")!!.setExecutor(commandRouter)
        getCommand("wlot")!!.tabCompleter = commandRouter

        //インスタンス化
        server.pluginManager.registerEvents(EditCommand(this), this)

        getLogger().info("宝くじプラグインが起動しました")
    }

    override fun onDisable() {
        getLogger().info("宝くじプラグインが停止しました")
    }
}
