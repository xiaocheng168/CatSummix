package cc.mcyx.catsummix.network.packet

class PacketLogText(log: String?) : ImplPacketBase() {
    init {
        setData(log!!)
        setType(2)
    }
}