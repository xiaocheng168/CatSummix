package cc.mcyx.catsummix.network.packet

import android.os.Build
import androidx.annotation.RequiresApi
import cc.mcyx.catsummix.view.ShareActivity
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject

//广播传递文件 用于判断需要接收这个文件
@RequiresApi(Build.VERSION_CODES.O)
class PacketBrDropFile(files: MutableList<ShareActivity.FileInfo>, encode: Boolean) : ImplPacketBase() {
    init {
        this.setEncode(encode)
        val jsonObject = JSONObject()
        var size: Long = 0
        val jsonArray = JSONArray()
        for (file in files) {
            size += file.fileSize
            JSONObject().apply {
                put("name", file.fileName)
                put("size", file.fileSize)
                jsonArray.add(this)
            }
        }
        jsonObject["count"] = files.size
        jsonObject["size"] = size
        jsonObject["files"] = jsonArray
        this.setData(jsonObject)
        this.setType(5)
    }
}