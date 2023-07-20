package cc.mcyx.catsummix.network.packet

import android.os.Build
import cc.mcyx.catsummix.code.Code
import cc.mcyx.catsummix.network.LanNetwork
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.net.DatagramPacket
import java.util.Base64
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

abstract class ImplPacketBase(encode: Boolean = false) : IPacket, JSONObject() {

    //是否数据加密
    private var encode: Boolean

    init {
        this.encode = encode
    }

    companion object {
        @JvmStatic
        val ts: ExecutorService = Executors.newCachedThreadPool()
    }

    //设置是否加密数据
    fun setEncode(b: Boolean = true): ImplPacketBase {
        this.encode = b
        this["encode"] = true
        return this
    }

    override fun send() {
        ts.execute(
            Thread {
                this["time"] = System.currentTimeMillis()
                this["derive"] = Build.DEVICE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LanNetwork.sendText(this.toJSONString())
                }
            }
        )
    }


    override fun send(ipaddress: String, int: Int) {
        ts.execute(
            Thread {
                this["time"] = System.currentTimeMillis()
                this["derive"] = Build.DEVICE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LanNetwork.sendText(this.toJSONString())
                }
            }
        )
    }

    /**
     * 监听Udp数据包回调
     * @author Zcc
     * @param timeout 有效时间 (ms)
     * @param onPacketReceive 回调数据
     *
     * 为了节省资源，有效时间不要大于 10 秒
     *
     * */
    override fun listener(timeout: Int, onPacketReceive: OnPacketReceive) {
        Thread {
            val outTime = System.currentTimeMillis() + timeout
            while (System.currentTimeMillis() < outTime || LanNetwork.datagramSocket.isClosed) {
                if (!LanNetwork.datagramSocket.isClosed) {
                    val byteArray = ByteArray(512)
                    val datagramPacket = DatagramPacket(byteArray, byteArray.size)
                    LanNetwork.datagramSocket.receive(datagramPacket)
                    val sb = StringBuffer()
                    for (datum in datagramPacket.data) {
                        if (datum.toInt() > 0) {
                            sb.append(datum.toInt().toChar())
                        }
                    }
                    //callback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        JSON.parseObject(String(Base64.getDecoder().decode(sb.toString()))).apply {
                            println(this)
                            datagramPacket.address.hostAddress?.let {
                                onPacketReceive.onMessage(
                                    this,
                                    it,
                                    datagramPacket.port
                                )
                            }
                        }
                    }
                }
            }
        }.start()
    }


    override fun close() {
        LanNetwork.datagramSocket.close()
    }

    override fun setType(t: Int) {
        this["t"] = t
    }

    fun setData(d: String) {
        this["d"] = if (this.encode) {
            Code.enCodeRc4Base64(d)
        } else {
            d
        }
    }

    protected fun setData(d: JSONObject) {
        this["d"] = if (this.encode) {
            Code.enCodeRc4Base64(d.toString())
        } else {
            d
        }
    }

    fun put(key: String, data: String): ImplPacketBase {
        this[key] = data
        return this
    }


    abstract class OnPacketReceive {

        lateinit var any: Any


        abstract fun onMessage(jsonObject: JSONObject, ipaddress: String, port: Int)
    }
}