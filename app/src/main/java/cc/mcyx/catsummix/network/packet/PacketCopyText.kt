package cc.mcyx.catsummix.network.packet

class PacketCopyText(text: String) : ImplPacketBase() {
    init {
        setData(text)
        setType(1)
    }
}