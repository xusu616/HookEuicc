package cn.unicorn369

import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.util.function.Consumer

/** giffgaff 的原生桥接层同时检查系统服务和 EID，物理 eUICC 卡不满足这些条件。 */
class GiffgaffEsimHooks(
    private val module: XposedModule,
    private val activationCodeConsumer: Consumer<String>,
) {
    fun install(classLoader: ClassLoader) {
        val managerClass = findManagerClass(classLoader) ?: return

        try {
            val promiseClass = classLoader.loadClass("com.facebook.react.bridge.Promise")
            val resolvePromise = promiseClass.getMethod("resolve", Any::class.java)
            val rejectPromise = promiseClass.getMethod("reject", String::class.java, String::class.java)

            val supportedMethod = managerClass.getDeclaredMethod("isEsimSupported", promiseClass)
            module.hook(supportedMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    resolvePromise.invoke(chain.getArg(0), true)
                    module.log(Log.INFO, TAG, "giffgaff eSIM eligibility resolved: true")
                    null
                }

            val readableMapClass = classLoader.loadClass("com.facebook.react.bridge.ReadableMap")
            val hasKey = readableMapClass.getMethod("hasKey", String::class.java)
            val getString = readableMapClass.getMethod("getString", String::class.java)
            val setupMethod = managerClass.getDeclaredMethod("setupEsim", readableMapClass, promiseClass)
            module.hook(setupMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val options = chain.getArg(0)
                    val promise = chain.getArg(1)
                    val activationCode = if (hasKey.invoke(options, "confirmationCode") == true) {
                        getString.invoke(options, "confirmationCode") as String?
                    } else {
                        null
                    }

                    // 激活码来自 App 的 JS 桥接参数；缺少它时必须报告失败，不能宣称安装成功。
                    if (activationCode.isNullOrBlank()) {
                        rejectPromise.invoke(
                            promise,
                            "HOOKEUICC_MISSING_ACTIVATION_CODE",
                            "giffgaff did not provide an eSIM activation code. No profile was installed.",
                        )
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            activationCodeConsumer.accept(activationCode)
                        }
                        rejectPromise.invoke(
                            promise,
                            "HOOKEUICC_MANUAL_INSTALL_REQUIRED",
                            "eSIM activation code received for export. Install it with your physical eUICC card's LPA; " +
                                "the Android eSIM service did not install a profile.",
                        )
                        module.log(Log.INFO, TAG, "giffgaff activation code received for manual LPA installation")
                    }
                    null
                }

            module.log(Log.INFO, TAG, "giffgaff hooks installed: ${managerClass.name}")
        } catch (exception: ReflectiveOperationException) {
            // App 更新可能改变原生接口；记录具体失败，便于按新 APK 修复。
            module.log(Log.ERROR, TAG, "giffgaff native bridge signature changed", exception)
        }
    }

    private fun findManagerClass(classLoader: ClassLoader): Class<*>? {
        for (className in MANAGER_CLASSES) {
            try {
                return classLoader.loadClass(className)
            } catch (_: ClassNotFoundException) {
                // 旧版使用上游库包名，新版使用 giffgaff 自己的包名。
            }
        }
        module.log(Log.ERROR, TAG, "giffgaff SimCardsManagerModule not found; inspect this App version")
        return null
    }

    companion object {
        const val PACKAGE_NAME = "com.giffgaffmobile.controller"
        private const val TAG = "HookEUICC"
        private val MANAGER_CLASSES = listOf(
            "$PACKAGE_NAME.simmanager.SimCardsManagerModule",
            "com.reactnativesimcardsmanager.SimCardsManagerModule",
        )
    }
}
