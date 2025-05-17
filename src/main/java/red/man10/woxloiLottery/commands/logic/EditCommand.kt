package red.man10.woxloiLottery.commands.logic

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
import red.man10.woxloiLottery.LotteryManager
import red.man10.woxloiLottery.WoxloiLottery

class EditCommand(private val plugin: JavaPlugin) : CommandExecutor, Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    data class EditSession(
        val type: EditType,
        val lottery: LotteryManager.Lottery,
        var prizeTier: Int? = null // 追加: 賞金等級を格納

    )

    enum class EditType {
        PRIZE, CHANCE, RANGE_MIN, RANGE_MAX, DESCRIPTION, PRICE, DISCOUNT, FREE_TIMES, ANTI_ALT_IP, ANTI_ALT_UUID
    }

    private val waitingForInput = mutableMapOf<Player, EditSession>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player || args.size < 2) {
            sender.sendMessage(Component.text("${WoxloiLottery.prefix}コマンドが違います", NamedTextColor.RED))
            return false
        }

        val name = args[1]
        val lottery = LotteryManager.getLottery(name)
        if (lottery == null) {
            sender.sendMessage(Component.text("${WoxloiLottery.prefix}宝くじが見つかりません", NamedTextColor.RED))
            return false
        }

        openMainEditMenu(sender, lottery)
        return true
    }

    private fun openMainEditMenu(player: Player, lottery: LotteryManager.Lottery) {
        val inv = Bukkit.createInventory(null, 54, "§6宝くじ編集: ${lottery.name}")

        inv.setItem(0, guiItem("§e賞金設定", Material.GOLD_INGOT, "等級ごとの賞金を設定"))
        inv.setItem(1, guiItem("§b当選確率設定", Material.DIAMOND, "各等級の確率を設定"))
        inv.setItem(2, guiItem("§a番号範囲設定", Material.PAPER, "最小・最大番号を設定"))
        inv.setItem(3, guiItem("§d説明文設定", Material.BOOK, "説明文を設定"))
        inv.setItem(4, guiItem("§6価格・割引設定", Material.EMERALD, "価格・無料回数などを設定"))
        inv.setItem(5, guiItem("§cサブ垢対策", Material.BARRIER, "制限条件を設定"))

        player.openInventory(inv)
    }

    private fun guiItem(name: String, material: Material, description: String): ItemStack {
        return ItemStack(material).apply {
            itemMeta = itemMeta?.also {
                it.setDisplayName(name)
                it.lore = listOf("§7$description")
            }
        }
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return
        val title = e.view.title
        if (!title.startsWith("宝くじ編集: ")) return

        e.isCancelled = true

        // プレイヤーのインベントリをクリックした場合は無視
        if (e.clickedInventory == null || e.clickedInventory != e.view.topInventory) return

        val item = e.currentItem ?: return
        val meta = item.itemMeta ?: return
        val displayName = meta.displayName

        val lotteryName = title.removePrefix("宝くじ編集: ").trim()
        val lottery = LotteryManager.getLottery(lotteryName) ?: return

        when (displayName) {
            "§e賞金設定" -> openPrizeEdit(player, lottery)
            "§b当選確率設定" -> openChanceEdit(player, lottery)
            "§a番号範囲設定" -> openRangeEdit(player, lottery)
            "§d説明文設定" -> promptTextInput(player, EditType.DESCRIPTION, lottery, "説明文を入力してください")
            "§6価格・割引設定" -> openPriceEdit(player, lottery)
            "§cサブ垢対策" -> openAntiAltEdit(player, lottery)
            else -> player.sendMessage(Component.text("クリックされた項目は未対応です。", NamedTextColor.RED))
        }
    }

    private fun openPrizeEdit(player: Player, lottery: LotteryManager.Lottery) {
        val inv = Bukkit.createInventory(null, 27, "§e賞金設定: ${lottery.name}")

        for (i in 1..5) {
            val slot = 9 + (i - 1)
            inv.setItem(slot, guiItem("§e${i}等の賞金設定", Material.GOLD_INGOT, "${i}等の賞金額を設定"))
        }
        inv.setItem(26, guiItem("§c戻る", Material.BARRIER, "メインメニューに戻ります"))

        player.openInventory(inv)
    }

    private fun openChanceEdit(player: Player, lottery: LotteryManager.Lottery) {
        player.closeInventory()
        player.sendMessage(Component.text("当選確率を入力してください（例: 1等=10）", NamedTextColor.YELLOW))
        waitingForInput[player] = EditSession(EditType.CHANCE, lottery)
    }

    private fun openRangeEdit(player: Player, lottery: LotteryManager.Lottery) {
        player.closeInventory()
        player.sendMessage(Component.text("最小番号を入力してください", NamedTextColor.YELLOW))
        waitingForInput[player] = EditSession(EditType.RANGE_MIN, lottery)
    }

    private fun openPriceEdit(player: Player, lottery: LotteryManager.Lottery) {
        player.closeInventory()
        player.sendMessage(Component.text("価格を入力してください（例: 500）", NamedTextColor.YELLOW))
        waitingForInput[player] = EditSession(EditType.PRICE, lottery)
    }

    private fun openAntiAltEdit(player: Player, lottery: LotteryManager.Lottery) {
        val inv = Bukkit.createInventory(null, 27, "§cサブ垢対策設定: ${lottery.name}")

        inv.setItem(10, guiItem("§eIP制限ON/OFF", Material.REDSTONE_TORCH, "同一IPからの購入を制限"))
        inv.setItem(12, guiItem("§bUUID制限ON/OFF", Material.NAME_TAG, "同一UUIDからの購入を制限"))
        inv.setItem(14, guiItem("§a最小プレイ時間", Material.CLOCK, "アカウント作成後のプレイ時間で制限"))
        inv.setItem(26, guiItem("§c戻る", Material.BARRIER, "メインメニューに戻る"))

        player.openInventory(inv)
    }

    private fun promptTextInput(player: Player, type: EditType, lottery: LotteryManager.Lottery, message: String) {
        player.closeInventory()
        player.sendMessage(Component.text(message, NamedTextColor.GREEN))
        waitingForInput[player] = EditSession(type, lottery)
    }

    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val session = waitingForInput.remove(e.player) ?: return
        e.isCancelled = true

        val msg = e.message
        when (session.type) {
            EditType.PRIZE -> {
                val amount = msg.toIntOrNull()
                val tier = session.prizeTier
                if (amount != null && amount > 0 && tier != null) {
                    e.player.sendMessage(Component.text("${tier}等の賞金額を ${amount} に設定しました。", NamedTextColor.GREEN))
                    // 保存処理: lotteryに金額を設定
                }
            }
            EditType.CHANCE -> {
                e.player.sendMessage(Component.text("確率設定: $msg", NamedTextColor.GREEN))
            }
            EditType.RANGE_MIN -> {
                val min = msg.toIntOrNull()
                if (min != null) {
                    e.player.sendMessage(Component.text("最小番号を $min に設定しました。最大番号も入力してください。", NamedTextColor.GREEN))
                    waitingForInput[e.player] = EditSession(EditType.RANGE_MAX, session.lottery)
                }
            }
            EditType.RANGE_MAX -> {
                val max = msg.toIntOrNull()
                if (max != null) {
                    e.player.sendMessage(Component.text("最大番号を $max に設定しました。", NamedTextColor.GREEN))
                }
            }
            EditType.DESCRIPTION -> {
                e.player.sendMessage(Component.text("説明文を設定しました: $msg", NamedTextColor.GREEN))
            }
            EditType.PRICE -> {
                val price = msg.toIntOrNull()
                if (price != null && price >= 0) {
                    e.player.sendMessage(Component.text("価格を ${price} に設定しました。", NamedTextColor.GREEN))
                    e.player.sendMessage(Component.text("割引率を入力してください（例: 20）", NamedTextColor.YELLOW))
                    waitingForInput[e.player] = EditSession(EditType.DISCOUNT, session.lottery)
                }
            }
            EditType.DISCOUNT -> {
                val discount = msg.toIntOrNull()
                if (discount != null && discount in 0..100) {
                    e.player.sendMessage(Component.text("割引率を $discount% に設定しました。無料回数を入力してください（例: 3）", NamedTextColor.GREEN))
                    waitingForInput[e.player] = EditSession(EditType.FREE_TIMES, session.lottery)
                }
            }
            EditType.FREE_TIMES -> {
                val times = msg.toIntOrNull()
                if (times != null && times >= 0) {
                    e.player.sendMessage(Component.text("無料回数を $times 回に設定しました。", NamedTextColor.GREEN))
                }
            }
            EditType.ANTI_ALT_IP -> {
                e.player.sendMessage(Component.text("IPアドレスによるサブ垢制限を設定しました。", NamedTextColor.GREEN))
            }
            EditType.ANTI_ALT_UUID -> {
                e.player.sendMessage(Component.text("UUIDによるサブ垢制限を設定しました。", NamedTextColor.GREEN))
            }
        }
    }
}
