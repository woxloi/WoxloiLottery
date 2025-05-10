package red.man10.woxloiLottery.commands.logic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.LotteryManager

class StartCommand(plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // コマンドが /wlot start で始まるか確認
        if (args.size < 2) {
            // 引数が不足している場合、正しい使い方を表示
            sender.sendMessage(WoxloiLottery.prefix + "§c§lコマンドの使い方が違います")
            return false
        }

        val lotteryName = args[1]

        // 宝くじが存在するか確認
        if (!LotteryManager.exists(lotteryName)) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lその名前の宝くじは存在しません")

            val existingLotteries = LotteryManager.getAllLotteryNames()
            if (existingLotteries.isEmpty()) {
                sender.sendMessage(WoxloiLottery.prefix + "§e現在、作成されている宝くじはありません。")
            } else {
                sender.sendMessage(WoxloiLottery.prefix + "§e現在利用可能な宝くじ一覧:")
                existingLotteries.forEach {
                    sender.sendMessage("§f- §b$it")
                }
            }

            return false
        }

        try {
            // 宝くじの抽選を開始
            LotteryManager.startLottery(lotteryName)
            sender.sendMessage(WoxloiLottery.prefix + "§a§l宝くじ「$lotteryName」の抽選が開始されました！")
        } catch (e: Exception) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l抽選中にエラーが発生しました: ${e.message}")
        }

        return true
    }
}
