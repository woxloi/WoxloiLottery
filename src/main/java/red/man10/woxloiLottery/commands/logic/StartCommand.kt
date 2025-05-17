package red.man10.woxloiLottery.commands.logic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.LotteryManager
import org.bukkit.Bukkit
import net.milkbowl.vault.economy.Economy

class StartCommand(plugin: JavaPlugin) : CommandExecutor {

    private val economy: Economy = WoxloiLottery.economy  // Vault経済API

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

    // 当選者に賞金を送る処理
    private fun sendPrizes(winners: List<String>) {
        // 当選者リストから、等級に応じて賞金を送金
        val prizeAmounts = mapOf(
            1 to 100000000.0,  // 1等: 1億円
            2 to 10000000.0,   // 2等: 1000万円
            3 to 1000000.0,    // 3等: 100万円
            4 to 30000.0,      // 4等: 30000円
            5 to 1000.0        // 5等: 1000円
        )

        var winnerIndex = 0
        for ((rank, prize) in prizeAmounts) {
            if (winnerIndex < winners.size) {
                val winnerName = winners[winnerIndex]
                val winner = Bukkit.getPlayer(winnerName)
                if (winner != null) {
                    // 賞金を送金
                    economy.depositPlayer(winner, prize)
                    winner.sendMessage(WoxloiLottery.prefix + "§aおめでとうございます！あなたは${rank}等に当選しました！賞金: ${prize}円")
                }
                winnerIndex++
            }
        }

        // 残りの当選者にもランダムな賞金を送金（デフォルト処理）
        while (winnerIndex < winners.size) {
            val winnerName = winners[winnerIndex]
            val winner = Bukkit.getPlayer(winnerName)
            if (winner != null) {
                val defaultPrize = 5000.0  // デフォルトの賞金額
                economy.depositPlayer(winner, defaultPrize)
                winner.sendMessage(WoxloiLottery.prefix + "§aおめでとうございます！あなたはランダム賞に当選しました。賞金: $defaultPrize 円")
            }
            winnerIndex++
        }
    }
}
