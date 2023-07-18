package cc.mcyx.catsummix.view

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cc.mcyx.catsummix.R
import cc.mcyx.catsummix.code.Code
import cc.mcyx.catsummix.network.packet.ImplPacketBase
import cc.mcyx.catsummix.network.packet.PacketBindDrive
import cc.mcyx.catsummix.network.packet.PacketScanDriver
import cn.hutool.crypto.symmetric.RC4
import com.alibaba.fastjson.JSONObject
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {
    /**
     * 设置广播位置
     *
     *
     * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val scanBtn = findViewById<Button>(R.id.scanPc)
        scanBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PacketScanDriver().apply {
                    //发送数据包
                    send().apply {
                        val timeout = 10 * 1000
                        listener(timeout, object : ImplPacketBase.OnPacketReceive {
                            override fun onMessage(jsonObject: JSONObject, ip: String, port: Int) {
                                val builder = AlertDialog.Builder(this@MainActivity)
                                Looper.prepare()
                                val message =
                                    "设备名：" + jsonObject.getString("derive") + " \n自动取消了昂 (%timeout%s)"
                                builder.setTitle("发现你了~")
                                builder.setIcon(R.mipmap.ic_launcher)
                                builder.setMessage(
                                    message.replace(
                                        "%timeout%",
                                        (timeout / 1000).toString()
                                    )
                                )
                                builder.setPositiveButton(
                                    "连!"
                                ) { dialogInterface, i ->
                                    run {
                                        PacketBindDrive(Code.key).send(ip, port)
                                    }
                                }
                                builder.setNegativeButton("不连", null)
                                val show = builder.show()
                                val timeoutS = (System.currentTimeMillis() + timeout)
                                Timer().schedule(object : TimerTask() {
                                    override fun run() {
                                        val ts = (timeoutS - System.currentTimeMillis()) / 1000
                                        show.setMessage(
                                            message.replace(
                                                "%timeout%",
                                                (ts).toString()
                                            )
                                        )
                                        if (ts <= 0) {
                                            show.cancel()
                                            cancel()
                                        }
                                    }
                                }, 0, 1000)
                                Looper.loop()
                            }
                        })
                    }
                }
            }
        }
    }
}