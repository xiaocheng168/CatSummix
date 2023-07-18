package cc.mcyx.catsummix.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import cc.mcyx.catsummix.MainInject
import de.robv.android.xposed.XposedBridge

//蓝牙网关 处理
class BlueToothNetwork {
    //获取蓝牙网关处理
    private val defaultAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    companion object {
        @JvmStatic
        val address: HashMap<String, BluetoothDevice> = hashMapOf()
    }

    init {
        println("Hook bluetooth！")
        Thread {
            Thread.sleep(1000)
            if (defaultAdapter.isEnabled) {
                MainInject.log("蓝牙已开启")
                //注册广播
                scanBlueToothDev()
                this.scanBlueToothDev()
            }
        }.start()
    }

    private fun scanBlueToothDev() {
        //开始扫描蓝牙设备
        if (!defaultAdapter.isDiscovering) {
            //添加广播过滤器
            val intentFilter = IntentFilter()
            intentFilter.apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            }
            //注册蓝牙扫描广播
            MainInject.registerBar(ScanDevices(), intentFilter)
            //开始扫描
            defaultAdapter.startDiscovery()
        }

    }

    class ScanDevices : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            val action = intent!!.action
            //发现设备
            if (action == BluetoothDevice.ACTION_FOUND) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    address[device.address] = device
                    MainInject.log(device.address)
                }
            }
            //扫描完成
            if (action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                XposedBridge.log("扫描完成")
                for (ar in address) {
                    XposedBridge.log(ar.key)
                }
            }
        }
    }
}