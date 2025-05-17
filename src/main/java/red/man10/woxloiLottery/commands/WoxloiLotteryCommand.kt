package red.man10.woxloiLottery.commands

import red.man10.woxloiLottery.commands.logic.*
import com.shojabon.mcutils.Utils.SCommandRouter.SCommandArgument
import com.shojabon.mcutils.Utils.SCommandRouter.SCommandObject
import com.shojabon.mcutils.Utils.SCommandRouter.SCommandRouter
import red.man10.woxloiLottery.WoxloiLottery
import red.man10.woxloiLottery.WoxloiLottery.Companion.plugin

class WoxloiLotteryCommand(plugin: WoxloiLottery) : SCommandRouter() {
    // plugin変数の再定義を削除し、コンストラクタで受け取ったpluginをそのまま使用
    init {
        registerCommands()
        registerEvents()
        pluginPrefix = WoxloiLottery.prefix
    }

    fun registerEvents() {
        setNoPermissionEvent { e -> e.sender.sendMessage(WoxloiLottery.prefix + "§c§lあなたは権限がありません") }
        setOnNoCommandFoundEvent { e -> e.sender.sendMessage(WoxloiLottery.prefix + "§c§lコマンドが存在しません") }
    }

    fun registerCommands() {

        //===========================
        //
        //   Lottery Command
        //   Initial commit
        //
        //===========================
        // コマンド「reload」を追加
        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("reload"))
                .addRequiredPermission("woxloilottery.reload")
                .addExplanation("コンフィグをリロードする")
                .setExecutor(ReloadCommand(plugin))  // ReloadCommandが正しく機能することを確認
        )

        // コマンド「create」を追加
        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("create"))
                .addArgument(SCommandArgument().addAlias("内部名"))
                .addRequiredPermission("woxloilottery.create")
                .addExplanation("宝くじを作成する")
                .setExecutor(CreateCommand(plugin))  // CreateCommandが正しく機能することを確認
        )

        //===========================
        //
        // 基本的なコマンドを追加
        // 2025/01/13
        //
        //===========================
        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("edit"))
                .addArgument(SCommandArgument().addAlias("内部名"))
                .addRequiredPermission("woxloilottery.edit")
                .addExplanation("宝くじを編集する")
                .setExecutor(EditCommand(plugin))  // CreateCommandが正しく機能することを確認
        )

        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("buy"))
                .addArgument(SCommandArgument().addAlias("購入する宝くじの名前"))
                .addArgument(SCommandArgument().addAlias("枚数"))
                .addRequiredPermission("woxloilottery.buy")
                .addExplanation("宝くじを購入する")
                .setExecutor(BuyCommand(plugin))
        )


        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("broadcast"))
                .addArgument(SCommandArgument().addAllowedString("start"))
                .addArgument(SCommandArgument().addAlias("tick"))
                .addArgument(SCommandArgument().addAlias("回数"))
                .addRequiredPermission("woxloilottery.broadcast.start")
                .addExplanation("宝くじの告知を開始する")
                .setExecutor(BroadCommand(plugin))  // BroadCommandのstart処理を実行
        )

        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("broadcast"))
                .addArgument(SCommandArgument().addAllowedString("stop"))
                .addRequiredPermission("woxloilottery.broadcast.stop")
                .addExplanation("宝くじの告知を停止する")
                .setExecutor(BroadCommand(plugin))  // BroadCommandのstop処理を実行
        )
        //===========================
        //
        // 抽選スタートコマンドを追加した
        // 2025/04/25
        //
        //===========================
        addCommand(
            SCommandObject()
                .addArgument(SCommandArgument().addAllowedString("start"))
                .addArgument(SCommandArgument().addAlias("内部名"))
                .addRequiredPermission("woxloilottery.start")
                .addExplanation("宝くじの抽選を開始する")
                .setExecutor(StartCommand(plugin))  // CreateCommandが正しく機能することを確認
        )
        //===========================
        //
        // 最終変更
        // 2025/05/10
        //
        //===========================
    }
}

