package red.man10.woxloiLottery

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.woxloiLottery.database.MySQLManager
import kotlin.random.Random

object LotteryManager {
    private var economy: Economy? = null

    init {
        if (!setupEconomy()) {
            println("Vaultの経済APIが見つかりませんでした。")
        }
    }

    private fun setupEconomy(): Boolean {
        val economyProvider = Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)
        economy = economyProvider?.provider
        return economy != null
    }

    private val lotteries = mutableMapOf<String, Lottery>()

    fun exists(name: String): Boolean {
        return lotteries.containsKey(name) || isLotteryInDatabase(name)
    }

    private fun isLotteryInDatabase(name: String): Boolean {
        val query = "SELECT * FROM lotteries WHERE name = '$name'"
        val resultSet = MySQLManager.connection?.createStatement()?.executeQuery(query)
        return resultSet?.next() == true
    }

    fun createLottery(name: String) {
        val defaultPrize = 1000
        val defaultChances = 10
        val defaultMinTicketNumber = 1
        val defaultMaxTicketNumber = 100000

        if (exists(name)) {
            throw IllegalArgumentException("この名前の宝くじは既に存在します")
        }

        val lottery = Lottery(name, defaultPrize, defaultChances, defaultMinTicketNumber, defaultMaxTicketNumber)
        lotteries[name] = lottery
        saveLotteryToDatabase(lottery)
        println("宝くじ '$name' が作成されました。")
    }

    fun createLotteryItem(name: String, ticketNumber: Int): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta
        meta?.setDisplayName("§6§l宝くじ")

        val lore = mutableListOf(
            "§7ここで生きるか死ぬかの勝負！！",
            "§a当たれば人生一発逆転..",
            "§c全財産全つっぱだ！！！",
            "§4§l番号: $ticketNumber"
        )
        meta?.lore = lore
        meta?.setCustomModelData(1)
        item.itemMeta = meta

        return item
    }

    fun buyLottery(name: String, player: Player, quantity: Int, ticketNumber: Int? = null): Boolean {
        val lottery = getLottery(name) ?: return false
        val playerBalance = getPlayerBalance(player)
        val pricePerTicket = lottery.prize

        if (playerBalance < pricePerTicket * quantity) {
            player.sendMessage("§c§l所持金が足りません。")
            return false
        }

        if (!deductPlayerBalance(player, pricePerTicket * quantity)) {
            player.sendMessage("§c§lお金の引き落としに失敗しました。")
            return false
        }

        for (i in 0 until quantity) {
            // 番号が指定されていない場合はランダムな番号を割り当て
            val ticket = ticketNumber ?: nextTicketNumber(name)
            val lotteryItem = createLotteryItem(name, ticket)
            player.inventory.addItem(lotteryItem)
            createLotteryTicket(name, ticket, lottery.prize, player.name)
        }

        player.sendMessage("§a§l宝くじ「$name」を$quantity 枚購入しました。")
        return true
    }

    private fun getPlayerBalance(player: Player): Int {
        return economy?.getBalance(player)?.toInt() ?: 0
    }

    private fun deductPlayerBalance(player: Player, amount: Int): Boolean {
        val balance = economy?.getBalance(player) ?: return false
        if (balance >= amount) {
            economy?.withdrawPlayer(player, amount.toDouble())
            return true
        }
        return false
    }

    private fun saveLotteryToDatabase(lottery: Lottery) {
        val query = "INSERT INTO lotteries (name, prize, chances, min_ticket_number, max_ticket_number) VALUES ('${lottery.name}', ${lottery.prize}, ${lottery.chances}, ${lottery.minTicketNumber}, ${lottery.maxTicketNumber})"
        MySQLManager.executeUpdate(query)
    }

    fun getLottery(name: String): Lottery? {
        return lotteries[name] ?: getLotteryFromDatabase(name)
    }

    private fun getLotteryFromDatabase(name: String): Lottery? {
        val query = "SELECT * FROM lotteries WHERE name = '$name'"
        val resultSet = MySQLManager.connection?.createStatement()?.executeQuery(query)

        return if (resultSet != null && resultSet.next()) {
            Lottery(
                resultSet.getString("name"),
                resultSet.getInt("prize"),
                resultSet.getInt("chances"),
                resultSet.getInt("min_ticket_number"),
                resultSet.getInt("max_ticket_number")
            )
        } else null
    }

    fun nextTicketNumber(lotteryName: String): Int {
        val query = "SELECT MAX(ticket_number) AS max_number FROM lottery_tickets WHERE lottery_name = '$lotteryName'"
        val resultSet = MySQLManager.connection?.createStatement()?.executeQuery(query)
        return if (resultSet != null && resultSet.next()) {
            resultSet.getInt("max_number") + 1
        } else {
            1
        }
    }

    private fun createLotteryTicket(lotteryName: String, ticketNumber: Int, prize: Int, playerName: String) {
        val query = "INSERT INTO lottery_tickets (lottery_name, ticket_number, prize, winner_name) VALUES ('$lotteryName', $ticketNumber, $prize, '$playerName')"
        MySQLManager.executeUpdate(query)
    }

    fun startLottery(lotteryName: String) {
        val winningNumber = String.format("%05d", Random.nextInt(0, 100000))
        Bukkit.broadcastMessage("§6§l宝くじ『$lotteryName』の当選番号は... §e$winningNumber")

        val query = "SELECT * FROM lottery_tickets WHERE lottery_name = '$lotteryName'"
        val resultSet = MySQLManager.connection?.createStatement()?.executeQuery(query)

        val winners = mutableMapOf<Int, MutableList<String>>()

        // チケット番号を照合して当選者を決定
        while (resultSet != null && resultSet.next()) {
            val ticketNumber = String.format("%05d", resultSet.getInt("ticket_number"))
            val player = resultSet.getString("winner_name")

            val matchCount = ticketNumber.zip(winningNumber).count { it.first == it.second }

            val prizeLevel = when (matchCount) {
                5 -> 1
                4 -> 2
                3 -> 3
                2 -> 4
                1 -> 5
                else -> 0
            }

            if (prizeLevel > 0) {
                winners.computeIfAbsent(prizeLevel) { mutableListOf() }.add(player)
            }
        }

        // 当選者がいない場合
        if (winners.isEmpty()) {
            Bukkit.broadcastMessage("§c§l今回の当選者はいませんでした...")
        } else {
            // 賞金の設定
            val prizeAmounts = mapOf(
                1 to 100000000.0,  // 1等: 1億円
                2 to 10000000.0,   // 2等: 1000万円
                3 to 1000000.0,    // 3等: 100万円
                4 to 30000.0,      // 4等: 30000円
                5 to 1000.0        // 5等: 1000円
            )

            // 当選者リストをまとめて表示
            val winnerMessages = mutableListOf<String>()

            for (i in 1..5) {
                val winnerList = winners[i] ?: continue
                if (winnerList.isNotEmpty()) {
                    val prize = prizeAmounts[i] ?: 0.0
                    winnerMessages.add("§b§l${i}等: ${winnerList.joinToString(", ")} (賞金: ¥$prize)")

                    // 賞金の送付
                    for (winnerName in winnerList) {
                        val winner = Bukkit.getPlayer(winnerName)
                        winner?.let {
                            economy?.depositPlayer(it, prize)
                            it.sendMessage("§aおめでとうございます！あなたは${i}等に当選しました！賞金: ¥$prize")
                        }
                    }
                }
            }

            // まとめて当選者情報を表示
            if (winnerMessages.isNotEmpty()) {
                Bukkit.broadcastMessage("§e§l当選者一覧:")
                winnerMessages.forEach { Bukkit.broadcastMessage(it) }
            }
        }
    }

    fun removeLottery(name: String) {
        if (!exists(name)) {
            throw IllegalArgumentException("その名前の宝くじは存在しません")
        }
        lotteries.remove(name)
        deleteLotteryFromDatabase(name)
    }

    fun getAllLotteryNames(): List<String> {
        return lotteries.keys.toList()
    }

    private fun deleteLotteryFromDatabase(name: String) {
        val query = "DELETE FROM lotteries WHERE name = '$name'"
        MySQLManager.executeUpdate(query)
    }


    data class Lottery(
        val name: String,
        val prize: Int = 1000,
        val chances: Int = 10,
        val minTicketNumber: Int = 0,
        val maxTicketNumber: Int = 100000
    )
}
