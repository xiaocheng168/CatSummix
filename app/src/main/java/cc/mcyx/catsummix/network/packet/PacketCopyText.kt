package cc.mcyx.catsummix.network.packet

class PacketCopyText(text: String = "", encode: Boolean) : ImplPacketBase() {
    init {
        this.setEncode(encode)
        this.setData(text)
        this.setType(1)
    }
}