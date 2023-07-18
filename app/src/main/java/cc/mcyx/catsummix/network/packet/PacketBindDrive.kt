package cc.mcyx.catsummix.network.packet

class PacketBindDrive(key: String) : ImplPacketBase() {
    init {
        setType(4)
        setData(key)
    }
}