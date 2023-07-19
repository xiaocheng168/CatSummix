package cc.mcyx.catsummix.network

import android.os.Build
import androidx.annotation.RequiresApi
import java.net.*
import java.util.*
import kotlin.random.Random

//传输数据层 UDP!!!
//这里监听的端口，均为随机！
class LanNetwork {
    companion object {
        //广播地址
        @JvmStatic
        val sendHost = "255.255.255.255"

        //广播端口
        @JvmStatic
        val sendPort = 3333

        @JvmStatic
        lateinit var datagramSocket: DatagramSocket

        init {
            initUdpService()
        }

        @JvmStatic
        fun initUdpService() {
            //UDP客户端监听
            while (true) try {
                datagramSocket =
                    DatagramSocket(Random(System.currentTimeMillis()).nextInt(1, 65535) + 1)
                break
            } catch (_: java.lang.Exception) {
            }
        }

        //发送字节数据包
        @JvmStatic
        @Synchronized
        fun sendPacket(byteArray: ByteArray, host: String = sendHost, port: Int = sendPort) {
            datagramSocket.send(
                DatagramPacket(
                    byteArray,
                    byteArray.size,
                    InetSocketAddress(host, port)
                )
            )
        }

        //发送文本数据包
        @RequiresApi(Build.VERSION_CODES.O)
        fun sendText(string: String, host: String = sendHost, port: Int = sendPort) {
            val byteArray = Base64.getEncoder().encode(string.toByteArray(Charsets.UTF_8))
            try {
                if (getHostIP() !== null) {
                    sendPacket(byteArray, host, port)
                    val ips = getHostIP()!!.split(".")
                    sendPacket(byteArray, "${ips[0]}.${ips[1]}.${ips[2]}.255", port)
                }
            } catch (_: java.lang.Exception) {
            }
        }

        /**
         * 获取ip地址
         * @return
         */
        @JvmStatic
        fun getHostIP(): String? {
            var hostIp: String? = null
            try {
                val nis = NetworkInterface.getNetworkInterfaces()
                var ia: InetAddress?
                while (nis.hasMoreElements()) {
                    val ni: NetworkInterface = nis.nextElement() as NetworkInterface
                    val ias: Enumeration<InetAddress> = ni.inetAddresses
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement()
                        if (ia is Inet6Address) {
                            continue  // skip ipv6
                        }
                        val ip: String = ia.hostAddress as String
                        if ("127.0.0.1" != ip) {
                            hostIp = ia.hostAddress
                            break
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            }
            return hostIp
        }
    }
}
