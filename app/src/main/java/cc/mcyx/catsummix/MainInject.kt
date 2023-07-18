package cc.mcyx.catsummix

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import cc.mcyx.catsummix.network.BlueToothNetwork
import cc.mcyx.catsummix.network.packet.PacketCopyText
import cc.mcyx.catsummix.network.packet.PacketLogText
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.system.exitProcess


class MainInject : IXposedHookLoadPackage {

    companion object {
        @JvmStatic
        lateinit var appContext: Context


        //来点消息~
        @JvmStatic
        fun message(string: String) {
            Toast.makeText(appContext, string, Toast.LENGTH_SHORT).show()
        }

        //注册广播
        @JvmStatic
        fun registerBar(r: BroadcastReceiver, i: IntentFilter) {
            appContext.registerReceiver(r, i)
        }

        //Write Log~~
        @JvmStatic
        fun log(a: Any) {
            XposedBridge.log(a.toString())
            PacketLogText(a.toString()).send()
        }
    }

    //设置切入点~~~ 类似Spring AOP Inject~
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        //Hook Packet Name
        val packageName = lpparam!!.packageName
        println("Hook Pack $packageName")
        ///Test Block==============
        if (packageName.lowercase() == "com.android.bluetooth") {
            val blueToothNetwork = BlueToothNetwork()
        }
        //        =========================


        //Hook AppContext
        val findClass = findClass("android.app.Instrumentation", null)
        findAndHookMethod(findClass,
            "callApplicationOnCreate",
            Application::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    if ((param!!.args[0] is Application)) {
                        val application = param.args[0] as Application
                        appContext = application.applicationContext
                    }
                }
            })
        //切入复制方法~~~
        findAndHookMethod(
            //类
            ClipboardManager::class.java,
            //方法
            "setPrimaryClip",
            //返回对象
            ClipData::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                }

                //复制后截取数据
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val clipData = param!!.args[0] as ClipData
                    val cp = clipData.getItemAt(0).text.toString()
                    PacketCopyText(cp, true).send()
                    param.result = true
                }
            })

    }
}