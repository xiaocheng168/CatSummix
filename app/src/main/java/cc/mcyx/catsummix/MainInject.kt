package cc.mcyx.catsummix

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.annotation.RequiresApi
import cc.mcyx.catsummix.network.packet.PacketCopyText
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage


class MainInject : IXposedHookLoadPackage {

    //设置切入点~~~ 类似Spring AOP Inject~
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        findAndHookMethod(
            //类
            ClipboardManager::class.java,
            //方法
            "setPrimaryClip",
            //返回对象
            ClipData::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                }
                //复制后截取数据
                @RequiresApi(Build.VERSION_CODES.O)
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    XposedBridge.log(lpparam!!.packageName)
                    val clipData = param!!.args[0] as ClipData
                    val cp = clipData.getItemAt(0).text.toString()
                    PacketCopyText(cp).send()
                    param.result = true
                }
            })
    }
}