package red.man10.woxloiLottery.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

// 2025/05/10
// MySQLに接続

object MySQLManager {
    private val url = "jdbc:mysql://localhost:3306/lottery"  // データベースのURL
    private val user = "root"  // MySQLのユーザー名
    private val password = "woxloi0808"  // MySQLのパスワード
    var connection: Connection? = null

    init {
        connect()
    }

    // MySQLに接続する
    fun connect() {
        try {
            if (connection == null || connection!!.isClosed) {
                connection = DriverManager.getConnection(url, user, password)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    // SQLクエリを実行
    fun executeUpdate(query: String) {
        val statement: Statement? = connection?.createStatement()
        statement?.executeUpdate(query)
    }

    // 接続を閉じる
    fun disconnect() {
        try {
            if (connection != null && !connection!!.isClosed) {
                connection!!.close()
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }
}
