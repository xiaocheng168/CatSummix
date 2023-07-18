package cc.mcyx.catsummix.code

import android.os.Build
import cn.hutool.crypto.digest.MD5
import cn.hutool.crypto.symmetric.RC4

//加密项
class Code {
    companion object {
        //key算法 driver 加密为md5
        //密钥
        var key: String = MD5.create().digestHex(Build.DEVICE)

        /**
         * Rc4 加密
         * @param string 加密字符串
         *
         * @return 返回加密完成的Base64字符串
         * */
        @JvmStatic
        fun enCodeRc4Base64(string: String): String {
            RC4(key).apply {
                return this.encryptBase64(string.toByteArray(Charsets.UTF_8))
            }
        }

        /**
         * Rc4 解密
         * @param base64 解密文本 Base64字符串
         *
         * @return  返回解密完成的明文字符串
         * */
        @JvmStatic
        fun deCode(base64: String): String {
            RC4(key).apply {
                return decrypt(base64.toByteArray(Charsets.UTF_8))
            }
        }
    }
}