package red.man10.woxloiLottery.commands.logic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import red.man10.woxloiLottery.WoxloiLottery
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class BroadCommand(private val plugin: JavaPlugin) : CommandExecutor {


    //停止中
    //2025/05/10
    private var broadcastTask: BukkitRunnable? = null

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        Bukkit.getLogger().info("Received command with args: ${args.joinToString(", ")}")
        if (args.isEmpty()) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l引数が不足しています。")
            return false
        }

        return when (args[0].lowercase()) {
            "start" -> handleStart(sender, args)
            "stop" -> stopBroadcast(sender)
            else -> {
                sender.sendMessage(WoxloiLottery.prefix + "§c§l無効なコマンドです。")
                false
            }
        }
    }

    private fun handleStart(sender: CommandSender, args: Array<String>): Boolean {
        if (args.size < 3) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l使用方法: /wlot broadcast start <秒> <回数>")
            return false
        }

        val tickInterval = args[1].toIntOrNull()
        val repeatCount = args[2].toIntOrNull()

        if (tickInterval == null || repeatCount == null) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l数値が無効です。")
            return false
        }

        return startBroadcast(sender, tickInterval, repeatCount)
    }

    private fun startBroadcast(sender: CommandSender, tickInterval: Int, repeatCount: Int): Boolean {
        if (broadcastTask != null) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§lすでに告知中です。")
            return false
        }

        broadcastTask = object : BukkitRunnable() {
            var remaining = repeatCount

            override fun run() {
                if (remaining <= 0) {
                    cancel()
                    broadcastTask = null
                    sender.sendMessage(WoxloiLottery.prefix + "§a§l告知が終了しました。")
                    return
                }

                Bukkit.broadcastMessage(WoxloiLottery.prefix + "§a§l宝くじの告知です！")
                Bukkit.getLogger().info("告知送信（残り: $remaining）")
                remaining--
            }
        }

        broadcastTask!!.runTaskTimer(plugin, 0L, tickInterval * 20L)
        sender.sendMessage(WoxloiLottery.prefix + "§a§l告知を開始しました。$tickInterval 秒間隔、$repeatCount 回繰り返します。")
        return true
    }

    private fun stopBroadcast(sender: CommandSender): Boolean {
        if (broadcastTask == null) {
            sender.sendMessage(WoxloiLottery.prefix + "§c§l現在、告知は行われていません。")
            return false
        }

        broadcastTask!!.cancel()
        broadcastTask = null

        sender.sendMessage(WoxloiLottery.prefix + "§a§l告知を停止しました。")
        Bukkit.getLogger().info("告知タスクを停止しました。")
        return true
    }
}
