package cc.mcyx.catsummix.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cc.mcyx.catsummix.R
import cc.mcyx.catsummix.network.packet.ImplPacketBase
import cc.mcyx.catsummix.network.packet.PacketBrDropFile
import com.alibaba.fastjson.JSONObject
import java.io.InputStream
import java.net.Socket
import kotlin.system.exitProcess


//最大切块数

const val MAX_BLOCK = ((1024 * 1024) * 100)


//分享数据
class ShareActivity : AppCompatActivity() {
    companion object {
        // 多文件分享
        val dropFiles = mutableListOf<FileInfo>()
    }

    private val REQUEST_CODE_STORAGE_PERMISSION = 1

    // 检查文件读写权限
    private fun checkStoragePermission(): Boolean {
        // 检查当前系统版本是否大于等于 Android 6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查读取和写入权限是否已被授予
            val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            // 判断权限是否已被授予
            if (readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
            ) {
                // 权限已授予，可以进行文件读写操作
                return true
            } else {
                // 权限未授予，需要申请权限
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
                return false
            }
        } else {
            // Android 6.0 以下的版本，不需要动态申请权限
            return true
        }
    }

    @SuppressLint("Range", "Recycle")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置界面
        setContentView(R.layout.share_activity)

        //判断是否有权限
        if (!checkStoragePermission()) {
            Toast.makeText(this, "莫得权限", Toast.LENGTH_SHORT).show()
            return
        }
        //判断是否为分享图片
        if (Intent.ACTION_SEND_MULTIPLE === intent.action) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            uris?.forEach {
                if (it != null) {
                    //发出广播，判断谁需要这个文件
                    FileInfo(
                        getFileName(it),
                        getFileSize(contentResolver.openInputStream(it)!!),
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
                    getFileSize(contentResolver.openInputStream(it)!!),
                    contentResolver.openInputStream(it)!!
                ).apply {
                    dropFiles.add(this)
                }
            }
        }


        //发送所有已选择的文件
        val packetBrDropFile = PacketBrDropFile(dropFiles, true)  //发包需要想一个新的思路

        val builder = AlertDialog.Builder(this)
        builder.setTitle("分享提示")
        builder.setMessage(
            "文件一共有 ${
                String.format(
                    "%.2f",
                    ((packetBrDropFile.fileSize.toDouble() / 1024 / 1024))
                )
            } M\n文件数量 ${packetBrDropFile.files.size} 个"
        )
        builder.setPositiveButton("发送") { p0, p1 ->
            //确认发送
            //发送drop的文件列表json
            packetBrDropFile.send()
        }
        builder.setNegativeButton("取消") { p0, p1 ->
            Toast.makeText(this@ShareActivity, "你取消了操作！", Toast.LENGTH_SHORT).show()
            finish()
        }
        builder.show()
        packetBrDropFile.listener(10000,
            object : ImplPacketBase.OnPacketReceive() {
                override fun onMessage(jsonObject: JSONObject, ipaddress: String, port: Int) {
                    println(dropFiles)
                    //使用多线程去分发这些文件

                    Thread {
                        println("线程!")
                        val socket = Socket(ipaddress, 3333)
                        val outputStream = socket.getOutputStream()
                        var alreadySize = 0
                        //遍历循环所有drop文件
                        for (dropFile in dropFiles) {
                            println("发送文件 ${dropFile.fileName}")
//                            println("发送文件 $dropFile")
                            //解决Java并包的问题
                            val read = ByteArray(MAX_BLOCK)
                            var reads: Int
                            //循环读入读入文件
                            while (dropFile.inputStream.read(read).also { reads = it } != -1) {
                                outputStream.write(read, 0, reads)
                                alreadySize += reads
                                val pr = ((alreadySize.toDouble() / packetBrDropFile.fileSize.toDouble()) * 100)

                                //设置进度
                                val handler = Handler(Looper.getMainLooper())
                                handler.post {
                                    val textView = findViewById<TextView>(R.id.already)
                                    textView.text = String.format("%.2f", pr)
                                }
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
                        finish()
                        exitProcess(0)
                    }.start()
                }
            })

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

    /**
     *
     * 使用文件输入流获取文件大小
     *
     * @param inputStream 输入流
     *
     * */
    private fun getFileSize(inputStream: InputStream): Long {
        val byteArray = ByteArray(MAX_BLOCK)
        var reads: Int
        var size: Long = 0
        while (inputStream.read(byteArray).also { reads = it } != -1) {
            size += reads
        }
        //关闭文件输入流
        inputStream.close()
        return size
    }
}