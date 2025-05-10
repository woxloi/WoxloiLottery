package red.man10.woxloiLottery.commands.logic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.LotteryManager

class BuyCommand(private var plugin: JavaPlugin) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        // 引数が正しく渡されているか確認
        if (args.size < 3) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lコマンドの使い方が違います")
            return false
        }

        val name = args[1]  // 宝くじの名前を取得
        val quantity: Int // 購入する個数
        var ticketNumber: Int? = null  // 番号を格納する変数

        try {
            // 個数を整数に変換
            quantity = args[2].toInt()

            // 番号が指定されていれば、その番号を使用
            if (args.size > 3) {
                ticketNumber = args[3].toInt()
            }
        } catch (e: NumberFormatException) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l個数と番号は整数で指定してください")
            return false
        }

        // プレイヤーからコマンドが実行されたか確認
        if (sender !is Player) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lプレイヤーだけがこのコマンドを使用できます")
            return false
        }

        // 宝くじが存在するか確認
        if (!LotteryManager.exists(name)) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lその名前の宝くじは存在しません")
            return false
        }

        // 番号が指定されていない場合はランダムな番号を選択
        if (ticketNumber == null) {
            ticketNumber = LotteryManager.nextTicketNumber(name)
        }

        // 購入処理（購入に成功した場合はメッセージを送信）
        try {
            val success = LotteryManager.buyLottery(name, sender as Player, quantity, ticketNumber)
            if (success) {
                sender.sendMessage(WoxloiLottery.prefix + "§a§l宝くじ「$name」を$quantity 枚購入しました（番号: $ticketNumber）")
            } else {
                sender.sendMessage(WoxloiLottery.prefix + "§c§l宝くじの購入に失敗しました")
            }
        } catch (e: Exception) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lエラーが発生しました: ${e.message}")
            return false
        }

        return true
    }
}
