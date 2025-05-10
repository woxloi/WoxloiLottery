package red.man10.woxloiLottery.commands.logic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.LotteryManager

class CreateCommand(private var plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // 引数が正しく渡されているか確認
        if (args.size < 2) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lコマンドの使い方が違います")
            return false
        }

        val name = args[1] // 宝くじの名前を取得

        // プレイヤーからコマンドが実行されたか確認
        if (sender !is Player) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lプレイヤーだけがこのコマンドを使用できます")
            return false
        }

        // 既に同じ名前の宝くじが存在するか確認
        if (LotteryManager.exists(name)) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lその名前の宝くじは既に存在します")
            return false
        }

        try {
            // 宝くじを作成する
            LotteryManager.createLottery(name)
            sender.sendMessage(WoxloiLottery.prefix + "§a§l宝くじ「$name」を作成しました")
            sender.sendMessage(WoxloiLottery.prefix + "§a§l/wlot edit 「$name」で管理可能")
        } catch (e: Exception) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lエラーが発生しました: ${e.message}")
            return false
        }
        return true
    }
}
