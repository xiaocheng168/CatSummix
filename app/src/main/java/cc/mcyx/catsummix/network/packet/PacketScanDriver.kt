package cc.mcyx.catsummix.network.packet

import android.os.Build
import cc.mcyx.catsummix.network.LanNetwork
import com.alibaba.fastjson.JSONObject
import java.security.MessageDigest
import kotlin.experimental.or

class PacketScanDriver : ImplPacketBase() {
    init {
        val phoneInfo = JSONObject()
        phoneInfo["ipaddress"] = LanNetwork.getHostIP()
        phoneInfo["id"] = Build.DEVICE
        phoneInfo["mac"] = Build.ID
        setData(phoneInfo)
        setType(3)
    }
}