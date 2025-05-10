package red.man10.woxloiLottery.commands.logic

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.LotteryManager

class EditCommand(private var plugin: JavaPlugin) : CommandExecutor, Listener {

    companion object {
        val waitingForPrizeInput = mutableMapOf<Player, LotteryManager.Lottery>()
        val waitingForChanceInput = mutableMapOf<Player, LotteryManager.Lottery>()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.size < 2) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lコマンドの使い方が違います。")
            return false
        }

        val lotteryName = args[1]

        if (sender !is Player) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lプレイヤーだけがこのコマンドを使用できます。")
            return false
        }

        if (!LotteryManager.exists(lotteryName)) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lその名前の宝くじは存在しません。")
            return false
        }

        openLotteryEditGUI(sender, lotteryName)
        return true
    }

    private fun openLotteryEditGUI(player: Player, lotteryName: String) {
        val inventory: Inventory = Bukkit.createInventory(null, 36, "宝くじ設定: $lotteryName")

        inventory.setItem(0, createGUIItem("賞金額設定", Material.GOLD_INGOT))
        inventory.setItem(1, createGUIItem("当選確率設定", Material.DIAMOND))
        inventory.setItem(2, createGUIItem("番号設定", Material.PAPER))
        inventory.setItem(8, createGUIItem("閉じる", Material.BARRIER))

        player.openInventory(inventory)
    }

    private fun createGUIItem(name: String, material: Material): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.setDisplayName(name)
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem ?: return
        if (clickedItem.type == Material.AIR) return

        val title = event.view.title
        if (!title.startsWith("宝くじ設定:")) return

        event.isCancelled = true

        val lotteryName = title.split(": ").getOrNull(1) ?: return
        val lottery = LotteryManager.getLottery(lotteryName) ?: return

        when (clickedItem.type) {
            Material.GOLD_INGOT -> openPrizeAmountMenu(player, lottery)
            Material.DIAMOND -> openChancesMenu(player, lottery)
            Material.PAPER -> openLotteryDetails(player, lottery)
            Material.BARRIER -> player.closeInventory()
            else -> player.sendMessage(WoxloiLottery.prefix + "§c§lそのアイテムは無効です。")
        }
    }

    private fun openPrizeAmountMenu(player: Player, lottery: LotteryManager.Lottery) {
        player.closeInventory()
        player.sendMessage(WoxloiLottery.prefix + "§a新しい賞金額をチャットで入力してください。キャンセルは「cancel」")
        waitingForPrizeInput[player] = lottery
    }

    private fun openChancesMenu(player: Player, lottery: LotteryManager.Lottery) {
        player.closeInventory()
        player.sendMessage(WoxloiLottery.prefix + "§a新しい当選確率（0.0〜100.0）をチャットで入力してください。キャンセルは「cancel」")
        waitingForChanceInput[player] = lottery
    }

    private fun openLotteryDetails(player: Player, lottery: LotteryManager.Lottery) {
        val inventory: Inventory = Bukkit.createInventory(null, 36, "宝くじ詳細: ${lottery.name}")

        inventory.setItem(0, createGUIItem("賞金額: ¥${lottery.prize}", Material.GOLD_INGOT))
        inventory.setItem(1, createGUIItem("当選確率: ${lottery.chances}%", Material.DIAMOND))
        inventory.setItem(2, createGUIItem("最小番号: ${lottery.minTicketNumber}", Material.PAPER))
        inventory.setItem(3, createGUIItem("最大番号: ${lottery.maxTicketNumber}", Material.PAPER))
        inventory.setItem(8, createGUIItem("閉じる", Material.BARRIER))

        player.openInventory(inventory)
    }

    @EventHandler
    fun onChatInput(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message

        // 賞金額の入力処理
        if (waitingForPrizeInput.containsKey(player)) {
            event.isCancelled = true
            val lottery = waitingForPrizeInput.remove(player)!!

            if (message.equals("cancel", ignoreCase = true)) {
                player.sendMessage(WoxloiLottery.prefix + "§c入力をキャンセルしました。")
                return
            }

            val amount = message.toDoubleOrNull()
            if (amount == null || amount < 0) {
                player.sendMessage(WoxloiLottery.prefix + "§c無効な金額です。もう一度入力してください。")
                waitingForPrizeInput[player] = lottery // 再度元のオブジェクトをマップに戻す
                return
            }

            // 新しい Lottery オブジェクトを作成してマップに再代入
            val updatedLottery = lottery.copy(prize = amount.toInt()) // prize を更新
            waitingForPrizeInput[player] = updatedLottery // マップを更新

            player.sendMessage(WoxloiLottery.prefix + "§a賞金額を ¥${updatedLottery.prize} に設定しました。")
            return
        }

        // 確率の入力処理
        if (waitingForChanceInput.containsKey(player)) {
            event.isCancelled = true
            val lottery = waitingForChanceInput.remove(player)!!

            if (message.equals("cancel", ignoreCase = true)) {
                player.sendMessage(WoxloiLottery.prefix + "§c入力をキャンセルしました。")
                return
            }

            val chance = message.toDoubleOrNull()
            if (chance == null || chance < 0.01 || chance > 100) {
                player.sendMessage(WoxloiLottery.prefix + "§c無効な確率です（0.01〜100%）。もう一度入力してください。")
                waitingForChanceInput[player] = lottery // 再度元のオブジェクトをマップに戻す
                return
            }

            // 新しい Lottery オブジェクトを作成してマップに再代入
            val updatedLottery = lottery.copy(chances = chance.toInt()) // chances を更新
            waitingForChanceInput[player] = updatedLottery // マップを更新

            player.sendMessage(WoxloiLottery.prefix + "§a当選確率を $chance% に設定しました。")
        }
    }
}
