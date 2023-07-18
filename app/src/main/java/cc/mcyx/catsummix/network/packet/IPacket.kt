package cc.mcyx.catsummix.network.packet

interface IPacket {
    //发送数据包
    fun send()

    //设置数据包类型
    fun setType(t: Int)
    fun close()
    fun listener(timeout: Int, onPacketReceive: ImplPacketBase.OnPacketReceive)
    fun send(ipaddress: String, int: Int)
}