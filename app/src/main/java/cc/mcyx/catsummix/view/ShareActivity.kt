package cc.mcyx.catsummix.view

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import cc.mcyx.catsummix.network.packet.ImplPacketBase
import cc.mcyx.catsummix.network.packet.PacketBrDropFile
import com.alibaba.fastjson.JSONObject
import java.io.InputStream
import java.net.Socket


//最大切块数

const val MAX_BLOCK = ((1024 * 1024) * 100)

//分享数据
class ShareActivity : AppCompatActivity() {
    @SuppressLint("Range", "Recycle")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //判断是否为分享图片

        // 多文件分享
        val dropFiles = mutableListOf<FileInfo>()
        if (Intent.ACTION_SEND_MULTIPLE === intent.action) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            uris?.forEach {
                if (it != null) {
                    //发出广播，判断谁需要这个文件
                    FileInfo(
                        getFileName(it),
                        contentResolver.openInputStream(it)!!.readBytes().size.toLong(),
                        contentResolver.openInputStream(it)!!
                    ).apply {
                        dropFiles.add(this)
                    }
                }
            }
        }

        //单文件分享
        if (Intent.ACTION_SEND === intent.action) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.also {
                FileInfo(
                    getFileName(it),
                    contentResolver.openInputStream(it)!!.readBytes().size.toLong(),
                    contentResolver.openInputStream(it)!!
                ).apply {
                    dropFiles.add(this)
                }
            }
        }

        //发送所有已选择的文件
        val packetBrDropFile = PacketBrDropFile(dropFiles, true)  //发包需要想一个新的思路
        packetBrDropFile.listener(1,
            object : ImplPacketBase.OnPacketReceive {
                override fun onMessage(jsonObject: JSONObject, ipaddress: String, port: Int) {
                    //使用多线程去分发这些文件
                    Thread {
                        val socket = Socket(ipaddress, 3333)
                        val outputStream = socket.getOutputStream()
                        //遍历循环所有drop文件
                        for (dropFile in dropFiles) {
//                            println("发送文件 $dropFile")
                            //解决Java并包的问题
                            val read = ByteArray(MAX_BLOCK)
                            var reads: Int
                            while (dropFile.inputStream.read(read).also { reads = it } != -1) {
                                outputStream.write(read, 0, reads)
//                                println("send ${read.size}")
                            }
                        }
                        //关闭 输出流 客户端
                        outputStream.close()
                        socket.close()
                        //清空drop表
                        dropFiles.clear()
                        //提示客户端
                        Looper.prepare()
                        Toast.makeText(this@ShareActivity, "已发送完毕！", Toast.LENGTH_SHORT).show()

                    }.start()
                }
            })
        //发送drop的文件列表json
        packetBrDropFile.send()
        //返回上一页
        finish()
    }

    data class FileInfo(val fileName: String, val fileSize: Long, val inputStream: InputStream)

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                result = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            }
        }
        return result ?: ""
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize: Long = 0
        val projection = arrayOf(OpenableColumns.SIZE)
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileSize = it.getLong(it.getColumnIndexOrThrow(OpenableColumns.SIZE))
            }
        }
        return fileSize
    }
}