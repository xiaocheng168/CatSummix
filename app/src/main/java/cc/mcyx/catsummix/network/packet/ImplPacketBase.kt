package cc.mcyx.catsummix.network.packet

import android.os.Build
import androidx.annotation.RequiresApi
import cc.mcyx.catsummix.network.MainNetwork
import com.alibaba.fastjson.JSONObject
import de.robv.android.xposed.XposedBridge
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class ImplPacketBase : IPacket, JSONObject() {

    companion object {
        @JvmStatic
        val ts: ExecutorService = Executors.newCachedThreadPool()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun send() {
        ts.execute(
            Thread {
                this["time"] = System.currentTimeMillis()
                MainNetwork.sendText(this.toJSONString())
            }
        )
    }

    override fun setType(t: Int) {
        this["t"] = t
    }

    protected fun setData(d: String) {
        this["d"] = d
    }

    fun put(key: String, data: String): ImplPacketBase {
        this[key] = data
        return this
    }
}